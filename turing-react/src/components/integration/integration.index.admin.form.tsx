import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model"
import { TurIntegrationAemSourceService } from "@/services/integration/integration-aem-source.service"
import { TurIntegrationIndexAdminService } from "@/services/integration/integration-index-admin.service"
import { useEffect, useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { DynamicIndexingRuleFields } from "./dynamic.indexing.rule.field"

interface IndexAdminFormValues {
  action?: "INDEX" | "DEINDEX";
  source: string;
  attribute?: "id" | "url";
  values: string[];
  recursive: boolean;
}

interface IntegrationIndexAdminFormProps {
  integrationId: string;
}

export const IntegrationIndexAdminForm: React.FC<IntegrationIndexAdminFormProps> = ({ integrationId }) => {
  const turIntegrationIndexAdminService = useMemo(() => new TurIntegrationIndexAdminService(integrationId), [integrationId]);
  const turIntegrationAemSourceService = useMemo(() => new TurIntegrationAemSourceService(integrationId), [integrationId]);

  const form = useForm<IndexAdminFormValues>({
    defaultValues: {
      action: undefined,
      source: "",
      attribute: undefined,
      values: [""],
      recursive: false,
    }
  })

  const [sources, setSources] = useState<TurIntegrationAemSource[]>([])

  useEffect(() => {
    turIntegrationAemSourceService.query().then(setSources)
  }, [turIntegrationAemSourceService])

  async function onSubmit(data: IndexAdminFormValues) {
    try {
      if (!data.action || !data.attribute) {
        toast.error("Please select Action and Target Attribute")
        return
      }
      const isRecursive = data.attribute === "id" && data.recursive;
      const eventType: "PUBLISHING" | "UNPUBLISHING" = data.action === "INDEX" ? "PUBLISHING" : "UNPUBLISHING";
      const payload = {
        attribute: data.attribute!.toUpperCase() as "ID" | "URL",
        paths: data.values,
        event: eventType,
        ...(isRecursive && { recursive: true }),
      };

      if (data.action === "INDEX") {
        await turIntegrationIndexAdminService.index(data.source, payload);
        toast.success("Indexing requested successfully")
      } else {
        await turIntegrationIndexAdminService.deindex(data.source, payload);
        toast.success("Deindexing requested successfully")
      }
      form.reset({
        action: undefined,
        source: "",
        attribute: undefined,
        values: [""],
        recursive: false,
      })
    } catch (error) {
      toast.error("Request failed")
      console.error(error)
    }
  }

  const selectedAttribute = form.watch("attribute")

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <FormField
          control={form.control}
          name="action"
          rules={{ required: true }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Action</FormLabel>
              <FormControl>
                <ToggleGroup type="single" value={field.value} onValueChange={field.onChange} className="gap-2">
                  <ToggleGroupItem value="INDEX" aria-label="Index">Index</ToggleGroupItem>
                  <ToggleGroupItem value="DEINDEX" aria-label="Deindex">De-index</ToggleGroupItem>
                </ToggleGroup>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="source"
          rules={{ required: true }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Content Source</FormLabel>
              <FormControl>
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select a source" />
                  </SelectTrigger>
                  <SelectContent>
                    {sources.map((s) => (
                      <SelectItem key={s.id} value={s.name}>{s.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="attribute"
          rules={{ required: true }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Target Attribute</FormLabel>
              <FormControl>
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select an attribute" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="id">ID</SelectItem>
                    <SelectItem value="url">URL</SelectItem>
                  </SelectContent>
                </Select>
              </FormControl>
              <FormDescription>Attribute used to match the values</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="values"
          rules={{ required: true }}
          render={() => (
            <FormItem>
              <FormLabel>Matching Values</FormLabel>
              <FormControl>
                <DynamicIndexingRuleFields control={form.control} register={form.register} fieldName="values" />
              </FormControl>
              <FormDescription>One or more values to match</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {selectedAttribute === "id" && (
          <Accordion type="single" collapsible>
            <AccordionItem value="advanced-options">
              <AccordionTrigger>Advanced Options</AccordionTrigger>
              <AccordionContent>
                <FormField
                  control={form.control}
                  name="recursive"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4 shadow">
                      <FormControl>
                        <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                      </FormControl>
                      <div className="space-y-1 leading-none">
                        <FormLabel>Recursive</FormLabel>
                        <FormDescription>Apply this action to child pages/assets</FormDescription>
                      </div>
                    </FormItem>
                  )}
                />
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        )}

        <div className="flex gap-2">
          <Button type="submit">Submit</Button>
        </div>
      </form>
    </Form>
  )
}
