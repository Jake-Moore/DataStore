package com.kamikazejam.datastore.base.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.block.Block

@Serializable
data class BlockSurrogate(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int
)

@Suppress("unused")
object BlockSerializer : KSerializer<Block> {
    override val descriptor = BlockSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Block) {
        val surrogate = BlockSurrogate(
            world = value.world.name,
            x = value.x,
            y = value.y,
            z = value.z
        )
        encoder.encodeSerializableValue(BlockSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Block {
        val surrogate = decoder.decodeSerializableValue(BlockSurrogate.serializer())
        val world = requireNotNull(Bukkit.getWorld(surrogate.world)) {
            "World ${surrogate.world} not found"
        }
        return world.getBlockAt(surrogate.x, surrogate.y, surrogate.z)
    }
}