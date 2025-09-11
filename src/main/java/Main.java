import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.net.URLDecoder;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args)throws Exception {
    
        String command = args[0];
    
        if ("download".equals(command)) {
          // Expected: download <torrent_file>
          String torrentFileName;
          if (args.length == 2) {
            torrentFileName = args[1];
          } else {
            throw new IllegalArgumentException("Usage: download <torrent_file>");
          }
          downloadFile(torrentFileName);
        } else if ("magnet_download".equals(command)) {
          // magnet_download <magnet_link>
          String magnetLink;
          if (args.length == 2) {
            magnetLink = args[1];
            System.out.println("Magnet link: " + magnetLink);
          } else {
            throw new IllegalArgumentException("Usage: magnet_download <magnet_link>");
          }
          handleMagnetDownload(magnetLink);
        } else {
          System.out.println("Unknown command: " + command);
        }
    
      }
    
      private static void downloadFile(String torrentFileName) throws Exception {
        // Parse torrent
        Path torrentFilePath = Paths.get(System.getProperty("user.home"), "Downloads", torrentFileName);
        byte[] bytes = Files.readAllBytes(torrentFilePath);
        String bencoded = new String(bytes, StandardCharsets.ISO_8859_1);
        Object decoded = decodeBencode(bencoded);
        if (!(decoded instanceof Map)) {
          throw new RuntimeException("Top-level bencode is not a dictionary");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> top = (Map<String, Object>) decoded;
        String trackerUrl = (String) top.get("announce");
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) top.get("info");
        if (trackerUrl == null || info == null) {
          throw new RuntimeException("Missing required fields in torrent file");
        }
        Number totalLength = (Number) info.get("length");
        Number pieceLengthNum = (Number) info.get("piece length");
        String piecesStr = (String) info.get("pieces");
        String name = (String) info.get("name");
        if (totalLength == null || pieceLengthNum == null || piecesStr == null) {
          throw new RuntimeException("Missing length/piece length/pieces in torrent");
        }
        byte[] infoBencoded = bencode(info);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] infoHash = sha1.digest(infoBencoded);
    
        byte[] piecesBytes = piecesStr.getBytes(StandardCharsets.ISO_8859_1);
        int numPieces = piecesBytes.length / 20;
        int pieceLength = pieceLengthNum.intValue();
        long totalLen = totalLength.longValue();
        int lastPieceLength = (int) (totalLen - (long) pieceLength * (numPieces - 1));
    
        // Discover a peer
        String peerId = generatePeerIdString();
        String url = buildTrackerUrl(trackerUrl, infoHash, peerId, 6881, 0, 0, totalLen, 1);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
          throw new RuntimeException("Tracker request failed: " + response.statusCode());
        }
        String respBody = new String(response.body(), StandardCharsets.ISO_8859_1);
        Object resp = decodeBencode(respBody);
        if (!(resp instanceof Map)) {
          throw new RuntimeException("Tracker response not a dictionary");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = (Map<String, Object>) resp;
        String peersStr = (String) respMap.get("peers");
        if (peersStr == null) {
          throw new RuntimeException("No peers in tracker response");
        }
        byte[] peersBytes = peersStr.getBytes(StandardCharsets.ISO_8859_1);
        if (peersBytes.length < 6) {
          throw new RuntimeException("Empty peers list");
        }
        // Prefer local peer 127.0.0.1 if present
    
        int b0 = peersBytes[0] & 0xFF;
        int b1 = peersBytes[1] & 0xFF;
        int b2 = peersBytes[2] & 0xFF;
        int b3 = peersBytes[3] & 0xFF;
        int portHigh = peersBytes[4] & 0xFF;
        int portLow = peersBytes[5] & 0xFF;
        String host = b0 + "." + b1 + "." + b2 + "." + b3;
        int port = (portHigh << 8) | portLow;
    
        // Connect and handshake once
        byte[] peerIdBytes = peerId.getBytes(StandardCharsets.ISO_8859_1);
        byte[] handshake = buildHandshake(infoHash, peerIdBytes);
    
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress(host, port), 10000);
          socket.setSoTimeout(20000);
          OutputStream os = socket.getOutputStream();
          InputStream is = socket.getInputStream();
    
          os.write(handshake);
          os.flush();
          byte[] respHs = readN(is, 68);
          if (respHs[0] != 19) throw new RuntimeException("Invalid handshake length");
    
          // Interested/unchoke gating
          sendInterested(os);
          boolean unchoked = false;
          while (!unchoked) {
            PeerMessage m = readPeerMessage(is);
            if (m == null) continue;
            if (m.id == 1) { // unchoke
              unchoked = true;
            }
          }
          Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", name);
          // Download all pieces
          try (OutputStream fos = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            for (int p = 0; p < numPieces; p++) {
              int thisLen = (p == numPieces - 1) ? lastPieceLength : pieceLength;
              byte[] pieceData = new byte[thisLen];
              final int blockSize = 16 * 1024;
              int offset = 0;
              int remaining = thisLen;
              // Pipeline: send all requests for this piece first
              while (remaining > 0) {
                int reqLen = Math.min(blockSize, remaining);
                sendRequest(os, p, offset, reqLen);
                offset += reqLen;
                remaining -= reqLen;
              }
              int received = 0;
              // Now read piece messages until we've filled the piece
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
              // Verify hash
              byte[] expectedHash = new byte[20];
              System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
              byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
              for (int i = 0; i < 20; i++) {
                if (calc[i] != expectedHash[i]) {
                  throw new RuntimeException("Piece hash mismatch at index " + p);
                }
              }
              fos.write(pieceData);
            }
          }
        }
      }
    
      private static void handleMagnetDownload(String magnetLink) throws Exception {
        // Parse magnet
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
    
        // Discover a peer
        String peerId = generatePeerIdString();
        String url = buildTrackerUrl(trackerUrl, infoHash, peerId, 6881, 0, 0, 999, 1);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) throw new RuntimeException("Tracker request failed: " + response.statusCode());
        String respBody = new String(response.body(), StandardCharsets.ISO_8859_1);
        Object resp = decodeBencode(respBody);
        @SuppressWarnings("unchecked") Map<String, Object> respMap = (Map<String, Object>) resp;
        String peersStr = (String) respMap.get("peers");
        byte[] peersBytes = peersStr.getBytes(StandardCharsets.ISO_8859_1);
        if (peersBytes.length < 6) throw new RuntimeException("Empty peers list");
        int b0 = peersBytes[0] & 0xFF, b1 = peersBytes[1] & 0xFF, b2 = peersBytes[2] & 0xFF, b3 = peersBytes[3] & 0xFF;
        String host = b0 + "." + b1 + "." + b2 + "." + b3;
        int port = ((peersBytes[4] & 0xFF) << 8) | (peersBytes[5] & 0xFF);
    
        byte[] peerIdBytes = peerId.getBytes(StandardCharsets.ISO_8859_1);
        byte[] handshake = buildHandshakeWithExtensions(infoHash, peerIdBytes);
    
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress(host, port), 8000);
          socket.setSoTimeout(20000);
          OutputStream os = socket.getOutputStream();
          InputStream is = socket.getInputStream();
          os.write(handshake); os.flush();
          byte[] respHs = readN(is, 68);
          boolean peerSupportsExtensions = (respHs[20 + 5] & 0x10) != 0;
          try { readPeerMessage(is); } catch (Exception ignored) {}
          if (!peerSupportsExtensions) throw new RuntimeException("Peer doesn't support extensions");
    
          // Send ext handshake and read peer's ut_metadata id
          sendExtensionHandshake(os, 1);
          Integer peerUtMetadataId = null;
          long deadline = System.currentTimeMillis() + 5000;
          while (System.currentTimeMillis() < deadline) {
            PeerMessage m = readPeerMessage(is);
            if (m == null) continue;
            if (m.id == 20 && m.payload.length > 0 && (m.payload[0] & 0xFF) == 0) {
              String dictStr = new String(m.payload, 1, m.payload.length - 1, StandardCharsets.ISO_8859_1);
              Object d = decodeBencode(dictStr);
              if (d instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> root = (Map<String, Object>) d;
                Object mv = root.get("m");
                if (mv instanceof Map) {
                  @SuppressWarnings("unchecked") Map<String, Object> mm = (Map<String, Object>) mv;
                  Object ut = mm.get("ut_metadata");
                  if (ut instanceof Number) { peerUtMetadataId = ((Number) ut).intValue(); break; }
                }
              }
            }
          }
          if (peerUtMetadataId == null) throw new RuntimeException("Peer didn't send extension handshake");
    
          // Request metadata
          Map<String, Object> req = new LinkedHashMap<>();
          req.put("msg_type", 0);
          req.put("piece", 0);
          byte[] hdr = bencode(req);
          int len = 2 + hdr.length;
          ByteArrayOutputStream out = new ByteArrayOutputStream(4 + len);
          byte[] lbuf = new byte[4]; writeInt(lbuf, 0, len);
          out.write(lbuf, 0, 4); out.write(20); out.write(peerUtMetadataId); out.write(hdr, 0, hdr.length);
          os.write(out.toByteArray()); os.flush();
    
          // Receive metadata
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
                @SuppressWarnings("unchecked") Map<String, Object> h = (Map<String, Object>) ho;
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
          String metaStr = new String(metadataBytes, StandardCharsets.ISO_8859_1);
          Object metaObj = decodeBencode(metaStr);
          @SuppressWarnings("unchecked") Map<String, Object> infoDict = (Map<String, Object>) metaObj;
          Number totalLength = (Number) infoDict.get("length");
          Number pieceLengthNum = (Number) infoDict.get("piece length");
          String piecesStr = (String) infoDict.get("pieces");
          String name = (String) infoDict.get("name");
          if (totalLength == null || pieceLengthNum == null || piecesStr == null) throw new RuntimeException("Missing fields in metadata");
    
          // Interested and wait for unchoke
          sendInterested(os);
          boolean unchoked = false;
          while (!unchoked) { PeerMessage msg = readPeerMessage(is); if (msg == null) continue; if (msg.id == 1) unchoked = true; }
    
          // Download all pieces
    
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
              // Verify hash
              byte[] expectedHash = new byte[20]; System.arraycopy(piecesBytes, p * 20, expectedHash, 0, 20);
              byte[] calc = MessageDigest.getInstance("SHA-1").digest(pieceData);
              for (int i = 0; i < 20; i++) if (calc[i] != expectedHash[i]) throw new RuntimeException("Piece hash mismatch at index " + p);
              fos.write(pieceData);
            }
          }
        }
      }
    
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
    
      private static String urlEncodeBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
          int v = b & 0xFF;
          // Encode all non-unreserved characters with %HH
          if ((v >= 'A' && v <= 'Z') || (v >= 'a' && v <= 'z') || (v >= '0' && v <= '9') || v == '-' || v == '_' || v == '.' || v == '~') {
            sb.append((char) v);
          } else {
            sb.append('%');
            String hex = Integer.toHexString(v).toUpperCase();
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
          }
        }
        return sb.toString();
      }
    
      private static void sendInterested(OutputStream os) throws IOException {
        // length=1, id=2
        byte[] buf = new byte[4 + 1];
        writeInt(buf, 0, 1);
        buf[4] = 2;
        os.write(buf);
        os.flush();
      }
    
      private static void sendRequest(OutputStream os, int index, int begin, int length) throws IOException {
        byte[] buf = new byte[4 + 1 + 12];
        writeInt(buf, 0, 13);
        buf[4] = 6;
        writeInt(buf, 5, index);
        writeInt(buf, 9, begin);
        writeInt(buf, 13, length);
        os.write(buf);
        os.flush();
      }
    
      private static class PeerMessage {
        final int id; // -1 for keep-alive
        final byte[] payload; // excludes id
        PeerMessage(int id, byte[] payload) { this.id = id; this.payload = payload; }
      }
    
      private static PeerMessage readPeerMessage(InputStream is) throws Exception {
        byte[] lenBytes = readN(is, 4);
        int length = readInt(lenBytes, 0);
        if (length == 0) {
          return null; // keep-alive
        }
        byte[] body = readN(is, length);
        int id = body[0] & 0xFF;
        byte[] payload = new byte[length - 1];
        System.arraycopy(body, 1, payload, 0, payload.length);
        return new PeerMessage(id, payload);
      }
    
      private static void writeInt(byte[] arr, int offset, int value) {
        arr[offset] = (byte) ((value >>> 24) & 0xFF);
        arr[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        arr[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        arr[offset + 3] = (byte) (value & 0xFF);
      }
    
      private static int readInt(byte[] arr, int offset) {
        return ((arr[offset] & 0xFF) << 24) | ((arr[offset + 1] & 0xFF) << 16) | ((arr[offset + 2] & 0xFF) << 8) | (arr[offset + 3] & 0xFF);
      }
    
      private static String generatePeerIdString() {
        // 20 ASCII bytes; use random hex-like for readability
        byte[] id = generatePeerIdBytes();
        return new String(id, StandardCharsets.ISO_8859_1);
      }
    
      private static byte[] generatePeerIdBytes() {
        byte[] id = new byte[20];
        new SecureRandom().nextBytes(id);
        return id;
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
    
      private static void writeRaw(String s, ByteArrayOutputStream out) {
        byte[] data = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(data, 0, data.length);
      }
    
      private static void sendExtensionHandshake(OutputStream os, int utMetadataId) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ut_metadata", utMetadataId);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("m", m);
        byte[] dict = bencode(root);
        int length = 2 + dict.length; // id (20) + ext id (0) + dict
        ByteArrayOutputStream out = new ByteArrayOutputStream(4 + length);
        byte[] lenBuf = new byte[4];
        writeInt(lenBuf, 0, length);
        out.write(lenBuf, 0, 4);
        out.write(20); // extension message
        out.write(0);  // handshake message id
        out.write(dict, 0, dict.length);
        os.write(out.toByteArray());
        os.flush();
      }
    
      private static byte[] buildHandshakeWithExtensions(byte[] infoHash, byte[] peerId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(68);
        out.write(19);
        writeRaw("BitTorrent protocol", out);
        byte[] reserved = new byte[8];
        // Set 20th bit from right (extension protocol): this is bit 4 of byte index 5 (0-based) in big-endian
        reserved[5] = 0x10; // 00010000
        out.write(reserved, 0, 8);
        out.write(infoHash, 0, 20);
        out.write(peerId, 0, 20);
        return out.toByteArray();
      }
    
      private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
          out[i/2] = (byte) Integer.parseInt(hex.substring(i, i+2), 16);
        }
        return out;
      }
    
      private static byte[] readN(InputStream is, int n) throws Exception {
        byte[] buf = new byte[n];
        int offset = 0;
        while (offset < n) {
          int read = is.read(buf, offset, n - offset);
          if (read == -1) {
            throw new RuntimeException("Unexpected EOF while reading handshake");
          }
          offset += read;
        }
        return buf;
      }
    
      static Object decodeBencode(String bencodedString) {
        if (bencodedString == null || bencodedString.isEmpty()) {
          throw new RuntimeException("Empty input");
        }
        else {
           return parseElement(bencodedString, 0).value;
        }
      }
      
      private static class ParseResult {
        final Object value;
        final int nextIndex;
        private ParseResult(Object value, int nextIndex) {
          this.value = value;
          this.nextIndex = nextIndex;
        }
      }
    
      private static ParseResult parseElement(String input, int startIndex) {
        char c = input.charAt(startIndex);
        if (Character.isDigit(c)) {
          int i = startIndex;
          while (i < input.length() && Character.isDigit(input.charAt(i))) {
            i++;
          }
          if (i >= input.length() || input.charAt(i) != ':') {
            throw new RuntimeException("Invalid string length encoding");
          }
          int length = Integer.parseInt(input.substring(startIndex, i));
          int begin = i + 1;
          int end = begin + length;
          if (end > input.length()) {
            throw new RuntimeException("String extends beyond input length");
          }
          return new ParseResult(input.substring(begin, end), end);
        } else if (c == 'i') {
          int end = startIndex + 1;
          while (end < input.length() && input.charAt(end) != 'e') {
            end++;
          }
          if (end >= input.length()) {
            throw new RuntimeException("Unterminated integer");
          }
          String numberPortion = input.substring(startIndex + 1, end);
          long value = Long.parseLong(numberPortion);
          Object number = (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) ? (int) value : value;
          return new ParseResult(number, end + 1);
        } else if (c == 'l') {
          return parseList(input, startIndex);
        } else if (c == 'd') {
          return parseDict(input, startIndex);
        } else {
          throw new RuntimeException("Unknown type: " + c);
        }
      }
    
      private static ParseResult parseList(String input, int startIndex) {
        if (input.charAt(startIndex) != 'l') {
          throw new RuntimeException("List must start with 'l'");
        }
        int index = startIndex + 1;
        List<Object> elements = new ArrayList<>();
        while (index < input.length()) {
          char c = input.charAt(index);
          if (c == 'e') {
            return new ParseResult(elements, index + 1);
          }
          ParseResult element = parseElement(input, index);
          elements.add(element.value);
          index = element.nextIndex;
        }
        throw new RuntimeException("Unterminated list");
      }
    
      private static ParseResult parseDict(String input, int startIndex) {
        if (input.charAt(startIndex) != 'd') {
          throw new RuntimeException("Dictionary must start with 'd'");
        }
        int index = startIndex + 1;
        Map<String, Object> map = new LinkedHashMap<>();
        while (index < input.length()) {
          char c = input.charAt(index);
          if (c == 'e') {
            return new ParseResult(map, index + 1);
          }
          // Keys must be strings
          if (!Character.isDigit(c)) {
            throw new RuntimeException("Dictionary keys must be strings");
          }
          ParseResult keyParsed = parseElement(input, index);
          String key = (String) keyParsed.value;
          index = keyParsed.nextIndex;
    
          ParseResult valueParsed = parseElement(input, index);
          map.put(key, valueParsed.value);
          index = valueParsed.nextIndex;
        }
        throw new RuntimeException("Unterminated dictionary");
      }
    
      private static byte[] bencode(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBencode(value, out, StandardCharsets.ISO_8859_1);
        return out.toByteArray();
      }
    
      private static void writeBencode(Object value, ByteArrayOutputStream out, Charset charset) {
        if (value instanceof String) {
          String s = (String) value;
          byte[] data = s.getBytes(charset);
          writeStrToByteOS(Integer.toString(data.length), out);
          out.write(':');
          out.write(data, 0, data.length);
        } else if (value instanceof Integer || value instanceof Long) {
          long v = (value instanceof Integer) ? ((Integer) value).longValue() : (Long) value;
          out.write('i');
           writeStrToByteOS(Long.toString(v), out);
          out.write('e');
        } else if (value instanceof List) {
          out.write('l');
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>) value;
          for (Object elem : list) {
            writeBencode(elem, out, charset);
          }
          out.write('e');
        } else if (value instanceof Map) {
          out.write('d');
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) value;
          List<String> keys = new ArrayList<>(map.keySet());
          keys.sort((a, b) -> a.compareTo(b));
          for (String key : keys) {
            writeBencode(key, out, charset);
            writeBencode(map.get(key), out, charset);
          }
          out.write('e');
        } else if (value == null) {
          throw new RuntimeException("Cannot bencode null");
        } else {
          throw new RuntimeException("Unsupported type for bencode: " + value.getClass().getName());
        }
      }
    
      private static void writeStrToByteOS(String s, ByteArrayOutputStream out) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        out.write(bytes, 0, bytes.length);
      }
    
      private static int bencodeElementLength(byte[] buf, int offset) {
        int i = offset;
        int end = buf.length;
        if (i >= end) return 0;
        byte c = buf[i];
        if (c >= '0' && c <= '9') {
          int j = i;
          while (j < end && buf[j] >= '0' && buf[j] <= '9') j++;
          if (j >= end || buf[j] != ':') return 0;
          int len = Integer.parseInt(new String(buf, i, j - i, StandardCharsets.US_ASCII));
          return (j - i) + 1 + len;
        } else if (c == 'i') {
          int j = i + 1;
          while (j < end && buf[j] != 'e') j++;
          if (j >= end) return 0;
          return (j - i) + 1;
        } else if (c == 'l' || c == 'd') {
          int j = i + 1;
          while (j < end && buf[j] != 'e') {
            int elemLen = bencodeElementLength(buf, j);
            if (elemLen <= 0) return 0;
            j += elemLen;
          }
          if (j >= end) return 0;
          return (j - i) + 1;
        }
        return 0;
    }
}

