package com.orgecc.util

import org.apache.commons.lang3.time.StopWatch

/**
 * Created by elifarley on 22/11/16.
 */
fun StopWatch.stopIfRunning(): StopWatch { if (!isStopped) stop(); return this }


private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

/**
 *  Returns the string of two characters representing the HEX value of the byte.
 */
fun Byte.toHexString() : String {
    val i = this.toInt()
    val char2 = HEX_CHARS[i and 0x0f]
    val char1 = HEX_CHARS[i ushr 4]
    return "$char1$char2"
}

fun ByteArray.toHexString() : String {
    val builder = StringBuilder(this.size * 2)
    for (b in this) {
        builder.append(b.toHexString())
    }
    return builder.toString()
}


fun ByteArray.toHex(): String {
    val result = StringBuffer(this.size * 2)

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}
