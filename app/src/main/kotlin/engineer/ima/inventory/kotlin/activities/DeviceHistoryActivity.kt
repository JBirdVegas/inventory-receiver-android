package engineer.ima.inventory.kotlin.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.helpers.TimeHelper
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import engineer.ima.inventory.kotlin.workers.PaqWorker
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.io.File
import java.util.*


class DeviceHistoryActivity : AppCompatActivity() {
    companion object {
        const val DEVICE_ID = "DEVICE_ID"
    }

    private var listView: TimeLineRecyclerView? = null
    private lateinit var deviceId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_history_activity)
        listView = findViewById(R.id.device_history_listview)
        deviceId = intent.extras?.getString(DEVICE_ID)!!

        val arrayList = Preferences(applicationContext).getDeviceCheckins(deviceId)
        listView?.adapter = HistoryAdapter(arrayList)

        listView?.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL,
                false)
        listView?.addItemDecoration(getSectionCallback(arrayList))

    }

    private fun getSectionCallback(arrayList: ArrayList<DeviceStructure>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean = true

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo? {
                val item = arrayList[position]
                val title = "${item.deviceCheckin?.location?.name} @ ${item.deviceCheckin?.lastUpdated}"
                return SectionInfo(title, item.deviceIds?.userIds?.userId)

            }

        }
    }

    class HistoryAdapter(private val ourList: ArrayList<DeviceStructure>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.device_history_listview_item,
                    parent,
                    false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = ourList[position]

            holder.address.text = item.deviceCheckin?.location?.address
            holder.wifiAp.text = item.deviceCheckin?.currentAccessPoint?.ssid
            holder.date.text = TimeHelper.format(item.deviceCheckin?.lastUpdated!!)

            if (item.deviceCheckin?.location?.mapUri != null) {
                val file = File(item.deviceCheckin?.location?.mapUri!!)
                if (file.exists()) {
                    val readBytes = file.readBytes()
                    val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, BitmapFactory.Options())
                    holder.map.setImageBitmap(b)
                } else {
                    holder.map.setImageBitmap(null)
                }
            } else {
                holder.map.setImageBitmap(null)
            }
        }

        override fun getItemCount(): Int = ourList.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val address: TextView = view.findViewById<View>(R.id.list_item_address) as TextView
            val wifiAp: TextView = view.findViewById<View>(R.id.list_item_wifi_ap) as TextView
            val date: TextView = view.findViewById<View>(R.id.list_item_date) as TextView
            val map: ImageView = view.findViewById<View>(R.id.list_item_map) as ImageView
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ping_device -> {
                PaqWorker.queueWork(deviceId, "ping")
                true
            }
            R.id.action_request_location_update -> {
                PaqWorker.queueWork(deviceId, "location_update")
                true
            }
            R.id.action_request_device_beep -> {
                PaqWorker.queueWork(deviceId, "play_sound")
                true
            }
            R.id.action_request_device_screenshot -> {
                PaqWorker.queueWork(deviceId, "screenshot")
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.device_history_menu, menu)
        return true
    }
}