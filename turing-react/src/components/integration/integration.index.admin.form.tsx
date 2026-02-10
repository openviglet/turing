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
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model"
import type { TurIntegrationIndexingManager } from "@/models/integration/integration-indexing-manager.model"
import { TurIntegrationAemSourceService } from "@/services/integration/integration-aem-source.service"
import { TurIntegrationIndexAdminService } from "@/services/integration/integration-index-admin.service"
import { useEffect, useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { DynamicIndexingRuleFields } from "./dynamic.indexing.rule.field"

interface IndexingManagerFormValues {
  source: string;
  attribute?: "id" | "url";
  values: string[];
  recursive: boolean;
}

interface IntegrationIndexingManagerFormProps {
  integrationId: string;
  mode: "PUBLISHING" | "UNPUBLISHING" | "INDEXING" | "DEINDEXING";
}

export const IntegrationIndexAdminForm: React.FC<IntegrationIndexingManagerFormProps> = ({ integrationId, mode }) => {
  const turIntegrationIndexAdminService = useMemo(() => new TurIntegrationIndexAdminService(integrationId), [integrationId]);
  const turIntegrationAemSourceService = useMemo(() => new TurIntegrationAemSourceService(integrationId), [integrationId]);

  const form = useForm<IndexingManagerFormValues>({
    defaultValues: {
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

  async function onSubmit(data: IndexingManagerFormValues) {
    try {
      if (!data.attribute) {
        toast.error("Please select Target Attribute")
        return
      }

      const isRecursive = data.attribute === "id" && data.recursive;

      const payload: TurIntegrationIndexingManager = {
        attribute: data.attribute.toUpperCase() as "ID" | "URL",
        paths: data.values,
        event: mode,
        ...(isRecursive && { recursive: true }),
      };

      await turIntegrationIndexAdminService.submit(data.source, payload);
      toast.success(`${mode} requested successfully`)

      form.reset({
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
          name="source"
          rules={{ required: true }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Content Source</FormLabel>
              <FormControl>
                <Select value={field.value || ""} onValueChange={field.onChange}>
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
                <Select value={field.value || ""} onValueChange={field.onChange}>
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
