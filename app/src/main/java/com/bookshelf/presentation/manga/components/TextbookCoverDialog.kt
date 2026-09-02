package com.bookshelf.presentation.manga.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updatePadding
import ca.mpreg.webgpuviewer.renderer.Image
import ca.mpreg.webgpuviewer.viewer.ImagePage
import ca.mpreg.webgpuviewer.viewer.ImageViewer
import ca.mpreg.webgpuviewer.viewer.ImageViewerState
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.bookshelf.app.di.appGraph
import com.bookshelf.data.coil.ImageDecoder
import com.bookshelf.data.coil.newDecoder
import com.bookshelf.domain.textbook.model.Textbook
import com.bookshelf.i18n.MR
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.Close
import com.bookshelf.icons.materialsymbols.rounded.Edit
import com.bookshelf.icons.materialsymbols.rounded.Save
import com.bookshelf.icons.materialsymbols.rounded.Share
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.components.AppBarActions
import com.bookshelf.presentation.components.DropdownMenu
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.util.clickableNoIndication
import com.bookshelf.presentation.manga.EditCoverAction
import com.bookshelf.ui.reader.viewer.ReaderPageImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Composable
fun TextbookCoverDialog(
    manga: Textbook,
    isCustomCover: Boolean,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val useNewRenderer = LocalContext.current.appGraph.basePreferences.highQualityRenderer.get()
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .navigationBarsPadding(),
                ) {
                    ActionsPill {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = stringResource(MR.strings.action_close),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ActionsPill {
                        AppBarActions(
                            actions = listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_share),
                                    icon = MaterialSymbols.Rounded.Share,
                                    onClick = onShareClick,
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_save),
                                    icon = MaterialSymbols.Rounded.Save,
                                    onClick = onSaveClick,
                                ),
                            ),
                        )
                        if (onEditClick != null) {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        if (isCustomCover) {
                                            expanded = true
                                        } else {
                                            onEditClick(EditCoverAction.EDIT)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.Rounded.Edit,
                                        contentDescription = stringResource(MR.strings.action_edit_cover),
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    offset = DpOffset(8.dp, 0.dp),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_edit)) },
                                        onClick = {
                                            onEditClick(EditCoverAction.EDIT)
                                            expanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_delete)) },
                                        onClick = {
                                            onEditClick(EditCoverAction.DELETE)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { contentPadding ->
            if (useNewRenderer) {
                val state = ImageViewerState()

                ImageRequest.Builder(view.context)
                    .data(manga)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .newDecoder(true)
                    .target { result ->
                        val res = (result as ImageDecoder.DecodeResultImage).res
                        val page = runBlocking(Dispatchers.Default) {
                            ImagePage.ImageSingle(
                                Image(
                                    res.image,
                                    res.width,
                                    res.height,
                                    createMipMaps = true,
                                    backgroundColor = 0,
                                ),
                            )
                        }
                        state.apply {
                            fetchPage = { index ->
                                if (index == 0) page else null
                            }
                            invalidate()
                        }
                    }
                    .build()
                    .let(view.context.imageLoader::enqueue)

                ImageViewer(state = state)
                return@Scaffold
            }

            val statusBarPaddingPx = with(LocalDensity.current) { contentPadding.calculateTopPadding().roundToPx() }
            val bottomPaddingPx = with(LocalDensity.current) { contentPadding.calculateBottomPadding().roundToPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoIndication(onClick = onDismissRequest),
            ) {
                AndroidView(
                    factory = {
                        ReaderPageImageView(it).apply {
                            onViewClicked = onDismissRequest
                            clipToPadding = false
                            clipChildren = false
                        }
                    },
                    update = { view ->
                        val request = ImageRequest.Builder(view.context)
                            .data(manga)
                            .size(Size.ORIGINAL)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .target { image ->
                                val drawable = image.asDrawable(view.context.resources)
                                // Copy bitmap in case it came from memory cache
                                // Because SSIV needs to thoroughly read the image
                                val copy = (drawable as? BitmapDrawable)
                                    ?.bitmap
                                    ?.copy(Bitmap.Config.HARDWARE, false)
                                    ?.toDrawable(view.context.resources)
                                    ?: drawable
                                view.setImage(copy, ReaderPageImageView.Config(zoomDuration = 500))
                            }
                            .build()
                        view.context.imageLoader.enqueue(request)

                        view.updatePadding(top = statusBarPaddingPx, bottom = bottomPaddingPx)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ActionsPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        content()
    }
}
