# TorrentX

Minimal BitTorrent CLI client written in Java. TorrentX implements the core of the BitTorrent protocol which supports magnet links and can download a single-file torrent from an HTTP tracker and a single peer, which then saves the result to your Downloads folder.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
  - [macOS (Homebrew)](#macos-homebrew)
  - [Windows (Clone + Build)](#windows-clone--build)
- [Usage](#usage)
  - [download <torrent_file>](#download-torrent_file)
  - [magnet_download <magnet_link>](#magnet_download-magnet_link)
  - [Notes and Limitations](#notes-and-limitations)
- [Resources](#resources)
- [Contributing](#contributing)
- [License](#license)

## Features

- Download files via BitTorrent protocol
- Magnet link support
- HTTP tracker announce with compact peer list
- Single peer download flow with piece pipelining
- SHA‑1 verification of each piece
- Saves output to your system `Downloads` directory

## Installation

```bash
brew tap kpraveenkumar19/torrentx
brew install torrentx
```

## Usage

After installing (via Homebrew) the commands are available globally:

```bash
download <torrent_file>
magnet_download <magnet_link>
```

### `download <torrent_file>`

Downloads a single-file torrent referenced by `<torrent_file>`.

Examples:

```bash
download sample.torrent
```

Important:
- Place the `.torrent` file inside your `~/Downloads` folder. The client reads the torrent file from `Downloads` by name, e.g. `~/Downloads/sample.torrent`.
- The downloaded content will be saved to `~/Downloads/<name>` where `<name>` is taken from the torrent's `info.name`.

### `magnet_download <magnet_link>`

Downloads via a magnet link. A tracker URL must be present in the magnet (the `tr` parameter), and the infohash must be provided via `xt=urn:btih:<infohash>`.

Examples:

```bash
magnet_download "magnet:?xt=urn:btih:<infohash>&tr=https%3A%2F%2Ftracker.example.org%2Fannounce"
```

### Notes and Limitations

- Single-file torrents only (multi-file torrents are not supported)
- HTTP trackers only (no UDP tracker)
- Connects to the first peer from the tracker compact list; no peer rotation or retry strategy
- Expects the `.torrent` file to reside in `~/Downloads` and writes output there

## Resources

- [BEP 3: The BitTorrent Protocol Specification](https://www.bittorrent.org/beps/bep_0003.html)
- [BEP 9: Extension for Peers to Send Metadata Files](https://www.bittorrent.org/beps/bep_0009.html)
- [BEP 10: Extension Protocol](https://www.bittorrent.org/beps/bep_0010.html)
- [Magnet URI scheme (overview)](https://en.wikipedia.org/wiki/Magnet_URI_scheme)

## Contributing

Contributions are welcome! To propose changes:

1. Fork the repository and create a feature branch
2. Make your changes
3. Open a Pull Request with a clear description and examples

Guidelines:
- Keep the implementation minimal and readable
- Prefer small, focused changes with clear commit messages
- Document user-facing behavior in this README when adding features

