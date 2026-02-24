package com.shinku.reader.util

import rx.Observable

expect suspend fun <T> Observable<T>.awaitSingle(): T
