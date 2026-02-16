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
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
            <Accordion type="multiple" defaultValue={["general", "facet"]} className="space-y-2">
              <AccordionItem value="general" className="border rounded-lg px-6">
                <AccordionTrigger className="hover:no-underline">
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-semibold">General Configuration</span>
                  </div>
                </AccordionTrigger>
                <AccordionContent className="space-y-6 pt-4">

                  <FormField
                    control={form.control}
                    name="name"
                    rules={{ required: "Name is required." }}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Name</FormLabel>
                        <FormControl>
                          <Input
                            {...field}
                            placeholder="Name"
                            type="text"
                          />
                        </FormControl>
                        <FormDescription>Name will appear on semantic navigation site field list.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Description</FormLabel>
                        <FormControl>
                          <Textarea
                            placeholder="Description"
                            className="resize-none"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>Description will appear on semantic navigation site field list.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="type"
                    rules={{ required: "Type is required." }}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Type</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
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
                        <FormDescription>Type of Field that is used in Search Engine.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="multiValued"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Multi Value</FormLabel>
                        <FormDescription>If the field allows a list of items.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="hl"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Highlighting</FormLabel>
                        <FormDescription>Enable highlighting terms of search on results.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="mlt"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>MLT</FormLabel>
                        <FormDescription>Enable "more like this" into search on results.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="enabled"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Enabled</FormLabel>
                        <FormDescription>Enable this field during search and indexing.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="required"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Required</FormLabel>
                        <FormDescription>If this field is required during indexing.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="defaultValue"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Default Value</FormLabel>
                        <FormControl>
                          <Input
                            {...field}
                            placeholder="Title"
                            type="text"
                          />
                        </FormControl>
                        <FormDescription>Default value used of this field during indexing is empty or null.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </AccordionContent>
              </AccordionItem>
              <AccordionItem value="facet" className="border rounded-lg px-6">
                <AccordionTrigger className="hover:no-underline">
                  <div className="flex items-center gap-2">
                    <IconReorder />
                    <span className="text-lg font-semibold">Facet</span>
                  </div>
                </AccordionTrigger>
                <AccordionContent className="space-y-6 pt-4">
                  <FormField
                    control={form.control}
                    name="facet"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Facet</FormLabel>
                        <FormDescription>Enable filter into search page.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value === 1}
                            onCheckedChange={(checked) => {
                              field.onChange(checked ? 1 : 0);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="secondaryFacet"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Secondary facet</FormLabel>
                        <FormDescription>It will be classified as a secondary facet. It can be used separately from the main facets.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value}
                            onCheckedChange={(checked) => {
                              field.onChange(checked);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="showAllFacetItems"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Show all facet items</FormLabel>
                        <FormDescription>Shows all facet items, even if there are no items.</FormDescription>
                        <FormControl>
                          <Switch checked={field.value}
                            onCheckedChange={(checked) => {
                              field.onChange(checked);
                            }}
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="facetSort"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Facet Sort</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
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
                        <FormDescription>How Turing will sort the facet.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="facetType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Operator between Facets</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
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
                        <FormDescription>How Turing will join the facet attributes, using OR or AND.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="facetItemType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Operator between Facet Items</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
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
                        <FormDescription>How Turing will join the facet item attributes, using OR or AND.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="facetRange"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Range</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
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
                        <FormDescription>Facet Range Gap.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="facetName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Default Name</FormLabel>
                        <FormControl>
                          <Input
                            {...field}
                            placeholder="Title"
                            type="text"
                          />
                        </FormControl>
                        <FormDescription>Default Name of this field into filter box.</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormItem>
                    <FormLabel>Facet Multi Languages</FormLabel>
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
            <GradientButton type="submit" disabled={isLoading}>Save</GradientButton>
          </form>
        </Form>
      )}
    </>
  )
}

