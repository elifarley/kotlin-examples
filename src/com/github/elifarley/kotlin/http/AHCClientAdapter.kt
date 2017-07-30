package com.orgecc.lib.http

import com.jsoniter.JsonIterator
import org.asynchttpclient.*
import java.util.Map

class AHCClientAdapter : HttpClientAdapter<AsyncHttpClient, BoundRequestBuilder>() {


    override val toSimpleHeaders: BoundRequestBuilder.() -> Headers = {
        val field = RequestBuilderBase::class.java.getDeclaredField("headers").apply { this.isAccessible = true }

        (field.get(this) as Iterable<Map.Entry<String, String>>).map { Pair(it.key!!, it.value!!) }.let {
            Headers(it)
        }

    }

    override val httpclient = DefaultAsyncHttpClient()

    override fun toRequestObject(reqInfo: HttpReqInfo): BoundRequestBuilder = when (reqInfo.method) {
        HttpMethod.GET -> httpclient.prepareGet(reqInfo.url)
        HttpMethod.POST -> httpclient.preparePost(reqInfo.url).apply {
            reqInfo.postData?.let {
                this.setBody(it)
            }
        }
    }.apply {
        reqInfo.headers.forEach {
            this.setHeader(it.first, it.second)
        }

    }

    private fun resultHandler(resultType: RESULT_TYPE, callback: (Any?) -> Unit): AsyncCompletionHandler<Response> =
            object : AsyncCompletionHandler<Response>() {

                override fun onCompleted(response: Response): Response {
                    when (resultType) {
                        RESULT_TYPE.BYTE_ARRAY -> callback(response.responseBodyAsBytes)
                        RESULT_TYPE.STRING -> callback(response.responseBody)
                        RESULT_TYPE.JSON -> response.responseBodyAsBytes?.let { JsonIterator.deserialize(it) }
                                .let { callback(it) }
                        else -> throw IllegalArgumentException("Invalid function type: $resultType")
                    }
                    return response
                }

                override fun onThrowable(t: Throwable) {
                    t.printStackTrace() // TODO
                }

            }

    // TODO Use Builders.blocking or Deferred.blockingAsync - https://github.com/Kotlin/kotlinx.coroutines/issues/79
    override fun execute(request: BoundRequestBuilder, resultType: RESULT_TYPE, callback: (Any?) -> Unit): Unit {
        request.execute(resultHandler(resultType, callback))
    }

}
