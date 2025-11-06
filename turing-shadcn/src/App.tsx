import { Navigate, Route, Routes } from "react-router-dom"
import ConsoleRootPage from "./app/console/console.root.page"
import IntegrationInstanceListPage from "./app/console/integration/integration.instance.list.page"
import IntegrationInstanceRootPage from "./app/console/integration/integration.instance.root.page"
import LLMInstanceListPage from "./app/console/llm/llm.instance.list.page"
import LLMInstanceRootPage from "./app/console/llm/llm.instance.root.page"
import LoggingInstanceListPage from "./app/console/logging/logging.instance.list.page"
import LoggingInstanceRootPage from "./app/console/logging/logging.instance.root.page"
import SEInstanceListPage from "./app/console/se/se.instance.list.page"
import SEInstanceRootPage from "./app/console/se/se.instance.root.page"
import SNSiteListPage from "./app/console/sn/sn.site.list.page"
import SNSiteRootPage from "./app/console/sn/sn.site.root.page"
import StoreInstanceListPage from "./app/console/store/store.instance.list.page"
import StoreInstanceRootPage from "./app/console/store/store.instance.root.page"
import TokenInstanceListPage from "./app/console/token/token.instance.list.page"
import TokenInstanceRootPage from "./app/console/token/token.instance.root.page"
import LoginPage from "./app/login/login.page"
import { ROUTES } from "./app/routes.const"
import { ThemeProvider } from "./components/theme-provider"
import { Toaster } from "./components/ui/sonner"

function App() {
  return (
    <div className="App">
      <ThemeProvider defaultTheme="dark" storageKey="turing-shadcn-theme">
        <Toaster />
        <Routes>
          <Route path={ROUTES.ROOT} element={<Navigate to={ROUTES.CONSOLE} replace />} />
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.CONSOLE} element={<ConsoleRootPage />} >
            <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
            <Route path={ROUTES.SE_ROOT} element={<SEInstanceRootPage />} >
              <Route path={ROUTES.SE_INSTANCE} element={<SEInstanceListPage />} />
            </Route>
            <Route path={ROUTES.SN_ROOT} element={<SNSiteRootPage />} >
              <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
              <Route path={ROUTES.SN_INSTANCE} element={<SNSiteListPage />} />
            </Route>
            <Route path={ROUTES.STORE_ROOT} element={<StoreInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.STORE_INSTANCE} replace />} />
              <Route path={ROUTES.STORE_INSTANCE} element={<StoreInstanceListPage />} />
            </Route>
            <Route path={ROUTES.TOKEN_ROOT} element={<TokenInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.TOKEN_INSTANCE} replace />} />
              <Route path={ROUTES.TOKEN_INSTANCE} element={<TokenInstanceListPage />} />
            </Route>
            <Route path={ROUTES.LLM_ROOT} element={<LLMInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.LLM_INSTANCE} replace />} />
              <Route path={ROUTES.LLM_INSTANCE} element={<LLMInstanceListPage />} />
            </Route>
            <Route path={ROUTES.INTEGRATION_ROOT} element={<IntegrationInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.INTEGRATION_INSTANCE} replace />} />
              <Route path={ROUTES.INTEGRATION_INSTANCE} element={<IntegrationInstanceListPage />} />
            </Route>
            <Route path={ROUTES.LOGGING_ROOT} element={<LoggingInstanceRootPage />} >
              <Route index element={<Navigate to={ROUTES.LOGGING_INSTANCE} replace />} />
              <Route path={ROUTES.LOGGING_INSTANCE} element={<LoggingInstanceListPage />} />
            </Route>
          </Route>
        </Routes>
      </ThemeProvider>
    </div >
  )
}

export default App
