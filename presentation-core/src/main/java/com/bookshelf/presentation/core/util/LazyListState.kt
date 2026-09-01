package com.bookshelf.presentation.core.util

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.shouldExpandFAB(): Boolean = lastScrolledBackward || !canScrollForward || !canScrollBackward
