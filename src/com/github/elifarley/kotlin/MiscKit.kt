package com.orgecc.util

import org.apache.commons.lang3.time.StopWatch

/**
 * Created by elifarley on 22/11/16.
 */
fun StopWatch.stopIfRunning(): StopWatch { if (!isStopped) stop(); return this }


private val HEX_CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/**
 *  Returns the string of two characters representing the HEX value of the byte.
 */
fun Byte.toHexString() : String {
    val i = this.toInt()
    val char2 = HEX_CHARS[i and 0x0f]
    val char1 = HEX_CHARS[i shr 4 and 0x0f]
    return "$char1$char2"
}

fun ByteArray.toHexString() : String {
    val builder = StringBuilder(this.size * 2)
    for (b in this) {
        builder.append(b.toHexString())
    }
    return builder.toString()
}
