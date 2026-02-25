package eu.kanade.tachiyomi.util

import rx.Observable
import com.shinku.reader.util.awaitSingle as awaitSingleNew

suspend fun <T> Observable<T>.awaitSingle(): T = awaitSingleNew()
