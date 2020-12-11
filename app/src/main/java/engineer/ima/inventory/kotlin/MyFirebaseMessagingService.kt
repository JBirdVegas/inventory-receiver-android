package engineer.ima.inventory.kotlin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import engineer.ima.inventory.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatterBuilder

data class AccessPoint(
        var ssid: String? = null,
        var bssid: String? = null,
        @SerializedName("link_auth")
        var linkAuth: String? = null,
        var channels: List<String>? = null
)

data class DeviceCheckin(
        var local: String? = null,
        var public: String? = null,
        @SerializedName("last_updated")
        var lastUpdated: String? = null,
        @SerializedName("current_access_point")
        var currentAccessPoint: AccessPoint? = null
)

data class KernelIds(
        @SerializedName("kernel_host_name")
        var kernelHostname: String? = null,
        @SerializedName("kernel_uuid")
        var kernelUuid: String? = null
)

data class HardwareIds(
        @SerializedName("hardware_model")
        var hardwareModel: String? = null,
        @SerializedName("cpu_id")
        var cpuId: String? = null
)

data class UserIds(
        @SerializedName("user_id")
        var userId: String? = null
)

data class DeviceId(
        @SerializedName("kernel_ids")
        var kernelID: KernelIds? = null,
        @SerializedName("hardware_ids")
        var hardwareIds: HardwareIds? = null,
        @SerializedName("user_ids")
        var userIds: UserIds? = null
)

data class DeviceStructure(
        @SerializedName("device_ids")
        var deviceIds: DeviceId? = null,
        @SerializedName("device_ids_unique_hash")
        var deviceIdsUniqueHash: String? = null,
        @SerializedName("device_checkin")
        var deviceCheckin: DeviceCheckin? = null
)


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val s = remoteMessage.data["data"]
            Log.d(TAG, "Received data: %s".format(s))
            val ipAddrs = Gson().fromJson(s, DeviceStructure::class.java)
            Log.d(TAG, "Parsed: %s".format(ipAddrs.toString()))
            sendNotification(ipAddrs)
        }
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    private fun sendNotification(deviceStructure: DeviceStructure) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

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

        val format = "Last Seen: %s\nPublic: %s\nLocal: %s\nWiFi AP: %s".format(zonedDateTime,
                deviceStructure.deviceCheckin?.public,
                deviceStructure.deviceCheckin?.local,
                deviceStructure.deviceCheckin?.currentAccessPoint?.ssid)
        val notificationBuilder = deviceStructure.deviceIdsUniqueHash?.let {
            NotificationCompat.Builder(this, it)
                    .setSmallIcon(R.drawable.ic_stat_ic_notification)
                    .setContentTitle("%s's laptop checked in".format(deviceStructure.deviceIds?.userIds?.userId))
                    .setContentText("Connected to %s".format(deviceStructure.deviceCheckin?.currentAccessPoint?.ssid))
                    .setStyle(NotificationCompat.BigTextStyle()
                            .bigText(format))
                    .setAutoCancel(true)
                    .setNotificationSilent()
                    .setContentIntent(pendingIntent)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    override fun onNewToken(token: String) {
        sendToServerJob(token)
    }

    fun getDayOfMonthSuffix(n: Int): String {
        return if (n >= 11 && n <= 13) {
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
