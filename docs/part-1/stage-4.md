---
title: Part 1 - Stage 4 — Download Pieces & File
---

# Stage 4: Download a piece and the whole file

After unchoke, TorrentX requests blocks for each piece, reconstructs the piece, verifies the SHA‑1 hash, and writes to disk.

## Download a piece

```155:176:src/main/java/Main.java
final int blockSize = 16 * 1024;
int offset = 0;
int remaining = thisLen;
while (remaining > 0) {
  int reqLen = Math.min(blockSize, remaining);
  sendRequest(os, p, offset, reqLen);
  offset += reqLen;
  remaining -= reqLen;
}
int received = 0;
while (received < thisLen) {
  PeerMessage msg = readPeerMessage(is);
  if (msg == null) continue;
  if (msg.id == 7) {
    int begin = readInt(msg.payload, 4);
    int dataLen = msg.payload.length - 8;
    System.arraycopy(msg.payload, 8, pieceData, begin, dataLen);
    received += dataLen;
  }
}
```

Request format helper:

```396:405:src/main/java/Main.java
private static void sendRequest(OutputStream os, int index, int begin, int length) throws IOException {
  byte[] buf = new byte[4 + 1 + 12];
  writeInt(buf, 0, 13);
  buf[4] = 6;
  writeInt(buf, 5, index);
  writeInt(buf, 9, begin);
  writeInt(buf, 13, length);
  os.write(buf);
  os.flush();
}
```

## Download the whole file

```149:188:src/main/java/Main.java
Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", name);
try (OutputStream fos = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
  for (int p = 0; p < numPieces; p++) {
    int thisLen = (p == numPieces - 1) ? lastPieceLength : pieceLength;
    byte[] pieceData = new byte[thisLen];
    // request and fill pieceData ...
    // Verify hash
    byte[] expectedHash = new byte[20];
    System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
    byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
    for (int i = 0; i < 20; i++) if (calc[i] != expectedHash[i]) throw new RuntimeException("Piece hash mismatch at index " + p);
    fos.write(pieceData);
  }
}
```


