"use client"
import { ROUTES } from "@/app/routes.const"
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
  const urlBase = `${ROUTES.SN_INSTANCE}/${snSiteId}/field`;
  const navigate = useNavigate()
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
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full gap-4">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Type</FormLabel>
                            <FormDescription>
                              Data type for this field (e.g., text, number, date).
                            </FormDescription>
                          </div>
                          <div className="w-1/2 flex justify-end">
                            <Select onValueChange={field.onChange} value={field.value}>
                              <FormControl>
                                <SelectTrigger className="w-full min-w-45">
                                  <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {snFieldTypes.map((snFieldType) => (
                                  <SelectItem key={snFieldType.id} value={snFieldType.id}>
                                    {snFieldType.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
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
                        </FormDescription>
                        <FormControl>
                          <Input {...field} placeholder="Default value" type="text" className="w-full" />
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
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Multi Value</FormLabel>
                            <FormDescription>
                              Allows this field to store multiple values (e.g., tags).
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Highlighting Switch */}
                  <FormField
                    control={form.control}
                    name="hl"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Highlighting</FormLabel>
                            <FormDescription>
                              Enables highlighting of matching search terms in results.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* MLT Switch */}
                  <FormField
                    control={form.control}
                    name="mlt"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>MLT</FormLabel>
                            <FormDescription>
                              Activates “More Like This” functionality.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Enabled Switch */}
                  <FormField
                    control={form.control}
                    name="enabled"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Enabled</FormLabel>
                            <FormDescription>
                              If enabled, this field will be included in search queries and indexing.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Required Switch */}
                  <FormField
                    control={form.control}
                    name="required"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Required</FormLabel>
                            <FormDescription>
                              Marks this field as mandatory during indexing.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
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
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Facet</FormLabel>
                            <FormDescription>
                              Enables this field as a filter in the search page.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={field.value === 1}
                              onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Secondary Facet Switch */}
                  <FormField
                    control={form.control}
                    name="secondaryFacet"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Secondary Facet</FormLabel>
                            <FormDescription>
                              Classifies this field as a secondary facet.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={!!field.value}
                              onCheckedChange={(checked) => field.onChange(checked)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Show All Facet Items Switch */}
                  <FormField
                    control={form.control}
                    name="showAllFacetItems"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Show All Facet Items</FormLabel>
                            <FormDescription>
                              Displays all possible facet values, even those with zero results.
                            </FormDescription>
                          </div>
                          <div className="flex justify-end w-1/2">
                            <Switch
                              checked={!!field.value}
                              onCheckedChange={(checked) => field.onChange(checked)}
                            />
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Facet Sort Select */}
                  <FormField
                    control={form.control}
                    name="facetSort"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full gap-4">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Facet Sort</FormLabel>
                            <FormDescription>
                              Determines how facet values are ordered in the filter box.
                            </FormDescription>
                          </div>
                          <div className="w-1/2 flex justify-end">
                            <Select onValueChange={field.onChange} value={field.value}>
                              <FormControl>
                                <SelectTrigger className="w-full min-w-45">
                                  <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {facetSorts.map((facetSort) => (
                                  <SelectItem key={facetSort.value} value={facetSort.value}>
                                    {facetSort.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Facet Type Select */}
                  <FormField
                    control={form.control}
                    name="facetType"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full gap-4">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Operator between Facets</FormLabel>
                            <FormDescription>
                              Specifies how multiple facets are combined in search.
                            </FormDescription>
                          </div>
                          <div className="w-1/2 flex justify-end">
                            <Select onValueChange={field.onChange} value={field.value}>
                              <FormControl>
                                <SelectTrigger className="w-full min-w-45">
                                  <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {facetTypes.map((facetType) => (
                                  <SelectItem key={facetType.value} value={facetType.value}>
                                    {facetType.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Facet Item Type Select */}
                  <FormField
                    control={form.control}
                    name="facetItemType"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full gap-4">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Operator between Facet Items</FormLabel>
                            <FormDescription>
                              Defines how multiple selected facet values are joined.
                            </FormDescription>
                          </div>
                          <div className="w-1/2 flex justify-end">
                            <Select onValueChange={field.onChange} value={field.value}>
                              <FormControl>
                                <SelectTrigger className="w-full min-w-45">
                                  <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {facetTypes.map((facetType) => (
                                  <SelectItem key={facetType.value} value={facetType.value}>
                                    {facetType.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {/* Facet Range Select */}
                  <FormField
                    control={form.control}
                    name="facetRange"
                    render={({ field }) => (
                      <FormItem className="w-full">
                        <div className="flex flex-row items-center justify-between w-full gap-4">
                          <div className="flex flex-col w-1/2">
                            <FormLabel>Range</FormLabel>
                            <FormDescription>
                              Enables range-based facet filtering.
                            </FormDescription>
                          </div>
                          <div className="w-1/2 flex justify-end">
                            <Select onValueChange={field.onChange} value={field.value}>
                              <FormControl>
                                <SelectTrigger className="w-full min-w-45">
                                  <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {facetRanges.map((facetRange) => (
                                  <SelectItem key={facetRange.value} value={facetRange.value}>
                                    {facetRange.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                        <FormMessage />
                      </FormItem>
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

