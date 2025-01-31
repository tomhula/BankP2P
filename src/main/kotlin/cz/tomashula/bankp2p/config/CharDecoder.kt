package cz.tomashula.bankp2p.config

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import kotlin.reflect.KType

object CharDecoder : Decoder<Char>
{
    override fun decode(node: Node, type: KType, context: DecoderContext): ConfigResult<Char>
    {
        if (node !is StringNode)
            return ConfigFailure.Generic("Expected a string, got ${node::class}").invalid()

        if (node.value.length != 1)
            return ConfigFailure.Generic("Expected a char, got a string").invalid()

        return node.value[0].valid()
    }

    override fun supports(type: KType) = type.classifier == Char::class
}
