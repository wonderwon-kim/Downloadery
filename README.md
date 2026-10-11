<p align="center">
  <img src="https://gitflic.ru/project/kurtbestor/hitomi-downloader/blob/raw?file=imgs%2Fcard_crop.png" width="600"/>
  <br>
</p>

[![GitHub release](https://img.shields.io/github/release/KurtBestor/Hitomi-Downloader.svg?logo=github)](https://github.com/KurtBestor/Hitomi-Downloader/releases/latest)
[![GitHub downloads](https://img.shields.io/github/downloads/KurtBestor/Hitomi-Downloader/latest/total.svg?logo=github)](https://github.com/KurtBestor/Hitomi-Downloader/releases/latest)
[![GitHub downloads](https://img.shields.io/github/downloads/KurtBestor/Hitomi-Downloader/total.svg?logo=github)](https://github.com/KurtBestor/Hitomi-Downloader/releases)

## Links
- [Download](https://gitflic.ru/project/kurtbestor/hitomi-downloader/release/latest)
- [Issues](https://gitflic.ru/project/kurtbestor/hitomi-downloader/issue)
- [Scripts & Plugins](https://github.com/KurtBestor/Hitomi-Downloader/wiki/Scripts-&-Plugins)
- [Chrome Extension](https://github.com/KurtBestor/Hitomi-Downloader/wiki/Chrome-Extension)

## Android (armv7a)

Downloadery now includes a lightweight Android client under `app/`. It is designed
for 32-bit ARM devices (`armeabi-v7a`) and uses Android's native `DownloadManager`
for resumable background downloads, notifications, and download history. It accepts
direct `http`/`https` file and media URLs, clipboard paste, Android share intents,
and links opened from a browser. Torrent/magnet downloads are intentionally not
included.

To build locally:

```bash
./gradlew --no-daemon :app:assembleRelease
```

The armv7a APK is generated at
`app/build/outputs/apk/armeabi-v7a/release/app-armeabi-v7a-release.apk`.
Pushing a tag such as `v1.0.0` runs
[the Android release workflow](.github/workflows/android-release.yml), builds the
armv7a APK, generates a SHA-256 checksum, and attaches both to a GitHub release.
The APK is currently unsigned; configure a repository signing secret before
distributing it as a production release.

For signed releases, configure `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`
repository secrets. Without those secrets the workflow intentionally produces
an unsigned validation APK instead of inventing or committing a signing key.

Tap a completed item to open it. Long-press a queued/running item to cancel it,
or long-press a failed/paused item to retry it.

## Demo
<img src="https://gitflic.ru/project/kurtbestor/hitomi-downloader/blob/raw?file=imgs%2Fhow_to_download.gif">

## Features
- 🍰 Simple and clear user interface
- 🚀 Download acceleration
- 💻 Supports 24 threads in a single task
- 🚥 Supports speed limit
- 📜 Supports user scripts
- 🎞️ Supports M3U8 & MPD format videos
- 🌙 Dark mode
- 🧳 Portable
- 📋 Clipboard monitor
- 🗃️ Easy to organize tasks

## Supported Sites
| Site | URL |
| :---: | :--- |
| **4chan** | <https://4chan.org> |
| **AfreecaTV** | <https://afreecatv.com> |
| **ArtStation** | <https://artstation.com> |
| **baraag.net** | <https://baraag.net> |
| **bilibili** | <https://bilibili.com> |
| **ComicWalker** | <https://comic-walker.com> |
| **Coub** | <https://coub.com> |
| **DeviantArt** | <https://deviantart.com> |
| **Facebook** | <https://facebook.com> |
| **FC2 Video** | <https://video.fc2.com> |
| **Flickr** | <https://flickr.com> |
| **Hameln** | <https://syosetu.org> |
| **Imgur** | <https://imgur.com> |
| **Instagram** | <https://instagram.com> |
| **カクヨム** | <https://kakuyomu.jp> |
| **Mastodon** | <https://mastodon.social> |
| **Misskey** | <https://misskey.io> |
| **Naver Blog** | <https://blog.naver.com> |
| **Naver Cafe** | <https://cafe.naver.com> |
| **Naver Post** | <https://post.naver.com> |
| **Naver Webtoon** | <https://comic.naver.com> |
| **Naver TV** | <https://tv.naver.com> |
| **Niconico** | <http://nicovideo.jp> |
| **Pawoo** | <https://pawoo.net> |
| **Pinterest** | <https://pinterest.com> |
| **Pixiv** | <https://pixiv.net> |
| **pixivコミック** | <https://comic.pixiv.net> |
| **Soundcloud** | <https://soundcloud.com> |
| **小説家になろう** | <https://syosetu.com> |
| **TikTok** | <https://tiktok.com><br><https://douyin.com>|
| **Tumblr** | <https://tumblr.com> |
| **Twitch** | <https://twitch.tv> |
| **Twitter** | <https://twitter.com> |
| **Vimeo** | <https://vimeo.com> |
| **Wayback Machine** | <https://archive.org> |
| **Weibo** | <https://weibo.com> |
| **WikiArt** | <https://www.wikiart.org> |
| **Youku** | <https://youku.com> |
| **YouTube** | <https://youtube.com> |
| **and more...** | [Supported sites by yt-dlp](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md) |

The legacy desktop extractor list above is retained as a compatibility reference.
The Android client currently ships the direct URL path; yt-dlp site extraction,
format selection, playlists, subtitles, and HLS/DASH processing are being added
as separate Android engine milestones. Torrent and magnet downloads are not
planned for the Android client. The currently reviewed `yt-dlp-android` AAR
supports `arm64-v8a` and `x86_64`, but not `armeabi-v7a`, so it cannot be added
to the armv7a release until an armv7-compatible runtime is built and verified.
