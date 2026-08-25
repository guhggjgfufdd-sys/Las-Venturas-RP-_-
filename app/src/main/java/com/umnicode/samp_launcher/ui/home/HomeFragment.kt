package com.umnicode.samp_launcher

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

    // روابط التحميل - عدلها حسب سيرفرك
    companion object {
        const val CACHE_DOWNLOAD_URL = "https://yourserver.com/files/cache.zip"
        const val MOD_DOWNLOAD_URL = "https://yourserver.com/files/mods.zip"
        const val DISCORD_URL = "https://discord.gg/yourserver"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // ربط العناصر بالكود
        initViews(view)
        
        // إعداد المستمعات (Listeners)
        setupListeners()
        
        // إعداد رسم البيانات
        setupPingChart()

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
    }

    private fun setupListeners() {
        // تعديل الاسم
        btnEditNick.setOnClickListener {
            showEditNicknameDialog()
        }

        // دخول السيرفر
        btnJoinServer.setOnClickListener {
            joinServer()
        }

        // رابط الديسكورد
        btnDiscord.setOnClickListener {
            openUrl(DISCORD_URL)
        }

        // تسجيل الدخول
        btnLogin.setOnClickListener {
            // انتقل لشاشة تسجيل الدخول
            Toast.makeText(context, "جاري فتح شاشة الدخول...", Toast.LENGTH_SHORT).show()
        }

        // تحميل الكاش
        cardDownloadCache.setOnClickListener {
            downloadFile(
                url = CACHE_DOWNLOAD_URL,
                fileName = "cache_files.zip",
                title = "تحميل الكاش",
                extractPath = "Android/data/com.rockstargames.gtasa/files/"
            )
        }

        // تحميل المود (الجديد)
        cardDownloadMod.setOnClickListener {
            showModDownloadDialog()
        }
    }

    /**
     * ديالوج اختيار المود قبل التحميل
     */
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
                downloadFile(
                    url = urls[which],
                    fileName = "mod_${mods[which]}.zip",
                    title = "تحميل ${mods[which]}",
                    extractPath = "Android/data/com.rockstargames.gtasa/files/mods/"
                )
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    /**
     * ديالوج تعديل الاسم
     */
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
                    // حفظ في SharedPreferences
                    requireContext().getSharedPreferences("launcher_prefs", 0)
                        .edit()
                        .putString("nickname", newName)
                        .apply()
                    Toast.makeText(context, "تم الحفظ!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    /**
     * دخول السيرفر (يفتح SA-MP)
     */
    private fun joinServer() {
        try {
            // إذا كان عندك Activity خاصة بالدخول
            val intent = Intent(requireContext(), ServerJoinActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            // fallback - افتح رابط samp مباشرة
            val sampUrl = "samp://your.server.ip:7777"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sampUrl))
            startActivity(intent)
        }
    }

    /**
     * فتح رابط خارجي
     */
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    /**
     * نظام التحميل العام (للكاش والمودات)
     */
    private fun downloadFile(url: String, fileName: String, title: String, extractPath: String) {
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
                
                // مجلد التحميلات
                val downloadDir = File(
                    Environment.getExternalStorageDirectory(),
                    "Download/LasVenturasRP"
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
                    
                    // تحديث الـ Progress
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
                    Toast.makeText(
                        context,
                        "تم التحميل: ${outputFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // هنا تقدر تضيف كود فك الضغط تلقائياً
                    // unzipFile(outputFile, extractPath)
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        context,
                        "خطأ في التحميل: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * إعداد رسم الـ Ping
     */
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

    override fun onResume() {
        super.onResume()
        // استرجاع الاسم المحفوظ
        val savedName = requireContext()
            .getSharedPreferences("launcher_prefs", 0)
            .getString("nickname", "YourInGameNick")
        tvNickname.text = savedName
    }
}
