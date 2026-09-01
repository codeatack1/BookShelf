package com.bookshelf.domain.source.model

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.bookshelf.app.di.appGraph
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.domain.source.model.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

suspend fun Source.icon(): ImageBitmap? = withIOContext {
    Injekt.get<Context>().appGraph.extensionManager.getAppIconForSource(id)
        ?.toBitmap()
        ?.asImageBitmap()
}
