package com.hexagonkt.args

import com.hexagonkt.helpers.requireNotBlank

/**
 * A program can have multiple commands with their own set of options and positional parameters.
 */
data class Command(
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val properties: LinkedHashSet<Property<*>> = linkedSetOf(),
    val subcommands: LinkedHashSet<Command> = LinkedHashSet(),
) {
    val flags: LinkedHashSet<Flag> =
        LinkedHashSet(properties.filterIsInstance<Flag>())

    val options: LinkedHashSet<Option<*>> =
        LinkedHashSet(properties.filterIsInstance<Option<*>>())

    val parameters: LinkedHashSet<Parameter<*>> =
        LinkedHashSet(properties.filterIsInstance<Parameter<*>>())

    val propertiesMap: Map<String, Property<*>> =
        properties
            .flatMap { p ->
                p.names.map { it to p }
            }
            .toMap()

    val optionsMap: Map<String, Option<*>> =
        propertiesMap
            .filterValues { it is Option<*> }
            .mapValues { it.value as Option<*> }

    val parametersMap: Map<String, Parameter<*>> =
        propertiesMap
            .filterValues { it is Parameter<*> }
            .mapValues { it.value as Parameter<*> }

    val subcommandsMap: Map<String, Command> =
        nestedSubcommands().associateBy { it.name }

    private val parametersList: List<Parameter<*>> by lazy {
        parameters.toList()
    }

    init {
        requireNotBlank(Command::name)
        requireNotBlank(Command::title)
        requireNotBlank(Command::description)

        if (parametersMap.isNotEmpty()) {
            val parameters = parametersMap.values.reversed().drop(1)
            require(parameters.all { !it.multiple }) {
                "Only the last positional parameter can be multiple"
            }
        }
    }

    fun findCommand(args: Array<String>): Command {
        val line = args.joinToString(" ")
        return subcommandsMap
            .mapKeys { it.key.removePrefix("$name ") }
            .entries
            .sortedByDescending { it.key.count { c -> c == ' ' } }
            .find { line.contains(it.key) }
            ?.let { (k, v) -> v.copy(name = k) }
            ?: this
    }

    fun parse(args: List<String>): Command {
        val argsIterator = args.iterator()
        var parsedProperties = emptyList<Property<*>>()
        var parsedParameter = 0

        argsIterator.forEach {
            parsedProperties = when {
                it.startsWith("--") ->
                    parsedProperties + parseOption(it.removePrefix("--"), argsIterator)

                it.startsWith('-') ->
                    parsedProperties + parseOptions(it.removePrefix("-"), argsIterator)

                else -> {
                    val argument = parseArgument(it, parsedParameter)
                    parsedParameter += 1
                    parsedProperties + argument
                }
            }
        }

        return copy(properties = LinkedHashSet(parsedProperties))
    }

    private fun parseArgument(it: String, parameterIndex: Int): Property<*> {
        val p = parametersList.getOrNull(parameterIndex) ?: parametersList.lastOrNull()
        return p?.addValue(it) ?: error("No parameter at position $parameterIndex")
    }

    private fun parseOptions(
        names: String, argsIterator: Iterator<String>
    ): Collection<Property<*>> {
        val namesIterator = names.iterator()
        var result = emptyList<Property<*>>()

        namesIterator.forEach {
            val name = it.toString()
            val isOption = optionsMap.contains(name)
            val option = if (isOption && namesIterator.hasNext()) {
                val firstValueChar = namesIterator.next()
                val valueStart = if (firstValueChar != '=') "=$firstValueChar" else firstValueChar
                val buffer = StringBuffer(name + valueStart)

                namesIterator.forEachRemaining(buffer::append)
                buffer.toString()
            }
            else name

            result = result + parseOption(option, argsIterator)
        }

        return result
    }

    private fun parseOption(option: String, propertiesIterator: Iterator<String>): Property<*> {
        val nameValue = option.split('=', limit = 2)
        val name = nameValue.first()
        val property = propertiesMap[name] ?: error("Option '$name' not found")
        val value =
            if (property is Option<*>) nameValue.getOrNull(1) ?: propertiesIterator.next()
            else "true"

        return property.addValue(value)
    }

    private fun nestedSubcommands(): LinkedHashSet<Command> =
        subcommands
            .map { it.copy(name = name + " " + it.name) }
            .let { c -> c + c.flatMap { it.nestedSubcommands() } }
            .let(::LinkedHashSet)
}
