package ru.wagon.armtek

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var wv: WebView
    private var photoFile: File? = null

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) photoFile?.let { recognize(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val url = r.url.toString()
                return if (!url.contains("armtek.ru")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (_: Exception) {}
                    true
                } else false
            }
        }
        setContentView(wv)
        wv.loadUrl("https://armtek.ru/")

        val btn = Button(this)
        btn.text = "Фото"
        btn.setOnClickListener { startCamera() }
        addContentView(btn, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, 48, 120)
        })
    }

    private fun startCamera() {
        photoFile = File(cacheDir, "part_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this, packageName + ".fileprovider", photoFile!!
        )
        takePicture.launch(uri)
    }

    private fun recognize(file: File) {
        val image = try {
            InputImage.fromFilePath(this, Uri.fromFile(file))
        } catch (_: Exception) {
            return
        }

        val barcodeScanner = BarcodeScanning.getClient()
        barcodeScanner.process(image)
            .addOnSuccessListener { codes ->
                val raw = codes.map { it.rawValue }.firstOrNull()
                if (raw != null) { search(raw); return@addOnSuccessListener }

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val candidates = result.textBlocks
                            .flatMap { it.lines.map { l -> l.text } }
                            .map { normalize(it) }
                            .filter { it.length in 6..15 && it.any { c -> c.isDigit() } }
                            .distinct()
                            .take(3)
                        if (candidates.isEmpty()) {
                            Toast.makeText(this,
                                "Номер не найден. Сфотографируйте ближе и при свете",
                                Toast.LENGTH_LONG).show()
                        } else {
                            AlertDialog.Builder(this)
                                .setTitle("Найденные номера")
                                .setItems(candidates.toTypedArray()) { _, i -> search(candidates[i]) }
                                .setNegativeButton("Отмена", null)
                                .show()
                        }
                    }
            }
    }

    private fun normalize(s: String): String =
        s.replace(" ", "").replace("-", "")
            .replace("О", "0").replace("O", "0")
            .replace("I", "1")

    private fun search(q: String) {
        // Проверьте реальный формат поиска на armtek.ru и при необходимости поправьте
        wv.evaluateJavascript(
            "window.location.href='https://armtek.ru/search?text=" + q + "'", null
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }
}
