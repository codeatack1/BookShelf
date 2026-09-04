package com.bookshelf.app.di

import android.content.Context
import com.bookshelf.core.metro.metroGraph

val Context.appGraph get() = metroGraph<AppGraph>()
