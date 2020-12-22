package engineer.ima.inventory.kotlin

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.activities.DeviceHistoryActivity
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


class LinkedDeviceAdapter(appContext: Context,
                          private val arrayList: java.util.ArrayList<String>)
    : ArrayAdapter<String>(appContext, R.layout.linked_device_list_item) {

    fun addAddToAdapter(vararg deviceIds: String) {
        for (id in deviceIds) {
            addToAdapter(id)
        }
    }

    fun addToAdapter(deviceId: String) {
        if (!arrayList.contains(deviceId)) {
            arrayList.add(deviceId)
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): String {
        return arrayList[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        Log.d(TAG, "Getting view for $position, ${arrayList[position]}")
        val v = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.linked_device_list_item, parent, false)
        val prefs = Preferences(context)

        val username: TextView = v.findViewById<View>(R.id.linked_device_username) as TextView
        val lastAddress: TextView = v.findViewById<View>(R.id.linked_device_last_seen_address) as TextView
        val lastTimestamp: TextView = v.findViewById<View>(R.id.linked_device_last_seen_timestamp) as TextView

        lastAddress.setOnClickListener(getClickListener(position))
        lastTimestamp.setOnClickListener(getClickListener(position))
        lastAddress.setOnClickListener(getClickListener(position))
        v.setOnClickListener(getClickListener(position))

        val item = arrayList[position]
        val checkins = prefs.getDeviceCheckins(item)

        Log.d(TAG, "Received item: $item, checkins: $checkins")
        val deviceUsername = prefs.getDeviceUsername(item)

        if (checkins.isEmpty()) {
            lastAddress.text = "Latest address: Unknown"
            lastTimestamp.text = "Last seen: Unknown"
            username.text = "Device user: $deviceUsername"
            return v
        }

        val rfc3339 = DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffset("+HH:MM", "Z")
                .optionalEnd()
                .toFormatter()
//        val zonedDateTime: ZonedDateTime = ZonedDateTime.from(rfc3339.parse(checkins.first().deviceCheckin?.lastUpdated))
//        lastAddress.text = "Latest address: ${checkins.first().deviceCheckin?.location?.name}"
//        lastTimestamp.text = "Last seen: ${DateTimeFormatter.RFC_1123_DATE_TIME.format(zonedDateTime)}"
        username.text = "Device user: $deviceUsername"

        return v
    }

    private fun getClickListener(position: Int): View.OnClickListener {
        return View.OnClickListener {
            val itemAtPosition = arrayList[position]
            val intent = Intent(context, DeviceHistoryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "onClicked on item: %s".format(itemAtPosition))
            intent.putExtra(DeviceHistoryActivity.DEVICE_ID, itemAtPosition)
            context.startActivity(intent)
        }
    }

    companion object {
        val TAG = LinkedDeviceAdapter::class.java.simpleName
    }
}