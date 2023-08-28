package com.projectronin.openapi.shared.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter

/**
 * Tweaks the json output because the linter doesn't like Jackson's default output
 */
class JsonPrettyPrinter(b: DefaultPrettyPrinter) : PrettyPrinter by b {
    override fun writeObjectFieldValueSeparator(gen: JsonGenerator?) {
        gen?.writeRaw(": ")
    }
}
