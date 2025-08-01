import { Navigate, Route, Routes } from "react-router-dom"
import LoginPage from "./app/login/login.page"
import ConsolePage from "./app/console/console.page"
import SEInstanceListPage from "./app/console/se/se.instance.list.page"
import { ThemeProvider } from "./components/theme-provider"
import SEInstancePage from "./app/console/se/se.instance.page"
import { Toaster } from "./components/ui/sonner"
import SNSiteListPage from "./app/console/sn/sn.site.list.page"
import StoreInstanceListPage from "./app/console/store/store.instance.list.page"
import StoreInstancePage from "./app/console/store/store.instance.page"
import TokenInstanceListPage from "./app/console/token/token.instance.list.page"
import TokenInstancePage from "./app/console/token/token.instance.page"
import LLMInstanceListPage from "./app/console/llm/llm.instance.list.page"
import LLMInstancePage from "./app/console/llm/llm.instance.page"
import IntegrationInstancePage from "./app/console/integration/integration.instance.page"
import IntegrationInstanceListPage from "./app/console/integration/integration.instance.list.page"
import LoggingInstanceListPage from "./app/console/logging/logging.instance.list.page"
import LoggingInstancePage from "./app/console/logging/logging.instance.page"
import SNSiteRootPage from "./app/console/sn/sn.site.root.page"
import ConsoleRootPage from "./app/console/console.root.page"
import SEInstanceRootPage from "./app/console/se/se.instance.root.page"
import LLMInstanceRootPage from "./app/console/llm/llm.instance.root.page"
import StoreInstanceRootPage from "./app/console/store/store.instance.root.page"
import IntegrationInstanceRootPage from "./app/console/integration/integration.instance.root.page"
import TokenInstanceRootPage from "./app/console/token/token.instance.root.page"
import LoggingInstanceRootPage from "./app/console/logging/logging.instance.root.page"
import SNSitePage from "./app/console/sn/sn.site.page"
import SNSiteDetailPage from "./app/console/sn/sn.site.detail.page"
import SNSiteMultiLanguagePage from "./app/console/sn/sn.site.multi.language.page"
import SNSiteFieldsPage from "./app/console/sn/sn.site.fields.page"
import SNSiteFacetOrderingPage from "./app/console/sn/sn.site.facets.ordering.page"
import SNSiteGenAIPage from "./app/console/sn/sn.site.genai.page"
import SNSiteResultRankingPage from "./app/console/sn/sn.site.result.ranking.page"
import SNSiteMergeProvidersPage from "./app/console/sn/sn.site.merge.providers.page"
import SNSiteSpotlightPage from "./app/console/sn/sn.site.spotlight.page"
import SNSiteTopSearchTermsPage from "./app/console/sn/sn.site.top.search.terms.page"
import SNSiteBehaviorPage from "./app/console/sn/sn.site.behavior.page"

function App() {
  return (
    <div className="App">
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <Toaster />
        <Routes>
          <Route path="/" element={<Navigate to="/console" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/admin" element={<ConsoleRootPage />} >
            <Route index element={<ConsolePage />} />
            <Route path="/admin/se" element={<SEInstanceRootPage />} >
              <Route path="/admin/se/instance" element={<SEInstanceListPage />} />
              <Route path="/admin/se/instance/:id" element={<SEInstancePage />} />
            </Route>
            <Route path="/admin/sn" element={<SNSiteRootPage />} >
              <Route path="/admin/sn/instance" element={<SNSiteListPage />} />
              <Route path="/admin/sn/instance/:id" element={<SNSitePage />} >
                <Route path="/admin/sn/instance/:id/detail" element={<SNSiteDetailPage />} />
                <Route path="/admin/sn/instance/:id/locale" element={<SNSiteMultiLanguagePage />} />
                <Route path="/admin/sn/instance/:id/field" element={<SNSiteFieldsPage />} />
                <Route path="/admin/sn/instance/:id/behavior" element={<SNSiteBehaviorPage />} />
                <Route path="/admin/sn/instance/:id/facet-ordering" element={<SNSiteFacetOrderingPage />} />
                <Route path="/admin/sn/instance/:id/ai" element={<SNSiteGenAIPage />} />
                <Route path="/admin/sn/instance/:id/result-ranking" element={<SNSiteResultRankingPage />} />
                <Route path="/admin/sn/instance/:id/merge-providers" element={<SNSiteMergeProvidersPage />} />
                <Route path="/admin/sn/instance/:id/spotlight" element={<SNSiteSpotlightPage />} />
                <Route path="/admin/sn/instance/:id/top-terms" element={<SNSiteTopSearchTermsPage />} />
              </Route>
            </Route>
            <Route path="/admin/store" element={<StoreInstanceRootPage />} >
              <Route path="/admin/store/instance" element={<StoreInstanceListPage />} />
              <Route path="/admin/store/instance/:id" element={<StoreInstancePage />} />
            </Route>
            <Route path="/admin/token" element={<TokenInstanceRootPage />} >
              <Route path="/admin/token/instance" element={<TokenInstanceListPage />} />
              <Route path="/admin/token/instance/:id" element={<TokenInstancePage />} />
            </Route>
            <Route path="/admin/llm" element={<LLMInstanceRootPage />} >
              <Route path="/admin/llm/instance" element={<LLMInstanceListPage />} />
              <Route path="/admin/llm/instance/:id" element={<LLMInstancePage />} />
            </Route>
            <Route path="/admin/integration" element={<IntegrationInstanceRootPage />} >
              <Route path="/admin/integration/instance" element={<IntegrationInstanceListPage />} />
              <Route path="/admin/integration/instance/:id" element={<IntegrationInstancePage />} />
            </Route>
            <Route path="/admin/logging" element={<LoggingInstanceRootPage />} >
              <Route path="/admin/logging/instance" element={<LoggingInstanceListPage />} />
              <Route path="/admin/logging/instance/:id" element={<LoggingInstancePage />} />
            </Route>
          </Route>
        </Routes>
      </ThemeProvider>
    </div >
  )
}

export default App