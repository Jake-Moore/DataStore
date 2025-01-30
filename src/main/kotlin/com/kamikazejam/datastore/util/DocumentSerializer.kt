package com.kamikazejam.datastore.util

import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.ByteBuf
import org.bson.ByteBufNIO
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.codecs.EncoderContext
import org.bson.io.BasicOutputBuffer
import org.bson.io.ByteBufferBsonInput
import java.nio.ByteBuffer

object DocumentSerializer {
    fun deepCopyDocument(original: Document): Document {
        val bytes = documentToByteArray(original)
        return byteArrayToDocument(bytes)
    }

    private fun documentToByteArray(document: Document): ByteArray {
        val outputBuffer = BasicOutputBuffer()
        BsonBinaryWriter(outputBuffer).use { writer ->
            DocumentCodec().encode(writer, document, EncoderContext.builder().build())
        }
        return outputBuffer.toByteArray()
    }

    private fun byteArrayToDocument(bytes: ByteArray): Document {
        val byteBuffer = ByteBuffer.wrap(bytes)
        val byteBuf: ByteBuf = ByteBufNIO(byteBuffer)
        return ByteBufferBsonInput(byteBuf).use { input ->
            BsonBinaryReader(input).use { reader ->
                DocumentCodec().decode(reader, DecoderContext.builder().build())
            }
        }
    }
}