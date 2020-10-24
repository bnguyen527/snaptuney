# SnapTuney

A Spotify playlist maker for when you only have **exactly** 17 minutes.

## Features

### The most music for the time you have
  
Ever found yourself in a situation where you want to listen to some music but only have *exactly* X minutes? SnapTuney got you covered. No more having to stop the player right before the chorus hits because class already started.

### Music timer

Maybe you're cooking something delicious that definitely shouldn't be overcooked, but a regular kitchen timer is too boring? Open up SnapTuney and get yourself a playlist of your favorite songs that happens to end exactly when your meal is ready.

### Mix multiple input sources

You've bored yourself to death with the same old playlists and would rather not listen to anything? SnapTuney can quickly get you a brand new playlist by mixing multiple sources, whether it be a playlist of songs you love but only in moderation, a Spotify curated playlist, or just an album you like but don't want to spend the whole time on.

*Tuney playlists are just a snap away with SnapTuney.*

## Getting Started

### Requirements

App is supported on Android 6.0 Marshmallow (API level 23) and later.

Dependencies are either automatically downloadable with Gradle or already bundled with the project.

### Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run app.

## Usage

1. Specify target playlist duration (in minutes).
2. Specify (currently exactly *two*) input sources from:
   - user's playlists,
   - playlists user follows, and
   - featured playlists by Spotify.
3. View and confirm the tracklist. Created playlist will be no longer than the target duration, interleave tracks from the (two) input sources and try to be as balanced between them as possible.
4. Save tracklist to a Spotify playlist with the name *SnapTuney*. (App should use the same playlist every time, but currently, expect a new playlist to be created when tapping "Save" button.)
5. Listen to the newly created playlist on Spotify.

## Limitations

- Currently app is essentially just a little bit more than a proof of concept. Therefore, even though core functionalities are complete, no data persisting is implemented yet (mostly due to lack of development time).
  - Orientation changes or killing and reopening app will not preserve input configurations or tracklist.
  - A new *SnapTuney* playlist is expected to be created in Spotify when tapping "Save" button (instead of reusing the same playlist for the app).
  - User is re-authorized every time instead of caching the access token (though just a quick loading animation should be visible to the user).
- Logging out is currently not possible.
- UI experience in general is unpolished.
  - User's display name is not displayed yet.

## Authors

- **Binh Q. Nguyen** - *Initial work* - [bnguyen527](https://github.com/bnguyen527)

## Acknowledgements

- [Spotify SDK Authorization Library](https://github.com/spotify/android-auth)
- [Spotify Web API](https://developer.spotify.com/documentation/web-api/)
- [Spotify Web API for Android](https://github.com/kaaes/spotify-web-api-android)
- [Retrofit](https://github.com/square/retrofit)

### Inspirations

- [Smarter Playlists](https://github.com/plamere/SmarterPlaylists)
