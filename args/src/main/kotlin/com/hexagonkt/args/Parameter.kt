package com.hexagonkt.args

import com.hexagonkt.core.parseOrNull
import kotlin.reflect.KClass

data class Parameter<T : Any>(
    override val type: KClass<T>,
    val name: String,
    override val description: String? = null,
    override val regex: Regex? = null,
    override val optional: Boolean = true,
    override val multiple: Boolean = false,
    override val tag: String? = null,
    override val values: List<T> = emptyList(),
) : Property<T> {

    override val names: Set<String> = setOf(name)

    companion object {
        private val parameterRegex = "[a-z0-9\\-]{2,}".toRegex()
    }

    constructor(
        type: KClass<T>,
        name: String,
        description: String? = null,
        regex: Regex? = null,
        optional: Boolean = true,
        tag: String? = null,
        value: T,
    ) : this(type, name, description, regex, optional, false, tag, listOf(value))

    init {
        check("Parameter", parameterRegex)
    }

    override fun addValue(value: String): Parameter<T> =
        value.parseOrNull(type)
            ?.let { copy(values = values + it) }
            ?: error("Parameter '$name' of type '${typeText()}' can not hold '$value'")
}
