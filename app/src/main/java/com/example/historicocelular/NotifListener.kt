package com.example.historicocelular

import android.app.Notification
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotifListener : NotificationListenerService() {

    private val client = OkHttpClient()
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("config", MODE_PRIVATE)
        Log.d("NotifListener", "Servico iniciado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val url = prefs.getString("url", "") ?: return
            if (url.isEmpty()) return

            val pacote = sbn.packageName ?: ""
            if (pacote == "android" || pacote == packageName) return

            val extras = sbn.notification?.extras ?: return
            val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subtexto = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val remetente = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString() ?: ""

            if (titulo.isEmpty() && texto.isEmpty()) return

            val json = JSONObject().apply {
                put("pacote", pacote)
                put("titulo", titulo)
                put("texto", if (texto.isNotEmpty()) texto else subtexto)
                put("remetente", remetente)
                put("ts", sbn.postTime)
            }

            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.e("NotifListener", "Falha: ${e.message}")
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (e: Exception) {
            Log.e("NotifListener", "Erro: ${e.message}")
        }
    }
}
