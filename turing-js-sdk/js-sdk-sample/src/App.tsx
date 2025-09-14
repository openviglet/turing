import { TurSNSiteSearchService, type TurSNSiteSearchDocument } from "@openviglet/turing-js-sdk";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

function App() {
  const [totalDocuments, setTotalDocuments] = useState<number>(0);
  const [searchDocuments, setSearchDocuments] = useState<TurSNSiteSearchDocument[]>([]);
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '*';
  useEffect(() => {
    const searchService = new TurSNSiteSearchService(import.meta.env.VITE_API_URL);
    try {
      searchService.search(import.meta.env.VITE_SN_SITE, {
        q: query,
        rows: 10,
        currentPage: 1,
        localeRequest: import.meta.env.VITE_LOCALE,
      }).then(res => {
        setSearchDocuments(res.results?.document || []);
        setTotalDocuments(res.queryContext?.count || 0);
      });
    } catch (error: Error | any) {
      console.error('Search failed:', error.message);
    }
  }, [])

  return (
    <div className="flex min-h-svh flex-col items-center justify-center">
      <div className="mb-4 text-lg font-semibold">Total Documents: {totalDocuments}</div>
      <ul className="w-full max-w-md divide-y divide-gray-200 rounded-md border border-gray-300 bg-white shadow">
        {searchDocuments && searchDocuments.map((doc, index) => (
          <li key={index} className="p-4">
            <h3
              className="text-base font-medium"
              dangerouslySetInnerHTML={{ __html: doc.fields?.title }}
            />

            <a
              href={doc.fields?.url}
              target="_blank"
              rel="noopener noreferrer"
            >
              {doc.fields?.url}
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default App