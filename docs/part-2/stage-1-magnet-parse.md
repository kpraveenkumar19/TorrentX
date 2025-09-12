## Stage 1: Parse Magnet and Announce Extension Support

### Parse magnet link
```java
if (!magnetLink.startsWith("magnet:?")) throw new IllegalArgumentException("Invalid magnet link");
String query = magnetLink.substring("magnet:?".length());
for (String p : query.split("&")) {
  int eq = p.indexOf('=');
  String key = eq >= 0 ? p.substring(0, eq) : p;
  String val = eq >= 0 ? p.substring(eq + 1) : "";
  val = URLDecoder.decode(val, StandardCharsets.UTF_8);
  if ("tr".equals(key)) trackerUrl = val;
  if ("xt".equals(key) && val.startsWith("urn:btih:")) infoHashHex = val.substring(9).toLowerCase();
}
byte[] infoHash = hexToBytes(infoHashHex);
```

### Announce to tracker (compact peers)
```java
String url = buildTrackerUrl(trackerUrl, infoHash, peerId, 6881, 0, 0, 999, 1);
```

### Handshake with extension support bit
```java
private static byte[] buildHandshakeWithExtensions(byte[] infoHash, byte[] peerId) {
  ByteArrayOutputStream out = new ByteArrayOutputStream(68);
  out.write(19);
  writeRaw("BitTorrent protocol", out);
  byte[] reserved = new byte[8];
  reserved[5] = 0x10; // extension protocol bit
  out.write(reserved, 0, 8);
  out.write(infoHash, 0, 20);
  out.write(peerId, 0, 20);
  return out.toByteArray();
}

// Validate peer supports extensions
byte[] respHs = readN(is, 68);
boolean peerSupportsExtensions = (respHs[20 + 5] & 0x10) != 0;
if (!peerSupportsExtensions) throw new RuntimeException("Peer doesn't support extensions");
```
