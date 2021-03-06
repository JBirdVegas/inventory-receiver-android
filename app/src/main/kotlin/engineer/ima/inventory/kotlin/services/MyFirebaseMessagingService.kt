package engineer.ima.inventory.kotlin.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.activities.DeviceHistoryActivity
import engineer.ima.inventory.kotlin.activities.MainActivity
import engineer.ima.inventory.kotlin.helpers.INet
import engineer.ima.inventory.kotlin.structures.ActionReply
import engineer.ima.inventory.kotlin.structures.ActionVersionCheck
import engineer.ima.inventory.kotlin.structures.DeviceFileUpload
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import engineer.ima.inventory.kotlin.workers.UpdateFcmToken
import java.io.File
import java.util.*


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received!")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Discovered incoming data: ${remoteMessage.data}")
            val data = remoteMessage.data["data"]
            val action = remoteMessage.data["action"]
            val deviceId = remoteMessage.data["deviceId"]
            when (action) {
                ACTION_DEVICE_CHECKIN -> {
                    val deviceStructure = Gson().fromJson(data, DeviceStructure::class.java)
                    Preferences(applicationContext).storeDeviceCheckin(deviceStructure)
                    sendNotification(deviceStructure)
                }
                ACTION_DEVICE_REPLY -> {
                    val reply = Gson().fromJson(data!!, ActionReply::class.java)
                    Log.d(TAG, "Received data wrapper: $reply")
                    reply.requestedAction?.deviceId = deviceId
                    Preferences(applicationContext).storeActionReply(reply)
                    sendDeviceReply(reply)
                }
                ACTION_DEVICE_FILE_UPLOAD -> {
                    Log.d(TAG, "Received: $data")
                    val fileUpload = Gson().fromJson(data, DeviceFileUpload::class.java)
                    Preferences(applicationContext).storeDeviceFileUpload(fileUpload)
                    sendDeviceFileUpload(action, fileUpload)
                }
                else -> {
                    Log.e(TAG, "Unhandled action type: $action")
                }
            }

            notifyUiOfUpdate()
        }
    }

    private fun notifyUiOfUpdate() {
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(DeviceHistoryActivity.MyReceiver.ACTION_UPDATE))
    }

    private fun sendDeviceFileUpload(action: String, fileUpload: DeviceFileUpload) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val channelId = "actions-$action-${fileUpload.deviceId}"

        val downloaded = INet.download(fileUpload.downloadUrl!!)

        val file = File(fileUpload.storedPath!!)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setAutoCancel(true)
                .setNotificationSilent()
                .setContentIntent(pendingIntent)

        if (file.extension == "png" || file.extension == "jpg") {
            val b = BitmapFactory.decodeByteArray(downloaded, 0, downloaded.size, BitmapFactory.Options())
            notificationBuilder.setContentTitle("Device uploaded image: ${file.name}")
                    .setStyle(NotificationCompat.BigPictureStyle()
                            .setBigContentTitle("Uploaded image: ${file.name}")
                            .bigPicture(b))
        } else {
            notificationBuilder.setContentTitle("Device uploaded file: ${file.name}")
        }


        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "User requested action responses",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = Preferences(applicationContext).getNotificationId(channelId)
        notificationManager.notify(notificationId, notificationBuilder?.build())
    }

    private fun sendDeviceReply(reply: ActionReply) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val channelId = "actions-${reply.requestedAction?.action}-${reply.requestedAction?.deviceId}"

        val timeDiff = (Calendar.getInstance().time.time / 1000) - reply.startTime?.toLong()!!
        Log.d(TAG, "timeDiff: ${Calendar.getInstance().time.time / 1000} - ${reply.startTime.toLong()} = $timeDiff")


        var title = ""
        var contentText = ""
        when (reply.requestedAction?.action) {
            Preferences.pingReplyKey -> {
                title = getString(R.string.notification_ping_reply_title)
                contentText = getString(R.string.notification_ping_reply_body).format(reply.data!!["ping"], timeDiff)
            }
            Preferences.versionCheckReplyKey -> {
                val gson = Gson()
                val check = gson.fromJson(gson.toJson(reply.data), ActionVersionCheck::class.java)
                title = getString(R.string.notification_version_check_reply_title)
                contentText = if (check.current == check.latest) {
                    getString(R.string.notification_version_check_reply_body_client_up_to_date).format(check.current)
                } else {
                    getString(R.string.notification_version_check_reply_body_client_should_update).format(check.current, check.latest)
                }
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setNotificationSilent()
                .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "User requested action responses",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = Preferences(applicationContext).getNotificationId(channelId)
        notificationManager.notify(notificationId, notificationBuilder?.build())
    }

    private fun sendNotification(deviceStructure: DeviceStructure) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val ssid = deviceStructure.deviceCheckin?.currentAccessPoint?.ssid
        val userName = deviceStructure.deviceIds?.userIds?.userId
        val options = BitmapFactory.Options()
        deviceStructure.deviceCheckin?.location?.mapUrl?.let {


            val download = File(deviceStructure.deviceCheckin?.location?.storedPath!!)
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
        requestBuilder.setInputData(dataBuilder.build())
        val work = requestBuilder.build()
        WorkManager.getInstance().beginWith(work).enqueue()
    }

    companion object {
        private val TAG = MyFirebaseMessagingService::class.java.simpleName
        val ACTION_DEVICE_CHECKIN = "device-checkin"
        val ACTION_DEVICE_REPLY = "paq-device-reply"
        val ACTION_DEVICE_FILE_UPLOAD = "device-file-upload"
    }
}
