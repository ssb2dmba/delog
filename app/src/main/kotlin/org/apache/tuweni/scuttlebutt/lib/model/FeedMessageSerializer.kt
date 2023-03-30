package org.apache.tuweni.scuttlebutt.lib.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class FeedMessageSerializer : JsonSerializer<FeedMessage>() {
    override fun serialize(
        value: FeedMessage?,
        gen: JsonGenerator?,
        serializers: SerializerProvider?
    ) {

        gen?.writeObject(value)
    }


}
