package engineer.ima.inventory.kotlin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import engineer.ima.inventory.kotlin.helpers.INet
import engineer.ima.inventory.kotlin.structures.ActionReply
import engineer.ima.inventory.kotlin.structures.DeviceFileUpload
import engineer.ima.inventory.kotlin.structures.DeviceItem
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.SecureRandom
import java.util.*

class Preferences(context: Context) {
    private val prefsFileName = "engineer.ima.inventory.kotlin.Preferences"
    private val fireBaseTokenKey = "firebase-token"
    private val associatedDevicesKey = "associated-devices"
    private val usernameTokenKey = "%s-username"
    private val notificationIdKey = "%s-notification-id"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFileName, 0)
    private var mContext = context
    val gson = Gson()

    private fun readAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)

    var firebaseToken: String?
        get() = prefs.getString(fireBaseTokenKey, null)
        set(value) = prefs.edit().putString(fireBaseTokenKey, value).apply()

    var associatedDevices: MutableSet<String>
        get() {
            return prefs.getStringSet(associatedDevicesKey, mutableSetOf())!!
        }
        private set(_) {}

    fun storeDeviceCheckin(checkin: DeviceStructure) {
        val deviceDir = File(mContext.filesDir, checkin.deviceIdsUniqueHash!!)
        deviceDir.mkdirs()

        val png = File(deviceDir, "%s-%s.png".format(deviceCheckinKey, checkin.deviceCheckin?.lastUpdated!!))
        val json = File(deviceDir, "%s-%s.json".format(deviceCheckinKey, checkin.deviceCheckin?.lastUpdated!!))
        checkin.deviceCheckin?.location?.storedPath = png.absolutePath
        png.writeBytes(INet.download(checkin.deviceCheckin?.location?.mapUrl!!))
        json.writeBytes(gson.toJson(checkin).toByteArray(Charset.defaultCharset()))
    }

    fun storeActionReply(reply: ActionReply) {
        val deviceDir = File(mContext.filesDir, reply.requestedAction?.deviceId!!)
        deviceDir.mkdirs()
        val json = File(deviceDir, "%s-%s.json".format(reply.requestedAction.action, reply.endTime))
        reply.storedPath = json.absolutePath
        json.writeBytes(gson.toJson(reply).toByteArray(Charset.defaultCharset()))
    }

    fun storeDeviceFileUpload(fileUpload: DeviceFileUpload) {
        val deviceDir = File(mContext.filesDir, fileUpload.deviceId!!)
        deviceDir.mkdirs()

        val png = File(deviceDir, "%s-%s.png".format(fileUploadKey, fileUpload.uploadTime))
        val json = File(deviceDir, "%s-%s.json".format(fileUploadKey, fileUpload.uploadTime))
        fileUpload.storedPath = png.absolutePath
        png.writeBytes(INet.download(fileUpload.downloadUrl!!))
        json.writeBytes(gson.toJson(fileUpload).toByteArray(Charset.defaultCharset()))
    }

    fun getFileCreationEpoch(file: File): Long {
        return try {
            val attr: BasicFileAttributes = Files.readAttributes(file.toPath(),
                    BasicFileAttributes::class.java)
            attr.creationTime()
                    .toInstant().toEpochMilli()
        } catch (e: IOException) {
            throw RuntimeException(file.absolutePath, e)
        }
    }

    fun getDeviceCheckins(deviceId: String): List<DeviceItem> {
        val deviceDir = File(mContext.filesDir, deviceId)
        if (!deviceDir.exists()) {
            deviceDir.mkdirs()
        }
        Log.d(TAG, "Looking for checkins: %s in dir: %s, exists? %s".format(deviceId, deviceDir.absolutePath, deviceDir.exists()))
        val listFiles = deviceDir.listFiles()
        Arrays.sort(listFiles, Comparator.comparing { obj: File ->
            getFileCreationEpoch(obj)
        }.reversed())

        val checkins = arrayListOf<DeviceItem>()
        for (file in listFiles) {
            if (file.isFile && file.extension == "json") {
                val text = readAsText(file.absolutePath)
                when {
                    deviceCheckinKey in file.name -> {
                        checkins.add(gson.fromJson(text, DeviceStructure::class.java))
                    }
                    pingReplyKey in file.name || versionCheckReplyKey in file.name -> {
                        checkins.add(gson.fromJson(text, ActionReply::class.java))
                    }
                    fileUploadKey in file.name -> {
                        checkins.add(gson.fromJson(text, DeviceFileUpload::class.java))
                    }
                    else -> Log.d(TAG, "Error unhandled file type: ${file.name}")
                }
            }
        }
        return checkins.toTypedArray().toList()
    }

    fun getLatestDeviceCheckin(deviceId: String): DeviceStructure {
        val deviceDir = File(mContext.filesDir, deviceId)
        Log.d(TAG, "Looking for latest checkins in: %s".format(deviceDir.absolutePath))
        val latest = findLatest(deviceDir.absolutePath)
        val data = readFileAsTextUsingInputStream(latest?.toFile()?.absolutePath!!)
        return gson.fromJson(data, DeviceStructure::class.java)
    }

    private fun readFileAsTextUsingInputStream(fileName: String) =
            File(fileName).inputStream().readBytes().toString(Charsets.UTF_8)

    private fun findLatest(sdir: String?): Path? {
        val dir: Path = Paths.get(sdir)
        if (Files.isDirectory(dir)) {
            val opPath: Optional<Path> = Files.list(dir)
                    .filter { p -> !Files.isDirectory(p) && p.toFile().extension != "png" }
                    .sorted { p1, p2 ->
                        java.lang.Long.valueOf(p2.toFile().lastModified())
                                .compareTo(p1.toFile().lastModified())
                    }
                    .findFirst()
            if (opPath.isPresent) {
                return opPath.get()
            }
        }
        return null
    }

    fun addAssociatedDevice(deviceId: String): Boolean {
        if (!associatedDevices.contains(deviceId)) {
            val mutableSetOf = mutableSetOf(deviceId)
            for (dev: String in associatedDevices) {
                mutableSetOf.add(dev)
            }
            Log.d(TAG, "Storing device list: %s".format(mutableSetOf))
            prefs.edit().putStringSet(associatedDevicesKey, mutableSetOf).apply()
            return true
        }
        return false
    }

    fun storeDeviceUsername(deviceToken: String, username: String) {
        val format = usernameTokenKey.format(deviceToken)
        Log.d(TAG, "Storing username: $username, deviceToken: $format")
        prefs.edit().putString(format, username).apply()
    }

    fun getDeviceUsername(deviceToken: String): String? {
        return prefs.getString(usernameTokenKey.format(deviceToken), "NOT FOUND: %s".format(deviceToken))
    }

    fun getNotificationId(deviceToken: String): Int {
        val int = prefs.getInt(notificationIdKey.format(deviceToken), 0)
        if (int != 0) {
            return int
        }
        val instanceStrong = SecureRandom.getInstanceStrong()
        val nextInt = instanceStrong.nextInt()
        prefs.edit()
                .putInt(notificationIdKey.format(deviceToken), nextInt)
                .apply()
        return nextInt
    }

    companion object {
        val TAG: String = Preferences::class.java.simpleName
        const val deviceCheckinKey = "device-checkin"
        const val pingReplyKey = "ping"
        const val versionCheckReplyKey = "version-check"
        const val fileUploadKey = "file-upload"
        const val locationUpdate = "location-update"
        const val playSound = "play-sound"
        const val screenshot = "screenshot"
    }
}