---
title: Installation & Usage
---

# Installation & Usage

Below are two ways to install and run TorrentX.

## Option A (recommended): Homebrew

```bash
brew tap kpraveenkumar19/torrentx
brew install torrentx
download <torrent_file>
magnet_download <magnet_link>
```

Quick start (Homebrew):

```bash
download sample.torrent
magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

## Option B (alternative): Standalone JAR

1. Build (requires Java 17+ and Maven 3.8+):

```bash
mvn -q -DskipTests package
```

2. Run using the packaged JAR:

```bash
java -cp target/torrent-x-1.0.0.jar Main <command> [args]
```

Quick start (JAR):

```bash
java -cp target/torrent-x-1.0.0.jar Main download sample.torrent
java -cp target/torrent-x-1.0.0.jar Main magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

## Notes

- Downloads are saved to `~/Downloads`.
- For `.torrent` downloads, place the file at `~/Downloads/<file>.torrent`.
- Copy the sample to Downloads:

```bash
cp torrent_files/sample.torrent ~/Downloads/
```

## Commands reference

```text
download <torrent_file>
  - <torrent_file>: Name of the .torrent file in ~/Downloads (e.g., sample.torrent)

magnet_download <magnet_link>
  - <magnet_link>: Full magnet URI, including xt and optionally tr parameters
```


