import { AppSidebar } from "@/components/app-sidebar"
import {
  SidebarInset,
  SidebarProvider,
} from "@/components/ui/sidebar"
import { Outlet } from "react-router-dom"

export default function ConsoleRootPage() {
  return (
    <SidebarProvider
      style={
        {
          "--sidebar-width": "calc(var(--spacing) * 72)",
          "--header-height": "calc(var(--spacing) * 12)",
        } as React.CSSProperties
      }
      className="min-h-svh"
    >
      <AppSidebar variant="inset" />
      <SidebarInset>
         <Outlet />
      </SidebarInset>
    </SidebarProvider>
  )
}
