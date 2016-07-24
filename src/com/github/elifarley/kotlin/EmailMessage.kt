package com.github.elifarley.kotlin

import java.io.Serializable

data class EmailMessage(val toAddress: String, val subject: String, val body: String, var myMutableField: String?) : Serializable
