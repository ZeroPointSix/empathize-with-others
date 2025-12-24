package com.empathy.ai.data.util

import android.util.Log
import com.empathy.ai.domain.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android平台的Logger实现
 *
 * 使用android.util.Log实现Logger接口
 */
@Singleton
class AndroidLogger @Inject constructor() : Logger {

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
}
