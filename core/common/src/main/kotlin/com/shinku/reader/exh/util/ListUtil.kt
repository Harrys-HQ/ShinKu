package com.shinku.reader.exh.util

fun <C : Collection<R>, R> C.nullIfEmpty() = ifEmpty { null }
