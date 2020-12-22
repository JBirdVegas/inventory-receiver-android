package engineer.ima.inventory.kotlin.workers

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import engineer.ima.inventory.BuildConfig
import engineer.ima.inventory.kotlin.Application.Companion.KEY_RESPONSE_BODY
import engineer.ima.inventory.kotlin.Application.Companion.KEY_STATUS_CODE
import engineer.ima.inventory.kotlin.Preferences
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UpdateFcmToken(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    private val preferences = Preferences(appContext)
    override fun doWork(): Result {
        val mUrl = URL("${BuildConfig.API_URL}/v1/device/handset-link")
        val token = preferences.firebaseToken ?: return Result.failure()
        if (preferences.associatedDevices.isEmpty()) {
            return Result.failure()
        }

        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        for (device in preferences.associatedDevices) {
            val results = Data.Builder()
            with(mUrl.openConnection() as HttpsURLConnection) {
                val map = mapOf("push_token" to token,
                        "push_type" to "android",
                        "handset_id" to androidId,
                        "device_id" to device,
                        "device_name" to preferences.getDeviceUsername(device))
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
        }
        return Result.success()
    }

    companion object {
        private val TAG = UpdateFcmToken::class.java.simpleName
    }
}