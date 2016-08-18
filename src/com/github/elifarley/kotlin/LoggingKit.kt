package com.orgecc.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.companionObject

/* Dependency:
		<dependency>
			<groupId>org.jetbrains.kotlin</groupId>
			<artifactId>kotlin-reflect</artifactId>
			<version>${kotlin.version}</version>
		</dependency>
*/

/**
 * See http://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
 */

// Return logger for Java class, if companion object fix the name
fun <T: Any> logger(forClass: Class<T>): Logger {
    return LoggerFactory.getLogger(unwrapCompanionClass(forClass).name)
}

// Return logger for Kotlin class
fun <T: Any> logger(forClass: KClass<T>): Logger {
    return logger(forClass.java)
}

// unwrap companion class to enclosing class given a Java Class
fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

// unwrap companion class to enclosing class given a Kotlin Class
fun <T: Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> {
    return unwrapCompanionClass(ofClass.java).kotlin
}

// return a lazy logger property delegate for enclosing class
fun <R : Any> R.lazyLogger(): Lazy<Logger> {
    return lazy { logger(this.javaClass) }
}

// return a logger property delegate for enclosing class
fun <R : Any> R.injectLogger(): Lazy<Logger> {
    return lazyOf(logger(this.javaClass))
}

interface Loggable {
    val LOG: Logger  // abstract required field

    fun logger(): Logger {
        return logger(this.javaClass)
    }
}

// abstract base class to provide logging, intended for companion objects more than classes but works for either
abstract class WithLogging: Loggable {
    override val LOG = logger()
}
