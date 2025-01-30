package com.kamikazejam.datastore.util.jackson.serialize

import org.bukkit.block.Block
import java.io.IOException

class BlockSerializer : JsonSerializer<Block>() {
    @Throws(IOException::class)
    override fun serialize(block: Block, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        // Implement your custom serialization logic here
        jsonGenerator.writeStartObject()
        jsonGenerator.writeStringField("world", block.world.name)
        jsonGenerator.writeNumberField("x", block.x)
        jsonGenerator.writeNumberField("y", block.y)
        jsonGenerator.writeNumberField("z", block.z)
        jsonGenerator.writeEndObject()
    }
}
