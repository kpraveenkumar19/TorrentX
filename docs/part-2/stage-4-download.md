## Stage 4: Download Pieces and File (after Metadata)

After obtaining `piece length`, `pieces`, `name`, and `length` from metadata, the download loop mirrors the `.torrent` flow.

```java
sendInterested(os);
boolean unchoked = false;
while (!unchoked) { PeerMessage msg = readPeerMessage(is); if (msg == null) continue; if (msg.id == 1) unchoked = true; }

try (OutputStream fos = Files.newOutputStream(Paths.get(System.getProperty("user.home"), "Downloads", name),
  StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
  byte[] piecesBytes = piecesStr.getBytes(StandardCharsets.ISO_8859_1);
  int numPieces = piecesBytes.length / 20;
  int lastPieceLength = (int) (totalLen - (long) pieceLength * (numPieces - 1));
  for (int p = 0; p < numPieces; p++) {
    int thisLen = (p == numPieces - 1) ? lastPieceLength : pieceLength;
    byte[] pieceData = new byte[thisLen];
    int offset = 0; int remaining = thisLen; final int blockSize = 16 * 1024;
    while (remaining > 0) { int reqLen2 = Math.min(blockSize, remaining); sendRequest(os, p, offset, reqLen2); offset += reqLen2; remaining -= reqLen2; }
    int received = 0;
    while (received < thisLen) {
      PeerMessage msg = readPeerMessage(is); if (msg == null) continue;
      if (msg.id == 7) { int begin = readInt(msg.payload, 4); int dataLen = msg.payload.length - 8; System.arraycopy(msg.payload, 8, pieceData, begin, dataLen); received += dataLen; }
    }
    byte[] expectedHash = new byte[20]; System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
    byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
    for (int i = 0; i < 20; i++) if (calc[i] != expectedHash[i]) throw new RuntimeException("Piece hash mismatch at index " + p);
    fos.write(pieceData);
  }
}
```
