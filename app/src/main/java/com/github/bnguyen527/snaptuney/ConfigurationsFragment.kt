package com.github.bnguyen527.snaptuney

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
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

private class PlaylistSimpleArrayAdapter(
    context: Context,
    objects: List<PlaylistSimple>
) : ArrayAdapter<PlaylistSimple>(context, 0, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val playlistItemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item_playlist, parent, false)
        getItem(position)?.let { playlistItem ->
            playlistItemView.findViewById<TextView>(R.id.playlistNameTextView).text =
                playlistItem.name
        }
        return playlistItemView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val playlistItemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_dropdown_item_playlist, parent, false)
        getItem(position)?.let { playlistItem ->
            playlistItemView.apply {
                findViewById<TextView>(R.id.playlistDropdownNameTextView).text =
                    playlistItem.name
                findViewById<TextView>(R.id.playlistDropdownTrackTotalTextView).text =
                    resources.getQuantityString(
                        R.plurals.playlist_dropdown_track_total_text_view_text,
                        playlistItem.tracks.total,
                        playlistItem.tracks.total
                    )
            }
        }
        return playlistItemView
    }
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
                val firstSource = firstSourceSpinner.selectedItem as PlaylistSimple
                val secondSource = secondSourceSpinner.selectedItem as PlaylistSimple
                findNavController().navigate(
                    ConfigurationsFragmentDirections.actionCreatePlaylist(
                        durationEditText.text.toString().toInt(),
                        firstSource.id,
                        firstSource.owner.id,
                        secondSource.id,
                        secondSource.owner.id
                    )
                )
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _spotify = (requireActivity() as MainActivity).spotify

        lifecycleScope.launch {
            val myPlaylists = fetchPlaylistList(PlaylistType.MY_PLAYLISTS)
            val featuredPlaylists = fetchPlaylistList(PlaylistType.FEATURED_PLAYLISTS)
            PlaylistSimpleArrayAdapter(
                requireContext(),
                myPlaylists.plus(featuredPlaylists)
            ).also { adapter ->
                binding.apply {
                    firstSourceSpinner.adapter = adapter
                    secondSourceSpinner.adapter = adapter
                }
            }
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