---
title: Part 1 - Stage 3 — Tracker and Handshake
---

# Stage 3: Discover peers and handshake

This stage covers building the tracker announce URL, reading a compact peers list, and performing a BitTorrent handshake.

## Discover peers (tracker announce)

```355:368:src/main/java/Main.java
private static String buildTrackerUrl(String baseUrl, byte[] infoHash, String peerId, int port,
                                      long uploaded, long downloaded, long left, int compact) {
  StringBuilder sb = new StringBuilder();
  sb.append(baseUrl);
  sb.append(baseUrl.contains("?") ? "&" : "?");
  sb.append("info_hash=").append(urlEncodeBytes(infoHash));
  sb.append("&peer_id=").append(urlEncodeBytes(peerId.getBytes(StandardCharsets.ISO_8859_1)));
  sb.append("&port=").append(port);
  sb.append("&uploaded=").append(uploaded);
  sb.append("&downloaded=").append(downloaded);
  sb.append("&left=").append(left);
  sb.append("&compact=").append(compact);
  return sb.toString();
}
```

Usage and decoding tracker response:

```89:111:src/main/java/Main.java
String peerId = generatePeerIdString();
String url = buildTrackerUrl(trackerUrl, infoHash, peerId, 6881, 0, 0, totalLen, 1);
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
String respBody = new String(response.body(), StandardCharsets.ISO_8859_1);
Object resp = decodeBencode(respBody);
Map<String, Object> respMap = (Map<String, Object>) resp;
String peersStr = (String) respMap.get("peers");
byte[] peersBytes = peersStr.getBytes(StandardCharsets.ISO_8859_1);
int b0 = peersBytes[0] & 0xFF; int b1 = peersBytes[1] & 0xFF; int b2 = peersBytes[2] & 0xFF; int b3 = peersBytes[3] & 0xFF;
String host = b0 + "." + b1 + "." + b2 + "." + b3;
int port = ((peersBytes[4] & 0xFF) << 8) | (peersBytes[5] & 0xFF);
```

## Peer handshake

```449:456:src/main/java/Main.java
private static byte[] buildHandshake(byte[] infoHash, byte[] peerId) {
  ByteArrayOutputStream out = new ByteArrayOutputStream(68);
  out.write(19);
  writeRaw("BitTorrent protocol", out);
  out.write(new byte[8], 0, 8);
  out.write(infoHash, 0, 20);
  out.write(peerId, 0, 20);
  return out.toByteArray();
}
```

Perform handshake and wait for unchoke:

```124:148:src/main/java/Main.java
byte[] peerIdBytes = peerId.getBytes(StandardCharsets.ISO_8859_1);
byte[] handshake = buildHandshake(infoHash, peerIdBytes);
try (Socket socket = new Socket()) {
  socket.connect(new InetSocketAddress(host, port), 10000);
  socket.setSoTimeout(20000);
  OutputStream os = socket.getOutputStream();
  InputStream is = socket.getInputStream();
  os.write(handshake); os.flush();
  byte[] respHs = readN(is, 68);
  sendInterested(os);
  boolean unchoked = false;
  while (!unchoked) {
    PeerMessage m = readPeerMessage(is);
    if (m == null) continue;
    if (m.id == 1) unchoked = true; // unchoke
  }
}
```


