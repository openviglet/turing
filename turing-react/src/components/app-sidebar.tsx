import * as React from "react"
import {
  IconHelp,
  IconInnerShadowTop,
  IconSearch,
  IconSettings,
  IconZoomCode,
  IconCpu2,
  IconDatabase,
  IconPlugConnectedX,
  IconReceiptRupee,
  IconCode
} from "@tabler/icons-react"

import { NavMain } from "@/components/nav-main"
import { NavSecondary } from "@/components/nav-secondary"
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
import { NavLink } from "react-router-dom"

const data = {
  user: {
    name: "shadcn",
    email: "m@example.com",
    avatar: "/avatars/shadcn.jpg",
  },
  navMain: [
    {
      title: "Search Engine",
      url: "/console/se/instance",
      icon: IconZoomCode,
    },
    {
      title: "Language Model",
      url: "/console/llm/instance",
      icon: IconCpu2,
    },
    {
      title: "Embedding Store",
      url: "/console/store/instance",
      icon: IconDatabase,
    },
    {
      title: "Semantic Navigation",
      url: "/console/sn/instance",
      icon: IconSearch,
    },
    {
      title: "Integration",
      url: "/console/integration/instance",
      icon: IconPlugConnectedX,
    },
    {
      title: "Logging",
      url: "/console/logging/instance",
      icon: IconReceiptRupee,
    },
    {
      title: "API Token",
      url: "/console/token/instance",
      icon: IconCode,
    },
  ],
  navSecondary: [
    {
      title: "Settings",
      url: "#",
      icon: IconSettings,
    },
    {
      title: "Get Help",
      url: "#",
      icon: IconHelp,
    },
    {
      title: "Search",
      url: "#",
      icon: IconSearch,
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {

  return (
    <Sidebar collapsible="offcanvas" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              asChild
              className="data-[slot=sidebar-menu-button]:!p-1.5">
              <NavLink to="/console">
                <IconInnerShadowTop className="!size-5" />
                <span className="text-base font-semibold">Turing ES</span>
              </NavLink>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
        <NavSecondary items={data.navSecondary} className="mt-auto" />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={data.user} />
      </SidebarFooter>
    </Sidebar>
  )
}
