---
title: Part 1 - Stage 2 — Torrent Parsing & Pieces
---

# Stage 2: Parse torrent file and compute info hash

This stage shows how TorrentX parses a `.torrent` file, extracts the `info` dictionary, computes the `info_hash`, and derives piece hashes.

## Parse torrent file

```55:79:src/main/java/Main.java
private static void downloadFile(String torrentFileName) throws Exception {
  Path torrentFilePath = Paths.get(System.getProperty("user.home"), "Downloads", torrentFileName);
  byte[] bytes = Files.readAllBytes(torrentFilePath);
  String bencoded = new String(bytes, StandardCharsets.ISO_8859_1);
  Object decoded = decodeBencode(bencoded);
  if (!(decoded instanceof Map)) throw new RuntimeException("Top-level bencode is not a dictionary");
  Map<String, Object> top = (Map<String, Object>) decoded;
  String trackerUrl = (String) top.get("announce");
  Map<String, Object> info = (Map<String, Object>) top.get("info");
  if (trackerUrl == null || info == null) throw new RuntimeException("Missing required fields in torrent file");
  Number totalLength = (Number) info.get("length");
  Number pieceLengthNum = (Number) info.get("piece length");
  String piecesStr = (String) info.get("pieces");
  String name = (String) info.get("name");
  if (totalLength == null || pieceLengthNum == null || piecesStr == null) throw new RuntimeException("Missing length/piece length/pieces in torrent");
```

## Calculate info hash

```79:85:src/main/java/Main.java
byte[] infoBencoded = bencode(info);
MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
byte[] infoHash = sha1.digest(infoBencoded);
```

## Piece hashes

```83:88:src/main/java/Main.java
byte[] piecesBytes = piecesStr.getBytes(StandardCharsets.ISO_8859_1);
int numPieces = piecesBytes.length / 20;
int pieceLength = pieceLengthNum.intValue();
long totalLen = totalLength.longValue();
int lastPieceLength = (int) (totalLen - (long) pieceLength * (numPieces - 1));
```


