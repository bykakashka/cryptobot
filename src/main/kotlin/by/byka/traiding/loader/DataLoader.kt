package by.byka.traiding.loader

import by.byka.traiding.constants.Constants.jacksonMapper
import by.byka.traiding.data.Candle
import by.byka.traiding.data.Coin
import com.fasterxml.jackson.core.type.TypeReference
import java.io.File
import java.time.Instant

interface DataLoader {
    fun load(coin: Coin, limit: Int = 10000): List<Candle>
}

class FileDataLoader() : DataLoader {
    override fun load(coin: Coin, limit: Int): List<Candle> {
        val file = File(coin.file)
        val serializedCandles = file.readText()
        return jacksonMapper.readerFor(object : TypeReference<List<Candle>>() {}).readValue(serializedCandles)
    }
}