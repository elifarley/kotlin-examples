package br.com.m4u

import java.io.Serializable

class EmailMessage(val toAddress: String, val subject: String, val body: String, var myMutableField: String?) : Serializable
