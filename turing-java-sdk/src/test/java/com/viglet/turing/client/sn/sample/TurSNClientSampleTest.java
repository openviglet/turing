package com.viglet.turing.client.sn.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.viglet.turing.client.sn.HttpTurSNServer;
import com.viglet.turing.client.sn.TurSNGroupList;
import com.viglet.turing.client.sn.TurSNQuery;
import com.viglet.turing.client.sn.didyoumean.TurSNDidYouMean;
import com.viglet.turing.client.sn.didyoumean.TurSNDidYouMeanText;
import com.viglet.turing.client.sn.facet.TurSNFacetFieldList;
import com.viglet.turing.client.sn.pagination.TurSNPagination;
import com.viglet.turing.client.sn.response.QueryTurSNResponse;

class TurSNClientSampleTest {

    @Test
    void shouldBuildQueryFromDefaultAndCustomArgs() throws Exception {
        HttpTurSNServer turSNServer = mock(HttpTurSNServer.class);
        QueryTurSNResponse queryTurSNResponse = new QueryTurSNResponse();
        when(turSNServer.query(any(TurSNQuery.class))).thenReturn(queryTurSNResponse);

        QueryTurSNResponse defaultResponse = invoke("query", new Class<?>[] { String[].class, HttpTurSNServer.class },
                new String[] {}, turSNServer);
        QueryTurSNResponse explicitResponse = invoke("query", new Class<?>[] { String[].class, HttpTurSNServer.class },
                new String[] { "custom search" }, turSNServer);

        assertThat(defaultResponse).isSameAs(queryTurSNResponse);
        assertThat(explicitResponse).isSameAs(queryTurSNResponse);

        ArgumentCaptor<TurSNQuery> queryCaptor = ArgumentCaptor.forClass(TurSNQuery.class);
        verify(turSNServer, times(2)).query(queryCaptor.capture());

        assertThat(queryCaptor.getAllValues().getFirst().getQuery()).isEqualTo("tast");
        assertThat(queryCaptor.getAllValues().getLast().getQuery()).isEqualTo("custom search");
    }

    @Test
    void shouldExecuteSampleHelpersWithoutExternalHttpCalls() throws Exception {
        HttpTurSNServer turSNServer = mock(HttpTurSNServer.class);
        when(turSNServer.getLatestSearches(10)).thenReturn(List.of("first", "second"));
        when(turSNServer.autoComplete(any())).thenReturn(List.of("viglet", "vigor"));

        QueryTurSNResponse queryResponse = new QueryTurSNResponse();
        TurSNGroupList turSNGroupList = new TurSNGroupList();
        turSNGroupList.setTurSNGroups(new ArrayList<>());
        queryResponse.setGroupResponse(turSNGroupList);
        when(turSNServer.query(any(TurSNQuery.class))).thenReturn(queryResponse);

        invoke("latestSearches", new Class<?>[] { HttpTurSNServer.class }, turSNServer);
        invoke("autoComplete", new Class<?>[] { HttpTurSNServer.class }, turSNServer);
        invoke("groupBy", new Class<?>[] { String[].class, HttpTurSNServer.class }, new String[] {}, turSNServer);

        QueryTurSNResponse helperResponse = new QueryTurSNResponse();
        helperResponse.setPagination(new TurSNPagination(new ArrayList<>()));
        helperResponse.setFacetFields(new TurSNFacetFieldList(null, null));
        helperResponse.setSpotlightDocuments(new ArrayList<>());

        TurSNDidYouMean turSNDidYouMean = new TurSNDidYouMean();
        turSNDidYouMean.setCorrectedText(true);
        TurSNDidYouMeanText original = new TurSNDidYouMeanText();
        original.setText("orig");
        original.setLink("/orig");
        TurSNDidYouMeanText corrected = new TurSNDidYouMeanText();
        corrected.setText("corr");
        corrected.setLink("/corr");
        turSNDidYouMean.setOriginal(original);
        turSNDidYouMean.setCorrected(corrected);
        helperResponse.setDidYouMean(turSNDidYouMean);

        invoke("pagination", new Class<?>[] { TurSNPagination.class }, helperResponse.getPagination());
        invoke("facet", new Class<?>[] { QueryTurSNResponse.class }, helperResponse);
        invoke("didYouMean", new Class<?>[] { QueryTurSNResponse.class }, helperResponse);
        invoke("spolight", new Class<?>[] { QueryTurSNResponse.class }, helperResponse);
        invoke("showKeyValue", new Class<?>[] { Map.Entry.class }, Map.entry("q", List.of("value")));

        verify(turSNServer, times(2)).getLatestSearches(10);
        verify(turSNServer, times(1)).query(any(TurSNQuery.class));
        verify(turSNServer).autoComplete(any());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TurSNClientSample.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}