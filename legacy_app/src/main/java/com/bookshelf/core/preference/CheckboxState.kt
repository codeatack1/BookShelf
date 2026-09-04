package com.bookshelf.core.preference

import androidx.compose.ui.state.ToggleableState
import com.bookshelf.core.common.preference.CheckboxState

fun <T> CheckboxState.TriState<T>.asToggleableState() = when (this) {
    is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
    is CheckboxState.TriState.Include -> ToggleableState.On
    is CheckboxState.TriState.None -> ToggleableState.Off
}
