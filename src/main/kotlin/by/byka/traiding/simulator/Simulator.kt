package by.byka.traiding.simulator

import by.byka.traiding.data.StopLoss
import by.byka.traiding.data.StopLossConfig
import by.byka.traiding.data.Candle
import by.byka.traiding.data.Wallet
import by.byka.traiding.patterns.Decision
import by.byka.traiding.round
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val PERCENT_10 = 0.1
const val PERCENT_1 = 0.01
const val PERCENT_0_5 = 0.005
const val PERCENT_0_25 = 0.0025
const val PERCENT_0_1 = 0.001

interface Simulator {
    companion object {
        val log: Logger = LogManager.getLogger(Simulator::class.java)
    }

    val taxes: Double
    val initWallet: Wallet

    fun simulate(instructions: List<Pair<Candle, Decision>>, currentPrice: Double): Wallet {
        var wallet = initWallet.copy()
        var operations = 0
        var stopLoss: StopLoss? = null

        for (i in instructions) {
            if (stopLoss != null) {
                val newWallet = validateStopLoss(i.first, stopLoss, wallet)
                if (newWallet != null) {
                    wallet = newWallet
                    stopLoss = null
                    operations++
                    continue
                }
            }

            if (i.second is Decision.BUY) {
                val price = i.first.close
                if (wallet.usd > 0) {
                    log.debug("Buying ${i.first.time} for $price. ${i.second}")
                    operations++
                    val coins = (wallet.usd / price) * (1 - taxes)
                    wallet = Wallet(0.0, coins)
                    stopLoss = getStopLoss(i)
                }
            }
            if (i.second == Decision.SELL) {
                val price = i.first.close
                if (wallet.coins > 0) {
                    operations++
                    val usd = (wallet.coins * price) * (1 - taxes)
                    wallet = Wallet(usd, 0.0)
                    log.debug("Selling ${i.first.time} for $price. $wallet")
                    stopLoss = null
                }
            }
        }

        if (wallet.coins > 0) {
            wallet = Wallet((wallet.coins * currentPrice).round() + wallet.usd, 0.0)
        }

        log.debug("Usd: ${wallet.usd}")

        return wallet.copy(operations = operations)
    }

    private fun validateStopLoss(candle: Candle, stopLoss: StopLoss, wallet: Wallet): Wallet? {
        val price = when {
            candle.low < stopLoss.low -> {
                stopLoss.low
            }
            candle.high > stopLoss.peak -> {
                stopLoss.peak
            }
            else -> {
                null
            }
        }

        if (price != null) {
            val usd = wallet.coins * price * (1 - taxes)
            return Wallet(usd, 0.0).also {
                log.debug("Stoploss ${candle.time}: $it")
            }
        }
        return null
    }

    fun getStopLoss(pair: Pair<Candle, Decision>): StopLoss
}

class DynamicStopLassSimulator(
    override val initWallet: Wallet = Wallet(usd = 1000.0, coins = 0.0),
    override val taxes: Double = PERCENT_0_1,
): Simulator {
    override fun getStopLoss(pair: Pair<Candle, Decision>): StopLoss {
        return (pair.second as Decision.BUY).stopLoss
    }
}

class ConstantStopLossSimulator(
    override val initWallet: Wallet = Wallet(usd = 1000.0, coins = 0.0),
    private val stopLossConfig: StopLossConfig = StopLossConfig(1.0 + PERCENT_1, 1.0 - PERCENT_0_25),
    override val taxes: Double = PERCENT_0_1
): Simulator {

    companion object {
        val log: Logger = LogManager.getLogger(Simulator::class.java)
    }

    override fun getStopLoss(pair: Pair<Candle, Decision>): StopLoss {
        val price = pair.first.close
        return StopLoss(price * stopLossConfig.peak, price * stopLossConfig.low)
    }
}