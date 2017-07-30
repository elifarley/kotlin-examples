
package com.orgecc.lib.http

import dao.JsonAny
import kotlin.coroutines.experimental.suspendCoroutine

enum class HttpMethod {
    GET, POST
}

class HttpReqInfo(
        val method: HttpMethod,
        val url: String,
        val headers: Headers = Headers(emptyArray()),
        val postData: String? = null
)

enum class RESULT_TYPE {
    STREAM, BYTE_BUFFER, BYTE_ARRAY, STRING, JSON
}

abstract class HttpClientAdapter<out HC, ReqBase> {

    abstract val httpclient: HC
    abstract val toSimpleHeaders: ReqBase.() -> Headers

    abstract fun toRequestObject(reqInfo: HttpReqInfo): ReqBase
    protected abstract fun execute(request: ReqBase, resultType: RESULT_TYPE, callback: (Any?) -> Unit): Unit

    override fun toString(): String {
        return "${HttpClientAdapter::class.simpleName}"
    }

    suspend fun executeAsyncToByteArray(reqInfo: HttpReqInfo): ByteArray = executeAsync(reqInfo, RESULT_TYPE.BYTE_ARRAY) as ByteArray
    suspend fun executeAsyncToString(reqInfo: HttpReqInfo): String = executeAsync(reqInfo, RESULT_TYPE.STRING) as String
    suspend fun executeAsyncToJson(reqInfo: HttpReqInfo): JsonAny = executeAsync(reqInfo, RESULT_TYPE.JSON) as JsonAny

    // See https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#wrapping-callbacks
    private suspend fun executeAsync(reqInfo: HttpReqInfo, resultType: RESULT_TYPE): Any? {
        val request = toRequestObject(reqInfo)
        return suspendCoroutine { cont ->
            execute(request, resultType) { cont.resume(it) }
        }
    }

}
