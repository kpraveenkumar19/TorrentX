## Stage 2: Extension Handshake

### Send extension handshake (advertise ut_metadata)
```java
private static void sendExtensionHandshake(OutputStream os, int utMetadataId) throws IOException {
  Map<String, Object> m = new LinkedHashMap<>();
  m.put("ut_metadata", utMetadataId);
  Map<String, Object> root = new LinkedHashMap<>();
  root.put("m", m);
  byte[] dict = bencode(root);
  int length = 2 + dict.length; // id (20) + ext id (0) + dict
  ByteArrayOutputStream out = new ByteArrayOutputStream(4 + length);
  byte[] lenBuf = new byte[4]; writeInt(lenBuf, 0, length);
  out.write(lenBuf, 0, 4); out.write(20); out.write(0); out.write(dict, 0, dict.length);
  os.write(out.toByteArray()); os.flush();
}
```

### Receive peer's extension handshake and extract ut_metadata id
```java
Integer peerUtMetadataId = null; long deadline = System.currentTimeMillis() + 5000;
while (System.currentTimeMillis() < deadline) {
  PeerMessage m = readPeerMessage(is);
  if (m == null) continue;
  if (m.id == 20 && m.payload.length > 0 && (m.payload[0] & 0xFF) == 0) {
    String dictStr = new String(m.payload, 1, m.payload.length - 1, StandardCharsets.ISO_8859_1);
    Object d = decodeBencode(dictStr);
    if (d instanceof Map) {
      Map<String, Object> root = (Map<String, Object>) d;
      Object mv = root.get("m");
      if (mv instanceof Map) {
        Map<String, Object> mm = (Map<String, Object>) mv;
        Object ut = mm.get("ut_metadata");
        if (ut instanceof Number) { peerUtMetadataId = ((Number) ut).intValue(); break; }
      }
    }
  }
}
if (peerUtMetadataId == null) throw new RuntimeException("Peer didn't send extension handshake");
```
