package com.slidingpuzzle.app.di

import android.app.Application
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class DependencyProvider(
    private val application: Application
) {
   val backgroundExecutor: Executor by lazy {
       Executors.newFixedThreadPool(2)
   }

    val mainExecutor: Executor by lazy {
        val handler = Handler(Looper.getMainLooper())
        Executor { handler.post(it) }
    }
}