package com.orgecc.util.io

import java.io.EOFException
import java.security.DigestInputStream

/**
 * Created by elifarley on 05/12/16.
 */
abstract class MDReaderInputStreamAdapter<out D>(protected val mdi: DigestInputStream) : IOWithDigest.MDReader(mdi.messageDigest) {

    private enum class State { HEADER, DETAIL, TRAILER, DONE}

    private var state: State = State.HEADER

    protected abstract val header: Map<String, Any>
    protected abstract val detailIterable: Iterable<D>
    protected abstract val trailer: Int

    private lateinit var detailIterator: Iterator<D>

    override fun read(): Any? = when (state) {

        State.HEADER -> { state = State.DETAIL; detailIterator = detailIterable.iterator(); header }

        State.DETAIL -> if (detailIterator.hasNext()) detailIterator.next()
                        else { state = State.TRAILER; trailer }

        State.TRAILER -> { state = State.DONE; null }

        State.DONE -> throw EOFException()
    }

    override fun close() {
        mdi.close()
    }

}

