---
title: Part 2 - Stage 1 — Magnet Link Parsing
---

# Stage 1: Parse magnet link and announce support

TorrentX parses a magnet URI to extract the tracker (`tr`) and info hash (`xt=urn:btih:`...).

## Parse magnet link

```192:207:src/main/java/Main.java
private static void handleMagnetDownload(String magnetLink) throws Exception {
  String trackerUrl = null;
  String infoHashHex = null;
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
  if (infoHashHex == null) throw new IllegalArgumentException("Missing xt");
  byte[] infoHash = hexToBytes(infoHashHex);
```

## Announce extension support

TorrentX uses the extension protocol to obtain metadata. The handshake advertises support:

```482:493:src/main/java/Main.java
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
```


