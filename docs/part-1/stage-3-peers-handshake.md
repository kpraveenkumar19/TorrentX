## Stage 3: Discover Peers and Handshake

### Build tracker URL (compact peer list)
```java
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

### Announce and parse compact peers
```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
String respBody = new String(response.body(), StandardCharsets.ISO_8859_1);
Object resp = decodeBencode(respBody);
Map<String, Object> respMap = (Map<String, Object>) resp;
String peersStr = (String) respMap.get("peers");
byte[] peersBytes = peersStr.getBytes(StandardCharsets.ISO_8859_1);
String host = (peersBytes[0] & 0xFF) + "." + (peersBytes[1] & 0xFF) + "." + (peersBytes[2] & 0xFF) + "." + (peersBytes[3] & 0xFF);
int port = ((peersBytes[4] & 0xFF) << 8) | (peersBytes[5] & 0xFF);
```

### Handshake
```java
byte[] handshake = buildHandshake(infoHash, peerIdBytes);
try (Socket socket = new Socket()) {
  socket.connect(new InetSocketAddress(host, port), 10000);
  socket.setSoTimeout(20000);
  OutputStream os = socket.getOutputStream();
  InputStream is = socket.getInputStream();
  os.write(handshake); os.flush();
  byte[] respHs = readN(is, 68);
}

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
