import { Outlet, Link } from "react-router-dom";
import { ROUTES } from "../routes.const";

export default function ConsoleRootPage() {
  return (
    <div className="min-h-screen bg-background">
      <nav className="bg-card border-b border-border">
        <div className="mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <h1 className="text-xl font-bold text-foreground">Turing Console</h1>
              </div>
              <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.SN_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Semantic Navigation
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.SE_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Search Engine
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.INTEGRATION_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Integration
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.LLM_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  LLM
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.STORE_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Store
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.TOKEN_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Token
                </Link>
                <Link
                  to={`${ROUTES.CONSOLE}/${ROUTES.LOGGING_INSTANCE}`}
                  className="border-transparent text-muted-foreground hover:border-primary hover:text-foreground inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Logging
                </Link>
              </div>
            </div>
          </div>
        </div>
      </nav>
      
      <main className="py-10">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
