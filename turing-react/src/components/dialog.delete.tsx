import { IconTrash } from "@tabler/icons-react";
import React, { type Dispatch, type SetStateAction } from "react";
import { Button } from "./ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";

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
          <Button variant={"outline"} className="mr-2" ><IconTrash /></Button>
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
            <Button onClick={onDelete} variant="destructive">I understand
              the consequences, delete this {feature}</Button>
          </DialogFooter>
        </DialogContent>
      </form>
    </Dialog>
  )
}
