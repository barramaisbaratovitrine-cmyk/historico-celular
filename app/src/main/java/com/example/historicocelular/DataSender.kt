package com.example.historicocelular

import android.content.Context
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object DataSender {
    private val client = OkHttpClient()

    fun postar(url: String, endpoint: String, json: JSONObject) {
        try {
            val fullUrl = url.trimEnd('/') + "/api/" + endpoint
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder().url(fullUrl).post(body).build()
            client.newCall(req).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (e: Exception) {}
    }

    fun testarConexao(ctx: Context, url: String) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("pacote", "teste.app")
                    put("titulo", "Teste")
                    put("texto", "Conexao OK!")
                }
                val fullUrl = url.trimEnd('/') + "/api/notificacao"
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(fullUrl).post(body).build()
                val resp = client.newCall(req).execute()
                val code = resp.code
                resp.close()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "Status: $code", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
