package com.umnicode.samp_launcher.ui.home

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.ServerConfig
import com.umnicode.samp_launcher.core.ServerResolveCallback
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class HomeFragment : Fragment() {

    private lateinit var tvNickname: TextView
    private lateinit var btnEditNick: Button
    private lateinit var btnJoinServer: MaterialButton
    private var btnEnterGame: MaterialButton? = null
    private lateinit var btnDiscord: MaterialButton
    private lateinit var btnRefreshStatus: ImageButton
    private lateinit var chartPing: LineChart
    private lateinit var cardDownloadCache: CardView
    private lateinit var cardDownloadMod: CardView
    private lateinit var tvPing: TextView
    private lateinit var tvServerStatus: TextView
    private lateinit var tvPlayersCount: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            checkServerStatus()
            handler.postDelayed(this, 30000)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "✅ تم منح الصلاحية بنجاح", Toast.LENGTH_SHORT).show()
            startProfessionalDownloadAndExtract()
        } else {
            Toast.makeText(context, "❌ تم رفض الصلاحية، لا يمكن تحميل الملف بدونها", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val DISCORD_URL = "https://discord.gg/eZFKQ83ke"
        const val SERVER_IP = "142.132.203.47"
        const val SERVER_PORT = 21299
        const val SAMP_PACKAGE = "com.rockstargames.gtasa"
        private const val HIDDEN_NAME = "••••••_"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        setupListeners()
        setupPingChart()
        loadNickname()
        checkServerStatus()
        animateEntrance(view)
        pulseStatus()
        checkCacheStatus()

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
        checkCacheStatus()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun initViews(view: View) {
        tvNickname = view.findViewById(R.id.tv_nickname)
        btnEditNick = view.findViewById(R.id.btn_edit_nick)
        btnJoinServer = view.findViewById(R.id.btn_join_server)
        btnEnterGame = view.findViewById(R.id.btn_enter_game)
        btnDiscord = view.findViewById(R.id.btn_discord)
        btnRefreshStatus = view.findViewById(R.id.btn_refresh_status)
        chartPing = view.findViewById(R.id.chart_ping)
        cardDownloadCache = view.findViewById(R.id.card_download_cache)
        cardDownloadMod = view.findViewById(R.id.card_download_mod)
        tvPing = view.findViewById(R.id.tv_ping)
        tvServerStatus = view.findViewById(R.id.tv_server_status)
        tvPlayersCount = view.findViewById(R.id.tv_players_count)
    }

    private fun setupListeners() {
        btnEditNick.setOnClickListener {
            showEditNicknameDialog()
        }

        btnJoinServer.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            joinServer()
        }

        btnEnterGame?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            joinServer()
        }

        btnDiscord.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openUrl(DISCORD_URL)
        }

        btnRefreshStatus.setOnClickListener {
            it.animate().rotationBy(360f).setDuration(500).start()
            checkServerStatus()
            Toast.makeText(context, "جاري التحقق...", Toast.LENGTH_SHORT).show()
        }

        cardDownloadCache.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            checkPermissionAndDownload()
        }

        cardDownloadMod.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showModDownloadDialog()
        }
    }

    private fun checkCacheStatus() {
        val targetDir = requireContext().getExternalFilesDir(null)
        if (targetDir != null && targetDir.exists() && (targetDir.list()?.isNotEmpty() == true)) {
            cardDownloadCache.visibility = View.GONE
        } else {
            cardDownloadCache.visibility = View.VISIBLE
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startProfessionalDownloadAndExtract()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else {
            startProfessionalDownloadAndExtract()
        }
    }

    private fun startProfessionalDownloadAndExtract() {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("📥 تحميل كاش SA-MP الاحترافي")
            setMessage("جاري تهيئة التحميل...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            max = 100
            setCancelable(false)
            show()
        }

        thread {
            try {
                val cacheUrl = getString(R.string.SAMP_data_url)
                val url = URL(cacheUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val input = BufferedInputStream(connection.inputStream)

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val zipFile = File(downloadDir, "samp_cache_pro.zip")

                val outputStream = FileOutputStream(zipFile)
                val data = ByteArray(16384)
                var total: Long = 0
                var count: Int
                
                var startTime = System.currentTimeMillis()
                var lastTime = startTime
                var lastTotal: Long = 0

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    outputStream.write(data, 0, count)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTime >= 400 || total == fileLength.toLong()) {
                        val timeElapsed = (currentTime - lastTime) / 1000.0
                        val bytesRead = total - lastTotal
                        val speedKBps = if (timeElapsed > 0) (bytesRead / 1024.0) / timeElapsed else 0.0
                        val speedMBps = speedKBps / 1024.0

                        lastTime = currentTime
                        lastTotal = total

                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength.toLong()).toInt()
                            val downloadedMB = total / (1024 * 1024)
                            val totalMB = fileLength / (1024 * 1024)

                            activity?.runOnUiThread {
                                progressDialog.progress = progress
                                progressDialog.setMessage(
                                    "⚡ جاري التحميل باحترافية...\n" +
                                            "📊 المنجز: $downloadedMB MB / $totalMB MB ($progress%)\n" +
                                            "🚀 السرعة: ${String.format("%.2f", speedMBps)} MB/s"
                                )
                            }
                        }
                    }
                }

                outputStream.flush()
                outputStream.fd.sync()
                outputStream.close()
                input.close()

                activity?.runOnUiThread {
                    progressDialog.isIndeterminate = true
                    progressDialog.setMessage("📂 جاري فك الضغط وحفظ الملفات في مسارها الصحيح...")
                }

                val targetDir = requireContext().getExternalFilesDir(null) ?: File(Environment.getExternalStorageDirectory(), "Android/data/com.umnicode.samp_launcher/files")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                unzipAndSavePermanently(zipFile, targetDir)

                if (zipFile.exists()) {
                    zipFile.delete()
                }

                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "✅ تمت عملية التحميل، الفك والحفظ بنجاح تام!", Toast.LENGTH_LONG).show()
                    cardDownloadCache.visibility = View.GONE
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "❌ حدث خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun unzipAndSavePermanently(zipFile: File, targetDirectory: File) {
        val zis = ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
        try {
            var zisEntry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (zisEntry != null) {
                val newFile = File(targetDirectory, zisEntry.name)
                if (zisEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    val fout = FileOutputStream(newFile)
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        fout.write(buffer, 0, count)
                    }
                    fout.flush()
                    fout.fd.sync()
                    fout.close()
                }
                zis.closeEntry()
                zisEntry = zis.nextEntry
            }
        } finally {
            zis.close()
        }
    }

    private fun joinServer() {
        try {
            val pm = requireActivity().packageManager
            val intent = pm.getLaunchIntentForPackage(SAMP_PACKAGE)
            if (intent != null) {
                intent.putExtra("ip", SERVER_IP)
                intent.putExtra("port", SERVER_PORT)
                startActivity(intent)
                Toast.makeText(context, "🎮 جاري الدخول إلى السيرفر...", Toast.LENGTH_SHORT).show()
            } else {
                val sampUrl = "samp://$SERVER_IP:$SERVER_PORT"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sampUrl))
                if (browserIntent.resolveActivity(pm) != null) {
                    startActivity(browserIntent)
                } else {
                    Toast.makeText(context, "⚠️ يرجى التأكد من تثبيت مشغل اللعبة أو العميل الداعم للرابط", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر الدخول: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkServerStatus() {
        val app = requireActivity().application as LauncherApplication
        if (app.userConfig.PingTimeout <= 0) {
            app.userConfig.PingTimeout = 3000
        }

        ServerConfig.Resolve(SERVER_IP, SERVER_PORT, app.userConfig.PingTimeout, requireContext(), object : ServerResolveCallback {
            override fun OnFinish(OutConfig: ServerConfig?) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    OutConfig?.let { config ->
                        tvPlayersCount.text = "لاعبين: ${config.OnlinePlayers}/${config.MaxPlayers}"

                        when {
                            ServerConfig.IsStatusOk(config.Status) -> {
                                tvServerStatus.text = "🟢 السيرفر أونلاين"
                                tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                                tvPing.text = "Ping: نشط"
                            }
                            ServerConfig.IsStatusNone(config.Status) -> {
                                tvServerStatus.text = "🟡 جاري التحقق..."
                                tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                            }
                            else -> {
                                tvServerStatus.text = "🔴 السيرفر أوفلاين"
                                tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                                tvPing.text = "Ping: --"
                                tvPlayersCount.text = "لاعبين: 0/0"
                            }
                        }
                    }
                }
            }

            override fun OnPingFinish(OutConfig: ServerConfig?) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    OutConfig?.let { config ->
                        if (ServerConfig.IsStatusOk(config.Status)) {
                            tvServerStatus.text = "🟢 السيرفر أونلاين"
                            tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                        } else {
                            tvServerStatus.text = "🔴 السيرفر أوفلاين"
                            tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                            tvPlayersCount.text = "لاعبين: 0/0"
                        }
                    }
                }
            }
        })
    }

    private fun showModDownloadDialog() {
        val mods = arrayOf("مود الخريطة الكاملة", "مود الشخصيات", "مود الأسلحة", "مود السيارات")
        val urls = arrayOf(
            "https://yourserver.com/mods/map.zip",
            "https://yourserver.com/mods/skins.zip",
            "https://yourserver.com/mods/weapons.zip",
            "https://yourserver.com/mods/cars.zip"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("🛠️ اختر المود للتحميل")
            .setItems(mods) { _, which ->
                Toast.makeText(context, "جاري تحضير ${mods[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEditNicknameDialog() {
        val app = requireActivity().application as LauncherApplication
        val editText = EditText(requireContext()).apply {
            setText(app.userConfig.Nickname)
            hint = "أدخل اسمك في اللعبة"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ تعديل الاسم")
            .setView(editText)
            .setPositiveButton("حفظ") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    app.userConfig.Nickname = newName
                    app.userConfig.Save()
                    tvNickname.text = HIDDEN_NAME
                    Toast.makeText(context, "✅ تم الحفظ", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun setupPingChart() {
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 24f))
        entries.add(Entry(1f, 28f))
        entries.add(Entry(2f, 22f))
        entries.add(Entry(3f, 30f))
        entries.add(Entry(4f, 24f))

        val dataSet = LineDataSet(entries, "Ping").apply {
            color = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            setDrawCircles(false)
            lineWidth = 2f
        }

        chartPing.data = LineData(dataSet)
        chartPing.description.isEnabled = false
        chartPing.legend.isEnabled = false
        chartPing.xAxis.isEnabled = false
        chartPing.axisLeft.isEnabled = false   // تم تصحيح الخطأ هنا (إلغاء الفاصلة ووضع =)
        chartPing.axisRight.isEnabled = false
        chartPing.invalidate()
    }

    private fun loadNickname() {
        tvNickname.text = HIDDEN_NAME
    }

    private fun animateEntrance(view: View) {
        view.alpha = 0f
        view.translationY = 50f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(80)
            .setDuration(450)
            .start()
    }

    private fun pulseStatus() {
        if (!isAdded) return
        tvServerStatus.animate()
            .alpha(0.45f)
            .setDuration(900)
            .withEndAction {
                if (!isAdded) return@withEndAction
                tvServerStatus.animate()
                    .alpha(1f)
                    .setDuration(900)
                    .withEndAction { pulseStatus() }
                    .start()
            }
            .start()
    }
}
