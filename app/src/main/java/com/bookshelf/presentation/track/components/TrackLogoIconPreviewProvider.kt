package com.bookshelf.presentation.track.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.bookshelf.com.bookshelf.data.track.Tracker
import com.bookshelf.test.DummyTracker

internal class TrackLogoIconPreviewProvider : PreviewParameterProvider<Tracker> {

    override val values: Sequence<Tracker>
        get() = sequenceOf(
            DummyTracker(
                id = 1L,
                name = "Dummy Tracker",
            ),
        )
}
