package com.orgecc.io

import org.beanio.BeanReader
import org.beanio.BeanWriter
import org.beanio.StreamFactory
import java.io.Closeable
import java.nio.file.Path

/**
 * Created by elifarley on 24/08/16.
 */
object BeanIOKit {

    private val streamFactory: StreamFactory by lazy {
        StreamFactory.newInstance().apply {
            load(Unit::class.java.getResourceAsStream("/bean-io-config.xml"))
        }
    }

    fun useNewReader(streamName: String, filePath: Path, block: (BeanReader) -> Unit) =
            streamFactory.createReader(streamName, filePath.toFile()).useAsCloseable(block)

    fun useNewWriter(streamName: String, filePath: Path, block: (BeanWriter) -> Unit) =
            streamFactory.createWriter(streamName, filePath.toFile()).useAsCloseable(block)

}

private class CloseableBeanReader(val b: BeanReader) : BeanReader by b, Closeable {
    override final inline fun close() = b.close()
}

private inline fun BeanReader.useAsCloseable(block: (BeanReader) -> Unit) =
        CloseableBeanReader(this).use(block)

private class CloseableBeanWriter(val b: BeanWriter) : BeanWriter by b, Closeable {
    override final inline fun close() = b.close()
}

private inline fun BeanWriter.useAsCloseable(block: (BeanWriter) -> Unit) =
        CloseableBeanWriter(this).use(block)
