package by.byka.traiding.data

enum class Coin(val coinName: String, val file: String = "$coinName.json") {
    BTC("BTC-USDT"), ETH("ETH-USDT"), XRP("XRP-USDT"), ETHW("ETHW-USDT"), PEOPLE("PEOPLE-USDT"),
    TAMA("TAMA-USDT")
}