import {
  IconCode,
  IconCpu2,
  IconDatabase,
  IconFileImport,
  IconPlugConnectedX,
  IconReceiptRupee,
  IconSearch,
  IconZoomCode
} from "@tabler/icons-react"
import * as React from "react"

import { ROUTES } from "@/app/routes.const"
import { NavMain } from "@/components/nav-main"
import { NavUser } from "@/components/nav-user"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar"
import type { TurUser } from "@/models/auth/user"
import { TurUserService } from "@/services/auth/user.service"
import { NavLink } from "react-router-dom"
import { TurLogo } from "./logo/tur-logo"

const data = {
  navMain: [
    {
      title: "Search Engine",
      url: "/admin/se/instance",
      icon: IconZoomCode,
    },
    {
      title: "Language Model",
      url: "/admin/llm/instance",
      icon: IconCpu2,
    },
    {
      title: "Embedding Store",
      url: "/admin/store/instance",
      icon: IconDatabase,
    },
    {
      title: "Semantic Navigation",
      url: "/admin/sn/instance",
      icon: IconSearch,
    },
    {
      title: "Integration",
      url: "/admin/integration/instance",
      icon: IconPlugConnectedX,
    },
    {
      title: "Logging",
      url: "/admin/logging/instance",
      icon: IconReceiptRupee,
    },
    {
      title: "Import",
      url: "/admin/exchange/import",
      icon: IconFileImport,
    },
    {
      title: "API Token",
      url: "/admin/token/instance",
      icon: IconCode,
    },
  ]
}
const turUserService = new TurUserService();
export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const [user, setUser] = React.useState<TurUser>({} as TurUser);
  React.useEffect(() => {

    turUserService.get().then(setUser);
  }, [])
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              asChild
              className="data-[slot=sidebar-menu-button]:p-1.5!">
              <NavLink to={ROUTES.CONSOLE}>
                <TurLogo className="size-6!" />
                <span className="text-base font-semibold">Turing ES</span>
              </NavLink>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
    </Sidebar>
  )
}
