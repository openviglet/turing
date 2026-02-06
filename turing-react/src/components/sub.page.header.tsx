import React from "react";
import { NavLink } from "react-router-dom";
import { DialogDelete } from "./dialog.delete";
import { Button } from "./ui/button";
import { Separator } from "./ui/separator";

interface Props {
  icon: React.ElementType
  feature: string;
  name: string;
  description: string;
  urlNew?: string;
  urlBase?: string;
  onDelete?: () => void;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
}
export const SubPageHeader: React.FC<Props> = ({ icon: Icon, feature, name, description, urlNew, urlBase, onDelete, open, setOpen }) => {
  return (
    <header className="mb-5">
      <div className="w-full flex items-center gap-1 pr-4 lg:gap-2 lg:pr-6">
        {urlBase ? (
          <NavLink to={urlBase} className="flex items-center gap-2">
            <h1 className="text-base font-medium">{Icon && <Icon />} {feature}</h1>
          </NavLink>
        ) : (
          <>
            {Icon && <Icon />}
            <h1 className="text-base font-medium"> {feature}</h1>
          </>
        )}
        {urlNew !== undefined && <div className="ml-auto flex items-center gap-2 pr-2">
          <Button>
            <NavLink to={urlNew} className="flex items-center gap-2">{Icon && <Icon />} New {feature}</NavLink>
          </Button>
        </div>}
        {open !== undefined && onDelete !== undefined && setOpen !== undefined && <div className="ml-auto flex items-center gap-2">
          <DialogDelete feature={feature} name={name} onDelete={onDelete} open={open} setOpen={setOpen} />
        </div>}
      </div>
      <div className="w-full text-muted-foreground text-sm mt-1">{description}</div>
      <Separator className="mt-2" />
    </header>
  )
}
