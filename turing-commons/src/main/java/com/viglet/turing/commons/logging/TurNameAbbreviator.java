package com.viglet.turing.commons.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * NameAbbreviator generates abbreviated logger and class names.
 */
public abstract class TurNameAbbreviator {

    /**
     * Abbreviator that drops starting path elements.
     */
    private static class DropElementAbbreviator extends TurNameAbbreviator {
        /**
         * Maximum number of path elements to output.
         */
        private final int count;

        /**
         * Create new instance.
         *
         * @param count maximum number of path elements to output.
         */
        public DropElementAbbreviator(final int count) {
            this.count = count;
        }

        /**
         * Abbreviate name.
         *
         * @param stringBuilder buffer to append abbreviation.
         * @param nameStart start of name to abbreviate.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuilder stringBuilder) {
            int i = count;
            for (int pos = stringBuilder.indexOf(".", nameStart); pos != -1; pos = stringBuilder.indexOf(".", pos + 1)) {
                if (--i == 0) {
                    stringBuilder.delete(nameStart, pos + 1);
                    break;
                }
            }
        }
    }

    /**
     * Abbreviator that drops starting path elements.
     */
    private static class MaxElementAbbreviator extends TurNameAbbreviator {
        /**
         * Maximum number of path elements to output.
         */
        private final int count;

        /**
         * Create new instance.
         *
         * @param count maximum number of path elements to output.
         */
        public MaxElementAbbreviator(final int count) {
            this.count = count;
        }

        /**
         * Abbreviate name.
         *
         * @param stringBuilder buffer to append abbreviation.
         * @param nameStart start of name to abbreviate.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuilder stringBuilder) {
            // We subtract 1 from 'len' when assigning to 'end' to avoid out of
            // bounds exception in return r.substring(end+1, len). This can happen if
            // precision is 1 and the category name ends with a dot.
            int end = stringBuilder.length() - 1;

            final String bufString = stringBuilder.toString();
            for (int i = count; i > 0; i--) {
                end = bufString.lastIndexOf(".", end - 1);

                if ((end == -1) || (end < nameStart)) {
                    return;
                }
            }

            stringBuilder.delete(nameStart, end + 1);
        }
    }

    /**
     * Abbreviator that simply appends full name to buffer.
     */
    private static class NOPAbbreviator extends TurNameAbbreviator {
        /**
         * Constructor.
         */
        public NOPAbbreviator() {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuilder stringBuilder) {}
    }

    /**
     * Pattern abbreviator.
     *
     *
     */
    private static class PatternAbbreviator extends TurNameAbbreviator {
        /**
         * Element abbreviation patterns.
         */
        private final PatternAbbreviatorFragment[] fragments;

        /**
         * Create PatternAbbreviator.
         *
         * @param fragments element abbreviation patterns.
         */
        public PatternAbbreviator(final List<PatternAbbreviatorFragment> fragments) {
            if (fragments.isEmpty()) {
                throw new IllegalArgumentException("fragments must have at least one element");
            }

            this.fragments = new PatternAbbreviatorFragment[fragments.size()];
            fragments.toArray(this.fragments);
        }

        /**
         * Abbreviate name.
         *
         * @param stringBuilder buffer that abbreviated name is appended.
         * @param nameStart start of name.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuilder stringBuilder) {
            //
            // all non-terminal patterns are executed once
            //
            int pos = nameStart;

            for (int i = 0; (i < (fragments.length - 1)) && (pos < stringBuilder.length()); i++) {
                pos = fragments[i].abbreviate(stringBuilder, pos);
            }

            //
            // last pattern in executed repeatedly
            //
            final PatternAbbreviatorFragment terminalFragment = fragments[fragments.length - 1];

            while ((pos < stringBuilder.length()) && (pos >= 0)) {
                pos = terminalFragment.abbreviate(stringBuilder, pos);
            }
        }
    }

    /**
     * Fragment of a pattern abbreviator.
     *
     * @param charCount Count of initial characters of element to output.
     * @param ellipsis  Character used to represent dropped characters. '\0' indicates no representation of dropped characters.
     */
        private record PatternAbbreviatorFragment(int charCount, char ellipsis) {

        /**
             * Abbreviate element of name.
             *
             * @param stringBuilder      buffer to receive element.
             * @param startPos starting index of name element.
             * @return starting index of next element.
             */
            public int abbreviate(final StringBuilder stringBuilder, final int startPos) {
                int nextDot = stringBuilder.toString().indexOf(".", startPos);

                if (nextDot != -1) {
                    if ((nextDot - startPos) > charCount) {
                        stringBuilder.delete(startPos + charCount, nextDot);
                        nextDot = startPos + charCount;

                        if (ellipsis != '\0') {
                            stringBuilder.insert(nextDot, ellipsis);
                            nextDot++;
                        }
                    }

                    nextDot++;
                }

                return nextDot;
            }
        }

    /**
     * Default (no abbreviation) abbreviator.
     */
    private static final TurNameAbbreviator DEFAULT = new NOPAbbreviator();

    /**
     * Gets an abbreviator.
     * For example, "%logger{2}" will output only 2 elements of the logger name, %logger{-2} will drop 2 elements from the
     * logger name, "%logger{1.}" will output only the first character of the non-final elements in the name,
     * "%logger{1~.2~} will output the first character of the first element, two characters of the second and subsequent
     * elements and will use a tilde to indicate abbreviated characters.
     *
     * @param pattern abbreviation pattern.
     * @return abbreviator, will not be null.
     */
    public static TurNameAbbreviator getAbbreviator(final String pattern) {
        if (!pattern.isEmpty()) {
            // if pattern is just spaces and numbers then
            // use MaxElementAbbreviator
            final String trimmed = pattern.trim();

            if (trimmed.isEmpty()) {
                return DEFAULT;
            }

            int i = 0;
            if (trimmed.charAt(0) == '-') {
                i++;
            }
            while ((i < trimmed.length()) && (trimmed.charAt(i) >= '0') && (trimmed.charAt(i) <= '9')) {
                i++;
            }

            //
            // if all blanks and digits
            //
            if (i == trimmed.length()) {
                final int elements = Integer.parseInt(trimmed);
                if (elements >= 0) {
                    return new MaxElementAbbreviator(elements);
                } else {
                    return new DropElementAbbreviator(-elements);
                }
            }

            final ArrayList<PatternAbbreviatorFragment> fragments = new ArrayList<>(5);
            char ellipsis;
            int charCount;
            int pos = 0;

            while ((pos < trimmed.length()) && (pos >= 0)) {
                int ellipsisPos = pos;

                if (trimmed.charAt(pos) == '*') {
                    charCount = Integer.MAX_VALUE;
                    ellipsisPos++;
                } else {
                    if ((trimmed.charAt(pos) >= '0') && (trimmed.charAt(pos) <= '9')) {
                        charCount = trimmed.charAt(pos) - '0';
                        ellipsisPos++;
                    } else {
                        charCount = 0;
                    }
                }

                ellipsis = '\0';

                if (ellipsisPos < trimmed.length()) {
                    ellipsis = trimmed.charAt(ellipsisPos);

                    if (ellipsis == '.') {
                        ellipsis = '\0';
                    }
                }

                fragments.add(new PatternAbbreviatorFragment(charCount, ellipsis));
                pos = trimmed.indexOf(".", pos);

                if (pos == -1) {
                    break;
                }

                pos++;
            }

            return new PatternAbbreviator(fragments);
        }

        //
        // no matching abbreviation, return defaultAbbreviator
        //
        return DEFAULT;
    }

    /**
     * Abbreviates a name in a StringBuilder.
     *
     * @param nameStart starting position of name in buf.
     * @param stringBuilder buffer, may not be null.
     */
    public abstract void abbreviate(final int nameStart, final StringBuilder stringBuilder);
}
