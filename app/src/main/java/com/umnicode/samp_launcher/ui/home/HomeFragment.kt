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

    private val SERVER_IP = "142.132.203.47"
    private val SERVER_PORT = "21299"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPreferences: SharedPreferences? = this.context?.getSharedPreferences("HomeFragment", Context.MODE_PRIVATE)

        this.rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val nicknameText: EditText = this.rootView.findViewById(R.id.nickname)
        val launcherApplication: LauncherApplication = activity?.application as LauncherApplication
        nicknameText.setText(launcherApplication.userConfig.Nickname)

        nicknameText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                launcherApplication.userConfig.Nickname = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val playButton: PlayButton = this.rootView.findViewById(R.id.play_btn) as PlayButton
        playButton.SetOnSAMPLaunchCallback {
            println("Launch SAMP")
        }

        val discordCard: View = this.rootView.findViewById(R.id.discord_card)
        discordCard.setOnClickListener {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://discord.gg/eZFKQ83ke")
            )
            startActivity(intent)
        }

        this.updateServerConfig()
        return this.rootView
    }

    private fun setStatusBadge(text: String, backgroundRes: Int, textColor: Int) {
        val badge: TextView = this.rootView.findViewById(R.id.serverStatusBadge)
        badge.text = text
        badge.setBackgroundResource(backgroundRes)
        badge.setTextColor(textColor)
    }

    private fun updateServerConfig() {
        val userConfig = (activity?.application as LauncherApplication).userConfig

        setStatusBadge("● جاري التحقق", R.drawable.status_pending_bg, 0xFFFFC107.toInt())

        ServerConfig.Resolve(SERVER_IP, SERVER_PORT.toInt(), userConfig.PingTimeout, this.context, object : ServerResolveCallback {
            override fun OnFinish(OutConfig: ServerConfig?) {
                val serverView: ServerView = rootView.findViewById(R.id.server_view)

                if (OutConfig != null) {
                    serverView.SetServer(OutConfig)
                    setStatusBadge("● أونلاين", R.drawable.status_online_bg, 0xFF4CAF50.toInt())

                    val playButton: PlayButton = rootView.findViewById(R.id.play_btn) as PlayButton
                    playButton.SetServerConfig(OutConfig)
                } else {
                    setStatusBadge("● صيانة حالياً", R.drawable.status_offline_bg, 0xFFE53935.toInt())
                }
            }

            override fun OnPingFinish(OutConfig: ServerConfig?) {
                val serverView: ServerView = rootView.findViewById(R.id.server_view)
                if (OutConfig != null) {
                    serverView.SetServer(OutConfig)
                }
            }
        })
    }
}
