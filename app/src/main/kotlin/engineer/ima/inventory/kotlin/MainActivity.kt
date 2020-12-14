package engineer.ima.inventory.kotlin

import android.Manifest
import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.zxing.integration.android.IntentIntegrator
import engineer.ima.inventory.R
import engineer.ima.inventory.databinding.ActivityMainBinding
import engineer.ima.inventory.kotlin.Application.Companion.runningQOrLater

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var listViewAdapter: ArrayAdapter<String>
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        listViewAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, preferences.associatedDevices.toMutableList())
        listView.adapter = listViewAdapter
        listViewAdapter.notifyDataSetChanged()
        println(preferences.associatedDevices)
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
                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val requestBuilder = OneTimeWorkRequest.Builder(UpdateFcmToken::class.java)
                val dataBuilder = Data.Builder()
                dataBuilder.putString("token", configs?.firebaseToken)

                Log.d(TAG, "Data: %s".format(fullData))
                val toTypedArray = fullData.split(":").toTypedArray()

                val deviceUserName = toTypedArray[0]
                val deviceHash = toTypedArray[1]
                configs?.storeDeviceUsername(deviceHash, deviceUserName)

                dataBuilder.putString("deviceName", deviceUserName)
                dataBuilder.putString("deviceId", deviceHash)
                requestBuilder.setInputData(dataBuilder.build())
                requestBuilder.setConstraints(constraints)

                val work = requestBuilder.build()
                WorkManager.getInstance().beginWith(work).enqueue()
                WorkManager.getInstance().getWorkInfoByIdLiveData(work.id).observe(this, Observer { workInfo ->
                    if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                        updateListViewUi(deviceHash)
                    }
                })
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateListViewUi(device: String) {
        val preferences = Preferences(applicationContext)
        val associatedDevices = preferences.associatedDevices.toMutableSet()
        if (listViewAdapter.isEmpty) {
            listViewAdapter.add(device)
            preferences.addAssociatedDevice(device)
            listViewAdapter.notifyDataSetChanged()
        } else {
            var willAdd = true
            for (i in 0 until associatedDevices.count()) {
                if (listViewAdapter.getItem(i) == device) {
                    willAdd = false
                }
            }
            if (willAdd) {
                listViewAdapter.add(device)
                listViewAdapter.notifyDataSetChanged()
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(
                                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                } else {
                    true
                }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
