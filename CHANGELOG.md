# Changelog

All notable changes to MineMelody are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-11-13

### Fixed
- Game music tracks now show their real names ("Aaron Cherof - A Familiar Room")
  instead of file names ("a_familiar_room"). Music discs were not affected.

## [1.0.0] - 2026-11-13

First public release on Modrinth. Includes everything below.

### Added
- MineMelody logo as the mod icon.

## [0.5.0] - 2026-11-12

### Added
- Banner mode: never, when a track starts, or always on screen with elapsed/total time and a progress bar.
- Banner size from 50% to 200%.
- Banner settings screen with a live preview in the chosen corner.

### Changed
- Config format v3: the old banner on/off setting is migrated automatically (backup kept).

## [0.4.1] - 2026-11-12

### Fixed
- The dimension playlist now starts right after changing dimension instead of staying silent for minutes.

## [0.4.0] - 2026-11-12

### Added
- Track durations in both lists and in the banner, read from the OGG file itself.
- Elapsed/total time and progress bar on the playlist screen.

## [0.3.0] - 2026-11-12

### Added
- Multiple named playlists: create, rename, delete and switch.
- Bind a playlist to the Overworld, Nether or End to play it automatically there.
- Optional keys: next playlist, pause/resume music, music volume up/down.

### Changed
- Config format v2: the single playlist becomes "Principal" (backup kept).

## [0.2.0] - 2026-11-12

### Added
- Search box (case and accent insensitive).
- Favorite tracks, with "add all favorites" and a `fav` search keyword.
- Export/import playlists as text through the clipboard.

### Changed
- Mod renamed to MineMelody (old config migrated automatically).

## [0.1.0] - 2026-11-12

### Added
- Playlist screen from Music & Sound options or the M key.
- In order, shuffle and repeat-one modes, with configurable gap between tracks.
- In survival, music discs unlock once you have them in your inventory (per world).
- "Now playing" banner and instant track preview.

[1.0.1]: https://github.com/Pedriii10/MineMelody/releases/tag/v1.0.1
[1.0.0]: https://github.com/Pedriii10/MineMelody/releases/tag/v1.0.0
