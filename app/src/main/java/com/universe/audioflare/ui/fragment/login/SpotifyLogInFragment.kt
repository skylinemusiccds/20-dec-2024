package com.universe.audioflare.ui.fragment.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.universe.audioflare.R
import com.universe.audioflare.common.Config
import com.universe.audioflare.databinding.FragmentSpotifyLogInBinding
import com.universe.audioflare.extension.isMyServiceRunning
import com.universe.audioflare.service.SimpleMediaService
import com.universe.audioflare.viewModel.LogInViewModel
import com.universe.audioflare.viewModel.SettingsViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class SpotifyLogInFragment : Fragment() {

    private var _binding: FragmentSpotifyLogInBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<LogInViewModel>()
    private val settingsViewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSpotifyLogInBinding.inflate(inflater, container, false)
        fetchSpotifyToken()
        return binding.root
    }

    private fun fetchSpotifyToken() {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://universe-audioflare-spdc.deno.dev/") // Replace with your URL
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val token = response.body?.string()
                    token?.let {
                        viewModel.saveSpotifySpdc(it)
                        CoroutineScope(Dispatchers.Main).launch {
                            settingsViewModel.setSpotifyLogIn(true)
                            Toast.makeText(
                                requireContext(),
                                R.string.login_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        binding.webView.visibility = View.GONE // Hide WebView as it's no longer needed
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    @UnstableApi
    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        bottom.visibility = View.VISIBLE
        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
        if (requireActivity().isMyServiceRunning(SimpleMediaService::class.java)) {
            miniplayer.animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            if (runBlocking { sharedViewModel.nowPlayingState.value?.mediaItem != null }) {
                miniplayer.visibility = View.VISIBLE
            }
        }
    }
}