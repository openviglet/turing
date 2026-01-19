"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"

import { ROUTES } from "@/app/routes.const"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import type { TurIntegrationIndexingRule } from "@/models/integration/integration-indexing-rule.model"
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model"
import type { TurSNSite } from "@/models/sn/sn-site.model"
import { TurIntegrationIndexingRuleService } from "@/services/integration/integration-indexing-rule.service"
import { TurSNFieldService } from "@/services/sn/sn.field.service"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { DynamicIndexingRuleFields } from "./dynamic.indexing.rule.field"

// Constants
const RULE_TYPES = [
  { value: "IGNORE", label: "Ignore" }
] as const;

// Props interface with descriptive naming
interface IntegrationIndexingRulesFormProps {
  value: TurIntegrationIndexingRule;
  integrationId: string;
  isNew: boolean;
}

export const IntegrationIndexingRulesForm: React.FC<IntegrationIndexingRulesFormProps> = ({
  value,
  integrationId,
  isNew
}) => {
  // Services - memoized to prevent recreation on each render
  const turSNSiteService = useMemo(() => new TurSNSiteService(), []);
  const turSNFieldService = useMemo(() => new TurSNFieldService(), []);
  const turIntegrationIndexingRuleService = useMemo(
    () => new TurIntegrationIndexingRuleService(integrationId),
    [integrationId]
  );

  // Form setup with default values
  const form = useForm<TurIntegrationIndexingRule>({
    defaultValues: value
  });
  const { control, register, setValue, watch } = form;

  // State
  const [turSNSites, setTurSNSites] = useState<TurSNSite[]>([]);
  const [turSNSiteFields, setTurSNSiteFields] = useState<TurSNSiteField[]>([]);
  const [isLoadingFields, setIsLoadingFields] = useState(false);

  // Ref to track pending attribute value that needs to be set after fields load
  const pendingAttributeRef = useRef<string | null>(value.attribute || null);

  // Navigation
  const navigate = useNavigate();
  // Watch selected source for dependent field loading
  const selectedSource = watch("source");

  // Find selected site - memoized for performance
  const selectedSite = useMemo(
    () => turSNSites.find((site) => site.name === selectedSource),
    [turSNSites, selectedSource]
  );

  // Load initial data (sites only)
  useEffect(() => {
    const loadSites = async () => {
      try {
        const sites = await turSNSiteService.query();
        setTurSNSites(sites);
      } catch (error) {
        console.error("Failed to load sites:", error);
        toast.error("Failed to load Semantic Navigation sites.");
      }
    };

    loadSites();
  }, [turSNSiteService]);

  // Reset form when value changes
  useEffect(() => {
    form.reset(value);
    // Store the attribute to be set after fields are loaded
    pendingAttributeRef.current = value.attribute || null;
  }, [form, value]);

  // Load fields when site selection changes
  useEffect(() => {
    const loadFields = async () => {
      if (!selectedSite?.id) {
        setTurSNSiteFields([]);
        return;
      }

      setIsLoadingFields(true);
      try {
        const fields = await turSNFieldService.query(selectedSite.id);
        setTurSNSiteFields(fields);
      } catch (error) {
        console.error("Failed to load fields:", error);
        toast.error("Failed to load site fields.");
      } finally {
        setIsLoadingFields(false);
      }
    };

    loadFields();
  }, [selectedSite?.id, turSNFieldService]);

  // Set attribute value after fields are loaded
  useEffect(() => {
    if (turSNSiteFields.length > 0 && pendingAttributeRef.current) {
      const attributeExists = turSNSiteFields.some(
        (field) => field.name === pendingAttributeRef.current
      );
      if (attributeExists) {
        setValue("attribute", pendingAttributeRef.current);
      }
      pendingAttributeRef.current = null;
    }
  }, [turSNSiteFields, setValue]);

  // Form submission handler - memoized with useCallback
  const onSubmit = useCallback(async (data: TurIntegrationIndexingRule) => {
    try {
      if (isNew) {
        const result = await turIntegrationIndexingRuleService.create(data);
        if (result) {
          form.reset(data); // Reset dirty state after successful save
          toast.success(`The "${data.name}" Integration Indexing Rule was created successfully.`);
          navigate(ROUTES.INTEGRATION_INSTANCE);
        } else {
          toast.error("Failed to create the rule. Please try again.");
        }
      } else {
        const result = await turIntegrationIndexingRuleService.update(data);
        if (result) {
          form.reset(data); // Reset dirty state after successful save
          toast.success(`The "${data.name}" Integration Indexing Rule was updated successfully.`);
        } else {
          toast.error("Failed to update the rule. Please try again.");
        }
      }
    } catch (error) {
      console.error("Form submission error:", error);
      toast.error("Failed to save the rule. Please try again.");
    }
  }, [isNew, turIntegrationIndexingRuleService, navigate, form]);

  // Check if attribute field should be disabled
  const isAttributeFieldDisabled = !selectedSource || isLoadingFields;

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 pr-8">
        {/* Rule Name */}
        <FormField
          control={control}
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
              <FormDescription>
                A unique, descriptive name to easily identify this rule in the list.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Rule Description */}
        <FormField
          control={control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Rule Description</FormLabel>
              <FormControl>
                <Textarea
                  {...field}
                  placeholder="e.g., Excludes all draft pages from indexing"
                  className="resize-none"
                />
              </FormControl>
              <FormDescription>
                Provide details about what this rule does and when it should be applied.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Semantic Navigation Site */}
        <FormField
          control={control}
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
                    <SelectItem key={site.id} value={site.name}>
                      {site.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                The Semantic Navigation site where this rule will be applied.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Target Attribute/Field */}
        <FormField
          control={control}
          name="attribute"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Field</FormLabel>
              <Select
                onValueChange={field.onChange}
                value={field.value}
                disabled={isAttributeFieldDisabled}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue
                      placeholder={isLoadingFields ? "Loading..." : "Choose..."}
                    />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {turSNSiteFields.map((siteField) => (
                    <SelectItem key={siteField.id} value={siteField.name}>
                      {siteField.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                The content attribute used to match against the values below.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Action Type */}
        <FormField
          control={control}
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
                  {RULE_TYPES.map((ruleType) => (
                    <SelectItem key={ruleType.value} value={ruleType.value}>
                      {ruleType.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                Defines the action to take when content matches this rule.
                "Ignore" will exclude matching content from indexing.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Matching Values */}
        <FormItem>
          <FormLabel>Matching Values</FormLabel>
          <FormDescription>
            Add the values that the attribute should match.
            Content with matching attribute values will have the rule applied.
          </FormDescription>
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
  );
}

