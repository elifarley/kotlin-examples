package com.orgecc.util.io

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * Created by elifarley on 02/09/16.
 */

fun Path.createParentDirectories(vararg attrs: FileAttribute<*>) = this.apply {
    Files.createDirectories(this.parent, *attrs)
}

fun File.digestInputStream(digest: MessageDigest = MessageDigest.getInstance("MD5")) =
        DigestInputStream(this.inputStream(), digest)

fun File.digestOutputStream(digest: MessageDigest = MessageDigest.getInstance("MD5")) =
        DigestOutputStream(this.outputStream(), digest)

