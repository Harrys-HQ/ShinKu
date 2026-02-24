package com.shinku.reader.presentation.track.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.shinku.reader.data.track.Tracker
import com.shinku.reader.test.DummyTracker

internal class TrackLogoIconPreviewProvider : PreviewParameterProvider<Tracker> {

    override val values: Sequence<Tracker>
        get() = sequenceOf(
            DummyTracker(
                id = 1L,
                name = "Dummy Tracker",
            ),
        )
}
