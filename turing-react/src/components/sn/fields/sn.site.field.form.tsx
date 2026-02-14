"use client"
import { ROUTES } from "@/app/routes.const"
import { SubPageHeader } from "@/components/sub.page.header"
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
import { Switch } from "@/components/ui/switch"
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurSNFieldType } from "@/models/sn/sn-field-type.model"
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model"
import { TurSNFieldService } from "@/services/sn/sn.field.service"
import { TurSNFieldTypeService } from "@/services/sn/sn.field.type.service"
import { IconReorder } from "@tabler/icons-react"
import { useEffect, useState } from "react"
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
  const form = useForm<TurSNSiteField>({
    defaultValues: snField
  });
  const { control, register } = form;
  const [snFieldTypes, setSnFieldTypes] = useState<TurSNFieldType[]>([]);
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
    form.reset(snField);
    turSNFieldTypeService.query().then(setSnFieldTypes);
  }, [snField]);


  async function onSubmit(snField: TurSNSiteField) {
    try {
      if (isNew) {

        const result = await turSNFieldService.create(snSiteId, snField);
        if (result) {
          toast.success(`The ${snField.name} SN Field was saved`);
          navigate(urlBase);
        }
        else {
          toast.error(`The ${snField.name} SN Field was not saved`);
        }
      }
      else {
        const result = await turSNFieldService.update(snSiteId, snField);
        if (result) {
          toast.success(`The ${snField.name} SN Field was updated`);
        } else {
          toast.error(`The ${snField.name} SN Field was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
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
        <SubPageHeader icon={IconReorder} name="Facet" feature="Facet" description="Filters on Search." />
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
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

