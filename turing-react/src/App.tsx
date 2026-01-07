import { Navigate, Route, Routes } from "react-router-dom"
import ConsoleRootPage from "./app/console/console.root.page"
import LoginPage from "./app/login/login.page"
import {
  IntegrationRoutes,
  LLMRoutes,
  LoggingRoutes,
  SERoutes,
  SNRoutes,
  StoreRoutes,
  TokenRoutes
} from "./app/routes"
import { ROUTES } from "./app/routes.const"
import { ThemeProvider } from "./components/theme-provider"
import { Toaster } from "./components/ui/sonner"

function App() {
  return (
    <div className="App">
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <Toaster />
        <Routes>
          <Route path={ROUTES.ROOT} element={<Navigate to={ROUTES.CONSOLE} replace />} />
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.CONSOLE} element={<ConsoleRootPage />}>
            <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
            {SERoutes}
            {SNRoutes}
            {StoreRoutes}
            {TokenRoutes}
            {LLMRoutes}
            {IntegrationRoutes}
            {LoggingRoutes}
          </Route>
        </Routes>
      </ThemeProvider>
    </div>
  )
}

export default App