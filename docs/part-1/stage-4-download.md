## Stage 4: Download Pieces and File

### Become interested and wait for unchoke
```java
private static void sendInterested(OutputStream os) throws IOException {
  byte[] buf = new byte[4 + 1];
  writeInt(buf, 0, 1); buf[4] = 2; os.write(buf); os.flush();
}

sendInterested(os);
boolean unchoked = false;
while (!unchoked) {
  PeerMessage m = readPeerMessage(is);
  if (m == null) continue;
  if (m.id == 1) { unchoked = true; }
}
```

### Pipeline requests per piece, then read blocks
```java
final int blockSize = 16 * 1024;
int offset = 0; int remaining = thisLen;
while (remaining > 0) {
  int reqLen = Math.min(blockSize, remaining);
  sendRequest(os, p, offset, reqLen);
  offset += reqLen; remaining -= reqLen;
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

### Verify and write
```java
byte[] expectedHash = new byte[20];
System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
for (int i = 0; i < 20; i++) if (calc[i] != expectedHash[i]) throw new RuntimeException("Piece hash mismatch at index " + p);
fos.write(pieceData);
```
