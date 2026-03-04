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
import { useGlobalDecimalSeparator } from "@/hooks/use-global-decimal-separator"
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model"
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate, useParams } from "react-router-dom"
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
  const { decimalSymbol, normalizeMaybeDecimalString } = useGlobalDecimalSeparator();
  const navigate = useNavigate()
  const { id, groupIdName } = useParams() as { id: string, groupIdName: string };

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(customFacet: TurSNSiteCustomFacet) {
    try {
      if (isNew) {
        const result = await turSNSiteCustomFacetService.create(customFacet, groupIdName ?? "");
        if (result) {
          toast.success(`The ${customFacet.label} Custom Facet was saved`);
          navigate(`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${groupIdName}`);
        } else {
          toast.error(`The ${customFacet.label} Custom Facet was not saved`);
        }
      }
      else {
        const result = await turSNSiteCustomFacetService.update(customFacet, groupIdName ?? "");
        if (result) {
          toast.success(`The ${customFacet.label} Custom Facet was updated`);
          navigate(`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${groupIdName}`);
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
        navigate(`${ROUTES.SN_INSTANCE}/${id}/custom-facet`);
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
    <div className="min-h-[60vh] w-full px-4 md:px-8 py-6">
      <Card className="mx-auto w-full max-w-4xl">
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
                rules={{
                  required: "Required",
                }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Facet Label</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., 0_<_2000"
                        type="text"
                        disabled={!isNew}
                      />
                    </FormControl>
                    <FormDescription>
                      Identificador do item (agora usado na URL e referência).
                    </FormDescription>
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
                        placeholder={`e.g., 2025-10-01 or 1200${decimalSymbol}3`}
                        type="text"
                        onBlur={(event) => {
                          field.onChange(normalizeMaybeDecimalString(event.target.value ?? ""));
                        }}
                      />
                    </FormControl>
                    <FormDescription>
                      Start value of the range. For dates, use ISO format (e.g., 2025-10-01). For numbers, use decimal format according to global settings (e.g., 1200{decimalSymbol}3).
                    </FormDescription>
                    {`Numeric values use ${decimalSymbol} as decimal separator.`}
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
                        placeholder={`e.g., 2025-11-30 or 1500${decimalSymbol}0`}
                        type="text"
                        onBlur={(event) => {
                          field.onChange(normalizeMaybeDecimalString(event.target.value ?? ""));
                        }}
                      />
                    </FormControl>
                    <FormDescription>
                      End value of the range. For dates, use ISO format (e.g., 2025-11-30). For numbers, use decimal format according to global settings (e.g., 1500{decimalSymbol}0).
                    </FormDescription>
                    {`Numeric values use ${decimalSymbol} as decimal separator.`}
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
