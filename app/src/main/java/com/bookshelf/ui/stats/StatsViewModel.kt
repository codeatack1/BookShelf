package com.bookshelf.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import com.bookshelf.core.util.fastCountNot
import com.bookshelf.presentation.more.stats.StatsScreenState
import com.bookshelf.presentation.more.stats.data.StatsData
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.source.model.STextbook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.bookshelf.core.common.util.lang.launchIO
import com.bookshelf.domain.history.interactor.GetTotalReadDuration
import com.bookshelf.domain.library.model.LibraryTextbook
import com.bookshelf.domain.library.service.LibraryPreferences
import com.bookshelf.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import com.bookshelf.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import com.bookshelf.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import com.bookshelf.domain.textbook.interactor.GetLibraryTextbook
import com.bookshelf.domain.track.interactor.GetTracks
import com.bookshelf.domain.track.model.Track
import com.bookshelf.source.local.isLocal

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class StatsViewModel(
    private val downloadManager: DownloadManager,
    private val getLibraryTextbook: GetLibraryTextbook,
    private val getTotalReadDuration: GetTotalReadDuration,
    private val getTracks: GetTracks,
    private val preferences: LibraryPreferences,
    private val trackerManager: TrackerManager,
) : ViewModel() {

    val state: StateFlow<StatsScreenState>
        field = MutableStateFlow<StatsScreenState>(StatsScreenState.Loading)

    private val loggedInTrackers by lazy { trackerManager.loggedInTrackers() }

    init {
        viewModelScope.launchIO {
            val libraryManga = getLibraryTextbook.await()

            val distinctLibraryTextbook = libraryManga.fastDistinctBy { it.id }

            val mangaTrackMap = getMangaTrackMap(distinctLibraryTextbook)
            val scoredMangaTrackerMap = getScoredMangaTrackMap(mangaTrackMap)

            val meanScore = getTrackMeanScore(scoredMangaTrackerMap)

            val overviewStatData = StatsData.Overview(
                libraryMangaCount = distinctLibraryTextbook.size,
                completedMangaCount = distinctLibraryTextbook.count {
                    it.textbook.status.toInt() == STextbook.COMPLETED && it.unreadCount == 0L
                },
                totalReadDuration = getTotalReadDuration.await(),
            )

            val titlesStatData = StatsData.Titles(
                globalUpdateItemCount = getGlobalUpdateItemCount(libraryManga),
                startedMangaCount = distinctLibraryTextbook.count { it.hasStarted },
                localMangaCount = distinctLibraryTextbook.count { it.textbook.isLocal() },
            )

            val chaptersStatData = StatsData.Chapters(
                totalChapterCount = distinctLibraryTextbook.sumOf { it.totalChapters }.toInt(),
                readChapterCount = distinctLibraryTextbook.sumOf { it.readCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = mangaTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedInTrackers.size,
            )

            state.update {
                StatsScreenState.Success(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    chapters = chaptersStatData,
                    trackers = trackersStatData,
                )
            }
        }
    }

    private fun getGlobalUpdateItemCount(libraryManga: List<LibraryTextbook>): Int {
        val includedCategories = preferences.updateCategories.get().map { it.toLong() }
        val excludedCategories = preferences.updateCategoriesExclude.get().map { it.toLong() }
        val updateRestrictions = preferences.autoUpdateTextbookRestrictions.get()

        return libraryManga.filter {
            val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
            val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
            included && !excluded
        }
            .fastCountNot {
                (MANGA_NON_COMPLETED in updateRestrictions && it.textbook.status.toInt() == STextbook.COMPLETED) ||
                    (MANGA_HAS_UNREAD in updateRestrictions && it.unreadCount != 0L) ||
                    (MANGA_NON_READ in updateRestrictions && it.totalChapters > 0 && !it.hasStarted)
            }
    }

    private suspend fun getMangaTrackMap(libraryManga: List<LibraryTextbook>): Map<Long, List<Track>> {
        val loggedInTrackerIds = loggedInTrackers.map { it.id }.toHashSet()
        return libraryManga.associate { manga ->
            val tracks = getTracks.await(manga.id)
                .fastFilter { it.trackerId in loggedInTrackerIds }

            manga.id to tracks
        }
    }

    private fun getScoredMangaTrackMap(mangaTrackMap: Map<Long, List<Track>>): Map<Long, List<Track>> {
        return mangaTrackMap.mapNotNull { (textbookId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            textbookId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredMangaTrackMap: Map<Long, List<Track>>): Double {
        return scoredMangaTrackMap
            .map { (_, tracks) ->
                tracks.map(::get10PointScore).average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: Track): Double {
        val service = trackerManager.get(track.trackerId)!!
        return service.get10PointScore(track)
    }
}
