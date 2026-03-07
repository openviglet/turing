package com.viglet.turing.api.llm;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.LocaleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.viglet.turing.client.sn.HttpTurSNServer;
import com.viglet.turing.client.sn.TurSNDocumentList;
import com.viglet.turing.client.sn.TurSNQuery;
import com.viglet.turing.client.sn.response.QueryTurSNResponse;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;

@Slf4j
@Service
public class TurSemanticNavToolService {

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    @Value("${server.port:2700}")
    private int serverPort;

    public TurSemanticNavToolService(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteLocaleRepository turSNSiteLocaleRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    @Tool(name = "list_sites", description = """
            Lists all Semantic Navigation sites available in Turing.
            Returns site name, description, and available locales for each site.
            Use this tool first to discover which sites and locales are available before searching.""")
    public String listSites() {
        log.info("[SemanticNav Tool] list_sites called");
        List<TurSNSite> sites = turSNSiteRepository.findAll(Sort.by("name"));
        if (sites.isEmpty()) {
            log.warn("[SemanticNav Tool] list_sites: No sites found");
            return "No Semantic Navigation sites found.";
        }
        StringBuilder sb = new StringBuilder("site_name;description;locales\n");
        for (TurSNSite site : sites) {
            List<TurSNSiteLocale> locales = turSNSiteLocaleRepository.findByTurSNSite(site);
            String localeStr = locales.stream()
                    .map(l -> l.getLanguage().toString())
                    .collect(Collectors.joining(", "));
            sb.append(site.getName()).append(";")
                    .append(site.getDescription() != null ? site.getDescription() : "").append(";")
                    .append(localeStr).append("\n");
        }
        String result = sb.toString();
        log.info("[SemanticNav Tool] list_sites returned {} sites:\n{}", sites.size(), result);
        return result;
    }

    @Tool(name = "get_site_fields", description = """
            Retrieves the field mappings for a Semantic Navigation site.
            Returns field name, type, and whether it is a facet, searchable, or required.
            Args:
                siteName: Name of the site (e.g., 'samplesite'). Use list_sites to find available sites.""")
    public String getSiteFields(String siteName) {
        log.info("[SemanticNav Tool] get_site_fields called with siteName={}", siteName);
        return turSNSiteRepository.findByName(siteName)
                .map(site -> {
                    List<TurSNSiteFieldExt> fields = turSNSiteFieldExtRepository
                            .findByTurSNSiteAndEnabled(site, 1);
                    if (fields.isEmpty()) {
                        return "No enabled fields found for site: " + siteName;
                    }
                    StringBuilder sb = new StringBuilder("field_name;type;facet;description\n");
                    for (TurSNSiteFieldExt field : fields) {
                        sb.append(field.getName()).append(";")
                                .append(field.getType()).append(";")
                                .append(field.getFacet() == 1 ? "yes" : "no").append(";")
                                .append(field.getDescription() != null ? field.getDescription() : "")
                                .append("\n");
                    }
                    return sb.toString();
                })
                .orElse("Site not found: " + siteName);
    }

    @Tool(name = "search_site", description = """
            Executes a search query against a Turing Semantic Navigation site.
            Args:
                siteName (str): Name of the site to search. Required. Use list_sites to find available sites.
                locale (str): Locale code for the search. Required. Example: 'en_US' or 'pt_BR'.
                query (str): Search query string. Example: 'machine learning'.
                rows (int): Number of results per page. Default: 10.
                page (int): Page number (1-based). Default: 1.
                filterQueries (list[str]): Filter queries in attribute:value format. Example: ['type:article'].
            Returns:
                Search results with document fields and metadata.""")
    public String searchSite(String siteName, String locale, String query,
            int rows, int page, List<String> filterQueries) {
        HttpTurSNServer turSNServer = new HttpTurSNServer(
                URI.create("http://localhost:" + serverPort),
                siteName, LocaleUtils.toLocale(locale));

        TurSNQuery turSNQuery = new TurSNQuery();
        turSNQuery.setQuery(query != null ? query : "*");
        if (filterQueries != null && !filterQueries.isEmpty()) {
            turSNQuery.addFilterQuery(filterQueries.toArray(String[]::new));
        }
        turSNQuery.setRows(rows > 0 ? rows : 10);
        turSNQuery.setPageNumber(page > 0 ? page : 1);
        turSNQuery.setSortField(TurSNQuery.Order.asc);

        QueryTurSNResponse response = turSNServer.query(turSNQuery);
        TurSNDocumentList results = response.getResults();

        if (results == null || results.getTurSNDocuments() == null
                || results.getTurSNDocuments().isEmpty()) {
            return "No results found for query: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.getQueryContext() != null
                ? results.getQueryContext().getCount() : results.getTurSNDocuments().size())
                .append(" results for query: ").append(query).append("\n\n");

        int idx = 0;
        for (var doc : results.getTurSNDocuments()) {
            idx++;
            sb.append("--- Result ").append(idx).append(" ---\n");
            if (doc.getContent() != null && doc.getContent().getFields() != null) {
                doc.getContent().getFields().forEach((key, value) ->
                        sb.append(key).append(": ").append(value).append("\n"));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
