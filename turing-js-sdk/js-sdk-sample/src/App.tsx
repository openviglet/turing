import { Route, Routes } from "react-router-dom";
import { ROUTES } from "./app/routes.const";
import SearchRootPage from "./app/search.root.page";
import { ThemeProvider } from "./components/theme-provider";

function App() {

  return (
    <div className="App">
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <Routes>
          <Route path={ROUTES.ROOT} element={<SearchRootPage />} />
        </Routes>
      </ThemeProvider>
    </div>
  );
}

export default App;