"use client"
import { ROUTES } from "@/app/routes.const"
import {
  Button
} from "@/components/ui/button"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select"
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurIntegrationIndexingRule } from "@/models/integration/integration-indexing-rule.model"
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model"
import type { TurSNSite } from "@/models/sn/sn-site.model"
import { TurIntegrationIndexingRuleService } from "@/services/integration/integration-indexing-rule.service"
import { TurSNFieldService } from "@/services/sn/sn.field.service"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DynamicIndexingRuleFields } from "./dynamic.indexing.rule.field"

interface Props {
  value: TurIntegrationIndexingRule;
  integrationId: string;
  isNew: boolean;
}

export const IntegrationIndexingRulesForm: React.FC<Props> = ({ value, integrationId, isNew }) => {
  const turSNSiteService = new TurSNSiteService();
  const turSNFieldService = new TurSNFieldService();
  const form = useForm<TurIntegrationIndexingRule>();
  const { control, register, setValue, watch } = form;
  const [turSNSites, setTurSNSites] = useState<TurSNSite[]>([] as TurSNSite[]);
  const [turSNSiteFields, setTurSNSiteFields] = useState<TurSNSiteField[]>([] as TurSNSiteField[]);
  const navigate = useNavigate();
  const selectedSource = watch("source");

  useEffect(() => {
    turSNSiteService.query().then((sites) => {
      setTurSNSites(sites);
    });

    setValue("id", value.id)
    setValue("name", value.name);
    setValue("description", value.description);
    setValue("ruleType", value.ruleType);
    setValue("source", value.source);
    setValue("attribute", value.attribute);
    setValue("values", value.values);
  }, [setValue, value]);

  useEffect(() => {
    if (selectedSource && turSNSites.length > 0) {
      const selectedSite = turSNSites.find((site) => site.name === selectedSource);
      if (selectedSite?.id) {
        turSNFieldService.query(selectedSite.id).then((fields) => {
          setTurSNSiteFields(fields);
          // Re-set attribute value after fields are loaded
          if (value.attribute) {
            setValue("attribute", value.attribute);
          }
        });
      }
    }
  }, [selectedSource, turSNSites]);


  function onSubmit(integrationIndexingRule: TurIntegrationIndexingRule) {
    try {
      if (isNew) {
        const turIntegrationIndexingRuleService = new TurIntegrationIndexingRuleService(integrationId);
        turIntegrationIndexingRuleService.create(integrationIndexingRule);
        toast.success(`The ${integrationIndexingRule.name} Integration Indexing Rule was saved`);
        navigate(ROUTES.INTEGRATION_INSTANCE);
      }
      else {
        const turIntegrationIndexingRuleService = new TurIntegrationIndexingRuleService(integrationId);
        turIntegrationIndexingRuleService.update(integrationIndexingRule);
        toast.success(`The ${integrationIndexingRule.name} Integration Indexing Rule was updated`);
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 pr-8">
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Rule Name</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="e.g., Ignore Draft Pages"
                  type="text"
                />
              </FormControl>
              <FormDescription>A unique, descriptive name to easily identify this rule in the list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Rule Description</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="e.g., Excludes all draft pages from indexing"
                  className="resize-none"
                  {...field}
                />
              </FormControl>
              <FormDescription>Provide details about what this rule does and when it should be applied.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="source"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Semantic Navigation Site</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {turSNSites.map((site) => (
                    <SelectItem key={site.name} value={site.name}>{site.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>The Semantic Navigation site where this rule will be applied.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="attribute"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Field</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {turSNSiteFields.map((field) => (
                    <SelectItem key={field.name} value={field.name}>{field.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>The content attribute used to match against the values below.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="ruleType"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Action Type</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem key="IGNORE" value="IGNORE">Ignore</SelectItem>
                </SelectContent>
              </Select>
              <FormDescription>Defines the action to take when content matches this rule. "Ignore" will exclude matching content from indexing.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormItem>
          <FormLabel>Matching Values</FormLabel>
          <FormDescription>Add the values that the attribute should match. Content with matching attribute values will have the rule applied.</FormDescription>
          <FormControl>
            <DynamicIndexingRuleFields
              fieldName="values"
              control={control}
              register={register}
            />
          </FormControl>
        </FormItem>

        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

