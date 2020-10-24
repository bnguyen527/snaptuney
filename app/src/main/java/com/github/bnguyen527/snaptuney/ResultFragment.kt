package com.github.bnguyen527.snaptuney

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.github.bnguyen527.snaptuney.databinding.FragmentResultBinding
import com.github.bnguyen527.snaptuney.databinding.ListItemPlaylistTrackBinding
import kaaes.spotify.webapi.android.SpotifyError
import kaaes.spotify.webapi.android.SpotifyService
import kaaes.spotify.webapi.android.models.PlaylistTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit.RetrofitError
import kotlin.math.roundToLong

private typealias PlaylistTrackWithDurationSeconds = Pair<PlaylistTrack, Long>

/**
 * Represents a tracklist to be created object with input sources [firstSource] and [secondSource]
 * and a target duration of [targetDuration] minutes.
 */
private class Tracklist(
    val targetDuration: Long,
    val firstSource: List<PlaylistTrack>,
    val secondSource: List<PlaylistTrack>
) {
    // List of playlist tracks, initialized lazily
    val tracks by lazy { makeTracklist() }

    // Set of unique playlist tracks to make tracklist from
    private var tracklist = mutableSetOf<PlaylistTrack>()
    private var secondsToTarget = targetDuration * 60
    private val firstSourceShuffledIter = firstSource.shuffled().iterator()
    private val secondSourceShuffledIter = secondSource.shuffled().iterator()

    /**
     * Returns a list of playlist tracks interleaving input sources [firstSource] and [secondSource]
     * and totaling no more than [targetDuration] minutes.
     */
    fun makeTracklist(): List<PlaylistTrack> {
        // Don't continue traversing if met target duration
        while (secondsToTarget > 0 && firstSourceShuffledIter.hasNext() && secondSourceShuffledIter.hasNext()) {
            firstSourceShuffledIter.addNextAddable()
            secondSourceShuffledIter.addNextAddable()
        }
        // Only check then add the remaining tracks if target duration not met yet
        if (secondsToTarget > 0) {
            firstSourceShuffledIter.addRemainingAddables()
            secondSourceShuffledIter.addRemainingAddables()
        }
        Log.d(
            TAG,
            "Tracklist of ${tracklist.size} track(s) and of length ${
                DateUtils.formatElapsedTime(targetDuration.times(60).minus(secondsToTarget))
            }"
        )
        Log.i(TAG, "Tracklist initialized")
        return tracklist.toList()
    }

    /** Returns a Pair of the next track coupled with its duration in seconds. */
    private fun Iterator<PlaylistTrack>.nextWithDurationSeconds() =
        with(next()) { Pair(this, convertMilliToSeconds(track.duration_ms)) }

    /** Adds the next (if any) addable track from this source to the tracklist. */
    private fun Iterator<PlaylistTrack>.addNextAddable() {
        var nextTrack = nextWithDurationSeconds()
        // Find the first addable track from first source
        while (!nextTrack.shouldAdd() && hasNext()) {
            nextTrack = nextWithDurationSeconds()
        }
        nextTrack.checkThenAdd()
    }

    /** Adds all of the remaining (if any) addable tracks from this source to the tracklist. */
    private fun Iterator<PlaylistTrack>.addRemainingAddables() {
        while (hasNext()) nextWithDurationSeconds().checkThenAdd()
    }

    /** Returns true if the track is addable. */
    private fun PlaylistTrackWithDurationSeconds.shouldAdd() =
        // If not yet added and duration wouldn't make playlist exceed target duration
        !tracklist.contains(first) && second <= secondsToTarget

    /** Checks if track is addable and if so, add it. */
    private fun PlaylistTrackWithDurationSeconds.checkThenAdd() {
        if (shouldAdd()) {
            tracklist.add(first)
            secondsToTarget -= second
        }
    }

    companion object {
        private val TAG = Tracklist::class.java.simpleName
    }
}

private class PlaylistTrackAdapter(private val tracklist: List<PlaylistTrack>) :
    RecyclerView.Adapter<PlaylistTrackAdapter.PlaylistTrackViewHolder>() {

    class PlaylistTrackViewHolder private constructor(private val binding: ListItemPlaylistTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(playlistTrack: PlaylistTrack) {
            playlistTrack.track.let { track ->
                binding.apply {
                    playlistTrackTitleTextView.text = track.name
                    playlistTrackArtistsTextView.text = track.artists.joinToString { it.name }
                    playlistTrackDurationTextView.text =
                        DateUtils.formatElapsedTime(convertMilliToSeconds(track.duration_ms))
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup) = PlaylistTrackViewHolder(
                ListItemPlaylistTrackBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlaylistTrackViewHolder.from(parent)

    override fun onBindViewHolder(holder: PlaylistTrackViewHolder, position: Int) {
        holder.bind(tracklist[position])
    }

    override fun getItemCount() = tracklist.size
}

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    // Reference to SpotifyService object from MainActivity
    private var _spotify: SpotifyService? = null
    private val spotify get() = _spotify!!
    private lateinit var currentUserId: String
    private lateinit var appPlaylistId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.newConfigurationsButton.setOnClickListener {
            findNavController().navigate(ResultFragmentDirections.actionNewConfigurations())
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
                val firstSource = fetchPlaylistTracks(firstSourceOwnerId, firstSourceId)
                val secondSource = fetchPlaylistTracks(secondSourceOwnerId, secondSourceId)
                // Switch to Default dispatcher for heavy computation
                val tracklist = withContext(Dispatchers.Default) {
                    Tracklist(targetDuration, firstSource, secondSource).tracks
                }
                Log.i(TAG, "Got tracklist")
                binding.apply {
                    playlistRecyclerView.adapter = PlaylistTrackAdapter(tracklist)
                    savePlaylistButton.setOnClickListener { onSavePlaylist(tracklist) }
                }
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

    /** Saves playlist of tracks as in [tracklist] and display corresponding responsive UI message. */
    private fun onSavePlaylist(tracklist: List<PlaylistTrack>) {
        lifecycleScope.launch {
            Toast.makeText(
                context,
                if (savePlaylist(tracklist))
                    getString(R.string.save_playlist_success_toast_text)
                else
                    getString(R.string.save_playlist_failed_toast_text),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Replaces tracks in app playlist with new tracklist as in [tracklist] after fetching current
     * user's ID and creating app playlist if necessary and returns true if no error has occurred.
     */
    private suspend fun savePlaylist(tracklist: List<PlaylistTrack>) = withContext(Dispatchers.IO) {
        try {
            checkAndFetchCurrentUserId()
            checkAndCreateAppPlaylist()
            overwriteAppPlaylist(tracklist)
            true
        } catch (error: RetrofitError) {
            Log.e(TAG, SpotifyError.fromRetrofitError(error).toString())
            false
        }
    }

    /**
     * Fetches current user's ID from Spotify and store it in appropriate member variable if it does
     * not exist yet.
     *
     * @throws RetrofitError
     */
    private fun checkAndFetchCurrentUserId() {
        if (!::currentUserId.isInitialized) {
            Log.i(TAG, "Fetching current user's ID")
            currentUserId = spotify.me.id
            Log.i(TAG, "Got current user's ID")
        } else {
            Log.i(TAG, "Using cached current user's ID")
        }
        Log.d(TAG, "Current user's ID: $currentUserId")
    }

    /**
     * Creates app playlist in Spotify and store its ID in appropriate member variable if it does
     * not exist yet.
     *
     * @throws RetrofitError
     */
    private fun checkAndCreateAppPlaylist() {
        if (!::appPlaylistId.isInitialized) {
            Log.i(TAG, "Creating app playlist")
            appPlaylistId = spotify.createPlaylist(
                currentUserId,
                mapOf(
                    "name" to getString(R.string.app_name),
                    "public" to false,
                    "description" to getString(R.string.playlist_description)
                )
            ).id
            Log.i(TAG, "Created app playlist")
        } else {
            Log.i(TAG, "Using existing app playlist with cached playlist ID")
        }
        Log.d(TAG, "App playlist ID: $appPlaylistId")
    }

    /**
     * Replaces app playlist in Spotify with tracks as in [newTracklist].
     *
     * @throws RetrofitError
     */
    private fun overwriteAppPlaylist(newTracklist: List<PlaylistTrack>) {
        Log.i(TAG, "Overwriting app playlist with new tracklist")
        spotify.replaceTracksInPlaylist(
            currentUserId,
            appPlaylistId,
            null,
            mapOf("uris" to newTracklist.map { it.track.uri })
        )
        Log.i(TAG, "Overwritten app playlist with new tracklist")
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

private fun convertMilliToSeconds(milli: Long) = milli.div(1000.0).roundToLong()
