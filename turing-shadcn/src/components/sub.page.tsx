import { NavLink, Outlet, useLocation } from "react-router-dom";
import React, { type Dispatch, type SetStateAction } from "react";
import { Card } from "./ui/card";
import { Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuAction, SidebarMenuButton, SidebarMenuItem, SidebarProvider } from "./ui/sidebar";
import { DialogDelete } from "./dialog.delete";

interface NavMainItem {
  title: string;
  url: string;
  icon?: React.ElementType;
}

interface DataType {
  navMain: NavMainItem[];
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
}

export const SubPage: React.FC<Props> = ({ icon: Icon, feature, name, urlBase, isNew, data, onDelete, open, setOpen }) => {
  const location = useLocation();
  const pathname = location.pathname;
  return (
    <div className="flex w-full items-center justify-center px-8 py-4">
      <Card className="w-full bg-sidebar py-1">
        <SidebarProvider
          style={
            {
              "--sidebar-width": "calc(var(--spacing) * 72)",
              "--header-height": "calc(var(--spacing) * 12)",
            } as React.CSSProperties
          }
        >
          <Sidebar collapsible="none" variant="inset" color="black">
            <SidebarHeader>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    asChild
                    className="data-[slot=sidebar-menu-button]:!p-1.5">
                    <NavLink to="/admin">
                      <Icon className="!size-5" />
                      {isNew ? (<span
                        className="text-base font-semibold">New {feature}</span>) : (
                        <span className="text-base font-semibold">{name}</span>)}
                    </NavLink>
                  </SidebarMenuButton>
                  <SidebarMenuAction>{!isNew &&
                    <DialogDelete feature={feature} name={name} onDelete={onDelete} open={open} setOpen={setOpen} />
                  }</SidebarMenuAction>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarHeader>
            <SidebarContent>
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
            <main className="flex flex-1 flex-col overflow-hidden xl:ml-8 pt-4">
              <Outlet />
            </main>
          </SidebarInset>
        </SidebarProvider>
      </Card>
    </div>
  )
}
