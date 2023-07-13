package com.alibaba.util.adt;

import java.util.*;

public class Trie {

    // a HashTable based Trie implementation

    private static class TrieNode {
        public HashMap<Character, TrieNode> letters = new HashMap<>();
        public boolean isEnd;

        private TrieNode getChild(char c) {
            return letters.get(c);
        }
        private TrieNode getChildWithCreating(char c) {
            TrieNode child = letters.get(c);
            if (child == null) {  // if no then create one,
                child = new TrieNode();
                letters.put(c, child);
            }
            return child;
        }
    }

    public static class Pair<P,Q> {
        public P p;
        public Q q;
        public Pair(P p, Q q) {
            this.p = p;
            this.q = q;
        }
    }

    private final TrieNode root;  // the first letter

    public Trie() {
        root = new TrieNode();
    }

    private TrieNode searchPrefix(String prefix) {  // helper: return the last child
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            current = current.getChild(c);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Pair<TrieNode, String> findLCP(String s) {
        TrieNode current = root;
        TrieNode prev = current;
        final char[] chars = s.toCharArray();
        int i;
        for (i = 0; i < chars.length; i++) {
            char c = chars[i];
            current = current.getChild(c);
            if (current == null) {
                break;
            }
            prev = current;
        }
        return new Pair<>(prev, new String(chars, 0, i /* i is excluded */));
    }

    /**
     * Add a String into the Trie.
     * @param word
     */
    public void add(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current = current.getChildWithCreating(c);  // Note: will create an empty Node as an EOF.
        }
        current.isEnd = true;
    }

    /**
     * Search a String in this Trie.
     * @param word
     * @return true/false
     */
    public boolean search(String word) {
        TrieNode end = searchPrefix(word);
        return end != null && end.isEnd;
    }

    /**
     * Search a String Prefix in this Trie.
     * @param prefix
     * @return true/false
     */
    public boolean startsWith(String prefix) {
        return searchPrefix(prefix) != null;
    }

    /**
     * get all strings with this prefix
     * @param prefix
     * @return candidates
     */
    public List<String> getCandidatesWithPrefix(String prefix) {
        TrieNode node = searchPrefix(prefix);
        if (node == null) {
            // no such prefix.
            return null;
        }

        List<String> candidates = new ArrayList<>();
        traversal(node, new StringBuilder(prefix), candidates);
        return candidates;
    }

    /**
     * get all strings that have the Longest Common Prefix with s.
     * @param s
     * @return candidates
     */
    public List<String> getCandidatesWithLCP(String s) {
        final Pair<TrieNode,String> pair = findLCP(s);
        final TrieNode node = pair.p;
        final String prefix = pair.q;
        // result will never be a null.
        List<String> candidates = new ArrayList<>();
        traversal(node, new StringBuilder(prefix), candidates);
        return candidates;
    }

    /**
     * Dump all Strings in this Trie.
     * @return String seperated in System.lineSeparator().
     */
    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        traversal(root, new StringBuilder(), list);

        StringBuilder sb = new StringBuilder();
        for (String string : list) {
            sb.append(string).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static void traversal(TrieNode current, StringBuilder prefix, List<String> list) {
        if (current.letters.size() == 0) {  // an EOF
            list.add(prefix.toString());
            return;
        }

        if (current.isEnd) {
            list.add(prefix.toString());
        }

        for (Map.Entry<Character, TrieNode> entry : current.letters.entrySet()) {
            prefix.append(entry.getKey());
            traversal(entry.getValue(), prefix, list);
            // backtracking
            prefix.deleteCharAt(prefix.length()-1);
        }
    }

}
