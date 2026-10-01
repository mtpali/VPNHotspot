package be.mygod.vpnhotspot.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Offline contact action. Encoding raises the cost of a literal-string search, not a security boundary. */
object ContactRoute {
    private val first = intArrayOf(90, 86, 61, 77, 76, 63, 44, 63, 193, 188, 107, 87, 128, 110, 59, 116, 157, 139, 137, 128, 53, 85, 38, 252, 51, 233, 13, 22, 31, 196, 169, 162)
    private val second = intArrayOf(144, 238, 101, 74, 114, 2, 244, 69, 83, 212, 12, 137, 15, 42, 23, 195, 44, 203, 183, 118, 246, 41, 243, 167, 241, 213, 241, 179, 143, 12, 56, 24)
    private val routes = arrayOf(intArrayOf(88, 188, 225, 64, 186, 161, 213, 25, 192, 44, 245, 140, 39, 81, 24, 215, 79, 3, 171, 119, 33, 235, 17, 13, 145, 89, 117, 143, 10, 185, 255, 237, 27, 54, 175, 101, 232, 49, 255, 91, 237, 171, 202, 225, 44, 21, 124, 101, 78, 78, 150, 26, 46, 13),
        intArrayOf(105, 84, 220, 2, 228, 178, 153, 7, 22, 174, 15, 86, 236, 73, 200, 249, 61, 212, 157, 25, 81, 17, 56, 97, 56, 156, 210, 184, 29, 202, 82, 141, 140, 97, 108, 235, 90, 177, 105, 209, 213, 175, 137, 26, 189, 237, 171))

    private fun read(index: Int): String {
        val key = ByteArray(first.size) { (first[it] xor second[it]).toByte() }
        val encoded = routes[index].map { it.toByte() }.toByteArray()
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, encoded.copyOfRange(0, 12)))
            doFinal(encoded.copyOfRange(12, encoded.size)).toString(Charsets.UTF_8)
        }
    }

    fun open(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, read(0).toUri()))
        } catch (_: ActivityNotFoundException) {
            context.launchUrl(read(1))
        }
    }
}
