---
title: Part 2 - Stage 3 — Request and Receive Metadata
---

# Stage 3: Request metadata and receive metadata

After learning the peer's `ut_metadata` message id, TorrentX requests piece 0 of the metadata and parses the response.

## Request metadata

```263:273:src/main/java/Main.java
Map<String, Object> req = new LinkedHashMap<>();
req.put("msg_type", 0);
req.put("piece", 0);
byte[] hdr = bencode(req);
int len = 2 + hdr.length;
ByteArrayOutputStream out = new ByteArrayOutputStream(4 + len);
byte[] lbuf = new byte[4]; writeInt(lbuf, 0, len);
out.write(lbuf, 0, 4); out.write(20); out.write(peerUtMetadataId); out.write(hdr, 0, hdr.length);
os.write(out.toByteArray()); os.flush();
```

## Receive metadata

```275:309:src/main/java/Main.java
byte[] metadataBytes = null;
deadline = System.currentTimeMillis() + 5000;
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
        int start = 1 + headerLen;
        int mlen = m.payload.length - start;
        if (mlen > 0) {
          metadataBytes = new byte[mlen];
          System.arraycopy(m.payload, start, metadataBytes, 0, mlen);
          break;
        }
      }
    }
  }
}
if (metadataBytes == null) throw new RuntimeException("No metadata received");
```


