## Stage 3: Request and Receive Metadata

### Request metadata piece 0
```java
Map<String, Object> req = new LinkedHashMap<>();
req.put("msg_type", 0);
req.put("piece", 0);
byte[] hdr = bencode(req);
int len = 2 + hdr.length;
ByteArrayOutputStream out = new ByteArrayOutputStream(4 + len);
byte[] lbuf = new byte[4]; writeInt(lbuf, 0, len);
out.write(lbuf, 0, 4); out.write(20); out.write(peerUtMetadataId);
out.write(hdr, 0, hdr.length);
os.write(out.toByteArray()); os.flush();
```

### Receive metadata and parse info dictionary
```java
byte[] metadataBytes = null; long deadline = System.currentTimeMillis() + 5000;
while (System.currentTimeMillis() < deadline) {
  PeerMessage m = readPeerMessage(is);
  if (m == null) continue;
  if (m.id == 20 && m.payload.length > 1 && (m.payload[0] & 0xFF) == peerUtMetadataId) {
    int headerLen = bencodeElementLength(m.payload, 1);
    if (headerLen <= 0 || 1 + headerLen > m.payload.length) break;
    String hdrStr = new String(m.payload, 1, headerLen, StandardCharsets.ISO_8859_1);
    Object ho = decodeBencode(hdrStr);
    if (ho instanceof Map) {
      Map<String, Object> h = (Map<String, Object>) ho;
      Number mt = (Number) h.get("msg_type");
      if (mt != null && mt.intValue() == 1) {
        int start = 1 + headerLen; int mlen = m.payload.length - start;
        if (mlen > 0) { metadataBytes = new byte[mlen]; System.arraycopy(m.payload, start, metadataBytes, 0, mlen); break; }
      }
    }
  }
}
if (metadataBytes == null) throw new RuntimeException("No metadata received");
String metaStr = new String(metadataBytes, StandardCharsets.ISO_8859_1);
Object metaObj = decodeBencode(metaStr);
Map<String, Object> infoDict = (Map<String, Object>) metaObj;
Number totalLength = (Number) infoDict.get("length");
Number pieceLengthNum = (Number) infoDict.get("piece length");
String piecesStr = (String) infoDict.get("pieces");
String name = (String) infoDict.get("name");
```
