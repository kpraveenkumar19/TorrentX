---
title: Part 2 - Stage 4 — Download with Magnet
---

# Stage 4: Download pieces and the whole file (magnet)

Once metadata is fetched, TorrentX follows the same piece download loop as the `.torrent` path.

## Download a piece

```324:344:src/main/java/Main.java
int thisLen = (p == numPieces - 1) ? lastPieceLength : pieceLength;
byte[] pieceData = new byte[thisLen];
final int blockSize = 16 * 1024;
int offset = 0; int remaining = thisLen;
while (remaining > 0) {
  int reqLen2 = Math.min(blockSize, remaining);
  sendRequest(os, p, offset, reqLen2);
  offset += reqLen2; remaining -= reqLen2;
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

## Download the whole file

```317:351:src/main/java/Main.java
Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", name);
try (OutputStream fos = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
  byte[] piecesBytes = piecesStr.getBytes(StandardCharsets.ISO_8859_1);
  int numPieces = piecesBytes.length / 20;
  int pieceLength = pieceLengthNum.intValue();
  long totalLen = totalLength.longValue();
  int lastPieceLength = (int) (totalLen - (long) pieceLength * (numPieces - 1));
  for (int p = 0; p < numPieces; p++) {
    int thisLen = (p == numPieces - 1) ? lastPieceLength : pieceLength;
    byte[] pieceData = new byte[thisLen];
    // request and fill pieceData ...
    byte[] expectedHash = new byte[20]; System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
    byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
    for (int i = 0; i < 20; i++) if (calc[i] != expectedHash[i]) throw new RuntimeException("Piece hash mismatch at index " + p);
    fos.write(pieceData);
  }
}
```


