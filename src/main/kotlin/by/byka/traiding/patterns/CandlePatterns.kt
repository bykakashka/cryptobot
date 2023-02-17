package by.byka.traiding.patterns

import by.byka.traiding.data.*
import by.byka.traiding.simulator.PERCENT_0_1
import kotlin.math.abs
import kotlin.math.min

const val DELTA: Int = 5

interface SinglePattern {
    fun getDecision(candle: Candle, thread: Double, history: List<Candle>): Decision
}


interface DoublePattern {
    fun getDecision(left: Candle, right: Candle): Decision
}

object FallenStar : SinglePattern {
    /**
     * thread: + up, - down
     */
    override fun getDecision(candle: Candle, thread: Double, history: List<Candle>): Decision {
        if (candle.isSmallBody() && abs(candle.close - thread) > (min(thread, candle.close) * PERCENT_0_1)) {
            if (!candle.containsLowShadow() && candle.haveBigHighShadow()) {
                return findDecision(candle.close - thread, history, candle)
            }
            if (!candle.containsHighShadow() && candle.haveBigLowShadow()) {
                return findDecision(candle.close - thread, history, candle)
            }
        }

        return Decision.UNKNOWN
    }

    private fun findDecision(
        diff: Double,
        history: List<Candle>,
        candle: Candle
    ): Decision {
        return if (diff < 0) {
            val sellValue = history.subList(0, DELTA).map { it.high }.max()
            if (sellValue - candle.close > PERCENT_0_1 * candle.close) {
                val avgBody = history.subList(0, DELTA).sumOf { abs(it.open - it.close) } / DELTA
                val fallValue = candle.close - avgBody // 2 times more than sell profit
                Decision.BUY(StopLoss(sellValue, fallValue))
            } else {
                Decision.UNKNOWN
            }
        } else {
            Decision.SELL
        }
    }
}

object CoverUp : DoublePattern {
    private fun covertMoreThenX(left: Candle, right: Candle, multiplier: Double = 10000.0): Boolean {
        return abs(left.open - left.close) * multiplier < abs(right.open - right.close)
    }

    override fun getDecision(left: Candle, right: Candle): Decision {
        if (left.close > left.open && right.open > right.close) {
            if (left.close < right.open && left.open > right.close) {
                return if (covertMoreThenX(left, right)) {
                    Decision.SELL
                } else {
                    Decision.UNKNOWN
                }
            }
        }

        if ((left.open - left.close) > 0 && (right.close - right.open) > 0) {
            if (left.open < right.close && left.close > right.open) {
                return if (covertMoreThenX(left, right)) {
//                    Decision.BUY TODO
                    Decision.UNKNOWN
                } else {
                    Decision.UNKNOWN
                }
            }
        }
        return Decision.UNKNOWN
    }

}