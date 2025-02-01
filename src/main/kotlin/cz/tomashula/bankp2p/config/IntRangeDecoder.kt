package cz.tomashula.bankp2p.config

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import kotlin.reflect.KType

object IntRangeDecoder : Decoder<IntRange>
{
    override fun decode(node: Node, type: KType, context: DecoderContext): ConfigResult<IntRange>
    {
        if (node !is StringNode)
            return ConfigFailure.Generic("Expected a string, got ${node::class}").invalid()

        val parts = node.value.replace(Regex("""\s+"""), "").split(Regex("""\.\.|-"""))

        if (parts.size != 2)
            return ConfigFailure.Generic("Expected a string in the form of 'start..end' or 'start-end'").invalid()

        val start = parts[0].toIntOrNull() ?: return ConfigFailure.Generic("Cannot parse int: ${parts[0]}").invalid()
        val end = parts[1].toIntOrNull() ?: return ConfigFailure.Generic("Cannot parse int: ${parts[1]}").invalid()

        if (start > end)
            return ConfigFailure.Generic("Start of the range must be less than or equal to the end").invalid()

        return (start..end).valid()
    }

    override fun supports(type: KType) = type.classifier == IntRange::class
}
