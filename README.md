## TorrentX

Simple, educational [BitTorrent](https://en.wikipedia.org/wiki/BitTorrent) client written in Java. TorrentX can download files from a [`.torrent` file](https://en.wikipedia.org/wiki/Torrent_file) or a [magnet link](https://en.wikipedia.org/wiki/Magnet_URI_scheme). It implements a minimal subset of the BitTorrent protocol end-to-end: parsing bencode, tracker announce, peer handshake, interest/unchoke, block requests, and piece hash verification.

### Features
- **.torrent download**: Parse bencoded metadata, announce to tracker, connect to a peer, request blocks, and write the final file to your `~/Downloads` directory.
- **Magnet link download**: Use the extension protocol (`ut_metadata`) to fetch metadata from a peer before downloading pieces.
- **Tracker** query: Compact peer list request/response and simple peer selection ([what is a tracker?](https://en.wikipedia.org/wiki/BitTorrent_tracker)).
- **Piece integrity**: Verifies every piece with SHA‑1 against the info dictionary.
- **Single peer flow**: Focuses on clarity and learning with one peer connection at a time.

> Note: TorrentX is intentionally minimal for learning and demonstration. It does not yet implement advanced peer management, DHT, multiple trackers, rate limiting, or multi-file torrents.

### Installation & Usage

#### Option A (recommended): Homebrew
```bash
brew tap kpraveenkumar19/torrentx
brew install torrentx
download <torrent_file>
magnet_download <magnet_link>
```


#### Option B (alternative): Standalone JAR
1. Build (requires Java 17+ and Maven 3.8+):
```bash
mvn -q -DskipTests package
```
2. Run using the packaged JAR:
```bash
java -cp target/torrent-x-1.0.0.jar Main <command> [args]
```

Before you start:
- Downloads are saved to `~/Downloads`.
- For `.torrent` downloads, the `.torrent` file must be located at `~/Downloads/<file>.torrent`.
- You can copy the sample to `~/Downloads` with: `cp torrent_files/sample.torrent ~/Downloads/`.

Quick start (Homebrew):
```bash
download sample.torrent
magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

Quick start (JAR):
```bash
java -cp target/torrent-x-1.0.0.jar Main download sample.torrent
java -cp target/torrent-x-1.0.0.jar Main magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

Commands reference:
```text
download <torrent_file>
  - <torrent_file>: Name of the .torrent file in ~/Downloads (e.g., sample.torrent)

magnet_download <magnet_link>
  - <magnet_link>: Full magnet URI, including xt and optionally tr parameters
```

Notes and limitations:
- Torrent file path resolution is `~/Downloads/<torrent_file>`.
- The client selects the first available peer from the tracker’s compact peer list.
- One peer connection at a time; no swarm management.
- Single-file torrents are supported; multi-file mode is not yet implemented.

### Sample files for testing
- `torrent_files/sample.torrent`: A sample `.torrent` file for quick testing.
- `torrent_files/magnet_links.txt`: Magnet links mapped to filenames for testing magnet downloads.

### Technologies Used
- **Language**: Java (standard library)
- **Build**: Apache Maven
- **Networking**: `java.net.http.HttpClient`, `Socket`
- **Protocol**: BitTorrent wire protocol, extension protocol (`ut_metadata`), bencode

### Resources
- [BEP 3: The BitTorrent Protocol Specification](https://www.bittorrent.org/beps/bep_0003.html)
- [BEP 9: Extension for Peers to Send Metadata Files](https://www.bittorrent.org/beps/bep_0009.html)
- [BEP 10: Extension Protocol](https://www.bittorrent.org/beps/bep_0010.html)
- [Magnet URI scheme (overview)](https://en.wikipedia.org/wiki/Magnet_URI_scheme)

### Contributing
Contributions are welcome! For bug fixes and small improvements:
1. Fork the repo and create a feature branch.
2. Make your change with clear commit messages.
3. Run a local build and basic manual tests.
4. Open a pull request describing the change and rationale.

For larger features (e.g., multi-peer, DHT, multi-file support), please open an issue first to discuss design/approach.

### License
No license file is currently included. If you intend to use or redistribute TorrentX, please open an issue to clarify licensing, or add a `LICENSE` file (e.g., MIT or Apache-2.0) to this repository.

### Disclaimer
Use responsibly and in accordance with your local laws. Download only content you have the right to access.


