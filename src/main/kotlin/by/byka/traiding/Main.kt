package by.byka.traiding

import by.byka.traiding.data.*
import by.byka.traiding.loader.ApiDataLoader
import by.byka.traiding.loader.DataLoader
import by.byka.traiding.loader.FileDataLoader
import by.byka.traiding.patterns.*
import by.byka.traiding.simulator.Simulator
import by.byka.traiding.simulator.ConstantStopLossSimulator
import by.byka.traiding.simulator.DynamicStopLassSimulator
import com.byka.neuralnetwork.load
import com.byka.neuralnetwork.network
import com.byka.neuralnetwork.save
import java.math.BigDecimal
import java.math.RoundingMode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Main

val log: Logger = LogManager.getLogger(Main::class.java)

enum class  COLOR {
    RED, GREEN, UNKNOWN
}

const val WINDOW = 5
const val FUTURE = 1

data class CandleInfo(
    val color: COLOR,
    val bodySize: Double,
    // shadow of body ratio. 1 if equals
    val topShadow: Double,
    val bottomShadow: Double,
    val candle: Candle
)

fun main() {
    val network = load("network2.json")

    val loadedCandles = ApiDataLoader().load(Coin.BTC, 10000)
    val candles = loadedCandles.subList(1, loadedCandles.size).reversed()

    val data = candles.map { ((it.high - it.low) * 100 /  it.low) to (it.open - it.close) * 100 / it.open}
    network.feedForward(data.subList(data.size-6, data.size-1).flatMap { listOf(it.first, it.second) })

    var wallet = Wallet(1000.0, 0.0)
    var buys = 0
    var sells = 0
    var stopLoss = Double.MIN_VALUE
    for (i in 0 until (data.size-WINDOW-FUTURE)) {
        if (candles[i].low < stopLoss) {
            if (wallet.coins > 0) {
                wallet = sell(candles, i, wallet)
                sells++
                continue
            }
        }
        val res = network.feedForward(data.subList(i, i+WINDOW).flatMap { listOf(it.first, it.second) })
        if (res[0] > 0.2) {
            if (wallet.usd > 0) {
                val currentPrice = candles[i+ WINDOW-1].close
                val coins = wallet.usd / currentPrice
                wallet = Wallet(0.0, coins)
                buys++
                stopLoss = res[1]
            }
        } else {
            if (wallet.coins > 0) {
                wallet = sell(candles, i, wallet)
                sells++
                stopLoss = Double.MIN_VALUE
            }
        }
    }

    if (wallet.usd == 0.0) {
        wallet = Wallet(candles[data.size-WINDOW-FUTURE].close * wallet.coins, 0.0)
    }
    println(wallet)
    println("$buys - $sells")
}

private fun sell(
    candles: List<Candle>,
    i: Int,
    wallet: Wallet,
): Wallet {
    val currentPrice = candles[i + WINDOW - 1].close
    val usd = wallet.coins * currentPrice
    return Wallet(usd, 0.0)
}

fun mainq() {
    val loadedCandles = FileDataLoader().load(Coin.BTC, 10000)
    val candles = loadedCandles.subList(1, loadedCandles.size).reversed()

    val data = candles.map { ((it.high - it.low) * 100 /  it.low) to (it.open - it.close) * 100 / it.open}
    val network = network {
        layer {
            nodes = WINDOW * 2
        }

        layer {
            nodes = 8
            prevLayerNodes = WINDOW * 2
        }
        layer {
            nodes = 2
            prevLayerNodes = 8
        }
        learningRate = 0.01
    }

    for (j in 0 until (data.size*1000)) {
        val i = Random().nextInt(data.size-WINDOW-FUTURE-1)

        val latest = candles[i+WINDOW-1].close
        val maxHigh = (0 until FUTURE).map { (candles[i+WINDOW+it].high - latest) * 100 / latest }.max()
        val minLow = (0 until FUTURE).map { (candles[i+WINDOW+it].low - latest) * 100 / latest }.min()

        network.train(data.subList(i, i+WINDOW).flatMap { listOf(it.first, it.second) }, listOf(maxHigh, minLow))
    }
//    for (i in 0 until (data.size-WINDOW-FUTURE)) {
//        val latest = candles[i+WINDOW-1].close
//        val maxHigh = (0 until FUTURE).map { (candles[i+WINDOW+it].high - latest) * 100 / latest }.max()
//        val minLow = (0 until FUTURE).map { (candles[i+WINDOW+it].low - latest) * 100 / latest }.min()
//
//        network.train(data.subList(i, i+WINDOW).flatMap { listOf(it.first, it.second) }, listOf(maxHigh, minLow))
//    }

    network.save("network2.json")
}

fun main1() {
    val coin = Coin.PEOPLE
    val instructionsByCoin = listOf(coin).associateWith {
        getInstructions(FileDataLoader(), it)
    }

    log.info("Instructions: \n${instructionsByCoin[coin]?.filter { it.second != Decision.UNKNOWN }?.map { "${it.first.time} -> ${it.second} \n" }}")

    instructionsByCoin.forEach { (coin, instructions) ->
//        val topConfigs = findBestStopLosses(instructions.reversed(), instructions.first().first.close)
//        log.info("Top configs: \n$topConfigs")
        val result = DynamicStopLassSimulator().simulate(instructions.reversed(), instructions.first().first.close)
        log.info("Wallet: $result")
    }
}

fun getInstructions(loader: DataLoader, coin: Coin): List<Pair<Candle, Decision>> {
    val loadedCandles = loader.load(coin, 1000)
    val candles = loadedCandles.subList(1, loadedCandles.size)
    val smma = smma(candles.reversed().map { it.close }).reversed()

    val singlePatterns = listOf<SinglePattern>(FallenStar)
    val doublePatterns = listOf<DoublePattern>()

    return candles.mapIndexed { index, it ->
        val patternDecisions = (singlePatterns.map { pattern ->
            pattern::class to pattern.getDecision(it, smma[index], candles.subList(index+1, candles.size))
        })
            .toMap()

        it to patternDecisions[FallenStar::class]!!
    }
}

fun findBestStopLosses(instructions: List<Pair<Candle, Decision>>, latestPrice: Double): List<StopLossConfig> {
    val map = mutableMapOf<StopLossConfig, Wallet>()

    for (i in 1..100) {
        for (j in 1..100) {
            val peak = 1.0 + (i * 0.001)
            val low = 1.0 - (j * 0.001)
            val config = StopLossConfig(peak, low)
            val wallet = ConstantStopLossSimulator(stopLossConfig = config).simulate(instructions, latestPrice)
            map[config] = wallet
        }
    }

    val sortedConfigs = map.entries.sortedByDescending { it.value.usd }
    val topConfigs = mutableMapOf<StopLossConfig, Wallet>()
    var i = 0
    while (topConfigs.size < 10) {
        val currentConfig = sortedConfigs[i++]
        if (!topConfigs.keys.map { it.peak }.contains(currentConfig.key.peak) && !topConfigs.keys.map{it.low }.contains(currentConfig.key.low)) {
            topConfigs[currentConfig.key] = currentConfig.value
        }
    }
    log.info(topConfigs)
    return topConfigs.map { it.key }
}

fun Double.round(): Double {
    return BigDecimal(this).setScale(5, RoundingMode.HALF_UP).toDouble()
}

fun smma(data: List<Double>, period: Int = 7): List<Double> {
    val result = mutableListOf<Double>()
    var prevSmoothed = data[0]
    result.add(prevSmoothed)
    for (i in 1 until data.size) {
        prevSmoothed = (prevSmoothed * (period - 1) + data[i]) / period
        result.add(prevSmoothed)
    }
    return result.map { it.round() }
}