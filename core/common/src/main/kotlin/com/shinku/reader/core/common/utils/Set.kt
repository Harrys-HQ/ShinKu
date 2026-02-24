package com.shinku.reader.core.common.utils

fun <T> Set<T>.mutate(action: (MutableSet<T>) -> Unit): Set<T> {
    return toMutableSet().apply(action)
}
