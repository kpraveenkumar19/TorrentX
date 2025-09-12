---
title: Part 1 - Stage 1 — Bencode Basics
---

# Stage 1: Decode Bencoded Types

This stage explains how TorrentX decodes bencoded values from `.torrent` files.

## Decode bencoded strings

Rule: `<len>:<bytes>`

```25:src/main/java/Main.java
static Object decodeBencode(String bencodedString) {
  if (bencodedString == null || bencodedString.isEmpty()) {
    throw new RuntimeException("Empty input");
  } else {
    return parseElement(bencodedString, 0).value;
  }
}
```

```535:552:src/main/java/Main.java
private static ParseResult parseElement(String input, int startIndex) {
  char c = input.charAt(startIndex);
  if (Character.isDigit(c)) {
    int i = startIndex;
    while (i < input.length() && Character.isDigit(input.charAt(i))) i++;
    if (i >= input.length() || input.charAt(i) != ':') throw new RuntimeException("Invalid string length encoding");
    int length = Integer.parseInt(input.substring(startIndex, i));
    int begin = i + 1;
    int end = begin + length;
    if (end > input.length()) throw new RuntimeException("String extends beyond input length");
    return new ParseResult(input.substring(begin, end), end);
  }
  // ...
}
```

## Decode bencoded integers

Rule: `i<digits>e`

```552:564:src/main/java/Main.java
} else if (c == 'i') {
  int end = startIndex + 1;
  while (end < input.length() && input.charAt(end) != 'e') end++;
  if (end >= input.length()) throw new RuntimeException("Unterminated integer");
  String numberPortion = input.substring(startIndex + 1, end);
  long value = Long.parseLong(numberPortion);
  Object number = (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) ? (int) value : value;
  return new ParseResult(number, end + 1);
}
```

## Decode bencoded lists

Rule: `l <elements> e`

```573:589:src/main/java/Main.java
private static ParseResult parseList(String input, int startIndex) {
  if (input.charAt(startIndex) != 'l') throw new RuntimeException("List must start with 'l'");
  int index = startIndex + 1;
  List<Object> elements = new ArrayList<>();
  while (index < input.length()) {
    char c = input.charAt(index);
    if (c == 'e') return new ParseResult(elements, index + 1);
    ParseResult element = parseElement(input, index);
    elements.add(element.value);
    index = element.nextIndex;
  }
  throw new RuntimeException("Unterminated list");
}
```

## Decode bencoded dictionaries

Rule: `d <pairs> e` where keys are strings

```591:615:src/main/java/Main.java
private static ParseResult parseDict(String input, int startIndex) {
  if (input.charAt(startIndex) != 'd') throw new RuntimeException("Dictionary must start with 'd'");
  int index = startIndex + 1;
  Map<String, Object> map = new LinkedHashMap<>();
  while (index < input.length()) {
    char c = input.charAt(index);
    if (c == 'e') return new ParseResult(map, index + 1);
    if (!Character.isDigit(c)) throw new RuntimeException("Dictionary keys must be strings");
    ParseResult keyParsed = parseElement(input, index);
    String key = (String) keyParsed.value;
    index = keyParsed.nextIndex;
    ParseResult valueParsed = parseElement(input, index);
    map.put(key, valueParsed.value);
    index = valueParsed.nextIndex;
  }
  throw new RuntimeException("Unterminated dictionary");
}
```


