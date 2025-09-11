## TorrentX

Simple, educational BitTorrent client written in Java. TorrentX can download files from a `.torrent` file or a magnet link. It implements a minimal subset of the BitTorrent protocol end-to-end: parsing bencode, tracker announce, peer handshake, interest/unchoke, block requests, and piece hash verification.

### Features
- **.torrent download**: Parse bencoded metadata, announce to tracker, connect to a peer, request blocks, and write the final file to your `~/Downloads` directory.
- **Magnet link download**: Use the extension protocol (`ut_metadata`) to fetch metadata from a peer before downloading pieces.
- **Tracker query**: Compact peer list request/response and simple peer selection.
- **Piece integrity**: Verifies every piece with SHA‑1 against the info dictionary.
- **Single peer flow**: Focuses on clarity and learning with one peer connection at a time.

> Note: TorrentX is intentionally minimal for learning and demonstration. It does not yet implement advanced peer management, DHT, multiple trackers, rate limiting, or multi-file torrents.

### Installation

#### Homebrew (macOS)
```bash
brew tap kpraveenkumar19/torrentx
brew install torrentx
```

#### From source (macOS/Linux)
Prerequisites:
- Java 17+ (JDK)
- Maven 3.8+

Clone/build/run:
```bash
git clone <your-repo-url>.git
cd TorrentX

# Option 1: Use the helper script
./your_program.sh --help  # builds and runs Main with your args

# Option 2: Build and run via Maven/Java
mvn -q -DskipTests package
java -cp target/classes Main <command> [args]

# Option 3: Run from the JAR (already produced by mvn package)
java -cp target/torrent-x-1.0.0.jar Main <command> [args]
```

### Usage

TorrentX expects outputs to be written to your `~/Downloads` directory. Place your `.torrent` file in `~/Downloads`, or use a magnet link.

Commands:
```bash
# Download using a .torrent file (file must be in ~/Downloads)
download <torrent_file>

# Download using a magnet link
magnet_download <magnet_link>
```

Examples:
```bash
# Homebrew install (macOS)
download <torrent file>
magnet_download <magnet link>

# From source via helper script
./your_program.sh download ubuntu.torrent
./your_program.sh magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"

# Direct with Java (after mvn package)
java -cp target/classes Main download ubuntu.torrent
java -cp target/classes Main magnet_download "magnet:?xt=urn:btih:<infohash>&tr=<tracker>"
```

Behavioral notes and limitations:
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


