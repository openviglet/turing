package com.viglet.turing.se;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.viglet.turing.solr.TurSolrInstance;

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

    @Test
    void shouldReturnNullWhenClasspathStopwordsCannotBeRead() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource("classpath:/solr/conf/lang/stopwords.txt")).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("cannot read"));

        TurSEStopWord stopWord = new TurSEStopWord(resourceLoader);
        Method method = TurSEStopWord.class.getDeclaredMethod("getStopWordsFromClassPath");
        method.setAccessible(true);

        InputStream stream = (InputStream) method.invoke(stopWord);

        assertThat(stream).isNull();
    }

    @Test
    void shouldReturnNullWhenAnalyzerHasNoFilters() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        TurSEStopWord stopWord = new TurSEStopWord(resourceLoader);

        AnalyzerDefinition analyzerDefinition = mock(AnalyzerDefinition.class);
        when(analyzerDefinition.getFilters()).thenReturn(null);

        Method method = TurSEStopWord.class.getDeclaredMethod("getStopWord", TurSolrInstance.class,
                AnalyzerDefinition.class);
        method.setAccessible(true);

        Object result = method.invoke(stopWord, mock(TurSolrInstance.class), analyzerDefinition);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenStopwordFilterHasNoWordsAttribute() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        TurSEStopWord stopWord = new TurSEStopWord(resourceLoader);

        AnalyzerDefinition analyzerDefinition = mock(AnalyzerDefinition.class);
        when(analyzerDefinition.getFilters()).thenReturn(List.of(Map.of("class", "solr.StopFilterFactory")));

        Method method = TurSEStopWord.class.getDeclaredMethod("getStopWord", TurSolrInstance.class,
                AnalyzerDefinition.class);
        method.setAccessible(true);

        Object result = method.invoke(stopWord, mock(TurSolrInstance.class), analyzerDefinition);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenStopwordDownloadFails() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        TurSEStopWord stopWord = new TurSEStopWord(resourceLoader);

        AnalyzerDefinition analyzerDefinition = mock(AnalyzerDefinition.class);
        when(analyzerDefinition.getFilters())
                .thenReturn(List.of(Map.of("class", "solr.StopFilterFactory", "words", "stopwords.txt")));

        TurSolrInstance turSolrInstance = mock(TurSolrInstance.class);
        when(turSolrInstance.getSolrUrl()).thenReturn(URI.create("http://localhost:65535/solr").toURL());
        when(turSolrInstance.getCore()).thenReturn("core1");

        Method method = TurSEStopWord.class.getDeclaredMethod("getStopWord", TurSolrInstance.class,
                AnalyzerDefinition.class);
        method.setAccessible(true);

        Object result = method.invoke(stopWord, turSolrInstance, analyzerDefinition);
        assertThat(result).isNull();
    }
}
