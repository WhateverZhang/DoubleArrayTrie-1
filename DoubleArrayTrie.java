package com.moonsun.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DoubleArrayTrie {
  private static final Logger LOG = LoggerFactory.getLogger(DoubleArrayTrie.class);

  private static final int DEFAULT_INIT_SIZE = 1024;
  private static final int END_VALUE = -1;

  private int count = 1;
  private Map<Character, Integer> charCodeMap;

  private int base[];
  private int check[];

  public DoubleArrayTrie() {
    this(DEFAULT_INIT_SIZE);
  }

  public DoubleArrayTrie(int initSize) {
    base = new int[initSize];
    check = new int[initSize];
    charCodeMap = new HashMap<>();
    base[0] = 1;
  }

  private void extendArray() {
    assert base.length == check.length;
    if (base.length < 4096) {
      base = Arrays.copyOf(base, base.length * 2);
      check = Arrays.copyOf(check, check.length * 2);
    } else {
      base = Arrays.copyOf(base, base.length + base.length / 4);
      check = Arrays.copyOf(check, check.length + check.length / 4);
    }
  }

  private int getCharCode(char c) {
    Integer code = charCodeMap.get(c);
    if (code == null) {
      synchronized (this) {
        charCodeMap.putIfAbsent(c, count++);
      }
    }
    return charCodeMap.get(c);
  }

  /**
   * construct a double array trie from a strings
   *
   * @param strs list of strings
   */
  public synchronized void insert(List<String> strs) {
    // clear base and check array before init trie
    base = new int[base.length];
    check = new int[check.length];
    base[0] = 1;
    this.insertStrings(strs, 0);
  }

  private void insertStrings(List<String> strs, int parentIndex) {
    // mapping of current char to the substring
    Map<Character, List<String>> substringMapping = new HashMap<>();
    for (String str : strs) {
      substringMapping.computeIfAbsent(str.charAt(0), k -> new ArrayList<>());
      // add substring to map if str has substring other than the first character
      if (str.length() > 1) {
        substringMapping.get(str.charAt(0)).add(str.substring(1));
      }
    }

    this.insertSiblings(substringMapping, parentIndex);
  }

  private void insertSiblings(Map<Character, List<String>> substringMapping, int parentIndex) {
    Set<Character> siblings = substringMapping.keySet();
    int begin = base[parentIndex];
    while (true) {
      int count = 0;
      for (Character ch : siblings) {
        if (begin + getCharCode(ch) >= base.length) {
          extendArray();
        }
        if (check[begin + getCharCode(ch)] != 0) {
          begin++;
          break;
        }
        count++;
      }
      // available begin for all chars
      if (count == siblings.size()) {
        for (Character ch : siblings) {
          check[begin + getCharCode(ch)] = begin;
          base[parentIndex] = begin;

        }
        break;
      }
    }

    for (Map.Entry<Character, List<String>> entry : substringMapping.entrySet()) {
      if (entry.getValue().isEmpty()) {
        base[begin + getCharCode(entry.getKey())] = END_VALUE;
      } else {
        this.insertStrings(entry.getValue(), begin + getCharCode(entry.getKey()));
      }
    }

  }

  /**
   * @param str the string to be matched
   * @return return the matched prefix or null if not matched
   */
  public String findPrefix(String str) {
    int count = 0;
    int prePos = 0;
    for (Character ch : str.toCharArray()) {
      int curPos = base[prePos] + getCharCode(ch);
      if (curPos >= base.length || check[curPos] != base[prePos]) {
        return null;
      }
      count++;
      if (base[curPos] < 0) {
        break;
      }
      prePos = curPos;
    }
    return str.substring(0, count + 1);
  }

  public void dumpArrays() {
    assert base.length == check.length;

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Character, Integer> entry : charCodeMap.entrySet()) {
      sb.append(entry.getKey()).append(" ==> ").append(entry.getValue()).append("; ");
    }
    LOG.info(sb.toString());

    for (int i = 0; i < base.length; i++) {
      if (!(base[i] == 0 && check[i] == 0)) {
        LOG.info("base[" + i + "]=" + base[i] + " ==> check[" + i + "]=" + check[i]);
      }
    }
  }

  public static void main(String[] args) {
    DoubleArrayTrie dart = new DoubleArrayTrie();
    List<String> words = Arrays.asList("ab", "ac", "bd", "cd", "bacd");
    dart.insert(words);
    dart.dumpArrays();

    assert dart.search("ab").equals("ab");
    assert dart.search("ac").equals("ac");
    assert dart.search("bacdd").equals("bacd");
    assert dart.search("awbc") == null;
    assert dart.search("acb") == null;
  }
}
