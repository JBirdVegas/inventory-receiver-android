package engineer.ima.inventory.kotlin

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import engineer.ima.inventory.kotlin.Application.Companion.KEY_RESPONSE_BODY
import engineer.ima.inventory.kotlin.Application.Companion.KEY_STATUS_CODE
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UpdateFcmToken(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Log.d(TAG, "Performing long running task in scheduled job")

        val deviceId = inputData.getString("deviceId")
        val deviceName = inputData.getString("deviceName")
        val mUrl = URL("https://inventory.ima.engineer/v1/device/handset-link")
        println("Using url %s".format(mUrl))
        val token = Preferences(applicationContext).firebaseToken
        println("Received token: %s, deviceId: %s".format(token, deviceId))

        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val results = Data.Builder()
        with(mUrl.openConnection() as HttpsURLConnection) {
            val map = mapOf("push_token" to token,
                    "push_type" to "android",
                    "handset_id" to androidId,
                    "device_id" to deviceId,
                    "device_name" to deviceName)
            requestMethod = "POST"
            BufferedWriter(OutputStreamWriter(outputStream)).use { bw ->
                bw.write(Gson().toJson(map).toString())
            }
            Log.d(TAG, "Status Code: %d".format(this.responseCode))
            Log.d(TAG, this.responseMessage)
            BufferedReader(InputStreamReader(inputStream)).use { br ->
                val response = StringBuffer()
                var inputLine = br.readLine()
                while (inputLine != null) {

                    response.append(inputLine)
                    inputLine = br.readLine()
                }
                Log.d(TAG, "Response : $response")
                results.putInt(KEY_STATUS_CODE, this.responseCode)
                        .putString(KEY_RESPONSE_BODY, response.toString())
            }
        }
        return if (results.build().getInt(KEY_STATUS_CODE, 0) == 204) {
            Result.success(results.build())
        } else {
            Result.failure(results.build())
        }
    }

    companion object {
        private val TAG = UpdateFcmToken::class.java.simpleName
    }
}