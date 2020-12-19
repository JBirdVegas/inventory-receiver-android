package engineer.ima.inventory.kotlin.workers


import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class PaqWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Log.d(TAG, "Performing long running task in scheduled job")

        val deviceId = inputData.getString(INPUT_DATA_DEVICE_ID)
        val action = inputData.getString(INPUT_DATA_POLL_ACTION)
        if (deviceId == null || action == null) {
            Log.e(TAG, "Missing input to worker")
            return Result.failure()
        }

        val url = URL("https://inventory.ima.engineer/v1/device/%s/paq".format(deviceId))
        val sendBody = hashMapOf(
                "action" to action,
                "uuid" to UUID.randomUUID().toString(),
                "start_time" to "${Calendar.getInstance().time.time / 1000}"
        )

        val body = Gson().toJson(sendBody)
        Log.d(TAG, "Sending paq request: $body")
        val sendPostRequest = post(url.toString(), body)
        Log.d(TAG, "Result: $sendPostRequest")
        return Result.success()
    }

    fun post(url: String, body: String) {
        val conn = URL(url).openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.doInput = true
        conn.doOutput = true
        conn.setRequestProperty("User-Agent", "ima.engineer/golang")
        conn.setRequestProperty("Content-Type", "application/json")
        val writer = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
        writer.write(body)
        writer.flush()
        writer.close()
        conn.outputStream.close()
        conn.connect()
        val status = conn.responseCode
        Log.d(TAG, "Response status: $status")
        conn.responseMessage + ""

        val stream = if (status !in intArrayOf(200, 202, 203, 204)) conn.errorStream else conn.inputStream
        Log.d(TAG, "RESULT: ${readFully(stream)}")
    }

    private fun readFully(inputStream: InputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length = 0
        while (inputStream.read(buffer).also { length = it } != -1) {
            baos.write(buffer, 0, length)
        }
        return baos.toString("UTF-8")
    }

    companion object {
        private val TAG = PaqWorker::class.java.simpleName
        const val INPUT_DATA_DEVICE_ID = "DEVICE_ID"
        const val INPUT_DATA_POLL_ACTION = "POLL_ACTION"

        fun queueWork(deviceId: String, action: String) {
            val requestBuilder = OneTimeWorkRequest.Builder(PaqWorker::class.java)
            val dataBuilder = Data.Builder()
            dataBuilder.putString(INPUT_DATA_DEVICE_ID, deviceId)
            dataBuilder.putString(INPUT_DATA_POLL_ACTION, action)
            requestBuilder.setInputData(dataBuilder.build())
            val work = requestBuilder.build()
            WorkManager.getInstance().beginWith(work).enqueue()
        }
    }
}