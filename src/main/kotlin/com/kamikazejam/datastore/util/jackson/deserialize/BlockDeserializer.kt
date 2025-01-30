package com.kamikazejam.datastore.util.jackson.deserialize

import org.bukkit.Bukkit
import org.bukkit.block.Block
import java.io.IOException
import java.util.*

class BlockDeserializer : JsonDeserializer<Block>() {
    @Throws(IOException::class)
    override fun deserialize(jp: JsonParser, context: DeserializationContext): Block {
        val codec = jp.codec
        val node = codec.readTree<JsonNode>(jp)

        val worldName = node["world"].asText()
        val world = Objects.requireNonNull(Bukkit.getWorld(worldName))

        return world.getBlockAt(
            node["x"].asInt(),
            node["y"].asInt(),
            node["z"].asInt()
        )
    }
}
