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
import engineer.ima.inventory.kotlin.structures.*
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
        const val DEVICE_ID = "extras.DEVICE_ID"
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
        listView?.adapter = HistoryAdapter(arrayList.toMutableList(), windowManager)

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

    class HistoryAdapter(private val ourList: MutableList<DeviceItem>, private val windowManager: WindowManager) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        val width: Int = getWidthForDisplay() / 8
        val sdf = SimpleDateFormat("E, MMM dd yy hh:mm:ss z", Locale.getDefault())

        fun getWidthForDisplay(): Int {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.device_history_listview_item,
                    parent,
                    false))
        }

        fun add(items: List<DeviceItem>) {
            for (i in items) {
                ourList.add(0, i)
            }
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Log.d(TAG, "Item: ${ourList[position]}")
            when (val item = ourList[position]) {
                is DeviceStructure -> {
                    updateViewForDeviceStructure(holder, item)
                }
                is DeviceFileUpload -> {
                    updateViewForFileUpload(width, holder, item)
                }
                is ActionReply -> {
                    updateViewForActionReply(holder, item)
                }
                else -> {
                    Log.d(TAG, "UNHANDLED: $item")
                }
            }
        }

        private fun decodeSampledBitmapFromItem(
                item: DeviceItemStorable,
                reqWidth: Int,
                reqHeight: Int
        ): Bitmap {
            return BitmapFactory.Options().run {
                val readBytes = File(item.storedPath!!).readBytes()
                Log.d(TAG, "Read bytes: ${readBytes.size}")
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, this)
            }
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        private fun updateViewForFileUpload(width: Int, holder: ViewHolder, item: DeviceFileUpload) {
            if (item.storedPath != null) {
                holder.image.setImageBitmap(decodeSampledBitmapFromItem(item, width, width))
            } else {
                holder.image.setImageBitmap(null)
            }
            holder.title.text = "Screenshot ${item.fileName}"
            holder.detail.text = ""
            val date = Date(item.uploadTime!!.toLong() * 1000)
            holder.date.text = sdf.format(date)
        }

        private fun updateViewForActionReply(holder: ViewHolder, actionReply: ActionReply) {
            var replyText = ""
            if (actionReply.requestedAction!!.action == Preferences.pingReplyKey) {
                replyText = actionReply.data!!.getValue("ping")
            } else if (actionReply.requestedAction.action == Preferences.versionCheckReplyKey) {
                replyText = "\nCurrent version: ${actionReply.data!!["current"]}\nLatest version: ${actionReply.data["latest"]}"
            }

            holder.title.text = "${actionReply.requestedAction?.action} replied with $replyText"
            actionReply.startTime.let { st ->
                actionReply.endTime.let { et ->
                    holder.detail.text = "Reply took ${et!!.toLong() - st!!.toLong()}"
                }
            }
            holder.date.text = actionReply.endTime
            holder.image.setImageBitmap(null)
        }

        private fun updateViewForDeviceStructure(holder: ViewHolder, item: DeviceStructure) {
            item.storedPath = item.deviceCheckin?.location?.storedPath
            holder.title.text = item.deviceCheckin?.location?.address
            holder.detail.text = item.deviceCheckin?.currentAccessPoint?.ssid
            holder.date.text = TimeHelper.format(item.deviceCheckin?.lastUpdated!!)

            if (item.deviceCheckin?.location?.storedPath != null) {
                val file = File(item.deviceCheckin?.location?.storedPath!!)
                if (file.exists()) {
                    holder.image.setImageBitmap(decodeSampledBitmapFromItem(item, width, width))
                } else {
                    holder.image.setImageBitmap(null)
                }
            } else {
                holder.image.setImageBitmap(null)
            }
        }

        override fun getItemCount(): Int = ourList.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.list_item_title)
            val detail: TextView = view.findViewById(R.id.list_item_detail)
            val date: TextView = view.findViewById(R.id.list_item_date)
            val image: ImageView = view.findViewById(R.id.list_item_image)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ping_device -> {
                PaqWorker.queueWork(deviceId, Preferences.pingReplyKey)
                true
            }
            R.id.action_request_location_update -> {
                PaqWorker.queueWork(deviceId, Preferences.locationUpdate)
                true
            }
            R.id.action_request_device_beep -> {
                PaqWorker.queueWork(deviceId, Preferences.playSound)
                true
            }
            R.id.action_request_device_screenshot -> {
                PaqWorker.queueWork(deviceId, Preferences.screenshot)
                true
            }
            R.id.action_request_device_version_check -> {
                PaqWorker.queueWork(deviceId, Preferences.versionCheckReplyKey)
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