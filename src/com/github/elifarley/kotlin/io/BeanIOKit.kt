package com.orgecc.util.io

import org.beanio.BeanReader
import org.beanio.BeanWriter
import org.beanio.StreamFactory
import java.io.Reader
import java.io.Writer
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Created by elifarley on 24/08/16.
 */

fun useNewBeanIOReaderPreKotlin1dot1(streamName: String, filePath: Path, block: (IOWithDigest.MDReader) -> Unit) =
        BeanIOKit.useNewReader(streamName, filePath, block)
fun useNewBeanIOWriterPreKotlin1dot1(streamName: String, filePath: Path, block: (IOWithDigest.MDWriter) -> Unit) =
        BeanIOKit.useNewWriter(streamName, filePath, block)

object BeanIOKit {

    private val streamFactory: StreamFactory by lazy {
        StreamFactory.newInstance().apply {
            load(Unit::class.java.getResourceAsStream("/bean-io-config.xml"))
        }
    }

    fun useNewReader(streamName: String, reader: Reader, digest: MessageDigest, block: (IOWithDigest.MDReader) -> Unit) =
            streamFactory.createReader(streamName, reader).useAsCloseable(digest, block)

    fun useNewWriter(streamName: String, writer: Writer, digest: MessageDigest, block: (IOWithDigest.MDWriter) -> Unit) =
            streamFactory.createWriter(streamName, writer).useAsCloseable(digest, block)

    fun useNewReader(streamName: String, filePath: Path, block: (IOWithDigest.MDReader) -> Unit) {
        val mdStream = filePath.toFile().digestInputStream()
        mdStream.bufferedReader().use {
            useNewReader(streamName, it, mdStream.messageDigest, block)

        }
    }

    fun useNewWriter(streamName: String, filePath: Path, block: (IOWithDigest.MDWriter) -> Unit) {
        val mdStream = filePath.toFile().digestOutputStream()
        mdStream.bufferedWriter().use {
            useNewWriter(streamName, it, mdStream.messageDigest, block)
        }
    }

}

private class CloseableBeanReaderWithDigest(b: BeanReader, md: MessageDigest)
    : BeanReader by b, IOWithDigest.MDReader(md)
private class CloseableBeanWriterWithDigest(b: BeanWriter, md: MessageDigest)
    : BeanWriter by b, IOWithDigest.MDWriter(md)

private inline fun BeanReader.useAsCloseable(digest: MessageDigest, block: (IOWithDigest.MDReader) -> Unit) =
        CloseableBeanReaderWithDigest(this, digest).use(block)
private inline fun BeanWriter.useAsCloseable(digest: MessageDigest, block: (IOWithDigest.MDWriter) -> Unit) =
        CloseableBeanWriterWithDigest(this, digest).use(block)
