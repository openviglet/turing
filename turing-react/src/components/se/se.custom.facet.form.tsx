"use client"
import { ROUTES } from "@/app/routes.const"
import {
  Button
} from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import {
  Input
} from "@/components/ui/input"
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model"
import { TurSECustomFacetService } from "@/services/se/se.custom.facet.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../ui/dialog"

const turSECustomFacetService = new TurSECustomFacetService();

interface Props {
  value: TurSECustomFacet;
  isNew: boolean;
}

export const SECustomFacetForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSECustomFacet>({
    defaultValues: value
  });
  const { control } = form;
  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(customFacet: TurSECustomFacet) {
    try {
      if (isNew) {
        const result = await turSECustomFacetService.create(customFacet);
        if (result) {
          toast.success(`The ${customFacet.label} Custom Facet was saved`);
          navigate(`${ROUTES.SE_INSTANCE}/custom-facet`);
        } else {
          toast.error(`The ${customFacet.label} Custom Facet was not saved`);
        }
      }
      else {
        const result = await turSECustomFacetService.update(customFacet);
        if (result) {
          toast.success(`The ${customFacet.label} Custom Facet was updated`);
        } else {
          toast.error(`The ${customFacet.label} Custom Facet was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";
      toast.error(`Failed to submit the form: ${errorMessage}`);
    }
  }

  async function onDelete() {
    try {
      if (await turSECustomFacetService.delete(value)) {
        toast.success(`The ${value.label} Custom Facet was deleted`);
        navigate(`${ROUTES.SE_INSTANCE}/custom-facet`);
      }
      else {
        toast.error(`The ${value.label} Custom Facet was not deleted`);
      }

    } catch (error) {
      console.error("Form deletion error", error);
      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";
      toast.error(`Failed to delete: ${errorMessage}`);
    }
    setOpen(false);
  }

  return (
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Custom Facet</CardTitle>
          <CardAction>
            {!isNew &&
              <Dialog open={open} onOpenChange={setOpen}>
                <form>
                  <DialogTrigger asChild>
                    <Button variant={"outline"}>Delete</Button>
                  </DialogTrigger>
                  <DialogContent className="sm:max-w-112.5">
                    <DialogHeader>
                      <DialogTitle>Are you absolutely sure?</DialogTitle>
                      <DialogDescription>
                        Unexpected bad things will happen if you don't read this!
                      </DialogDescription>
                    </DialogHeader>
                    <p className="grid gap-4">
                      This action cannot be undone. This will permanently delete the {value.label} custom facet.
                    </p>
                    <DialogFooter>
                      <Button onClick={onDelete} variant="destructive">I understand the consequences, delete this custom facet</Button>
                    </DialogFooter>
                  </DialogContent>
                </form>
              </Dialog>
            }
          </CardAction>
          <CardDescription>
            Custom facet settings for creating facets with specific ranges.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
              <FormField
                control={control}
                name="label"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Facet Label</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., Price Range, Date Range"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>Display name for the facet that will appear to users.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="field"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Field Selection</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., price, created_date, quantity"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>The underlying data field that this facet will be based on.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="rangeStart"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Range Start</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., 2025-10-01 or 1200.3"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>
                      Start value of the range. For dates, use ISO format (e.g., 2025-10-01). For numbers, use decimal format (e.g., 1200.3).
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="rangeEnd"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Range End</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., 2025-11-30 or 1500.0"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>
                      End value of the range. For dates, use ISO format (e.g., 2025-11-30). For numbers, use decimal format (e.g., 1500.0).
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Button type="submit">Save</Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
