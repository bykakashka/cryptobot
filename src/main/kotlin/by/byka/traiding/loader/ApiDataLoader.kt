package by.byka.traiding.loader

import by.byka.traiding.constants.Constants
import by.byka.traiding.data.Candle
import by.byka.traiding.data.Coin
import by.byka.traiding.retrofit.OkxApi
import com.fasterxml.jackson.core.type.TypeReference
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ApiDataLoader(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val client: OkxApi = Retrofit.Builder()
        .baseUrl("https://www.okx.com/priapi/v5/")
        .client(httpClient)
        .addConverterFactory(JacksonConverterFactory.create(Constants.jacksonMapper))
        .build().create(OkxApi::class.java)
) : DataLoader {
    override fun load(coin: Coin, limit: Int): List<Candle> {
        val time = Instant.now().minus(3, ChronoUnit.HOURS).toEpochMilli()
        val data = getData(client, time, coin.coinName, limit)
        serialize(data, coin)
        return data
    }

    private fun getData(client: OkxApi, date: Long, coinName: String, limit: Int = 1000): List<Candle> {
        val resp = client.getCandles("15m", date, instId = coinName, limit = limit).execute()
        return if (resp.body() != null) {
            val candles = resp.body()!!.data.map {
                Candle(
                    it[0].toLong(),
                    it[1].toDouble(),
                    it[2].toDouble(),
                    it[3].toDouble(),
                    it[4].toDouble()
                )
            }
            candles
        } else {
            emptyList()
        }
    }

    private fun serialize(candles: List<Candle>, coin: Coin) {
        val serializedCandles =
            Constants.jacksonMapper.writerFor(object : TypeReference<List<Candle>>() {}).writeValueAsString(candles)
        val file = File(coin.file)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(serializedCandles)
    }

}