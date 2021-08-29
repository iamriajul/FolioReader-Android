package com.folioreader.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaActionSound
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ScreenShortUtil {
    private var activity: Activity? = null
    private var context: Context? = null
    private val LOG_TAG = "Screenshot"
    private var bitmapBackup: Bitmap? = null
    private var linearLayout: LinearLayout? = null
    private var imageView: ImageView? = null
    private var takeSound: MediaActionSound? = null
    private var onResultListener: OnResultListener? = null
    private val androidUIHandler: Handler = Handler(Looper.getMainLooper())
    private var packageManager: PackageManager? = null

    private var fileName: String? = null
    private var preview = false
    private var notification = false
    private var notificationTitle: String? = null
    private var notificationShareTitle: String? = null
    private var notificationBigStyle = false
    private var notificationButton = false
    private var filePathBackup: String? = null
    private var fileNameBackup: String? = null
    private var dimAmount = 0f
    private var currentPackageName: String? = null
    private var readPermission = 0
    private var writePermission = 0

    constructor(context: Context?) {
        this.context = context
        activity = context as Activity?
        initialize()
    }

    private fun initialize() {
        fileName = "Screenshot.png"
        preview = true
        notification = true
        notificationTitle = "Screenshot.."
        notificationShareTitle = "Share"
        notificationBigStyle = false
        notificationButton = true
        dimAmount = 0.5f
        packageManager = context?.packageManager
        currentPackageName = context?.packageName.toString()
        readPermission = packageManager!!.checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            currentPackageName!!
        )
        writePermission = packageManager!!.checkPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            currentPackageName!!
        )
        takeSound = MediaActionSound()
        takeSound!!.load(MediaActionSound.SHUTTER_CLICK)
    }

    fun getFileName(): String {
        return "DailyIslam_${
            SimpleDateFormat(
                "yyyy MM dd _ hh mm ss",
                Locale.getDefault()
            ).format(Date()).replace(" ", "")
        }"
    }

    fun takeScreenshot() {
        fileName = getFileName()
        val view = activity!!.window.decorView.rootView
        take(view)
    }

    fun takeScreenshotFromView(view: View) {
        take(view)
    }

    private fun take(view: View) {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                androidUIHandler.post(Runnable {
                    view.isDrawingCacheEnabled = true
                    view.buildDrawingCache(true)
                    val bitmap = Bitmap.createBitmap(view.drawingCache)
                    bitmapBackup = bitmap
                    view.isDrawingCacheEnabled = false
                    saveUtil(bitmap)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(filePathBackup),
                        arrayOf("image/*"),
                        null
                    )
                    timer.cancel()
                })
            }
        }, 75)
    }

    interface OnResultListener {
        fun result(success: Boolean, filePath: String?, bitmap: Bitmap?)
    }

    fun setCallback(listener: OnResultListener?) {
        onResultListener = listener
    }

    private fun saveUtil(bitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
        val fileOutputStream: FileOutputStream
        var image: File? = null
        try {
            image = File(Environment.getExternalStorageDirectory().toString() + "/" + fileName)
            fileOutputStream = FileOutputStream(image)
            fileOutputStream.write(byteArrayOutputStream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "" + e)
            if (onResultListener != null) {
                onResultListener!!.result(false, "" + e, null)
            }
        } finally {
            filePathBackup = if (image != null) image.absolutePath else "There was a problem"
            fileNameBackup = if (image != null) image.name else "There was a problem"
            //image is null should never happen
            takeSound!!.play(MediaActionSound.SHUTTER_CLICK)
            if (preview) {
                preview()
            }
            if (onResultListener != null) {
                onResultListener!!.result(true, filePathBackup, bitmapBackup)
            }
        }
    }

    fun showPreview(enabled: Boolean) {
        preview = enabled
    }

    fun showPreview(): Boolean {
        return preview
    }

    fun showNotification(enabled: Boolean) {
        notification = enabled
    }

    fun showNotification(): Boolean {
        return notification
    }

    fun notificationTitle(title: String?) {
        notificationTitle = title
    }

    fun notificationTitle(): String? {
        return notificationTitle
    }

    fun notificationShareTitle(title: String?) {
        notificationShareTitle = title
    }

    fun notificationShareTitle(): String? {
        return notificationShareTitle
    }

    fun notificationBigStyle(enabled: Boolean) {
        notificationBigStyle = enabled
    }

    fun notificationBigStyle(): Boolean {
        return notificationBigStyle
    }

    fun notificationShareButton(enabled: Boolean) {
        notificationButton = enabled
    }

    fun notificationShareButton(): Boolean {
        return notificationButton
    }

    fun allowScreenshots(enabled: Boolean) {
        if (enabled) {
            activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity!!.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    fun setDimAmount(amount: Float) {
        dimAmount = amount
    }

    fun getDimAmount(): Float {
        return dimAmount
    }

    private fun preview() {
        val alert: AlertDialog.Builder = AlertDialog.Builder(activity!!)
        linearLayout = LinearLayout(context)
        imageView = ImageView(activity)
        linearLayout!!.orientation = LinearLayout.VERTICAL
        val display =
            (context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        if (display.rotation == Surface.ROTATION_0 || display.rotation == Surface.ROTATION_180) {
            //for portrait mode
            linearLayout!!.setPadding(15, 19, 15, 19)
        } else {
            //for landscape mode
            linearLayout!!.setPadding(27, 0, 27, 0)
        }
        linearLayout!!.setBackgroundColor(-1) //white
        imageView!!.setImageBitmap(bitmapBackup)
        linearLayout!!.addView(imageView)
        alert.setView(linearLayout)
        alert.setCancelable(false)
        val dialog: AlertDialog = alert.create()
        if (dialog.window != null) {
            dialog.window!!.setDimAmount(dimAmount)
        }
        dialog.show()
        val metrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(metrics)
        val height = (metrics.heightPixels * 0.86).toInt()
        val width = (metrics.widthPixels * 0.85).toInt()
        dialog.window!!.setLayout(width, height)
        val t = Timer()
        t.schedule(object : TimerTask() {
            override fun run() {
                if (dialog.isShowing()) {
                    dialog.dismiss()
                    t.cancel()
                }
            }
        }, 1250)
    }

}