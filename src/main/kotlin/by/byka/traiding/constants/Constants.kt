package by.byka.traiding.constants

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

object Constants {
    const val FILE_NAME: String = "candles.json"

    public val jacksonMapper: ObjectMapper =
        ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

}