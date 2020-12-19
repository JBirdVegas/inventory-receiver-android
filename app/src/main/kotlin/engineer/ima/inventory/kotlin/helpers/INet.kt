package engineer.ima.inventory.kotlin.helpers

import java.net.HttpURLConnection
import java.net.URL

class INet {
    companion object {
        fun download(url: String): ByteArray {
            val obj = URL(url)
            val con = obj.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            val responseCode = con.responseCode
            return if (responseCode == HttpURLConnection.HTTP_OK) { // connection ok
                return con.inputStream.readBytes()

            } else {
                ByteArray(0)
            }
        }
    }
}