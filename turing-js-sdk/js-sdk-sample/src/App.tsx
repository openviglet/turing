import { TurSNSiteSearchService, type TurSNSiteSearchDocument } from "@openviglet/turing-js-sdk";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

function App() {
  const [totalDocuments, setTotalDocuments] = useState(0);
  const [searchDocuments, setSearchDocuments] = useState<TurSNSiteSearchDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchParams] = useSearchParams();
  const query = searchParams.get("q") || "*";

  useEffect(() => {
    const fetchDocuments = async () => {
      setLoading(true);
      setError(null);
      try {
        const searchService = new TurSNSiteSearchService(import.meta.env.VITE_API_URL);
        const res = await searchService.search(import.meta.env.VITE_SN_SITE, {
          q: query,
          rows: 10,
          currentPage: 1,
          localeRequest: import.meta.env.VITE_LOCALE,
        });
        setSearchDocuments(res.results?.document || []);
        setTotalDocuments(res.queryContext?.count || 0);
      } catch (err: any) {
        setError("Search failed: " + (err?.message || "Unknown error"));
      } finally {
        setLoading(false);
      }
    };
    fetchDocuments();
  }, [query]);

  return (
    <div className="flex min-h-svh flex-col items-center justify-center">
      <div className="mb-4 text-lg font-semibold">
        Total Documents: {totalDocuments}
      </div>
      {loading && <div className="mb-4 text-gray-500">Loading...</div>}
      {error && <div className="mb-4 text-red-500">{error}</div>}
      <ul className="w-full max-w-md divide-y divide-gray-200 rounded-md border border-gray-300 bg-white shadow">
        {searchDocuments.map((doc, index) => (
          <li key={doc.fields?.url || index} className="p-4">
            <h3
              className="text-base font-medium"
              dangerouslySetInnerHTML={{ __html: doc.fields?.title || "" }}
            />
            {doc.fields?.url && (
              <a
                href={doc.fields.url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 underline break-all"
              >
                {doc.fields.url}
              </a>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;