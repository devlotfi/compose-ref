package com.devlotfi.composeref

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class Ref<T>(var current: T)

@Composable
fun <T> rememberRef(initialValue: T): Ref<T> {
    val ref = remember { Ref<T>(initialValue) }
    return ref
}