# Notices

Aeras is a modified version of **Seal**, the Android video/audio downloader by JunkFood02 and
contributors: https://github.com/JunkFood02/Seal

Seal is licensed under the GNU General Public License v3.0. Aeras is distributed under the same
licence; the full text is in [LICENSE](LICENSE).

## Changes made to the original

Modified for Windows in November 2026, starting from Seal 2.0.0-alpha.5:

- Replaced the Android platform layer with a desktop one: yt-dlp and FFmpeg run as external
  programs, settings and history are stored as JSON, Android resources are read by a loader, and
  Android framework classes used by the code have minimal desktop stand-ins
  (`src/main/kotlin/io/github/pratyuttar/aeras`, `src/main/kotlin/compat`).
- Adapted the original code under `src/main/kotlin/com/junkfood/seal` where it depended on Android:
  application start-up, file handling, notifications, cookie handling, updater, navigation
  arguments, and a few screens and settings that only apply to phones.
- Removed the Android-only entry points (activities, service, broadcast receiver), the in-app
  browser, the app updater, the sponsor page and the original project's community links.
- Renamed the application, replaced the application icon and the sample images, and changed the
  application's data and download folder names.

## Name

Aeras is an independent project. It is not affiliated with or endorsed by the author of Seal. The
name of the original project appears only to credit it, as its licence and its notice on
derivative builds require.

## Third-party material carried over from the original

- Colour utilities under `src/main/kotlin/io/material` and `src/main/kotlin/com/kyant/monet`
  (Material color utilities and Monet), Apache License 2.0
- Interface code adapted by the original project from Read You (GPL-3.0) and
  material-motion-compose (Apache License 2.0)
- Illustrations from unDraw
- Translations contributed to the original project

## Programs used at run time

- yt-dlp, https://github.com/yt-dlp/yt-dlp (The Unlicense)
- FFmpeg, https://ffmpeg.org/ (LGPL/GPL, depending on the build)

These are separate programs. Aeras runs them; it does not include their code.
