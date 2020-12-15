package engineer.ima.inventory.kotlin.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.activities.MainActivity
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import engineer.ima.inventory.kotlin.workers.UpdateFcmToken
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatterBuilder


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data["data"]
            val deviceStructure = Gson().fromJson(data, DeviceStructure::class.java)
            Preferences(applicationContext).storeDeviceCheckin(deviceStructure)
            sendNotification(deviceStructure)
        }
    }

    private fun sendNotification(deviceStructure: DeviceStructure) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val parsed = DateTimeFormatterBuilder()
                .append(ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffset("+HH:MM", "Z")
                .optionalEnd()
                .toFormatter()
                .parse(deviceStructure.deviceCheckin?.lastUpdated)

        val ofPattern = DateTimeFormatter.ofPattern("EEE, MMM dd'%s' 'at' h:m a")
        val from = ZonedDateTime.from(parsed)
        val zonedDateTime = from.format(ofPattern).format(getDayOfMonthSuffix(from.dayOfWeek.value))

        val ssid = deviceStructure.deviceCheckin?.currentAccessPoint?.ssid
        val userName = deviceStructure.deviceIds?.userIds?.userId
        val format = "Last Seen: %s\nPublic: %s\nLocal: %s\nWiFi AP: %s\nAddress: %s".format(zonedDateTime,
                deviceStructure.deviceCheckin?.public,
                deviceStructure.deviceCheckin?.local,
                ssid,
                deviceStructure.deviceCheckin?.location?.address)

        val options = BitmapFactory.Options()
        deviceStructure.deviceCheckin?.location?.mapUrl?.let {


            val download = File(deviceStructure.deviceCheckin?.location?.mapUri!!)
            val readBytes = download.readBytes()
            val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, options)

            val notificationBuilder = deviceStructure.deviceIdsUniqueHash?.let { hash ->
                NotificationCompat.Builder(this, hash)
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle("%s's laptop checked in".format(userName))
                        .setContentText("Connected to %s".format(ssid))
                        .setStyle(NotificationCompat.BigPictureStyle()
                                .setBigContentTitle("%s checkin via wifi ap: %s".format(userName, ssid))
                                .setSummaryText(deviceStructure.deviceCheckin?.location?.address)
                                .bigPicture(b))
                        .setAutoCancel(true)
                        .setNotificationSilent()
                        .setContentIntent(pendingIntent)
            }

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(deviceStructure.deviceIdsUniqueHash,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            deviceStructure.deviceIdsUniqueHash?.let {
                val notificationId = Preferences(applicationContext).getNotificationId(it)
                notificationManager.notify(notificationId, notificationBuilder?.build())
            }
        }
    }

    override fun onNewToken(token: String) {
        sendToServerJob(token)
    }

    private fun getDayOfMonthSuffix(n: Int): String {
        return if (n in 11..13) {
            "th"
        } else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    private fun sendToServerJob(token: String) {
        val requestBuilder = OneTimeWorkRequest.Builder(UpdateFcmToken::class.java)
        val dataBuilder = Data.Builder()
        dataBuilder.putString("token", token)
        dataBuilder.putString("deviceId", staticDevice)
        requestBuilder.setInputData(dataBuilder.build())
        val work = requestBuilder.build()
        WorkManager.getInstance().beginWith(work).enqueue()
    }

    companion object {
        const val staticDevice = "04ae3e47a6e59f26e6e0ed95da2ef4483e5e651dc1e90ae840911cda36aecbd7"
        private const val TAG = "MyFirebaseMsgService"
    }
}
