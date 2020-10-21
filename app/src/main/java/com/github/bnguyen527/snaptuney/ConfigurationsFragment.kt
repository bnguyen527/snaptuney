package com.github.bnguyen527.snaptuney

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.bnguyen527.snaptuney.databinding.FragmentConfigurationsBinding
import kaaes.spotify.webapi.android.SpotifyError
import kaaes.spotify.webapi.android.SpotifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit.RetrofitError

class ConfigurationsFragment : Fragment() {
    private var _binding: FragmentConfigurationsBinding? = null
    private val binding get() = _binding!!
    // Reference to SpotifyService object from MainActivity
    private var _spotify: SpotifyService? = null
    private val spotify get() = _spotify!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConfigurationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            createPlaylistButton.setOnClickListener {
                findNavController().navigate(ConfigurationsFragmentDirections.actionCreatePlaylist())
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _spotify = (requireActivity() as MainActivity).spotify

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val myPlaylists = spotify.myPlaylists
                    Log.d(TAG, "Fetched ${myPlaylists.total} playlists")
                    Log.i(TAG, "Fetched list of current user's playlists")
                } catch (error: RetrofitError) {
                    Log.e(TAG, SpotifyError.fromRetrofitError(error).toString())
                }
            }
            withContext(Dispatchers.IO) {
                try {
                    val featuredPlaylists = spotify.featuredPlaylists.playlists
                    Log.d(TAG, "Fetched ${featuredPlaylists.total} playlists")
                    Log.i(TAG, "Fetched list of featured playlists")
                } catch (error: RetrofitError) {
                    Log.e(TAG, SpotifyError.fromRetrofitError(error).toString())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        _spotify = null
    }

    companion object {
        private val TAG = ConfigurationsFragment::class.java.simpleName
    }
}