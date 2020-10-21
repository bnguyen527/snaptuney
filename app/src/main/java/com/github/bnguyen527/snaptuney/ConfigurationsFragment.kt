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
import kaaes.spotify.webapi.android.models.PlaylistSimple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit.RetrofitError

private enum class PlaylistType(val description: String) {
    MY_PLAYLISTS("current user's"),
    FEATURED_PLAYLISTS("featured")
}

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
            fetchPlaylistList(PlaylistType.MY_PLAYLISTS)
            fetchPlaylistList(PlaylistType.FEATURED_PLAYLISTS)
        }
    }

    /**
     * Returns fetched list of playlists of type [playlistType] and empty list when API request
     * response is an error.
     */
    private suspend fun fetchPlaylistList(playlistType: PlaylistType) =
        withContext(Dispatchers.IO) {
            try {
                val playlistsPager = when (playlistType) {
                    PlaylistType.MY_PLAYLISTS -> spotify.myPlaylists
                    PlaylistType.FEATURED_PLAYLISTS -> spotify.featuredPlaylists.playlists
                }
                Log.d(TAG, "Fetched ${playlistsPager.total} playlists")
                Log.i(TAG, "Fetched list of ${playlistType.description} playlists")
                playlistsPager.items
            } catch (error: RetrofitError) {
                Log.e(TAG, SpotifyError.fromRetrofitError(error).toString())
                emptyList<PlaylistSimple>()
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