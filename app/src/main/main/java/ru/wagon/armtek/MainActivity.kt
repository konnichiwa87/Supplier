class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            val t = TextView(this)
            t.text = "Запуск ОК"
            t.setPadding(40, 40, 40, 40)
            setContentView(t)

        } catch (e: Throwable) {
            val tv = TextView(this)
            tv.text = android.util.Log.getStackTraceString(e)
            tv.setPadding(40, 40, 40, 40)
            tv.textSize = 12f
            tv.movementMethod = android.text.method.ScrollingMovementMethod()
            setContentView(tv)
        }
    }
}
