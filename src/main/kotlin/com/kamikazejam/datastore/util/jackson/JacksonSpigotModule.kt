package com.kamikazejam.datastore.util.jackson

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.module.SimpleModule
import com.kamikazejam.datastore.util.jackson.deserialize.BlockDeserializer
import com.kamikazejam.datastore.util.jackson.serialize.BlockSerializer
import org.bukkit.block.Block

class JacksonSpigotModule : SimpleModule("JacksonSpigotModule", Version.unknownVersion()) {
    init {
        // Block
        addSerializer(Block::class.java, BlockSerializer())
        addDeserializer(Block::class.java, BlockDeserializer())
    }
}
