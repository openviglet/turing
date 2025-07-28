package com.viglet.turing.mcp.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
                health    status    index    uuid    pri    rep    docs.count    docs.deleted    store.size    pri.store.size
                green    open    .plugins-ml-model-group    lHgGEgJhT_mpADyOZoXl2g    1    1    9    2    33.4kb    16.7kb
                green    open    .plugins-ml-memory-meta    b2LEpv0QS8K60QBjXtRm6g    1    1    13    0    95.1kb    47.5kb
                """;
    }

    @Tool(name = "get_index_mapping_tool",
            description = "Retrieves index mapping and setting information for an index in Turing ES")
    public String indexMappingTool(String index) {
        return """
                index: sample-ecommerce
                
                mappings:
                properties={items_purchased_failure={type=integer}, items_purchased_success={type=integer}, order_id={type=integer}, timestamp={type=date}, total_revenue_usd={type=integer}}
                
                
                settings:
                index.creation_date=1706752839713
                index.number_of_replicas=1
                index.number_of_shards=1
                index.provided_name=sample-ecommerce
                index.replication.type=DOCUMENT
                index.uuid=UPYOQcAfRGqFAlSxcZlRjw
                index.version.created=137217827
                ;
                """;
    }
}
