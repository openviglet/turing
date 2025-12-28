import { Route, Routes } from "react-router-dom"
import { ThemeProvider } from "../components/theme-provider"
import { Toaster } from "../components/ui/sonner"
import SearchPage from "./pages/search.page"

function SearchApp() {
  return (
    <ThemeProvider defaultTheme="light" storageKey="turing-sn-theme">
      <Toaster />
      <Routes>
        <Route path="/sn/:siteName" element={<SearchPage />} />
      </Routes>
    </ThemeProvider>
  )
}

export default SearchApp
