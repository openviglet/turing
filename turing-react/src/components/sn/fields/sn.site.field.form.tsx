"use client"
import { ROUTES } from "@/app/routes.const"
import { BadgeFieldType } from "@/components/badge-field-type"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { FormItemTwoColumns } from "@/components/ui/form-item-two-columns"
import { GradientButton } from "@/components/ui/gradient-button"
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
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import {
  Textarea
} from "@/components/ui/textarea"
import { useGlobalDecimalSeparator } from "@/hooks/use-global-decimal-separator"
import type { TurSNFieldType } from "@/models/sn/sn-field-type.model"
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model"
import { TurSNFieldService } from "@/services/sn/sn.field.service"
import { TurSNFieldTypeService } from "@/services/sn/sn.field.type.service"
import { IconReorder } from "@tabler/icons-react"
import axios from "axios"
import { useEffect, useMemo, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DynamicLanguageFields } from "./dynamic-language-field"
const turSNFieldService = new TurSNFieldService();
const turSNFieldTypeService = new TurSNFieldTypeService();
interface Props {
  snSiteId: string;
  snField: TurSNSiteField;
  isNew: boolean;
}

export const SNSiteFieldForm: React.FC<Props> = ({ snSiteId, snField, isNew }) => {
  const normalizedField = useMemo(() => ({
    ...snField,
    name: snField.name ?? "",
    description: snField.description ?? "",
    defaultValue: snField.defaultValue ?? "",
    facetName: snField.facetName ?? "",
    multiValued: snField.multiValued ?? 0,
    hl: snField.hl ?? 0,
    mlt: snField.mlt ?? 0,
    enabled: snField.enabled ?? 0,
    required: snField.required ?? 0,
    facet: snField.facet ?? 0,
    secondaryFacet: snField.secondaryFacet ?? 0,
    showAllFacetItems: snField.showAllFacetItems ?? 0,
    facetSort: snField.facetSort ?? "DEFAULT",
    facetType: snField.facetType ?? "DEFAULT",
    facetItemType: snField.facetItemType ?? "DEFAULT",
    facetRange: snField.facetRange ?? "DISABLED"
  } as TurSNSiteField), [snField]);

  const form = useForm<TurSNSiteField>({
    defaultValues: normalizedField
  });
  const { control, register } = form;
  const [snFieldTypes, setSnFieldTypes] = useState<TurSNFieldType[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const {
    decimalSymbol,
    normalizeCurrencyString,
    normalizeDecimalString,
  } = useGlobalDecimalSeparator();
  const urlBase = `${ROUTES.SN_INSTANCE}/${snSiteId}/field`;
  const navigate = useNavigate()
  const selectedFieldType = form.watch("type");
  const isDecimalType = selectedFieldType === "FLOAT" || selectedFieldType === "DOUBLE";
  const isCurrencyType = selectedFieldType === "CURRENCY";

  const defaultValuePlaceholder = (() => {
    if (isCurrencyType) {
      return `e.g. 150${decimalSymbol}75,BRL`;
    }
    if (isDecimalType) {
      return `e.g. 150${decimalSymbol}75`;
    }
    return "Default value";
  })();
  const facetRanges = [
    { value: "DISABLED", name: "Disabled" },
    { value: "DAY", name: "Day" },
    { value: "MONTH", name: "Month" },
    { value: "YEAR", name: "Year" }
  ];
  const facetTypes = [
    { value: "DEFAULT", name: "Default" },
    { value: "AND", name: "And" },
    { value: "OR", name: "Or" }
  ];
  const facetSorts = [
    { value: "DEFAULT", name: "Default" },
    { value: "ALPHABETICAL", name: "Alphabetical" },
    { value: "COUNT", name: "Count" }
  ];
  useEffect(() => {
    turSNFieldTypeService.query().then((types) => {
      setSnFieldTypes(types);
      setIsLoading(false);
    });
  }, []);

  useEffect(() => {
    form.reset(normalizedField);
  }, [normalizedField, form]);


  async function onSubmit(snField: TurSNSiteField) {
    try {
      // Normalize all fields: ensure strings are empty strings instead of null/undefined
      const normalizedData: TurSNSiteField = {
        ...snField,
        name: snField.name ?? "",
        description: snField.description ?? "",
        defaultValue: snField.defaultValue ?? "",
        facetName: snField.facetName ?? "",
        multiValued: snField.multiValued ?? 0,
        hl: snField.hl ?? 0,
        mlt: snField.mlt ?? 0,
        enabled: snField.enabled ?? 0,
        required: snField.required ?? 0,
        facet: snField.facet ?? 0,
      };

      if (isNew) {

        const result = await turSNFieldService.create(snSiteId, normalizedData);
        if (result) {
          toast.success(`The ${normalizedData.name} SN Field was saved`);
          navigate(urlBase);
        }
        else {
          toast.error(`The ${normalizedData.name} SN Field was not saved`);
        }
      }
      else {
        const result = await turSNFieldService.update(snSiteId, normalizedData);
        if (result) {
          toast.success(`The ${normalizedData.name} SN Field was updated`);
        } else {
          toast.error(`The ${normalizedData.name} SN Field was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      if (axios.isAxiosError(error) && error.response?.status === 409) {
        toast.error("A field with this name already exists.");
        return;
      }
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  return (
    <>
      {isLoading ? (
        <div className="space-y-8 py-8 px-6">
          <div className="space-y-3">
            <Skeleton className="h-6 w-20" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-4 w-1/3" />
          </div>
          <div className="space-y-3">
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-4 w-1/2" />
          </div>
          <div className="space-y-3">
            <Skeleton className="h-6 w-16" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-4 w-1/3" />
          </div>
          <div className="flex gap-4">
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-6 w-10" />
          </div>
          <Skeleton className="h-10 w-20" />
        </div>
      ) : (
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-10 py-10 px-8">
            <Accordion
              type="multiple"
              defaultValue={["general", "facet"]}
              className="w-full space-y-4"
            >
              {/* General Configuration */}
              <AccordionItem value="general" className="border rounded-lg px-6">
                <AccordionTrigger className="hover:no-underline">
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-semibold">General Configuration</span>
                  </div>
                </AccordionTrigger>
                <AccordionContent className="flex flex-col gap-6 pt-4">
                  {/* Name */}
                  <FormField
                    control={form.control}
                    name="name"
                    rules={{ required: "Name is required." }}
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <FormLabel>Name</FormLabel>
                        <FormDescription>
                          Unique identifier for this field. Used for search and indexing.
                        </FormDescription>
                        <FormControl>
                          <Input {...field} placeholder="Name" type="text" className="w-full" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Type */}
                  <FormField
                    control={form.control}
                    name="type"
                    rules={{ required: "Type is required." }}
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Type</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Data type for this field (e.g., text, number, date).
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <FormControl>
                            <Select onValueChange={field.onChange} value={field.value}>
                              <SelectTrigger className="w-full min-w-45">
                                <SelectValue placeholder="Choose..." />
                              </SelectTrigger>
                              <SelectContent>
                                {snFieldTypes.map((snFieldType) => (
                                  <SelectItem key={snFieldType.id} value={snFieldType.id}>
                                    <div className="flex w-full items-center justify-between gap-3">
                                      <BadgeFieldType type={snFieldType.id} variation="short" />
                                      <span>{snFieldType.name}</span>
                                    </div>
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </FormControl>
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Description */}
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <FormLabel>Description</FormLabel>
                        <FormDescription>
                          Brief explanation of the field’s purpose or usage.
                        </FormDescription>
                        <FormControl>
                          <Textarea placeholder="Description" className="resize-none w-full" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Default Value */}
                  <FormField
                    control={form.control}
                    name="defaultValue"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <FormLabel>Default Value</FormLabel>
                        <FormDescription>
                          Value automatically assigned if no value is provided during indexing.
                          {isDecimalType
                            ? ` Use ${decimalSymbol} as decimal separator (example: 150${decimalSymbol}75).`
                            : null}
                          {isCurrencyType
                            ? ` Currency format: amount,ISO-4217 (example: 150${decimalSymbol}75,BRL).`
                            : null}
                        </FormDescription>
                        <FormControl>
                          <Input
                            {...field}
                            placeholder={defaultValuePlaceholder}
                            type="text"
                            className="w-full"
                            onBlur={(event) => {
                              const rawValue = event.target.value ?? "";

                              if (isDecimalType) {
                                field.onChange(normalizeDecimalString(rawValue));
                                return;
                              }

                              if (isCurrencyType) {
                                field.onChange(normalizeCurrencyString(rawValue));
                              }
                            }}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Multi Value Switch */}
                  <FormField
                    control={form.control}
                    name="multiValued"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Multi Value</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Allows this field to store multiple values (e.g., tags).
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Highlighting Switch */}
                  <FormField
                    control={form.control}
                    name="hl"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Highlighting</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Enables highlighting of matching search terms in results.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* MLT Switch */}
                  <FormField
                    control={form.control}
                    name="mlt"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>MLT</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Activates “More Like This” functionality.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Enabled Switch */}
                  <FormField
                    control={form.control}
                    name="enabled"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Enabled</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            If enabled, this field will be included in search queries and indexing.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Required Switch */}
                  <FormField
                    control={form.control}
                    name="required"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Required</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Marks this field as mandatory during indexing.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                </AccordionContent>
              </AccordionItem>
              {/* Facet Configuration */}
              <AccordionItem value="facet" className="border rounded-lg px-6">
                <AccordionTrigger className="hover:no-underline">
                  <div className="flex items-center gap-2">
                    <IconReorder />
                    <span className="text-lg font-semibold">Facet</span>
                  </div>
                </AccordionTrigger>
                <AccordionContent className="flex flex-col gap-6 pt-4">
                  {/* Facet Switch */}
                  <FormField
                    control={form.control}
                    name="facet"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Facet</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Enables this field as a filter in the search page.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={field.value === 1}
                            onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Secondary Facet Switch */}
                  <FormField
                    control={form.control}
                    name="secondaryFacet"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Secondary Facet</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Classifies this field as a secondary facet.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={!!field.value}
                            onCheckedChange={(checked) => field.onChange(checked)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Show All Facet Items Switch */}
                  <FormField
                    control={form.control}
                    name="showAllFacetItems"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Show All Facet Items</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Displays all possible facet values, even those with zero results.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <Switch
                            checked={!!field.value}
                            onCheckedChange={(checked) => field.onChange(checked)}
                          />
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Facet Sort Select */}
                  <FormField
                    control={form.control}
                    name="facetSort"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Facet Sort</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Determines how facet values are ordered in the filter box.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <FormControl>
                            <Select onValueChange={field.onChange} value={field.value}>
                              <SelectTrigger className="w-full min-w-45">
                                <SelectValue placeholder="Choose..." />
                              </SelectTrigger>
                              <SelectContent>
                                {facetSorts.map((facetSort) => (
                                  <SelectItem key={facetSort.value} value={facetSort.value}>
                                    {facetSort.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </FormControl>
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Facet Type Select */}
                  <FormField
                    control={form.control}
                    name="facetType"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Operator between Facets</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Specifies how multiple facets are combined in search.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <FormControl>
                            <Select onValueChange={field.onChange} value={field.value}>
                              <SelectTrigger className="w-full min-w-45">
                                <SelectValue placeholder="Choose..." />
                              </SelectTrigger>
                              <SelectContent>
                                {facetTypes.map((facetType) => (
                                  <SelectItem key={facetType.value} value={facetType.value}>
                                    {facetType.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </FormControl>
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Facet Item Type Select */}
                  <FormField
                    control={form.control}
                    name="facetItemType"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Operator between Facet Items</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Defines how multiple selected facet values are joined.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <FormControl>
                            <Select onValueChange={field.onChange} value={field.value}>
                              <SelectTrigger className="w-full min-w-45">
                                <SelectValue placeholder="Choose..." />
                              </SelectTrigger>
                              <SelectContent>
                                {facetTypes.map((facetType) => (
                                  <SelectItem key={facetType.value} value={facetType.value}>
                                    {facetType.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </FormControl>
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Facet Range Select */}
                  <FormField
                    control={form.control}
                    name="facetRange"
                    render={({ field }) => (
                      <FormItemTwoColumns className="w-full">
                        <FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Label>Range</FormItemTwoColumns.Label>
                          <FormItemTwoColumns.Description>
                            Enables range-based facet filtering.
                          </FormItemTwoColumns.Description>
                        </FormItemTwoColumns.Left>
                        <FormItemTwoColumns.Right>
                          <FormControl>
                            <Select onValueChange={field.onChange} value={field.value}>
                              <SelectTrigger className="w-full min-w-45">
                                <SelectValue placeholder="Choose..." />
                              </SelectTrigger>
                              <SelectContent>
                                {facetRanges.map((facetRange) => (
                                  <SelectItem key={facetRange.value} value={facetRange.value}>
                                    {facetRange.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </FormControl>
                        </FormItemTwoColumns.Right>
                        <FormMessage />
                      </FormItemTwoColumns>
                    )}
                  />
                  {/* Facet Name */}
                  <FormField
                    control={form.control}
                    name="facetName"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <FormLabel>Default Name</FormLabel>
                        <FormDescription>
                          Display name for this field in the filter box.
                        </FormDescription>
                        <FormControl>
                          <Input {...field} placeholder="Display name" type="text" className="w-full" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Facet Multi Languages */}
                  <FormItem className="w-full">
                    <FormLabel>Facet Multi Languages</FormLabel>
                    <FormDescription>
                      Configure localized names for this facet to support multi-language filter labels.
                    </FormDescription>
                    <FormControl>
                      <DynamicLanguageFields
                        fieldName="facetLocales"
                        control={control}
                        register={register}
                      />
                    </FormControl>
                  </FormItem>
                </AccordionContent>
              </AccordionItem>
            </Accordion>
            {/* Action Footer */}
            <div className="flex justify-end gap-4 pt-6">
              <GradientButton
                type="button"
                variant="outline"
                className="w-full md:w-auto"
                onClick={() => navigate(urlBase)}
              >
                Cancel
              </GradientButton>
              <GradientButton
                type="submit"
                disabled={isLoading}
                className="w-full md:w-auto"
              >
                Save Changes
              </GradientButton>
            </div>
          </form>
        </Form>
      )}
    </>
  )
}

