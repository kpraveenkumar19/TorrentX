## Stage 2: Torrent Parsing and Hashes

### Parse `.torrent`
```java
// Read .torrent from ~/Downloads and decode bencode
yte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.home"), "Downloads", torrentFileName));
String bencoded = new String(bytes, StandardCharsets.ISO_8859_1);
Object decoded = decodeBencode(bencoded);
Map<String, Object> top = (Map<String, Object>) decoded;
String trackerUrl = (String) top.get("announce");
Map<String, Object> info = (Map<String, Object>) top.get("info");
Number totalLength = (Number) info.get("length");
Number pieceLengthNum = (Number) info.get("piece length");
String piecesStr = (String) info.get("pieces");
String name = (String) info.get("name");
```

### Calculate info hash (SHA-1 of bencoded `info`)
```java
byte[] infoBencoded = bencode(info);
byte[] infoHash = MessageDigest.getInstance("SHA-1").digest(infoBencoded);
```

### Piece layout
```java
byte[] piecesBytes = piecesStr.getBytes(StandardCharsets.ISO_8859_1);
int numPieces = piecesBytes.length / 20;
int pieceLength = pieceLengthNum.intValue();
long totalLen = totalLength.longValue();
int lastPieceLength = (int) (totalLen - (long) pieceLength * (numPieces - 1));
```

### Notes
- The `pieces` field is a concatenation of 20-byte SHA-1 hashes, one per piece.
- `lastPieceLength` handles torrents where total size is not a multiple of `piece length`.
