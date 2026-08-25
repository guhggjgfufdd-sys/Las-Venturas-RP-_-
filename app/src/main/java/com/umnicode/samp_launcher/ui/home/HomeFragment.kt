package com.umnicode.samp_launcher.ui.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPPackageStatus
import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller
import com.umnicode.samp_launcher.core.ServerConfig
import com.umnicode.samp_launcher.core.ServerResolveCallback
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class HomeFragment : Fragment() {

    private lateinit var tvNickname: TextView
    private lateinit var btnEditNick: Button
    private lateinit var btnJoinServer: MaterialButton
    private lateinit var btnDiscord: Button
    private lateinit var btnLogin: Button
    private lateinit var btnInstallSamp: MaterialButton
    private lateinit var chartPing: LineChart
    private lateinit var cardDownloadCache: CardView
    private lateinit var cardDownloadMod: CardView
    private lateinit var cardInstallSamp: MaterialCardView
    private lateinit var tvPing: TextView
    private lateinit var tvServerStatus: TextView
    private lateinit var tvPlayersCount: TextView
    private lateinit var tvServerInfo: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            checkServerStatus()
            handler.postDelayed(this, 30000)
        }
    }

    companion object {
        const val DISCORD_URL = "https://discord.gg/eZFKQ83ke"
        const val SERVER_IP = "142.132.203.47"
        const val SERVER_PORT = 21299
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
        checkSAMPStatus()
        checkServerStatus()

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun initViews(view: View) {
        tvNickname = view.findViewById(R.id.tv_nickname)
        btnEditNick = view.findViewById(R.id.btn_edit_nick)
        btnJoinServer = view.findViewById(R.id.btn_join_server)
        btnDiscord = view.findViewById(R.id.btn_discord)
        btnLogin = view.findViewById(R.id.btn_login)
        btnInstallSamp = view.findViewById(R.id.btn_install_samp)
        chartPing = view.findViewById(R.id.chart_ping)
        cardDownloadCache = view.findViewById(R.id.card_download_cache)
        cardDownloadMod = view.findViewById(R.id.card_download_mod)
        cardInstallSamp = view.findViewById(R.id.card_install_samp)
        tvPing = view.findViewById(R.id.tv_ping)
        tvServerStatus = view.findViewById(R.id.tv_server_status)
        tvPlayersCount = view.findViewById(R.id.tv_players_count)
        tvServerInfo = view.findViewById(R.id.tv_server_info)
    }

    private fun setupListeners() {
        btnEditNick.setOnClickListener {
            showEditNicknameDialog()
        }

        btnJoinServer.setOnClickListener {
            try {
                val sampUrl = "samp://$SERVER_IP:$SERVER_PORT"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sampUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "تأكد من تثبيت SA-MP", Toast.LENGTH_SHORT).show()
            }
        }

        btnDiscord.setOnClickListener {
            openUrl(DISCORD_URL)
        }

        btnLogin.setOnClickListener {
            Toast.makeText(context, "جاري فتح شاشة الدخول...", Toast.LENGTH_SHORT).show()
        }

        btnInstallSamp.setOnClickListener {
            val app = requireActivity().application as LauncherApplication
            app.Installer?.Install(requireActivity())
        }

        cardDownloadCache.setOnClickListener {
            val app = requireActivity().application as LauncherApplication
            app.Installer?.InstallOnlyCache(requireActivity())
        }

        cardDownloadMod.setOnClickListener {
            showModDownloadDialog()
        }
    }

    private fun checkSAMPStatus() {
        val status = SAMPInstaller.IsInstalled(requireActivity().packageManager, resources)
        when (status) {
            SAMPPackageStatus.FOUND -> {
                cardInstallSamp.visibility = View.GONE
            }
            SAMPPackageStatus.CACHE_NOT_FOUND -> {
                cardInstallSamp.visibility = View.VISIBLE
                btnInstallSamp.text = "تثبيت الكاش"
            }
            SAMPPackageStatus.NOT_FOUND -> {
                cardInstallSamp.visibility = View.VISIBLE
                btnInstallSamp.text = "تثبيت SA-MP"
            }
        }
    }

    private fun checkServerStatus() {
        val app = requireActivity().application as LauncherApplication
        val pingTimeout = app.userConfig.PingTimeout
        if (pingTimeout <= 0) {
            app.userConfig.PingTimeout = 3000
        }

        ServerConfig.Resolve(SERVER_IP, SERVER_PORT, app.userConfig.PingTimeout, requireContext(), object : ServerResolveCallback {
            override fun OnFinish(OutConfig: ServerConfig?) {
                activity?.runOnUiThread {
                    OutConfig?.let { config ->
                        tvPlayersCount.text = "لاعبين: ${config.OnlinePlayers}/${config.MaxPlayers}"

                        when {
                            ServerConfig.IsStatusOk(config.Status) -> {
                                tvServerStatus.text = "🟢 السيرفر أونلاين"
                                tvServerStatus.setTextColor(resources.getColor(R.color.green))
                                tvPing.text = "Ping: نشط"
                            }
                            ServerConfig.IsStatusNone(config.Status) -> {
                                tvServerStatus.text = "🟡 جاري التحقق..."
                                tvServerStatus.setTextColor(resources.getColor(R.color.gold))
                            }
                            else -> {
                                tvServerStatus.text = "🔴 السيرفر أوفلاين"
                                tvServerStatus.setTextColor(resources.getColor(R.color.colorError))
                                tvPing.text = "Ping: --"
                                tvPlayersCount.text = "لاعبين: 0/0"
                            }
                        }
                    }
                }
            }

            override fun OnPingFinish(OutConfig: ServerConfig?) {
                activity?.runOnUiThread {
                    OutConfig?.let { config ->
                        if (ServerConfig.IsStatusOk(config.Status)) {
                            tvServerStatus.text = "🟢 السيرفر أونلاين"
                            tvServerStatus.setTextColor(resources.getColor(R.color.green))
                        } else {
                            tvServerStatus.text = "🔴 السيرفر أوفلاين"
                            tvServerStatus.setTextColor(resources.getColor(R.color.colorError))
                            tvPlayersCount.text = "لاعبين: 0/0"
                        }
                    }
                }
            }
        })
    }

    private fun showModDownloadDialog() {
        val mods = arrayOf("مود السيارات", "مود الأسلحة", "مود الشخصيات", "مود الخريطة الكاملة")
        val urls = arrayOf(
            "https://yourserver.com/mods/cars.zip",
            "https://yourserver.com/mods/weapons.zip",
            "https://yourserver.com/mods/skins.zip",
            "https://yourserver.com/mods/map.zip"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("اختر المود للتحميل")
            .setItems(mods) { _, which ->
                downloadFile(urls[which], "mod_${mods[which]}.zip", "تحميل ${mods[which]}")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEditNicknameDialog() {
        val editText = EditText(requireContext()).apply {
            setText(tvNickname.text)
            hint = "أدخل اسمك في اللعبة"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("تعديل الاسم")
            .setView(editText)
            .setPositiveButton("حفظ") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    tvNickname.text = newName
                    val app = requireActivity().application as LauncherApplication
                    app.userConfig.Nickname = newName
                    app.userConfig.Save()
                    Toast.makeText(context, "تم الحفظ!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun downloadFile(url: String, fileName: String, title: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle(title)
            setMessage("جاري التحميل...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            setCancelable(false)
            show()
        }

        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val input = connection.inputStream

                val downloadDir = File(
                    Environment.getExternalStorageDirectory(),
                    "Download/LasVenturasRP/Mods"
                )
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val outputFile = File(downloadDir, fileName)
                val output = FileOutputStream(outputFile)

                val buffer = ByteArray(1024)
                var total: Long = 0
                var count: Int

                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    output.write(buffer, 0, count)

                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        activity?.runOnUiThread {
                            progressDialog.progress = progress
                            progressDialog.setMessage("تم: $progress%")
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "تم التحميل!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupPingChart() {
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 24f))
        entries.add(Entry(1f, 28f))
        entries.add(Entry(2f, 22f))
        entries.add(Entry(3f, 30f))
        entries.add(Entry(4f, 24f))

        val dataSet = LineDataSet(entries, "Ping").apply {
            color = resources.getColor(android.R.color.holo_green_dark)
            setDrawCircles(false)
            lineWidth = 2f
        }

        chartPing.data = LineData(dataSet)
        chartPing.description.isEnabled = false
        chartPing.legend.isEnabled = false
        chartPing.xAxis.isEnabled = false
        chartPing.axisLeft.isEnabled = false
        chartPing.axisRight.isEnabled = false
        chartPing.invalidate()
    }

    private fun loadNickname() {
        val app = requireActivity().application as LauncherApplication
        if (app.userConfig.Nickname.isNotEmpty()) {
            tvNickname.text = app.userConfig.Nickname
        }
    }
}
