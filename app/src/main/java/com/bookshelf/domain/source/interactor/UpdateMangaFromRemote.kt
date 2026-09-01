package com.bookshelf.domain.source.interactor

import dev.zacsweers.metro.Inject
import com.bookshelf.domain.chapter.interactor.SyncChaptersWithSource
import com.bookshelf.domain.chapter.model.toSChapter
import com.bookshelf.domain.manga.model.hasCustomCover
import com.bookshelf.domain.manga.model.toSManga
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.source.Source
import com.bookshelf.source.model.SManga
import logcat.LogPriority
import com.bookshelf.domain.source.models.RemoteMangaUpdate
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.chapter.model.Chapter
import com.bookshelf.domain.chapter.repository.ChapterRepository
import com.bookshelf.domain.library.service.LibraryPreferences
import com.bookshelf.domain.manga.model.Manga
import com.bookshelf.domain.manga.model.MangaUpdate
import com.bookshelf.domain.manga.repository.MangaRepository
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.source.local.isLocal
import kotlin.time.Clock

@Inject
class UpdateMangaFromRemote(
    private val sourceManager: SourceManager,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val syncChaptersWithSource: SyncChaptersWithSource,
    private val coverCache: CoverCache,
    private val libraryPreferences: LibraryPreferences,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteMangaUpdate> {
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
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): Result<RemoteMangaUpdate> {
        return try {
            val chapters = chapterRepository.getChapterByMangaId(manga.id)
                .sortedBy { it.sourceOrder }
            val update = withIOContext {
                source.getMangaUpdate(
                    manga = manga.toSManga(),
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
            val updatedManga = mangaRepository.getMangaById(manga.id)

            Result.success(RemoteMangaUpdate(manga = updatedManga, newChapters = newChapters))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localManga: Manga,
        remoteManga: SManga,
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
            MangaUpdate(
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
