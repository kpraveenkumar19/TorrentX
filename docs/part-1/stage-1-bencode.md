## Stage 1: Bencode Decoding

### Concepts
- Strings: `<len>:<bytes>`
- Integers: `i<digits>e`
- Lists: `l<elements>e`
- Dictionaries: `d<key><value>...e` (keys are strings)

### Decoder entrypoint
```java
// decodeBencode
static Object decodeBencode(String bencodedString) {
  if (bencodedString == null || bencodedString.isEmpty()) {
    throw new RuntimeException("Empty input");
  } else {
     return parseElement(bencodedString, 0).value;
  }
}
```

### Core parsing
```java
// ParseResult container
private static class ParseResult {
  final Object value;
  final int nextIndex;
  private ParseResult(Object value, int nextIndex) {
    this.value = value;
    this.nextIndex = nextIndex;
  }
}

// parseElement dispatches on first char
private static ParseResult parseElement(String input, int startIndex) {
  char c = input.charAt(startIndex);
  if (Character.isDigit(c)) {
    int i = startIndex;
    while (i < input.length() && Character.isDigit(input.charAt(i))) { i++; }
    if (i >= input.length() || input.charAt(i) != ':') {
      throw new RuntimeException("Invalid string length encoding");
    }
    int length = Integer.parseInt(input.substring(startIndex, i));
    int begin = i + 1; int end = begin + length;
    if (end > input.length()) { throw new RuntimeException("String extends beyond input length"); }
    return new ParseResult(input.substring(begin, end), end);
  } else if (c == 'i') {
    int end = startIndex + 1; while (end < input.length() && input.charAt(end) != 'e') { end++; }
    if (end >= input.length()) { throw new RuntimeException("Unterminated integer"); }
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
  if (input.charAt(startIndex) != 'l') throw new RuntimeException("List must start with 'l'");
  int index = startIndex + 1; List<Object> elements = new ArrayList<>();
  while (index < input.length()) {
    char c = input.charAt(index);
    if (c == 'e') return new ParseResult(elements, index + 1);
    ParseResult element = parseElement(input, index);
    elements.add(element.value);
    index = element.nextIndex;
  }
  throw new RuntimeException("Unterminated list");
}

private static ParseResult parseDict(String input, int startIndex) {
  if (input.charAt(startIndex) != 'd') throw new RuntimeException("Dictionary must start with 'd'");
  int index = startIndex + 1; Map<String, Object> map = new LinkedHashMap<>();
  while (index < input.length()) {
    char c = input.charAt(index);
    if (c == 'e') return new ParseResult(map, index + 1);
    if (!Character.isDigit(c)) throw new RuntimeException("Dictionary keys must be strings");
    ParseResult keyParsed = parseElement(input, index);
    String key = (String) keyParsed.value; index = keyParsed.nextIndex;
    ParseResult valueParsed = parseElement(input, index);
    map.put(key, valueParsed.value); index = valueParsed.nextIndex;
  }
  throw new RuntimeException("Unterminated dictionary");
}
```

### Encoder (for creating bencoded bytes)
```java
private static byte[] bencode(Object value) {
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  writeBencode(value, out, StandardCharsets.ISO_8859_1);
  return out.toByteArray();
}

private static void writeBencode(Object value, ByteArrayOutputStream out, Charset charset) {
  if (value instanceof String) {
    String s = (String) value; byte[] data = s.getBytes(charset);
    writeStrToByteOS(Integer.toString(data.length), out);
    out.write(':'); out.write(data, 0, data.length);
  } else if (value instanceof Integer || value instanceof Long) {
    long v = (value instanceof Integer) ? ((Integer) value).longValue() : (Long) value;
    out.write('i'); writeStrToByteOS(Long.toString(v), out); out.write('e');
  } else if (value instanceof List) {
    out.write('l');
    @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
    for (Object elem : list) { writeBencode(elem, out, charset); }
    out.write('e');
  } else if (value instanceof Map) {
    out.write('d');
    @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
    List<String> keys = new ArrayList<>(map.keySet()); keys.sort((a, b) -> a.compareTo(b));
    for (String key : keys) { writeBencode(key, out, charset); writeBencode(map.get(key), out, charset); }
    out.write('e');
  } else if (value == null) {
    throw new RuntimeException("Cannot bencode null");
  } else {
    throw new RuntimeException("Unsupported type for bencode: " + value.getClass().getName());
  }
}
```
