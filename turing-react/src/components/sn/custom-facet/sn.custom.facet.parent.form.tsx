import { ROUTES } from "@/app/routes.const"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import type { TurSNSiteCustomFacetParent } from "@/models/sn/sn-site-custom-facet-parent.model"
import { TurSNSiteCustomFacetParentService } from "@/services/sn/sn.site.custom.facet.parent.service"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { useNavigate, useParams } from "react-router-dom"
import { toast } from "sonner"
import { GradientButton } from "../../ui/gradient-button"
const service = new TurSNSiteCustomFacetParentService();

interface Props {
  value: TurSNSiteCustomFacetParent;
  isNew: boolean;
}

export const SNSiteCustomFacetParentForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSNSiteCustomFacetParent>({ defaultValues: value });
  const { control } = form;
  const navigate = useNavigate()
  const { id } = useParams() as { id: string };

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(group: TurSNSiteCustomFacetParent) {
    try {
      if (isNew) {
        const result = await service.create(group);
        if (result) {
          toast.success(`The ${group.idName} Custom Facet Group was saved`);
          navigate(`${ROUTES.SN_INSTANCE}/${id}/custom-facet`);
        } else {
          toast.error(`The ${group.idName} Custom Facet Group was not saved`);
        }
      } else {
        const result = await service.update(group);
        if (result) {
          toast.success(`The ${group.idName} Custom Facet Group was updated`);
          navigate(`${ROUTES.SN_INSTANCE}/${id}/custom-facet`);
        } else {
          toast.error(`The ${group.idName} Custom Facet Group was not updated`);
        }
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";
      toast.error(`Failed to submit the form: ${errorMessage}`);
    }
  }

  return (
    <div className="min-h-[60vh] w-full px-4 md:px-8 py-6">
      <Card className="mx-auto w-full max-w-4xl">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Custom Facet Group</CardTitle>
          <CardAction></CardAction>
          <CardDescription>Define a group (NameID) and its base attributes.</CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
              <FormField
                control={control}
                name="idName"
                rules={{
                  required: "Required",
                  pattern: {
                    value: /^[A-Za-z0-9_-]+$/,
                    message: "Only letters, numbers, underscores, and hyphens are allowed."
                  }
                }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Group NameID</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="e.g., price, quantity" type="text" disabled={!isNew} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={control}
                name="attribute"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Attribute</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="e.g., price" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={control}
                name="selection"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Field Selection</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="e.g., price" type="text" />
                    </FormControl>
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
