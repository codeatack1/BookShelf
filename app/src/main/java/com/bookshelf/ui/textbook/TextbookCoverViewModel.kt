package com.bookshelf.ui.textbook

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.core.common.util.lang.launchIO
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.lang.withUIContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.cache.CoverCache
import com.bookshelf.data.saver.Image
import com.bookshelf.data.saver.ImageSaver
import com.bookshelf.data.saver.Location
import com.bookshelf.domain.textbook.interactor.GetTextbook
import com.bookshelf.domain.textbook.interactor.UpdateTextbook
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.i18n.MR
import com.bookshelf.source.local.image.LocalCoverManager
import com.bookshelf.util.editCover
import com.bookshelf.util.system.getBitmapOrNull
import com.bookshelf.util.system.toShareIntent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority

@AssistedInject
class TextbookCoverViewModel(
    @Assisted private val textbookId: Long,
    private val getManga: GetTextbook,
    private val imageSaver: ImageSaver,
    private val coverCache: CoverCache,
    private val updateManga: UpdateTextbook,
    private val coverManager: LocalCoverManager,
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(textbookId: Long): TextbookCoverViewModel
    }

    val state: StateFlow<Textbook?> = getManga.subscribe(textbookId)
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun saveCover(context: Context) {
        viewModelScope.launch {
            try {
                saveCoverInternal(context, temp = false)
                snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.cover_saved),
                    withDismissAction = true,
                )
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.error_saving_cover),
                    withDismissAction = true,
                )
            }
        }
    }

    fun shareCover(context: Context) {
        viewModelScope.launch {
            try {
                val uri = saveCoverInternal(context, temp = true) ?: return@launch
                withUIContext {
                    context.startActivity(uri.toShareIntent(context))
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.error_sharing_cover),
                    withDismissAction = true,
                )
            }
        }
    }

    /**
     * Save manga cover Bitmap to picture or temporary share directory.
     *
     * @param context The context for building and executing the ImageRequest
     * @return the uri to saved file
     */
    private suspend fun saveCoverInternal(context: Context, temp: Boolean): Uri? {
        val manga = state.value ?: return null
        val req = ImageRequest.Builder(context)
            .data(manga)
            .size(Size.ORIGINAL)
            .build()

        return withIOContext {
            val result = context.imageLoader.execute(req).image?.asDrawable(context.resources)

            // TODO: Handle animated cover
            val bitmap = result?.getBitmapOrNull() ?: return@withIOContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = manga.title,
                    location = if (temp) Location.Cache else Location.Pictures.create(),
                ),
            )
        }
    }

    /**
     * Update cover with local file.
     *
     * @param context Context.
     * @param data uri of the cover resource.
     */
    fun editCover(context: Context, data: Uri) {
        val manga = state.value ?: return
        viewModelScope.launchIO {
            context.contentResolver.openInputStream(data)?.use {
                try {
                    manga.editCover(coverManager, it, updateManga, coverCache)
                    notifyCoverUpdated(context)
                } catch (e: Exception) {
                    notifyFailedCoverUpdate(context, e)
                }
            }
        }
    }

    fun deleteCustomCover(context: Context) {
        viewModelScope.launchIO {
            try {
                coverCache.deleteCustomCover(textbookId)
                updateManga.awaitUpdateCoverLastModified(textbookId)
                notifyCoverUpdated(context)
            } catch (e: Exception) {
                notifyFailedCoverUpdate(context, e)
            }
        }
    }

    private fun notifyCoverUpdated(context: Context) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.cover_updated),
                withDismissAction = true,
            )
        }
    }

    private fun notifyFailedCoverUpdate(context: Context, e: Throwable) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.notification_cover_update_failed),
                withDismissAction = true,
            )
            logcat(LogPriority.ERROR, e)
        }
    }
}
