import { Navigate, Route, Routes, useParams } from "react-router-dom"
import ConsoleRootPage from "./app/console/console.root.page"
import IntegrationInstanceDetailPage from "./app/console/integration/integration.instance.detail.page"
import IntegrationInstanceIndexingRulePage from "./app/console/integration/integration.instance.indexing.rule.page"
import IntegrationInstanceListPage from "./app/console/integration/integration.instance.list.page"
import IntegrationInstanceMonitoringPage from "./app/console/integration/integration.instance.monitoring.page"
import IntegrationInstancePage from "./app/console/integration/integration.instance.page"
import IntegrationInstanceRootPage from "./app/console/integration/integration.instance.root.page"
import IntegrationInstanceSourcePage from "./app/console/integration/integration.instance.source.page"
import LLMInstanceListPage from "./app/console/llm/llm.instance.list.page"
import LLMInstancePage from "./app/console/llm/llm.instance.page"
import LLMInstanceRootPage from "./app/console/llm/llm.instance.root.page"
import LoggingInstanceListPage from "./app/console/logging/logging.instance.list.page"
import LoggingInstancePage from "./app/console/logging/logging.instance.page"
import LoggingInstanceRootPage from "./app/console/logging/logging.instance.root.page"
import SEInstanceListPage from "./app/console/se/se.instance.list.page"
import SEInstancePage from "./app/console/se/se.instance.page"
import SEInstanceRootPage from "./app/console/se/se.instance.root.page"
import SNSiteBehaviorPage from "./app/console/sn/sn.site.behavior.page"
import SNSiteDetailPage from "./app/console/sn/sn.site.detail.page"
import SNSiteFacetOrderingPage from "./app/console/sn/sn.site.facets.ordering.page"
import SNSiteFieldPage from "./app/console/sn/sn.site.field.page"
import SNSiteFieldsPage from "./app/console/sn/sn.site.fields.page"
import SNSiteGenAIPage from "./app/console/sn/sn.site.genai.page"
import SNSiteListPage from "./app/console/sn/sn.site.list.page"
import SNSiteMergeProvidersPage from "./app/console/sn/sn.site.merge.providers.page"
import SNSiteMultiLanguagePage from "./app/console/sn/sn.site.multi.language.page"
import SNSitePage from "./app/console/sn/sn.site.page"
import SNSiteResultRankingPage from "./app/console/sn/sn.site.result.ranking.page"
import SNSiteRootPage from "./app/console/sn/sn.site.root.page"
import SNSiteSpotlightPage from "./app/console/sn/sn.site.spotlight.page"
import SNSiteTopSearchTermsPage from "./app/console/sn/sn.site.top.search.terms.page"
import StoreInstanceListPage from "./app/console/store/store.instance.list.page"
import StoreInstancePage from "./app/console/store/store.instance.page"
import StoreInstanceRootPage from "./app/console/store/store.instance.root.page"
import TokenInstanceListPage from "./app/console/token/token.instance.list.page"
import TokenInstancePage from "./app/console/token/token.instance.page"
import TokenInstanceRootPage from "./app/console/token/token.instance.root.page"
import LoginPage from "./app/login/login.page"
import { ROUTES } from "./app/routes.const"
import { ThemeProvider } from "./components/theme-provider"
import { Toaster } from "./components/ui/sonner"



function RedirectToSNDetail() {
  const { id } = useParams();
  return <Navigate to={`${ROUTES.SN_INSTANCE}/${id}/detail`} replace />;
}

function RedirectToIntegrationDetail() {
  const { id } = useParams();
  return <Navigate to={`${ROUTES.INTEGRATION_INSTANCE}/${id}/detail`} replace />;
}



function App() {
  return (
    <div className="App">
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <Toaster />
        <Routes>
          <Route path={ROUTES.ROOT} element={<Navigate to={ROUTES.CONSOLE} replace />} />
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.CONSOLE} element={<ConsoleRootPage />} >
            <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
            <Route path={ROUTES.SE_ROOT} element={<SEInstanceRootPage />} >
              <Route path={ROUTES.SE_INSTANCE} element={<SEInstanceListPage />} />
              <Route path={`${ROUTES.SE_INSTANCE}/:id`} element={<SEInstancePage />} />
            </Route>
            <Route path={ROUTES.SN_ROOT} element={<SNSiteRootPage />} >
              <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
              <Route path={ROUTES.SN_INSTANCE} element={<SNSiteListPage />} />
              <Route path={`${ROUTES.SN_INSTANCE}/:id`} element={<SNSitePage />} >
                <Route index element={<RedirectToSNDetail />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/detail`} element={<SNSiteDetailPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/locale`} element={<SNSiteMultiLanguagePage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/field`} element={<SNSiteFieldsPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/behavior`} element={<SNSiteBehaviorPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/facet-ordering`} element={<SNSiteFacetOrderingPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/ai`} element={<SNSiteGenAIPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/result-ranking`} element={<SNSiteResultRankingPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/merge-providers`} element={<SNSiteMergeProvidersPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/spotlight`} element={<SNSiteSpotlightPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/top-terms`} element={<SNSiteTopSearchTermsPage />} />
                <Route path={`${ROUTES.SN_INSTANCE}/:id/field/:fieldId`} element={<SNSiteFieldPage />} ></Route>
              </Route>

            </Route>
            <Route path={ROUTES.STORE_ROOT} element={<StoreInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.STORE_INSTANCE} replace />} />
              <Route path={ROUTES.STORE_INSTANCE} element={<StoreInstanceListPage />} />
              <Route path={`${ROUTES.STORE_INSTANCE}/:id`} element={<StoreInstancePage />} />
            </Route>
            <Route path={ROUTES.TOKEN_ROOT} element={<TokenInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.TOKEN_INSTANCE} replace />} />
              <Route path={ROUTES.TOKEN_INSTANCE} element={<TokenInstanceListPage />} />
              <Route path={`${ROUTES.TOKEN_INSTANCE}/:id`} element={<TokenInstancePage />} />
            </Route>
            <Route path={ROUTES.LLM_ROOT} element={<LLMInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.LLM_INSTANCE} replace />} />
              <Route path={ROUTES.LLM_INSTANCE} element={<LLMInstanceListPage />} />
              <Route path={`${ROUTES.LLM_INSTANCE}/:id`} element={<LLMInstancePage />} />
            </Route>
            <Route path={ROUTES.INTEGRATION_ROOT} element={<IntegrationInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.INTEGRATION_INSTANCE} replace />} />
              <Route path={ROUTES.INTEGRATION_INSTANCE} element={<IntegrationInstanceListPage />} />
              <Route path={`${ROUTES.INTEGRATION_INSTANCE}/:id`} element={<IntegrationInstancePage />} >
                <Route index element={<RedirectToIntegrationDetail />} />
                <Route path={`${ROUTES.INTEGRATION_INSTANCE}/:id/detail`} element={<IntegrationInstanceDetailPage />} />
                <Route path={`${ROUTES.INTEGRATION_INSTANCE}/:id/source`} element={<IntegrationInstanceSourcePage />} />
                <Route path={`${ROUTES.INTEGRATION_INSTANCE}/:id/indexing-rule`} element={<IntegrationInstanceIndexingRulePage />} />
                <Route path={`${ROUTES.INTEGRATION_INSTANCE}/:id/monitoring`} element={<IntegrationInstanceMonitoringPage />} />
              </Route>
            </Route>
            <Route path={ROUTES.LOGGING_ROOT} element={<LoggingInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.LOGGING_INSTANCE} replace />} />
              <Route path={ROUTES.LOGGING_INSTANCE} element={<LoggingInstanceListPage />} />
              <Route path={`${ROUTES.LOGGING_INSTANCE}/:id`} element={<LoggingInstancePage />} />
            </Route>
          </Route>
        </Routes>
      </ThemeProvider>
    </div >
  )
}

export default App