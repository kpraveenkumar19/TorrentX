## TorrentX

TorrentX is a simple, educational BitTorrent client written in Java. It demonstrates an end-to-end, minimal implementation of the BitTorrent protocol: bencode parsing, tracker announce, peer handshake, interest/unchoke, block requests, and piece hash verification. It supports both .torrent files and magnet links via the extension protocol (ut_metadata).

### Why it exists

- To provide a concise, readable reference for how BitTorrent actually works on the wire
- To focus on learning: single peer, single-file torrents, and the clearest possible code

### What it does

- Parses a .torrent file, computes the info hash, announces to a tracker, connects to a peer, requests blocks, verifies piece hashes, and writes the result to `~/Downloads`
- For magnet links, uses the extension protocol to obtain metadata (the info dictionary) before downloading pieces

### Goals

- Keep the code small and approachable using only Java standard libraries
- Highlight the core protocol primitives and message flows
- Provide a step-by-step documentation of stages from decoding bencode through downloading the full file

### Scope and limitations

- One peer connection at a time (no swarm management)
- Single-file torrents only
- No DHT, multi-tracker rotation, rate limiting, or advanced peer strategies

### Quick links

- [Stages walkthrough](stages/)
- [Installation & Usage](../install/)


