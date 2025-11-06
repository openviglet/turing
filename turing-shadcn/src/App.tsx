import { Navigate, Route, Routes } from "react-router-dom"
import ConsoleRootPage from "./app/console/console.root.page"
import SEInstanceListPage from "./app/console/se/se.instance.list.page"
import SEInstanceRootPage from "./app/console/se/se.instance.root.page"
import SNSiteListPage from "./app/console/sn/sn.site.list.page"
import SNSiteRootPage from "./app/console/sn/sn.site.root.page"
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
          </Route>
        </Routes>
      </ThemeProvider>
    </div >
  )
}

export default App
