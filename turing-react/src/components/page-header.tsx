import { Separator } from "@/components/ui/separator";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { NavLink } from "react-router-dom";
import { ModeToggle } from "./mode-toggle";
import { Button } from "./ui/button";

interface MyComponentProps {
  turIcon?: React.ElementType;
  title: string;
  urlBase?: string;
  urlNew?: string;
}


export const PageHeader: React.FC<MyComponentProps> = ({ turIcon: TurIcon, title, urlBase, urlNew }) => {
  return (
    <header className="flex h-(--header-height) shrink-0 items-center gap-2 border-b transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-(--header-height)">
      <div className="flex w-full items-center gap-1 px-4 lg:gap-2 lg:px-6">
        <SidebarTrigger className="-ml-1" />
        <Separator
          orientation="vertical"
          className="mx-2 data-[orientation=vertical]:h-4"
        />
        {urlBase ? (
          <NavLink to={urlBase} className="flex items-center gap-2">
            {TurIcon && <TurIcon />}
            <h1 className="text-base font-medium"> {title}</h1>
          </NavLink>
        ) : (
          <>
            {TurIcon && <TurIcon />}
            <h1 className="text-base font-medium"> {title}</h1>
          </>
        )}
        <div className="ml-auto flex items-center gap-2">
          {urlNew !== undefined && (
            <Button>
              <NavLink to={urlNew} className="flex items-center gap-2">{TurIcon && <TurIcon />} Add</NavLink>
            </Button>
          )}
          <ModeToggle></ModeToggle>
        </div>
      </div>
    </header>
  )
}