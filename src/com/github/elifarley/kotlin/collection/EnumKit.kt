package com.orgecc.util.collection

import java.util.*

/**
 * Created by elifarley on 28/10/16.
 */

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): Map<K, V> =
        EnumMap<K, V>(K::class.java).apply {
            putAll(pairs)
        }

object EnumKit {

    interface GetCode<out C> {
        val code: C
    }
    interface GetStringCode : GetCode<String>
    interface GetIntCode : GetCode<Int>
    interface GetClassCode : GetCode<Class<*>>

    fun <E, C> toValue(enumType: Class<E>, code: C): E
            where E : Enum<E>, E : GetCode<C> =
            enumType.enumConstants.firstOrNull { it.code  == code }
                    ?: throw NoSuchElementException("No element in '${enumType.typeName}' has code '$code'.")

}
