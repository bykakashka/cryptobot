package by.byka.traiding.retrofit

import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


data class Response(
    @JsonProperty("code") val code: String,
    @JsonProperty("data") val data: List<List<String>>
) {

    data class Candle(
        @JsonProperty("0") val date: Long,
        @JsonProperty("1") val open: Double,
        @JsonProperty("2") val high: Double,
        @JsonProperty("3") val low: Double,
        @JsonProperty("4") val close: Double
    )

}

data class CoinResponse(
    @JsonProperty("data") val data: List<ApiCoin>
)

data class ApiCoin(
    @JsonProperty("name") val name: String,
    @JsonProperty("id") val id: String
)

interface OkxApi {
    @GET("market/candles") //&t=1663545197604
    fun getCandles(
        @Query("bar") bar: String,
        @Query("t") date: Long,
        @Query("instId") instId: String = "BTC-USDT",
        @Query("limit") limit: Int = 1000
    ): Call<Response>

    @GET("public/coins")
    fun getCoins(@Query("t") time: Long): Call<CoinResponse>
}