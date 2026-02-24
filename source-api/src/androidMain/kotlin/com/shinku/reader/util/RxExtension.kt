package com.shinku.reader.util

import rx.Observable
import com.shinku.reader.core.common.util.lang.awaitSingle

actual suspend fun <T> Observable<T>.awaitSingle(): T = awaitSingle()
