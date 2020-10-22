package com.github.bnguyen527.snaptuney

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.bnguyen527.snaptuney.databinding.FragmentResultBinding
import kaaes.spotify.webapi.android.SpotifyError
import kaaes.spotify.webapi.android.SpotifyService
import kaaes.spotify.webapi.android.models.PlaylistTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit.RetrofitError

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    // Reference to SpotifyService object from MainActivity
    private var _spotify: SpotifyService? = null
    private val spotify get() = _spotify!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            savePlaylistButton.setOnClickListener {
                Toast.makeText(context, "Saved to Spotify!", Toast.LENGTH_SHORT).show()
            }
            newConfigurationsButton.setOnClickListener {
                findNavController().navigate(ResultFragmentDirections.actionNewConfigurations())
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _spotify = (requireActivity() as MainActivity).spotify

        val configurations by navArgs<ResultFragmentArgs>()
        Log.d(TAG, configurations.toString())
        Log.i(TAG, "Received input configurations")
        lifecycleScope.launch {
            with(configurations) {
                fetchPlaylistTracks(firstSourceOwnerId, firstSourceId)
                fetchPlaylistTracks(secondSourceOwnerId, secondSourceId)
            }
        }
    }

    /**
     * Returns fetched playlist tracks of playlist [playlistId] owned by user [ownerId] and empty
     * list when API request response is an error.
     */
    private suspend fun fetchPlaylistTracks(ownerId: String, playlistId: String) =
        withContext(Dispatchers.IO) {
            try {
                val playlistTracksPager =
                    spotify.getPlaylistTracks(ownerId, playlistId, PLAYLIST_TRACKS_QUERY_OPTIONS)
                Log.d(TAG, "Fetched ${playlistTracksPager.items.size} tracks")
                Log.i(TAG, "Fetched tracks from $playlistId")
                playlistTracksPager.items
            } catch (error: RetrofitError) {
                Log.e(TAG, SpotifyError.fromRetrofitError(error).toString())
                emptyList<PlaylistTrack>()
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
        private val TAG = ResultFragment::class.java.simpleName
        private val PLAYLIST_TRACKS_QUERY_OPTIONS = mapOf(
            "fields" to "items.track(artists.name,duration_ms,id,name,uri)",
            "market" to "from_token"
        )
    }
}