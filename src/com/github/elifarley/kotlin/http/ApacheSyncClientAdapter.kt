package com.orgecc.lib.http

import com.jsoniter.JsonIterator
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.config.SocketConfig
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils

class ApacheSyncClientAdapter : HttpClientAdapter<CloseableHttpClient, HttpRequestBase>() {

    override val toSimpleHeaders: HttpRequestBase.() -> Headers = {
        allHeaders.map { Pair(it.name, it.value) }.let {
            Headers(it)
        }
    }

    override val httpclient = HttpClients.custom()
            .setConnectionManager(PoolingHttpClientConnectionManager().apply {
                maxTotal = 100
                defaultMaxPerRoute = 10
                defaultSocketConfig = SocketConfig.custom()
                        .setTcpNoDelay(true)
                        .build()
            })
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setExpectContinueEnabled(true)
                            .build()
            )
            .build()!!

    override fun toRequestObject(reqInfo: HttpReqInfo): HttpRequestBase = when (reqInfo.method) {
        HttpMethod.GET -> HttpGet(reqInfo.url)
        HttpMethod.POST -> HttpPost(reqInfo.url).apply {
            reqInfo.postData?.let {
                this.entity = StringEntity(it)
            }
        }
    }.apply {
        reqInfo.headers.forEach {
            this.setHeader(it.first, it.second)
        }

    }

    // TODO Use Builders.blocking or Deferred.blockingAsync - https://github.com/Kotlin/kotlinx.coroutines/issues/79
    override fun execute(request: HttpRequestBase, resultType: RESULT_TYPE, callback: (Any?) -> Unit): Unit {

        httpclient.execute(request, BasicHttpContext()).use { response ->

            when (resultType) {

                RESULT_TYPE.BYTE_ARRAY -> response.entity?.let {
                    EntityUtils.toByteArray(it)
                } ?: ""

                RESULT_TYPE.STRING -> response.entity?.let {
                    EntityUtils.toString(it)
                } ?: ""

                RESULT_TYPE.JSON ->
                    response.entity?.let {
                        JsonIterator.deserialize(EntityUtils.toByteArray(it))
                    } ?: JsonIterator.deserialize("{}")

                else -> throw IllegalArgumentException("Invalid function type: $resultType")

            }.let {
                callback(it)
            }

        }

    }

}
