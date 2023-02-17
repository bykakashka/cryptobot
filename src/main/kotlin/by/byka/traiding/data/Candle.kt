package by.byka.traiding.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.lang.Double.max
import java.lang.Double.min
import java.time.Instant
import kotlin.math.abs

data class Candle(
    val date: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun create(
            @JsonProperty("date") date: Long,
            @JsonProperty("open") open: Double,
            @JsonProperty("high") high: Double,
            @JsonProperty("close") close: Double,
            @JsonProperty("low") low: Double
        ): Candle = Candle(date, open, high, low, close)
    }

    override fun toString(): String {
        return "(date=${Instant.ofEpochMilli(date)}, open=$open, high=$high, low=$low, close=$close)\n"
    }

    val body: Double
        @JsonIgnore
        get() = close - open

    val time: Instant
        @JsonIgnore
        get() = Instant.ofEpochMilli(date)
}

fun Candle.containsHighShadow(): Boolean {
    val highShadow = high - max(this.close, this.open)
    return (high - low) * 0.1 < highShadow
}

// low shadow > 10% of full candle
fun Candle.containsLowShadow(): Boolean {
    val lowShadow = min(this.close, this.open) - low
    return (high - low) * 0.1 < lowShadow
}

fun Candle.isSmallBody(): Boolean {
    val shadow = high - low
    return shadow * 0.1 >= abs(body)
}

fun Candle.isVerySmallBody(): Boolean {
    val shadow = high - low
    return shadow * 0.05 >= abs(body)
}

fun Candle.haveBigHighShadow(): Boolean {
    val minBody = min(close, open)
    val shadow = high - minBody
    return shadow * 0.25 >= abs(body)
}

fun Candle.haveBigLowShadow(): Boolean {
    val maxBody = max(close, open)
    val shadow = maxBody - low
    return shadow * 0.25 >= abs(body)
}