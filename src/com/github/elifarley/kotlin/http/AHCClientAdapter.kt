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

    private fun resultHandler(resultType: RESULT_TYPE, callback: (Any?) -> Unit) = object : AsyncCompletionHandler<Response>() {

        override fun onThrowable(t: Throwable) {
            t.printStackTrace() // TODO
        }

        override fun onCompleted(response: Response) = response.also {
                    when (resultType) {
                        RESULT_TYPE.BYTE_ARRAY -> callback(response.responseBodyAsBytes)
                        RESULT_TYPE.STRING -> callback(response.responseBody)
                        RESULT_TYPE.JSON -> response.responseBodyAsBytes?.let(JsonIterator::deserialize)
                                .let(callback)
                        else -> throw IllegalArgumentException("Invalid function type: $resultType")
                    }

        }

    }

    override fun execute(request: BoundRequestBuilder, resultType: RESULT_TYPE, callback: (Any?) -> Unit): Unit {
        request.execute(resultHandler(resultType, callback))
    }

}
