package engineer.ima.inventory.kotlin.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.helpers.TimeHelper
import engineer.ima.inventory.kotlin.structures.DeviceFileUpload
import engineer.ima.inventory.kotlin.structures.DeviceItem
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import engineer.ima.inventory.kotlin.structures.PingReply
import engineer.ima.inventory.kotlin.workers.PaqWorker
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class DeviceHistoryActivity : AppCompatActivity() {
    companion object {
        private val TAG = DeviceHistoryActivity::class.java.simpleName
        const val DEVICE_ID = "DEVICE_ID"
    }

    private var listView: TimeLineRecyclerView? = null
    private lateinit var deviceId: String
    private lateinit var bReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_history_activity)
        listView = findViewById(R.id.device_history_listview)
        deviceId = intent.extras?.getString(DEVICE_ID)!!

        val arrayList = Preferences(applicationContext).getDeviceCheckins(deviceId)
        listView?.adapter = HistoryAdapter(arrayList, windowManager)

        listView?.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL,
                false)
        listView?.addItemDecoration(getSectionCallback(arrayList))

        bReceiver = MyReceiver(listView!!, deviceId)
        val filter = IntentFilter(MyReceiver.ACTION_UPDATE)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(bReceiver, filter)
    }

    private fun getSectionCallback(arrayList: List<DeviceItem>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean = true

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo? {
//                val item = arrayList[position]
//                val title = "${item.deviceCheckin?.location?.name} @ ${item.deviceCheckin?.lastUpdated}"
                return SectionInfo("Test title", "test values")

            }

        }
    }

    class MyReceiver(val listView: TimeLineRecyclerView, val deviceId: String) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            listView.adapter.let {
                Log.d(TAG, "Asking listview to update listview!")
                val checkins = Preferences(listView.context).getDeviceCheckins(deviceId)
                (it as HistoryAdapter).add(checkins)
            }
        }

        companion object {
            val TAG = MyReceiver::class.java.simpleName
            val ACTION_UPDATE = "action-update"
        }
    }

    class HistoryAdapter(private val ourList: List<DeviceItem>, private val windowManager: WindowManager) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.device_history_listview_item,
                    parent,
                    false))
        }

        fun add(items: List<DeviceItem>) {
            for (i in items) {
                ourList.toMutableList().add(i)
            }
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (val item = ourList[position]) {
                is DeviceStructure -> {
                    updateViewForDeviceStructure(holder, item)
                }
                is DeviceFileUpload -> {
                    updateViewForFileUpload(holder, item)
                }
                is PingReply -> {
                    updateViewForPingReply(holder, item)
                }
            }
        }

        private fun resizeBitmap(temp: Bitmap, size: Int): Bitmap? {
            return if (size > 0) {
                val width = temp.width
                val height = temp.height
                val ratioBitmap = width.toFloat() / height.toFloat()
                var finalWidth = size
                var finalHeight = size
                if (ratioBitmap < 1) {
                    finalWidth = (size.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (size.toFloat() / ratioBitmap).toInt()
                }
                Bitmap.createScaledBitmap(temp, finalWidth, finalHeight, true)
            } else {
                temp
            }
        }

        private fun updateViewForFileUpload(holder: ViewHolder, item: DeviceFileUpload) {
            Log.d(TAG, "Got File upload ${item.storedPath}")
            if (item.storedPath != null) {
                val readBytes = File(item.storedPath!!).readBytes()
                val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, BitmapFactory.Options())

                val displayMetrics = DisplayMetrics()

                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val width = displayMetrics.widthPixels

                val scaled = resizeBitmap(b, width)
                holder.image.setImageBitmap(scaled)
            } else {
                holder.image.setImageBitmap(null)
            }
            holder.title.text = "Screenshot ${item.fileName}"
            holder.detail.text = ""
            val date = Date(item.uploadTime!!.toLong() * 1000)

            val sdf = SimpleDateFormat("E, MMM dd yy hh:mm:ss z", Locale.getDefault())

            holder.date.text = sdf.format(date)
        }

        private fun updateViewForPingReply(holder: ViewHolder, pingReply: PingReply) {
            holder.title.text = "Ping got reply ${pingReply.ping}"
            pingReply.startTime.let { st ->
                pingReply.endTime.let { et ->
                    val s = et!!.toLong() - st!!.toLong()
                    holder.detail.text = "Ping reply took $s"
                }
            }

            holder.date.text = pingReply.endTime
            holder.image.setImageBitmap(null)
        }

        private fun updateViewForDeviceStructure(holder: ViewHolder, item: DeviceStructure) {
            holder.title.text = item.deviceCheckin?.location?.address
            holder.detail.text = item.deviceCheckin?.currentAccessPoint?.ssid
            holder.date.text = TimeHelper.format(item.deviceCheckin?.lastUpdated!!)

            if (item.deviceCheckin?.location?.storedPath != null) {
                val file = File(item.deviceCheckin?.location?.storedPath!!)
                if (file.exists()) {
                    val readBytes = file.readBytes()
                    val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, BitmapFactory.Options())
                    holder.image.setImageBitmap(b)
                } else {
                    holder.image.setImageBitmap(null)
                }
            } else {
                holder.image.setImageBitmap(null)
            }
        }

        override fun getItemCount(): Int = ourList.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById<View>(R.id.list_item_title) as TextView
            val detail: TextView = view.findViewById<View>(R.id.list_item_detail) as TextView
            val date: TextView = view.findViewById<View>(R.id.list_item_date) as TextView
            val image: ImageView = view.findViewById<View>(R.id.list_item_image) as ImageView
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

    override fun onPause() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(bReceiver)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.device_history_menu, menu)
        return true
    }
}