## Installation & Usage

The instructions below are adapted from the project's README.

### Option A (recommended): Homebrew

```bash
brew tap kpraveenkumar19/torrentx
brew install torrentx

# Download a .torrent file
download sample.torrent

# Download via magnet link
magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

Quick start:

```bash
download sample.torrent
magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

### Option B (alternative): Standalone JAR

1) Build (Java 17+ and Maven 3.8+):

```bash
mvn -q -DskipTests package
```

2) Run using the packaged JAR:

```bash
java -cp target/torrent-x-1.0.0.jar Main <command> [args]
```

Quick start (JAR):

```bash
java -cp target/torrent-x-1.0.0.jar Main download sample.torrent
java -cp target/torrent-x-1.0.0.jar Main magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

### Notes

- Downloads are saved to `~/Downloads`.
- For `.torrent` downloads, the `.torrent` file must be located at `~/Downloads/<file>.torrent`.
- You can copy the sample: `cp torrent_files/sample.torrent ~/Downloads/`.
- The client selects the first available peer from the tracker's compact peer list.
- One peer connection at a time; single-file torrents only.

### Commands reference

```text
download <torrent_file>
  - <torrent_file>: Name of the .torrent file in ~/Downloads (e.g., sample.torrent)

magnet_download <magnet_link>
  - <magnet_link>: Full magnet URI, including xt and optionally tr parameters
```


