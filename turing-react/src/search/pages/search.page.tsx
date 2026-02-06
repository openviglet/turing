import { ChevronDown, Search } from "lucide-react";
import moment from "moment";
import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import type { TurSNChat } from "../models/sn-chat.model";
import type { TurSNSearch } from "../models/sn-search.model";
import { TurSNSearchService } from "../services/sn-search.service";

export default function SearchPage() {
  const { siteName } = useParams<{ siteName: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const [snSearch, setSnSearch] = useState<TurSNSearch | null>(null);
  const [llmChat, setLlmChat] = useState<TurSNChat | null>(null);
  const [turQuery, setTurQuery] = useState(searchParams.get("q") || "");
  const [turLocale] = useState(searchParams.get("_setlocale") || "en_US");
  const [turSort] = useState(searchParams.get("sort") || "relevance");
  const [autoComplete, setAutoComplete] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const turSiteName = siteName || "Sample"; // Get from URL params or use default
  const navigate = useNavigate();
  const sortOptions: Record<string, string> = {
    relevance: "Relevance",
    newest: "Newest",
    oldest: "Oldest",
  };

  useEffect(() => {
    performSearch();
  }, [searchParams]);

  const performSearch = async () => {
    setLoading(true);
    setError(null);
    try {
      const q = searchParams.get("q") || "*";
      const p = searchParams.get("p") || "1";
      const _setlocale = searchParams.get("_setlocale") || "pt";
      const sort = searchParams.get("sort") || "relevance";
      const fq = searchParams.getAll("fq") || [];
      const tr = searchParams.getAll("tr") || [];
      const nfpr = searchParams.get("nfpr") || "";

      const [searchResult, chatResult] = await Promise.all([

        TurSNSearchService.query(turSiteName, q, p, _setlocale, sort, fq, tr, nfpr),
        q !== "*" ? TurSNSearchService.chat(turSiteName, q, _setlocale) : Promise.resolve(null),
      ]);

      setSnSearch(searchResult);
      setLlmChat(chatResult);
    } catch (error) {
      console.error("Search error:", error);
      setError("Search failed");
      setSnSearch(null);
    } finally {
      setLoading(false);
    }
  };

  const searchIt = () => {
    const params = new URLSearchParams(searchParams);
    params.set("q", turQuery || "*");
    params.set("p", "1");
    setSearchParams(params);
  };

  const retrieveAutoComplete = async () => {
    if (turQuery && turQuery.length > 2) {
      try {
        const results = await TurSNSearchService.autoComplete(
          turSiteName,
          turQuery,
          "1",
          turLocale,
          turSort,
          [],
          [],
          ""
        );
        setAutoComplete(results);
      } catch (error) {
        console.error("Autocomplete error:", error);
      }
    } else {
      setAutoComplete([]);
    }
  };

  const turRedirect = (href: string) => {
    const result: Record<string, string[]> = {};
    const queryString = href.split('?')[1];

    if (queryString) {
      new URLSearchParams(queryString).forEach((value, key) => {
        if (Object.prototype.hasOwnProperty.call(result, key)) {
          result[key].unshift(value);
        } else {
          result[key] = [value];
        }
      });
    }

    const searchParams = new URLSearchParams();
    Object.entries(result).forEach(([key, values]) => {
      values.forEach(val => searchParams.append(key, val));
    });

    navigate({
      pathname: location.pathname,
      search: `?${searchParams.toString()}`
    });

    // performSearch will be triggered by useEffect on searchParams change
  };

  const showAll = () => {
    setTurQuery("*");
    searchIt();
  };

  const changeLocale = (locale: string) => {
    const params = new URLSearchParams(searchParams);
    params.set("_setlocale", locale);
    setSearchParams(params);
  };

  const changeOrderBy = (sort: string) => {
    const params = new URLSearchParams(searchParams);
    params.set("sort", sort);
    setSearchParams(params);
  };

  const camelize = (text: string) => {
    return text
      .split("_")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(" ");
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <span className="text-lg">Loading</span>
          <span className="animate-pulse">...</span>
        </div>
      </div>
    );
  }

  if (error || !snSearch) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <header className="border-b border-border bg-card">
          <div className="container mx-auto px-4 py-4">
            <div className="flex items-center gap-4">
              <svg className="w-8 h-8" viewBox="0 0 549 549">
                <rect className="fill-primary" x="0.063" width="548" height="548.188" rx="100" ry="100" />
                <text className="fill-primary-foreground font-bold text-6xl" transform="translate(64.825 442.418) scale(2.74 2.741)">Tu</text>
              </svg>
              <span>{turSiteName}</span>
            </div>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6">
          <div className="text-center py-12">
            <h3 className="text-xl font-semibold mb-2">No content to display</h3>
            <p className="text-muted-foreground mb-4">{error ? "There was an error performing the search." : "Try adjusting your query or locale."}</p>
            <button className="px-6 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90" onClick={showAll}>
              Show all the available content
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Header */}
      <header className="border-b border-border bg-card">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center gap-4">
            <button
              onClick={showAll}
              className="flex items-center gap-2 text-lg font-semibold hover:text-primary"
            >
              <svg className="w-8 h-8" viewBox="0 0 549 549">
                <rect
                  className="fill-primary"
                  x="0.063"
                  width="548"
                  height="548.188"
                  rx="100"
                  ry="100"
                />
                <text
                  className="fill-primary-foreground font-bold text-6xl"
                  transform="translate(64.825 442.418) scale(2.74 2.741)"
                >
                  Tu
                </text>
              </svg>
              <span>{turSiteName}</span>
            </button>

            <div className="flex-1 relative">
              <div className="relative">
                <input
                  value={turQuery}
                  onChange={(e) => {
                    setTurQuery(e.target.value);
                    retrieveAutoComplete();
                  }}
                  onKeyPress={(e) => e.key === "Enter" && searchIt()}
                  type="search"
                  className="w-full px-4 py-2 pr-10 rounded-md border border-input bg-background focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="Search..."
                  autoComplete="off"
                />
                <Search className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              </div>

              {autoComplete.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-card border border-border rounded-md shadow-lg">
                  {autoComplete.map((term, idx) => (
                    <div
                      key={idx}
                      className="px-4 py-2 hover:bg-accent cursor-pointer"
                      onClick={() => {
                        setTurQuery(term);
                        setAutoComplete([]);
                      }}
                    >
                      {term}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Locale Selector */}
            <div className="relative group">
              <button className="px-4 py-2 border border-border rounded-md flex items-center gap-2">
                <em>Language:</em>
                <span>{snSearch.queryContext.query.locale}</span>
                <ChevronDown className="w-4 h-4" />
              </button>
              <div className="hidden group-hover:block absolute right-0 mt-1 w-48 bg-card border border-border rounded-md shadow-lg">
                {snSearch.widget.locales.map((locale) => (
                  <button
                    key={locale.locale}
                    className="w-full px-4 py-2 text-left hover:bg-accent"
                    onClick={() => changeLocale(locale.locale)}
                  >
                    {locale.locale}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-6" style={{ minHeight: "600px" }}>
        {/* Chat/AI Response */}
        {llmChat?.text && (
          <div className="mb-6 p-6 bg-card border border-border rounded-md">
            <h3 className="text-xl font-semibold mb-2">Assistant</h3>
            <p className="whitespace-pre-wrap">{llmChat.text}</p>
          </div>
        )}

        {/* No Results */}
        {snSearch.results.document.length === 0 && (
          <div className="text-center py-12">
            <Search className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
            <h3 className="text-xl font-semibold mb-2">
              We couldn't find any results matching '{turQuery}'.
            </h3>
            {snSearch.widget.spellCheck.correctedText && (
              <h3 className="text-lg mb-4">
                Did you mean{" "}
                <button
                  className="text-primary underline font-semibold"
                  onClick={() => turRedirect(snSearch.widget.spellCheck.corrected.link)}
                >
                  {snSearch.widget.spellCheck.corrected.text}
                </button>
                ?
              </h3>
            )}
            {!snSearch.widget.spellCheck.correctedText && !llmChat?.text && (
              <div>
                <p className="mb-4">
                  You can try to see all the available content, maybe you have a new idea. :-)
                </p>
                <button
                  className="px-6 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
                  onClick={showAll}
                >
                  Show all the available content
                </button>
              </div>
            )}
          </div>
        )}

        {/* Results */}
        {snSearch.results.document.length > 0 && (
          <div className="flex gap-6">
            {/* Facets Sidebar */}
            <aside className="w-64 flex-shrink-0">
              {/* Applied Filters */}
              {snSearch.widget.facetToRemove?.facets && (
                <div className="mb-4 border border-primary rounded-md">
                  <div className="px-4 py-2 bg-primary/10 border-b border-primary font-semibold flex justify-between items-center">
                    <span>Applied Filters</span>
                    <button
                      className="text-sm text-primary hover:underline"
                      onClick={() => turRedirect(snSearch.widget.cleanUpFacets)}
                    >
                      Clean up all
                    </button>
                  </div>
                  <div>
                    {snSearch.widget.facetToRemove.facets.map((facet, idx) => (
                      <button
                        key={idx}
                        className="w-full px-4 py-2 text-left hover:bg-accent border-b border-border text-sm"
                        onClick={() => turRedirect(facet.link)}
                      >
                        <span>{facet.label}</span>
                        <span className="float-right">(Remove)</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Facet Groups */}
              {snSearch.widget.facet.map((facets, idx) => (
                <div key={idx} className="mb-4 border border-border rounded-md">
                  <div className="px-4 py-2 bg-card border-b border-border font-semibold flex justify-between items-center">
                    <span>{facets.label.text}</span>
                    <button
                      className="text-sm text-primary hover:underline"
                      onClick={() => turRedirect(facets.cleanUpLink)}
                    >
                      Clean up
                    </button>
                  </div>
                  <div>
                    {facets.facets.map((facet, fidx) => (
                      <button
                        key={fidx}
                        className="w-full px-4 py-2 text-left hover:bg-accent border-b border-border text-sm flex items-center justify-between"
                        onClick={() => turRedirect(facet.link)}
                      >
                        <span>
                          {facet.label}{" "}
                          <span className="inline-block px-2 py-0.5 bg-muted text-muted-foreground rounded-full text-xs">
                            {facet.count}
                          </span>
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </aside>

            {/* Results Column */}
            <div className="flex-1">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold">
                  Showing {snSearch.queryContext.pageStart} - {snSearch.queryContext.pageEnd} of{" "}
                  {snSearch.queryContext.count} results
                </h3>

                {/* Sort Selector */}
                <div className="relative group">
                  <button className="px-4 py-2 border border-border rounded-md flex items-center gap-2 text-sm">
                    <em>Order by:</em>
                    <span>{camelize(turSort)}</span>
                    <ChevronDown className="w-4 h-4" />
                  </button>
                  <div className="hidden group-hover:block absolute right-0 mt-1 w-48 bg-card border border-border rounded-md shadow-lg">
                    {Object.entries(sortOptions).map(([key, value]) => (
                      <button
                        key={key}
                        className="w-full px-4 py-2 text-left hover:bg-accent flex items-center"
                        onClick={() => changeOrderBy(key)}
                      >
                        {turSort === key && <span className="mr-2">✓</span>}
                        {value}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Spell Check Messages */}
              {snSearch.widget.spellCheck.usingCorrectedText &&
                snSearch.widget.spellCheck.correctedText && (
                  <div className="mb-4">
                    <h4 className="mb-1">
                      Showing results for{" "}
                      <button
                        className="text-primary underline font-semibold italic"
                        onClick={() => turRedirect(snSearch.widget.spellCheck.corrected.link)}
                      >
                        {snSearch.widget.spellCheck.corrected.text}
                      </button>
                      .
                    </h4>
                    <p>
                      Instead, search for{" "}
                      <button
                        className="text-primary underline font-semibold"
                        onClick={() => turRedirect(snSearch.widget.spellCheck.original.link)}
                      >
                        {snSearch.widget.spellCheck.original.text}
                      </button>
                    </p>
                  </div>
                )}

              {!snSearch.widget.spellCheck.usingCorrectedText &&
                snSearch.widget.spellCheck.correctedText && (
                  <div className="mb-4">
                    <h4 className="mb-1">
                      Did you mean '
                      <button
                        className="text-primary underline font-semibold italic"
                        onClick={() => turRedirect(snSearch.widget.spellCheck.corrected.link)}
                      >
                        {snSearch.widget.spellCheck.corrected.text}
                      </button>
                      '?
                    </h4>
                  </div>
                )}

              {/* Documents */}
              <div className="space-y-4">
                {snSearch.results.document.map((document, idx) => {
                  const url = document.fields[snSearch.queryContext.defaultFields.url];
                  const title = document.fields[snSearch.queryContext.defaultFields.title];
                  const description =
                    document.fields[snSearch.queryContext.defaultFields.description];
                  const date = document.fields[snSearch.queryContext.defaultFields.date];

                  if (!url) return null;

                  return (
                    <div
                      key={idx}
                      className="border-t border-border pt-4 flex gap-4"
                    >
                      <div className="flex-1">
                        <h4 className="text-lg mb-1">
                          <a
                            href={url}
                            className="text-primary hover:underline"
                            dangerouslySetInnerHTML={{ __html: title || url }}
                          />
                        </h4>
                        {description && (
                          <p
                            className="text-sm text-muted-foreground mb-2 line-clamp-2"
                            dangerouslySetInnerHTML={{ __html: description }}
                          />
                        )}
                        {document.metadata.map((metadata, midx) => (
                          <button
                            key={midx}
                            className="text-xs px-2 py-1 border border-border rounded mr-2 hover:bg-accent"
                            onClick={() => turRedirect(metadata.href)}
                            title={metadata.text}
                            dangerouslySetInnerHTML={{ __html: metadata.text }}
                          />
                        ))}
                        {date && (
                          <div className="text-xs text-muted-foreground mt-2">
                            Updated {moment(date).format("LL")}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Pagination */}
              <nav className="mt-6 flex justify-center gap-2">
                {snSearch.pagination.map((page, idx) => {
                  if (page.type === "CURRENT") {
                    return (
                      <span
                        key={idx}
                        className="px-3 py-1 bg-primary text-primary-foreground rounded"
                      >
                        {camelize(page.text)}
                      </span>
                    );
                  }
                  return (
                    <button
                      key={idx}
                      className="px-3 py-1 border border-border rounded hover:bg-accent"
                      onClick={() => turRedirect(page.href)}
                    >
                      {camelize(page.text)}
                    </button>
                  );
                })}
              </nav>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-border mt-12">
        <div className="container mx-auto px-4 py-6">
          <div className="flex flex-wrap justify-between items-center gap-4 text-sm text-muted-foreground">
            <div className="flex gap-4">
              <span>© {new Date().getFullYear()} Turing ES.</span>
              <a href="https://github.com/openturing" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Github
              </a>
              <a href="https://linkedin.com/company/viglet" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                LinkedIn
              </a>
              <a href="/swagger-ui.html" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                API
              </a>
              <a href="/console" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Console
              </a>
            </div>
            <div className="flex gap-4">
              <a href="https://viglet.com/#contact" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Contact
              </a>
              <a href="https://viglet.com" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Viglet
              </a>
              <a href="https://docs.viglet.com/turing" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Documentation
              </a>
              <a href="https://viglet.com/turing" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Website
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
