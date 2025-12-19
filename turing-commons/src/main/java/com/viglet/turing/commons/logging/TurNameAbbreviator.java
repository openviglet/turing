package com.viglet.turing.commons.logging;

import java.util.ArrayList;
import java.util.List;

public sealed abstract class TurNameAbbreviator
        permits TurNameAbbreviator.DropElementAbbreviator,
        TurNameAbbreviator.MaxElementAbbreviator,
        TurNameAbbreviator.NOPAbbreviator,
        TurNameAbbreviator.PatternAbbreviator {

    private static final char DOT = '.';

    private static final class Holder {
        static final TurNameAbbreviator DEFAULT = new NOPAbbreviator();
    }

    public abstract void abbreviate(int nameStart, StringBuilder sb);

    public static TurNameAbbreviator getAbbreviator(final String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return Holder.DEFAULT;
        }

        final String trimmed = pattern.trim();

        if (isNumeric(trimmed)) {
            try {
                int val = Integer.parseInt(trimmed);
                return val >= 0 ? new MaxElementAbbreviator(val) : new DropElementAbbreviator(-val);
            } catch (NumberFormatException e) {
                return Holder.DEFAULT;
            }
        }

        return parsePattern(trimmed);
    }

    private static boolean isNumeric(String s) {
        if (s.isEmpty()) return false;
        int start = (s.charAt(0) == '-') ? 1 : 0;
        for (int i = start; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return s.length() > start;
    }

    private static TurNameAbbreviator parsePattern(String pattern) {
        List<PatternAbbreviatorFragment> fragments = new ArrayList<>();
        String[] parts = pattern.split("\\.", -1);

        for (String part : parts) {
            if (part.isEmpty()) {
                fragments.add(new PatternAbbreviatorFragment(0, '\0'));
                continue;
            }

            char first = part.charAt(0);
            int charCount = (first == '*') ? Integer.MAX_VALUE :
                    (Character.isDigit(first) ? Character.getNumericValue(first) : 0);

            char ellipsis = (part.length() > 1) ? part.charAt(1) : '\0';
            fragments.add(new PatternAbbreviatorFragment(charCount, ellipsis));
        }
        return new PatternAbbreviator(fragments);
    }

    private record PatternAbbreviatorFragment(int charCount, char ellipsis) {
        public int apply(StringBuilder sb, int startPos) {
            int nextDot = indexOf(sb, startPos);
            int effectiveEnd = (nextDot == -1) ? sb.length() : nextDot;

            if ((effectiveEnd - startPos) > charCount) {
                sb.delete(startPos + charCount, effectiveEnd);
                int currentPos = startPos + charCount;
                if (ellipsis != '\0') {
                    sb.insert(currentPos, ellipsis);
                    currentPos++;
                }
                nextDot = (nextDot == -1) ? -1 : currentPos + 1;
            } else {
                nextDot = (nextDot == -1) ? -1 : nextDot + 1;
            }
            return nextDot;
        }
    }

    static final class DropElementAbbreviator extends TurNameAbbreviator {
        private final int count;
        DropElementAbbreviator(int count) { this.count = count; }

        @Override
        public void abbreviate(int nameStart, StringBuilder sb) {
            int i = count;
            for (int pos = indexOf(sb, nameStart); pos != -1; pos = indexOf(sb, pos + 1)) {
                if (--i == 0) {
                    sb.delete(nameStart, pos + 1);
                    break;
                }
            }
        }
    }

    static final class MaxElementAbbreviator extends TurNameAbbreviator {
        private final int count;
        MaxElementAbbreviator(int count) { this.count = count; }

        @Override
        public void abbreviate(int nameStart, StringBuilder sb) {
            if (count == 0) return;
            int end = sb.length() - 1;
            for (int i = count; i > 0; i--) {
                end = lastIndexOf(sb, end - 1);
                if (end < nameStart) return;
            }
            sb.delete(nameStart, end + 1);
        }
    }

    static final class NOPAbbreviator extends TurNameAbbreviator {
        @Override
        public void abbreviate(int nameStart, StringBuilder sb) {}
    }

    static final class PatternAbbreviator extends TurNameAbbreviator {
        private final PatternAbbreviatorFragment[] fragments;

        PatternAbbreviator(List<PatternAbbreviatorFragment> fragments) {
            this.fragments = fragments.toArray(PatternAbbreviatorFragment[]::new);
        }

        @Override
        public void abbreviate(int nameStart, StringBuilder sb) {
            int pos = nameStart;
            for (int i = 0; i < fragments.length && pos < sb.length() && pos != -1; i++) {
                pos = fragments[i].apply(sb, pos);
                if (i == fragments.length - 1) {
                    while (pos < sb.length() && pos != -1) {
                        pos = fragments[i].apply(sb, pos);
                    }
                }
            }
        }
    }

    private static int indexOf(StringBuilder sb, int fromIndex) {
        int len = sb.length();
        for (int i = Math.max(fromIndex, 0); i < len; i++) {
            if (sb.charAt(i) == TurNameAbbreviator.DOT) return i;
        }
        return -1;
    }

    private static int lastIndexOf(StringBuilder sb, int fromIndex) {
        for (int i = Math.min(fromIndex, sb.length() - 1); i >= 0; i--) {
            if (sb.charAt(i) == TurNameAbbreviator.DOT) return i;
        }
        return -1;
    }
}