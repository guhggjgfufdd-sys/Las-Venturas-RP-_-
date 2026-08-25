package com.umnicode.samp_launcher.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.core.ServerConfig
import com.umnicode.samp_launcher.core.ServerResolveCallback
import com.umnicode.samp_launcher.core.ServerView
import com.umnicode.samp_launcher.ui.widgets.playbutton.PlayButton

class HomeFragment : Fragment() {
    private lateinit var rootView: View

    // ✅ بيانات سيرفرك (Las Venturas RP) من Lemehost
    private val SERVER_IP = "142.132.203.47"
    private val SERVER_PORT = "21299"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPreferences: SharedPreferences? = this.context?.getSharedPreferences("HomeFragment", Context.MODE_PRIVATE)
        val preferencesEditor: SharedPreferences.Editor? = sharedPreferences?.edit()

        this.rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val nicknameText: EditText = this.rootView.findViewById(R.id.nickname)
        val launcherApplication: LauncherApplication = activity?.application as LauncherApplication
        nicknameText.setText(launcherApplication.userConfig.Nickname)

        val ipEditText: EditText = this.rootView.findViewById(R.id.ip)
        val portEditText: EditText = this.rootView.findViewById(R.id.port)
        val passwordEditText: EditText = this.rootView.findViewById(R.id.password)

        // 🔒 تثبيت IP والبورت تلقائيًا على سيرفرك (المستخدم ما يقدر يشوفهم أو يغيرهم)
        ipEditText.setText(SERVER_IP)
        portEditText.setText(SERVER_PORT)

        if (sharedPreferences != null) {
            passwordEditText.setText(sharedPreferences.getString(R.id.password.toString(), ""))
        }

        nicknameText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                launcherApplication.userConfig.Nickname = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                preferencesEditor?.putString(R.id.password.toString(), s.toString())
                preferencesEditor?.apply()
            }
            override fun afterTextChanged(s: Editable?) {
                updateServerConfig()
            }
        })

        val playButton: PlayButton = this.rootView.findViewById(R.id.play_btn) as PlayButton
        playButton.SetOnSAMPLaunchCallback {
            println("Launch SAMP")
        }

        this.updateServerConfig()
        return this.rootView
    }

    private fun updateServerConfig() {
        val userConfig = (activity?.application as LauncherApplication).userConfig

        ServerConfig.Resolve(SERVER_IP, SERVER_PORT.toInt(), userConfig.PingTimeout, this.context, object : ServerResolveCallback {
            override fun OnFinish(OutConfig: ServerConfig?) {
                val serverView: ServerView = rootView.findViewById(R.id.server_view)
                serverView.SetServer(OutConfig)

                val playButton: PlayButton = rootView.findViewById(R.id.play_btn) as PlayButton
                playButton.SetServerConfig(OutConfig)
            }

            override fun OnPingFinish(OutConfig: ServerConfig?) {
                val serverView: ServerView = rootView.findViewById(R.id.server_view)
                serverView.SetServer(OutConfig)
            }
        })
    }
}
