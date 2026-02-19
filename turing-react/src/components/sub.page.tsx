import { IconDownload } from "@tabler/icons-react";
import React, { type Dispatch, type SetStateAction } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { DialogDelete } from "./dialog.delete";
import { Card } from "./ui/card";
import { GradientButton } from "./ui/gradient-button";
import { Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuBadge, SidebarMenuButton, SidebarMenuItem, SidebarProvider } from "./ui/sidebar";

interface NavMainItem {
  title: string;
  url: string;
  icon?: React.ElementType;
}

interface NavCountItem {
  title: string;
  icon?: React.ElementType;
  count?: number;
}

interface DataType {
  navMain: NavMainItem[];
  counts?: NavCountItem[];
}

interface Props {
  icon: React.ElementType
  feature: string;
  name: string;
  urlBase?: string;
  isNew?: boolean;
  data?: DataType;
  open: boolean;
  setOpen: Dispatch<SetStateAction<boolean>>
  onDelete: () => void;
  onExport?: () => void;
}

const formatCount = (value?: number) => (value ?? 0).toLocaleString();

export const SubPage: React.FC<Props> = ({ icon: Icon, feature, name, urlBase, isNew, data, onDelete, onExport, open, setOpen }) => {
  const location = useLocation();
  const pathname = location.pathname;
  return (
    <div className="flex w-full items-center justify-center px-8 py-4">
      <Card className="w-full bg-sidebar py-1 overflow-hidden">
        <SidebarProvider
          style={
            {
              "--sidebar-width": "calc(var(--spacing) * 72)",
              "--header-height": "calc(var(--spacing) * 12)",
            } as React.CSSProperties
          }
        >
          <Sidebar collapsible="offcanvas" variant="inset" position="absolute" color="black">
            <SidebarHeader>
              <SidebarMenu>
                <SidebarMenuItem className="flex items-center">
                  <SidebarMenuButton asChild className="data-[slot=sidebar-menu-button]:p-1.5!">
                    <NavLink to={urlBase ?? "#"} className="flex items-center gap-2">
                      <Icon className="size-5!" />
                      <span className="text-base font-semibold">
                        {isNew ? `New ${feature}` : name}
                      </span>
                    </NavLink>
                  </SidebarMenuButton>
                  <div className="flex items-center gap-1 ml-auto">
                    {!isNew && onExport && (
                      <GradientButton
                        variant="ghost"
                        size="icon-sm"
                        onClick={onExport}
                      >
                        <IconDownload className="size-5!" />
                      </GradientButton>
                    )}
                    {!isNew && (
                      <DialogDelete feature={feature} name={name} onDelete={onDelete} open={open} setOpen={setOpen} />
                    )}
                  </div>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarHeader>
            <SidebarContent>
              {data?.counts && data.counts.length > 0 && (
                <SidebarGroup>
                  <SidebarGroupContent className="flex flex-col gap-2 pt-4">
                    <SidebarMenu>
                      {data.counts.map((item) => (
                        <SidebarMenuItem key={item.title}>
                          <SidebarMenuButton
                            tooltip={item.title}
                            variant="outline"
                            size="sm"
                            disabled
                          >
                            {item.icon && <item.icon />}
                            <span>{item.title}</span>
                          </SidebarMenuButton>
                          <SidebarMenuBadge>{formatCount(item.count)}</SidebarMenuBadge>
                        </SidebarMenuItem>
                      ))}
                    </SidebarMenu>
                  </SidebarGroupContent>
                </SidebarGroup>
              )}
              <SidebarGroup>
                <SidebarGroupContent className="flex flex-col gap-2">
                  <SidebarMenu>
                    {data?.navMain.map((item) => (
                      <SidebarMenuItem key={item.title}>
                        <SidebarMenuButton tooltip={item.title}
                          isActive={pathname.startsWith(urlBase + item.url)}
                          asChild>
                          <NavLink to={urlBase + item.url}>
                            {item.icon && <item.icon />}
                            <span>{item.title}</span>
                          </NavLink>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    ))}
                  </SidebarMenu>
                </SidebarGroupContent>
              </SidebarGroup>

            </SidebarContent>
          </Sidebar>
          <SidebarInset className="mr-1 rounded-xl border ">
            <main className="flex flex-1 flex-col overflow-hidden pt-4">
              <Outlet />
            </main>
          </SidebarInset>
        </SidebarProvider>
      </Card>
    </div>
  )
}
