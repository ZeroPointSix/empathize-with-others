package com.empathy.ai.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMessageMapperTest {


    @Test
    fun map_unknownHostException_returnsNetworkError() {
        val error = UnknownHostException()
        val result = ErrorMessageMapper.map(error)
        
        assertEquals("无法连接服务器", result.title)
        assertEquals("刷新", result.actionLabel)
    }

    @Test
    fun map_socketTimeoutException_returnsTimeoutError() {
        val error = SocketTimeoutException()
        val result = ErrorMessageMapper.map(error)
        
        assertEquals("连接超时", result.title)
        assertEquals("重试", result.actionLabel)
    }

    @Test
    fun map_genericException_returnsUnknownError() {
        val error = RuntimeException("Something went wrong")
        val result = ErrorMessageMapper.map(error)
        
        assertEquals("发生错误", result.title)
        assertEquals("重试", result.actionLabel)
    }
}
