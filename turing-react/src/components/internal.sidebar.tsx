import { IconDownload } from "@tabler/icons-react";
import React from "react";
import { NavLink, useLocation } from "react-router-dom";
import { DialogDelete } from "./dialog.delete";
import { GradientButton } from "./ui/gradient-button";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "./ui/sidebar";

// Reutilizando as interfaces do seu arquivo principal
interface InternalSidebarProps {
  icon: React.ElementType;
  feature: string;
  name: string;
  urlBase?: string;
  isNew?: boolean;
  data?: any; // Ajuste para DataType se estiver no mesmo arquivo
  onDelete: () => void;
  onExport?: () => void;
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
}

const formatCount = (value?: number) => (value ?? 0).toLocaleString();

export const InternalSidebar: React.FC<InternalSidebarProps> = ({
  icon: Icon,
  feature,
  name,
  urlBase,
  isNew,
  data,
  onDelete,
  onExport,
  open,
  setOpen,
}) => {
  const location = useLocation();
  const pathname = location.pathname;
  const { state } = useSidebar();
  const isCollapsed = state === "collapsed";

  return (
    <Sidebar
      collapsible="icon"
      variant="inset"
      position="absolute"
    >
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem className="flex items-center">
            <SidebarMenuButton asChild className="data-[slot=sidebar-menu-button]:p-1.5!">
              <NavLink to={urlBase ?? "#"} className="flex items-center gap-2">
                <Icon className="size-7!" />
                {!isCollapsed && (
                  <span className="text-base font-semibold whitespace-nowrap">
                    {isNew ? `New ${feature}` : name}
                  </span>
                )}
              </NavLink>
            </SidebarMenuButton>
            {!isCollapsed && (
              <div className="flex items-center gap-1 mr-auto">
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
                  <DialogDelete
                    feature={feature}
                    name={name}
                    onDelete={onDelete}
                    open={open}
                    setOpen={setOpen}
                  />
                )}
              </div>
            )}
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        {data?.counts && data.counts.length > 0 && (
          <SidebarGroup>
            <SidebarGroupLabel>Indexing</SidebarGroupLabel>
            <SidebarGroupContent className="flex flex-col gap-2 pt-4">
              <SidebarMenu>
                {data.counts.map((item: any) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      tooltip={item.title + ": " + formatCount(item.count)}
                      variant="outline"
                    >
                      {item.icon && <item.icon className="size-6!" />}
                      <span>{item.title}</span>
                    </SidebarMenuButton>
                    {!isCollapsed && (
                      <SidebarMenuBadge>{formatCount(item.count)}</SidebarMenuBadge>
                    )}
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
        <SidebarGroup>
          <SidebarGroupLabel>{feature}</SidebarGroupLabel>
          <SidebarGroupContent className="flex flex-col gap-2">
            <SidebarMenu>
              {data?.navMain.map((item: any) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    tooltip={item.title}
                    isActive={pathname.startsWith(urlBase + item.url)}
                    asChild
                  >
                    <NavLink to={urlBase + item.url}>
                      {item.icon && <item.icon className="size-6!" />}
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
  );
};