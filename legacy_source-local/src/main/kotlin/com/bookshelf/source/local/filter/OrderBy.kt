package com.bookshelf.source.local.filter

import android.content.Context
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.i18n.MR
import com.bookshelf.source.model.Filter

sealed class OrderBy(context: Context, selection: Selection) : Filter.Sort(
    context.stringResource(MR.strings.local_filter_order_by),
    arrayOf(context.stringResource(MR.strings.title), context.stringResource(MR.strings.date)),
    selection,
) {
    class Popular(context: Context) : OrderBy(context, Selection(0, true))
    class Latest(context: Context) : OrderBy(context, Selection(1, false))
}
