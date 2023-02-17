package by.byka.traiding.patterns

import by.byka.traiding.data.StopLoss

//
//enum class Decision {
//    BUY, SELL, UNKNOWN
//}

sealed class Decision {
    object SELL: Decision() {
        override fun toString(): String {
            return "SELL"
        }
    }
    object UNKNOWN: Decision()
    class BUY(val stopLoss: StopLoss): Decision() {
        override fun toString(): String {
            return "BUY: $stopLoss"
        }
    }
}