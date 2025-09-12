## TorrentX

### Overview
TorrentX is a minimal, educational BitTorrent client written in Java. It demonstrates an end-to-end flow for both .torrent files and magnet links: bencode parsing, tracker announce, peer handshake, interest/unchoke, block requests, and piece hash verification. The design favors clarity over features (single peer, single-file torrents), making it ideal for learning and experimentation.

### Goals
- Teach the core BitTorrent concepts with clear, readable Java code
- Show both metadata-first (.torrent) and metadata-later (magnet + ut_metadata) workflows
- Keep the implementation self-contained with standard Java libraries

### What it does
- Parses bencoded data structures used by BitTorrent
- Fetches peers from a tracker (compact response)
- Connects to a peer and performs the BitTorrent handshake
- Requests blocks to assemble pieces, verifies against SHA-1, and writes the final file
- Supports magnet links via the extension protocol to fetch metadata from peers

> Note: TorrentX intentionally implements a subset of the protocol. It focuses on single-file torrents, one peer at a time, without DHT or multi-tracker support.
