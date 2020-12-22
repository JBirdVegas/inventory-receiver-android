package engineer.ima.inventory.kotlin.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.zxing.integration.android.IntentIntegrator
import engineer.ima.inventory.R
import engineer.ima.inventory.databinding.ActivityMainBinding
import engineer.ima.inventory.kotlin.LinkedDeviceAdapter
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.workers.PaqWorker
import engineer.ima.inventory.kotlin.workers.UpdateFcmToken
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var prefs: Preferences

    private val networkConnectedConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Preferences(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW))
        }
        val preferences = Preferences(applicationContext)

        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            preferences.firebaseToken = token
        })

        findViewById<Button>(R.id.scan_qr_code).setOnClickListener {
            scanQRCode()
        }

        listView = findViewById(R.id.linked_devices)
        val arrayList = ArrayList(preferences.associatedDevices.toMutableList())
        Log.d(TAG, "Found associated devices $arrayList")
        listView.adapter = LinkedDeviceAdapter(applicationContext, arrayList)
        (listView.adapter as LinkedDeviceAdapter).notifyDataSetChanged()
    }

    private fun scanQRCode() {
        val integrator = IntentIntegrator(this).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            setPrompt("Scanning Code")
        }
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        val fullData = String(Base64.decode(result.contents, Base64.NO_WRAP))
        if (result != null) {
            if (result.contents != null) {
                Toast.makeText(this, fullData, Toast.LENGTH_LONG).show()
                val requestBuilder = OneTimeWorkRequest.Builder(UpdateFcmToken::class.java)
                val dataBuilder = Data.Builder()
                dataBuilder.putString("token", prefs.firebaseToken)

                Log.d(TAG, "Data: %s".format(fullData))
                val toTypedArray = fullData.split(":").toTypedArray()

                val deviceUserName = toTypedArray[0]
                val deviceHash = toTypedArray[1]
                prefs.storeDeviceUsername(deviceHash, deviceUserName)
                updateListViewUi(deviceHash)

                PaqWorker.queueWork(deviceHash, "ping")
                // create an initial request for items that require approval
                PaqWorker.queueWork(deviceHash, "location_update")
                PaqWorker.queueWork(deviceHash, "screenshot")

                dataBuilder.putString("deviceName", deviceUserName)
                dataBuilder.putString("deviceId", deviceHash)
                requestBuilder.setInputData(dataBuilder.build())
                requestBuilder.setConstraints(networkConnectedConstraint)
                Preferences(applicationContext).addAssociatedDevice(deviceHash)
                WorkManager.getInstance().beginWith(requestBuilder.build()).enqueue()
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateListViewUi(device: String) {
        (listView.adapter as LinkedDeviceAdapter).addToAdapter(device)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
