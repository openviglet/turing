import { Route, Routes } from "react-router-dom"
import SearchPage from "./pages/search.page"
import { ThemeProvider } from "../components/theme-provider"
import { Toaster } from "../components/ui/sonner"

function SearchApp() {
  return (
    <ThemeProvider defaultTheme="light" storageKey="turing-sn-theme">
      <Toaster />
      <Routes>
        <Route path="/" element={<SearchPage />} />
        <Route path="/:siteName" element={<SearchPage />} />
      </Routes>
    </ThemeProvider>
  )
}

export default SearchApp
