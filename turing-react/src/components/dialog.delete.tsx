import { IconTrash } from "@tabler/icons-react";
import React, { type Dispatch, type SetStateAction } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { GradientButton } from "./ui/gradient-button";

interface Props {
  feature: string;
  name: string;
  onDelete: () => void;
  open: boolean;
  setOpen: Dispatch<SetStateAction<boolean>>
}
export const DialogDelete: React.FC<Props> = ({ feature, name, onDelete, open, setOpen }) => {
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <form>
        <DialogTrigger asChild >
          <GradientButton variant={"ghost"} size={"icon-sm"} ><IconTrash className="size-4" /></GradientButton>
        </DialogTrigger>
        <DialogContent className="sm:max-w-150">
          <DialogHeader>
            <DialogTitle>Are you absolutely sure?</DialogTitle>
            <DialogDescription>
              Unexpected bad things will happen if you don't read this!
            </DialogDescription>
          </DialogHeader>
          <p>This action cannot be undone. This will permanently delete <span className="font-semibold">{name}</span> {feature}.</p>
          <DialogFooter className="sm:justify-center">
            <GradientButton onClick={onDelete} variant="destructive">I understand
              the consequences, delete this {feature}</GradientButton>
          </DialogFooter>
        </DialogContent>
      </form>
    </Dialog>
  )
}
