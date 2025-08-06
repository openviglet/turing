package com.viglet.turing.mcp.server;

import com.viglet.turing.client.sn.HttpTurSNServer;
import com.viglet.turing.client.sn.TurSNDocumentList;
import com.viglet.turing.client.sn.TurSNQuery;
import com.viglet.turing.client.sn.pagination.TurSNPagination;
import com.viglet.turing.client.sn.response.QueryTurSNResponse;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Service
public class TurToolsService {

    private final String turingUrl;
    private final String turingApiKey;

    public TurToolsService(@Value("${turing.url}") String turingUrl,
                           @Value("${turing.apiKey}") String turingApiKey) {
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
    }

    @Tool(name = "list_indices_tool",
            description = "Lists all indices in the Turing ES cluster with full information including docs.count. If an index parameter is provided, returns detailed information about that specific index.")
    public String listIndexTool(String index) {
        return """
                index;locale;description
                wknd-author;en_US;About WKND Site
                """;
    }

    @Tool(name = "get_index_mapping_tool",
            description = "Retrieves index mapping and setting information for an index in Turing ES")
    public String indexMappingTool(String index, String locale) {
        return """
                index: wknd-author
                
                mappings:
                properties={title={type=string}, description={type=string}, url={type=string}, modificationDate={type=date}, publicationDate={type=date}
                """;
    }

    @Tool(name = "search_index_tool", description = """
            Executes a search query against the Turing Elasticsearch (ES) instance.
            
            Args:
                index (str): Name of the index to query. Required. Example: 'samplesite'.
                query (str, optional): Search query string. Example: 'foobar'. If date need be ISO-8601 format.
                page (int, optional): Page number for paginated results. Example: 1.
                fq (list[str], optional): List of filter queries using attribute:value format. If date need be ISO-8601 format. Example: ['title:foobar'].
                sort (str, optional): Sort order for results. Options: 'asc', 'desc', 'relevant'. Example: 'relevant'.
                rows (int, optional): Number of results to return per page. Example: 10.
                group (str, optional): Field name to group results by. Example: 'foobar'.
                locales (str): Locale or language code for the search. Required. Example: 'en-US' or 'pt-BR'.
            Returns:
                A dictionary containing the search results that match the provided parameters.
            """)
    public TurSNDocumentList searchIndexTool(String index, String locale, String query, int page,
                                  List<String> fq, String sort, int rows, String group) {

        HttpTurSNServer turSNServer = new HttpTurSNServer(URI.create("http://localhost:2700"), index, LocaleUtils.toLocale(locale));

        TurSNQuery turSNQuery = new TurSNQuery();
        turSNQuery.setQuery(query);
        turSNQuery.addFilterQuery(fq.toArray(String[]::new));
        turSNQuery.setRows(rows);
        turSNQuery.setSortField(TurSNQuery.ORDER.asc);
        turSNQuery.setPageNumber(page);
        QueryTurSNResponse response = turSNServer.query(turSNQuery);
        return response.getResults();

    }
}
