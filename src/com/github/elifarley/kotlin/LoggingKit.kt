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
fun <T: Any> logger(forClass: Class<T>): Logger = LoggerFactory.getLogger(unwrapCompanionClass(forClass).name)

// Return logger for Kotlin class
fun <T: Any> logger(forClass: KClass<T>): Logger = logger(forClass.java)

// unwrap companion class to enclosing class given a Java Class
fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> =
    if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }

// unwrap companion class to enclosing class given a Kotlin Class
fun <T: Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> = unwrapCompanionClass(ofClass.java).kotlin

// return a lazy logger property delegate for enclosing class
fun <R : Any> R.lazyLogger(): Lazy<Logger> = lazy { logger(this.javaClass) }

// return a logger property delegate for enclosing class
fun <R : Any> R.injectLogger(): Lazy<Logger> = lazyOf(logger(this.javaClass))

interface Loggable {
    val LOG: Logger  // abstract required field
    fun logger(): Logger = logger(this.javaClass)
}

/**
 * Base class to provide logging.
 * Intended for companion objects more than classes but works for either.
 * Usage example: `companion object: WithLogging() {}`
 */
abstract class WithLogging { val LOG: Logger by lazyLogger() }


inline fun String.prefixWith(prefix: String?) = if (prefix == null) this else prefix + this

/**
 * See https://stackoverflow.com/questions/4165558/best-practices-for-using-markers-in-slf4j-logback
 **/
class MDCCloseable(): Closeable {

    private val keys = mutableSetOf<String>()

    @JvmOverloads
    fun putAll(map: Map<String, Any?>, prefix: String? = null): MDCCloseable {
        map.forEach {
            MDC.put(it.key.prefixWith(prefix).apply { keys.add(this) }, it.value.toString())
        }
        return this
    }
    fun put(key: String, value: Any?): MDCCloseable {
        MDC.put(key.apply { keys.add(this) }, value.toString())
        return this
    }
    fun get(key: String): String? = MDC.get(key)
    fun clear() { keys.forEach { MDC.remove(it) }; keys.clear() }
    fun remove(key: String) = MDC.remove(key.apply { keys.remove(this) })

    override fun close() = clear()

}
