package com.bookshelf.presentation.util

import android.content.Context
import com.bookshelf.network.HttpException
import com.bookshelf.util.system.isOnline
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.data.source.NoResultsException
import com.bookshelf.domain.source.model.SourceNotInstalledException
import com.bookshelf.i18n.MR
import java.net.UnknownHostException

context(context: Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return context.stringResource(MR.strings.exception_http, code)
            is UnknownHostException -> {
                return if (!context.isOnline()) {
                    context.stringResource(MR.strings.exception_offline)
                } else {
                    context.stringResource(MR.strings.exception_unknown_host, message ?: "")
                }
            }

            is NoResultsException -> return context.stringResource(MR.strings.no_results_found)
            is SourceNotInstalledException -> return context.stringResource(MR.strings.loader_not_implemented_error)
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
