package com.umnicode.samp_launcher.ui.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
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
    private lateinit var btnJoinServer: Button
    private lateinit var btnDiscord: Button
    private lateinit var btnLogin: Button
    private lateinit var chartPing: LineChart
    private lateinit var cardDownloadCache: CardView
    private lateinit var cardDownloadMod: CardView
    private lateinit var tvPing: TextView

    companion object {
        const val DISCORD_URL = "https://discord.gg/yourserver"
        const val SERVER_IP = "your.server.ip"
        const val SERVER_PORT = 7777
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

        return view
    }

    private fun initViews(view: View) {
        tvNickname = view.findViewById(R.id.tv_nickname)
        btnEditNick = view.findViewById(R.id.btn_edit_nick)
        btnJoinServer = view.findViewById(R.id.btn_join_server)
        btnDiscord = view.findViewById(R.id.btn_discord)
        btnLogin = view.findViewById(R.id.btn_login)
        chartPing = view.findViewById(R.id.chart_ping)
        cardDownloadCache = view.findViewById(R.id.card_download_cache)
        cardDownloadMod = view.findViewById(R.id.card_download_mod)
        tvPing = view.findViewById(R.id.tv_ping)
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

        cardDownloadCache.setOnClickListener {
            val app = requireActivity().application as LauncherApplication
            app.Installer?.InstallOnlyCache(requireActivity())
        }

        cardDownloadMod.setOnClickListener {
            showModDownloadDialog()
        }
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

    private fun checkServerStatus() {
        ServerConfig.Resolve(SERVER_IP, SERVER_PORT, 3000, requireContext(), object : ServerResolveCallback {
            override fun OnFinish(OutConfig: ServerConfig?) {
                OutConfig?.let {
                    if (it.OnlinePlayers >= 0) {
                        tvPing.text = "Ping: نشط | لاعبين: ${it.OnlinePlayers}/${it.MaxPlayers}"
                    }
                }
            }
            override fun OnPingFinish(OutConfig: ServerConfig?) {}
        })
    }
}
