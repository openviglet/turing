import { Button } from "./ui/button";
import { Separator } from "./ui/separator";
import { NavLink } from "react-router-dom";
import React from "react";

interface Props {
  icon: React.ElementType
  title: string;
  description: string;
  urlNew?: string;
  urlBase?:string;
}
export const SubPageHeader: React.FC<Props> = ({ icon: Icon, title, description, urlNew, urlBase }) => {
  return (
    <header className="mb-5 h-(--header-height) shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-(--header-height)">
      <div className="w-full flex items-center gap-1 pr-4 lg:gap-2 lg:pr-6">
        {urlBase !== undefined ? (
          <>
            {Icon && <Icon />}
            <NavLink to={urlBase}>
              <h1 className="text-base font-medium"> {title}</h1>
            </NavLink>
          </>
        ) :
          (<> 
          {Icon && <Icon />}
            <h1 className="text-base font-medium"> {title}</h1>
            
          </>)
        }
        <div className="ml-auto flex items-center gap-2">
          {urlNew !== undefined && (
            <Button>
              {Icon && <Icon />}
              <NavLink to={urlNew}>New {title}</NavLink>
            </Button>
          )}
        </div>
      </div>
       <div className="w-full text-muted-foreground text-sm mt-1">{description}</div>
       <Separator className="mt-2" />
    </header>
  )
}
