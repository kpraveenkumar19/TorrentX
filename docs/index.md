# TorrentX

TorrentX is a simple, educational BitTorrent client written in Java. It can download files from a `.torrent` file or a magnet link, implementing a minimal subset of the BitTorrent protocol end-to-end: bencode parsing, tracker announce, peer handshake, interest/unchoke, block requests, and piece hash verification.

> TorrentX is intentionally minimal for learning and demonstration. It does not implement advanced peer management, DHT, multiple trackers, rate limiting, or multi-file torrents.

## Goals

- Provide a clean, readable reference implementation of core BitTorrent flows
- Focus on clarity with a single peer connection at a time
- Demonstrate both metadata-first (`.torrent`) and magnet-based (`ut_metadata`) workflows

## Features

- .torrent download: parse metadata, announce to tracker, connect to a peer, request blocks, verify and write the output file
- Magnet link download: use the extension protocol (`ut_metadata`) to fetch metadata from a peer before downloading pieces

See the left sidebar to navigate stages for both `.torrent` and magnet workflows.


