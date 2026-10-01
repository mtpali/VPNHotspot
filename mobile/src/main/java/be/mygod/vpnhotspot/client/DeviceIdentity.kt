package be.mygod.vpnhotspot.client

import java.io.InputStream
import java.util.Locale

/** Passive, offline hints: names and manufacturer prefixes are never proof of a phone model. */
object DeviceIdentity {
    private val samsungModel = Regex("(?i)(?:^|[-_ ])(SM-[A-Z0-9]+)(?:$|[-_ ])")
    private val phoneNames = listOf(
        "iphone" to "Apple iPhone", "ipad" to "Apple iPad",
        "galaxy" to "Samsung Galaxy", "samsung" to "Samsung",
        "redmi" to "Xiaomi Redmi", "poco" to "Xiaomi POCO", "xiaomi" to "Xiaomi",
        "oneplus" to "OnePlus", "pixel" to "Google Pixel", "huawei" to "Huawei",
        "honor" to "Honor", "oppo" to "OPPO", "realme" to "realme", "vivo" to "vivo",
        "motorola" to "Motorola", "nothing" to "Nothing", "nokia" to "Nokia",
        "xperia" to "Sony Xperia", "asus" to "ASUS", "zte" to "ZTE",
    )

    fun readManufacturers(input: InputStream): Map<String, String> = input.bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            val separator = line.indexOf('\t')
            if (separator != 6) null else line.substring(0, separator) to line.substring(separator + 1)
        }.toMap()
    }

    fun guess(mac: String, hostnames: Iterable<String>, manufacturers: Map<String, String>): String? {
        for (hostname in hostnames) {
            val name = hostname.removeSuffix(".local")
            samsungModel.find(name)?.let { return "Samsung ${it.groupValues[1].uppercase(Locale.ROOT)}" }
            phoneNames.firstOrNull { (token, _) ->
                name.equals(token, true) || name.startsWith("$token-", true) ||
                    name.startsWith("${token}_", true) || name.startsWith("$token ", true)
            }?.let { return it.second }
        }
        val compact = mac.replace(":", "").uppercase(Locale.ROOT)
        if (compact.length != 12 || compact.any { it !in '0'..'9' && it !in 'A'..'F' }) return null
        if (compact.substring(0, 2).toInt(16) and 3 != 0) return null
        return manufacturers[compact.take(6)]
    }
}
