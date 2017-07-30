package com.orgecc.lib.http

import com.jsoniter.JsonIterator
import okhttp3.*
import java.io.IOException

class OkHttpClientAdapter : HttpClientAdapter<OkHttpClient, Request>() {

    override val toSimpleHeaders: Request.() -> lib.http.Headers = {
        this.headers().toMultimap().flatMap { (key, value) -> value.map { Pair(key!!, it!!) } }.let {
            Headers(it)
        }
    }

    override val httpclient = OkHttpClient()

    override fun toRequestObject(reqInfo: HttpReqInfo): Request = when (reqInfo.method) {

        HttpMethod.GET -> Request.Builder()
                .url(reqInfo.url)

        HttpMethod.POST -> Request.Builder()
                .url(reqInfo.url)
                .apply {
            reqInfo.postData?.let {
                this.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), it))
            }
        }

    }.apply {
        reqInfo.headers.forEach {
            this.addHeader(it.first, it.second)
        }

    }.build()

    private fun resultHandler(resultType: RESULT_TYPE, callback: (Any?) -> Unit) = object : Callback {

        override fun onFailure(call: Call, exception: IOException?) {
            exception?.printStackTrace() // TODO
        }

        override fun onResponse(call: Call?, response: Response) = when (resultType) {
            RESULT_TYPE.BYTE_ARRAY -> callback(response.body()?.bytes())
            RESULT_TYPE.STRING -> callback(response.body()?.string())
            RESULT_TYPE.JSON -> (response.body()?.bytes()?.let { JsonIterator.deserialize(it) })
                    .let { callback(it) }
            else -> throw IllegalArgumentException("Invalid function type: $resultType")
        }

            }

    // TODO Use Builders.blocking or Deferred.blockingAsync - https://github.com/Kotlin/kotlinx.coroutines/issues/79
    override fun execute(request: Request, resultType: RESULT_TYPE, callback: (Any?) -> Unit): Unit {
        httpclient.newCall(request).enqueue(resultHandler(resultType, callback))
    }

}
