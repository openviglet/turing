package com.viglet.turing.se;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class TurSEStopWordTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldParseStopwordLinesAndIgnoreCommentsAfterPipe() throws Exception {
        String rawStopwords = "the|article\nand\nor|logical\n";
        InputStreamReader isr = new InputStreamReader(
                new ByteArrayInputStream(rawStopwords.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

        Method addStopwordsToList = TurSEStopWord.class.getDeclaredMethod("addStopwordsToList",
                InputStreamReader.class);
        addStopwordsToList.setAccessible(true);

        List<String> stopWords = (List<String>) addStopwordsToList.invoke(null, isr);

        assertThat(stopWords).containsExactly("the", "and", "or");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnImmutableEmptyStopwordList() throws Exception {
        Method getEmptyStopwordList = TurSEStopWord.class.getDeclaredMethod("getEmptyStopwordList");
        getEmptyStopwordList.setAccessible(true);

        List<String> empty = (List<String>) getEmptyStopwordList.invoke(null);

        assertThat(empty).isEmpty();
        assertThatThrownBy(() -> empty.add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
