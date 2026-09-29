package com.example.historicocelular

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.okhttp3.MediaType.Companion.toMediaType
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import com.squareup.okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val urlSalva = prefs.getString("url", "http://100.80.79.104:4000/api/notificacao") ?: ""

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 200, 50, 50)
        }

        val titulo = android.widget.TextView(this).apply {
            text = "Historico Celular\n\n1) Salve a URL\n2) Ative acesso a notificacoes\n3) Teste a conexao"
            textSize = 15f
        }

        val campoUrl = android.widget.EditText(this).apply {
            hint = "URL do servidor"
            setText(urlSalva)
        }

        val btnSalvar = android.widget.Button(this).apply {
            text = "1. Salvar URL"
            setOnClickListener {
                prefs.edit().putString("url", campoUrl.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "URL salva!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnPermissao = android.widget.Button(this).apply {
            text = "2. Ativar acesso a notificacoes"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val btnTeste = android.widget.Button(this).apply {
            text = "3. Testar conexao"
            setOnClickListener {
                val url = campoUrl.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Preencha a URL", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Thread {
                    try {
                        val client = OkHttpClient()
                        val json = """{"pacote":"teste.app","titulo":"Teste","texto":"Se você está lendo isso, funcionou!","remetente":"Sistema"}"""
                        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(url).post(body).build()
                        val resp = client.newCall(req).execute()
                        val code = resp.code
                        resp.close()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(this@MainActivity, "Status: $code", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }

        layout.addView(titulo)
        layout.addView(campoUrl)
        layout.addView(btnSalvar)
        layout.addView(btnPermissao)
        layout.addView(btnTeste)

        setContentView(layout)
    }
}
