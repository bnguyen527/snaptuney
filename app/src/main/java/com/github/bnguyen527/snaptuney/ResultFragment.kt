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
import com.github.bnguyen527.snaptuney.databinding.FragmentResultBinding
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
            var nextTrack = firstSourceShuffledIter.nextWithDurationSeconds()
            // Find the first addable track from first source
            while (!nextTrack.shouldAdd() && firstSourceShuffledIter.hasNext()) {
                nextTrack = firstSourceShuffledIter.nextWithDurationSeconds()
            }
            nextTrack.checkThenAdd()
            nextTrack = secondSourceShuffledIter.nextWithDurationSeconds()
            // Find the first addable track from second source
            while (!nextTrack.shouldAdd() && secondSourceShuffledIter.hasNext()) {
                nextTrack = secondSourceShuffledIter.nextWithDurationSeconds()
            }
            nextTrack.checkThenAdd()
        }
        // Only check then add the remaining tracks if target duration not met yet
        if (secondsToTarget > 0) {
            // Check which source still has more tracks, then check then add the remaining
            while (firstSourceShuffledIter.hasNext()) {
                firstSourceShuffledIter.nextWithDurationSeconds().checkThenAdd()
            }
            while (secondSourceShuffledIter.hasNext()) {
                secondSourceShuffledIter.nextWithDurationSeconds().checkThenAdd()
            }
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
        with(next()) { Pair(this, track.duration_ms.div(1000.0).roundToLong()) }

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
                val firstSource = fetchPlaylistTracks(firstSourceOwnerId, firstSourceId)
                val secondSource = fetchPlaylistTracks(secondSourceOwnerId, secondSourceId)
                // Switch to Default dispatcher for heavy computation
                withContext(Dispatchers.Default) {
                    Tracklist(targetDuration, firstSource, secondSource).tracks
                }
                Log.i(TAG, "Got tracklist")
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