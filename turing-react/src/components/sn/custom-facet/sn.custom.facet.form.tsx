"use client"
import { ROUTES } from "@/app/routes.const"
import { DialogDelete } from "@/components/dialog.delete"
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
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model"
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { GradientButton } from "../../ui/gradient-button"

const turSNSiteCustomFacetService = new TurSNSiteCustomFacetService();

interface Props {
  value: TurSNSiteCustomFacet;
  isNew: boolean;
}

export const SNSiteCustomFacetForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSNSiteCustomFacet>({
    defaultValues: value
  });
  const { control } = form;
  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(customFacet: TurSNSiteCustomFacet) {
    try {
      if (isNew) {
        const result = await turSNSiteCustomFacetService.create(customFacet);
        if (result) {
          toast.success(`The ${customFacet.label} Custom Facet was saved`);
          navigate(`${ROUTES.SN_INSTANCE}/custom-facet`);
        } else {
          toast.error(`The ${customFacet.label} Custom Facet was not saved`);
        }
      }
      else {
        const result = await turSNSiteCustomFacetService.update(customFacet);
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
      if (await turSNSiteCustomFacetService.delete(value)) {
        toast.success(`The ${value.label} Custom Facet was deleted`);
        navigate(`${ROUTES.SN_INSTANCE}/custom-facet`);
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
            {!isNew && <DialogDelete feature="Custom Facet" name={value.label} onDelete={onDelete} open={open} setOpen={setOpen} />}
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

              <GradientButton type="submit">Save</GradientButton>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
