package com.bookshelf.domain.source.interactor

import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.domain.chapter.interactor.SyncChaptersWithSource
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.model.toSChapter
import com.bookshelf.domain.chapter.repository.ChapterRepository
import com.bookshelf.domain.library.service.LibraryPreferences
import com.bookshelf.domain.source.models.RemoteTextbookUpdate
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.domain.textbook.model.TextbookUpdate
import com.bookshelf.domain.textbook.model.hasCustomCover
import com.bookshelf.domain.textbook.model.toSTextbook
import com.bookshelf.domain.textbook.repository.TextbookRepository
import com.bookshelf.source.Source
import com.bookshelf.source.local.isLocal
import com.bookshelf.source.model.STextbook
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import logcat.LogPriority

@Inject
class UpdateTextbookFromRemote(
    private val sourceManager: SourceManager,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: TextbookRepository,
    private val syncChaptersWithSource: SyncChaptersWithSource,
    private val coverCache: CoverCache,
    private val libraryPreferences: LibraryPreferences,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(
        manga: Textbook,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteTextbookUpdate> {
        val source = sourceManager.getOrStub(manga.source)
        return invoke(
            source = source,
            manga = manga,
            fetchDetails = fetchDetails,
            fetchChapters = fetchChapters,
            manualFetch = manualFetch,
        )
    }

    suspend operator fun invoke(
        source: Source,
        manga: Textbook,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteTextbookUpdate> {
        return try {
            val chapters = chapterRepository.getChapterByTextbookId(manga.id)
                .sortedBy { it.sourceOrder }
            val update = withIOContext {
                source.getTextbookUpdate(
                    manga = manga.toSTextbook(),
                    chapters = chapters.map(Chapter::toSChapter),
                    fetchDetails = fetchDetails,
                    fetchChapters = fetchChapters,
                )
            }
            awaitUpdateFromSource(manga, update.manga, manualFetch)
            val newChapters = syncChaptersWithSource.await(
                rawSourceChapters = update.chapters,
                manga = manga,
                source = source,
                manualFetch = manualFetch,
                fetchWindow = fetchWindow,
            )
            val updatedManga = mangaRepository.getTextbookById(manga.id)

            Result.success(RemoteTextbookUpdate(manga = updatedManga, newChapters = newChapters))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localManga: Textbook,
        remoteManga: STextbook,
        manualFetch: Boolean,
    ): Boolean {
        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite (or 'update titles' preference is enabled), set its title from source and update in db
        val title =
            if (remoteTitle.isNotEmpty() && (!localManga.favorite || libraryPreferences.updateMangaTitles.get())) {
                remoteTitle
            } else {
                null
            }

        val coverLastModified = when {
            // Never refresh covers if the url is empty to avoid "losing" existing covers
            remoteManga.thumbnail_url.isNullOrEmpty() -> null
            !manualFetch && localManga.thumbnailUrl == remoteManga.thumbnail_url -> null
            localManga.isLocal() -> Clock.System.now().toEpochMilliseconds()
            localManga.hasCustomCover(coverCache) -> {
                coverCache.deleteFromCache(localManga, false)
                null
            }
            else -> {
                coverCache.deleteFromCache(localManga, false)
                Clock.System.now().toEpochMilliseconds()
            }
        }

        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        val success = mangaRepository.update(
            TextbookUpdate(
                id = localManga.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
                memo = remoteManga.memo,
            ),
        )
        if (success && title != null) {
            downloadManager.renameManga(localManga, title)
        }
        return success
    }
}
