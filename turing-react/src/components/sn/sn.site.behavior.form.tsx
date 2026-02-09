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
import type { TurSNSite } from "@/models/sn/sn-site.model.ts"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { IconCodeAsterisk, IconCopy, IconHighlight, IconLayoutListFilled, IconListDetails, IconProgressHelp } from "@tabler/icons-react"
import { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { SubPageHeader } from "../sub.page.header"
import { Switch } from "../ui/switch"
const turSNSiteService = new TurSNSiteService();
interface Props {
  value: TurSNSite;
  isNew: boolean;
}

export const SNSiteBehaviorForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSNSite>();
  const { setValue } = form;
  const urlBase = ROUTES.SN_INSTANCE;
  const navigate = useNavigate()
  useEffect(() => {
    setValue("id", value.id)
    setValue("name", value.name)
    setValue("description", value.description);
    setValue("turSEInstance", value.turSEInstance);
    setValue("rowsPerPage", value.rowsPerPage)
    setValue("exactMatch", value.exactMatch);
    setValue("wildcardNoResults", value.wildcardNoResults);
    setValue("wildcardAlways", value.wildcardAlways);
    setValue("facet", value.facet);
    setValue("rowsPerPage", value.rowsPerPage);
    setValue("facetSort", value.facetSort);
    setValue("facetType", value.facetType);
    setValue("facetItemType", value.facetItemType);
    setValue("hl", value.hl);
    setValue("hlPre", value.hlPre);
    setValue("hlPost", value.hlPost);
    setValue("spellCheck", value.spellCheck);
    setValue("spellCheckFixes", value.spellCheckFixes);
    setValue("mlt", value.mlt);
    setValue("spotlightWithResults", value.spotlightWithResults);
    setValue("exactMatchField", value.exactMatchField);
    setValue("defaultField", value.defaultField);
    setValue("defaultTitleField", value.defaultTitleField);
    setValue("defaultTextField", value.defaultTextField);
    setValue("defaultDescriptionField", value.defaultDescriptionField);
    setValue("defaultDateField", value.defaultDateField);
    setValue("defaultImageField", value.defaultImageField);
    setValue("defaultURLField", value.defaultURLField);
  }, [setValue, value]);


  function onSubmit(snSite: TurSNSite) {
    try {
      if (isNew) {
        turSNSiteService.create(snSite);
        toast.success(`The ${snSite.name} SN Site was saved`);
        navigate(urlBase);
      }
      else {
        turSNSiteService.update(snSite);
        toast.success(`The ${snSite.name} SN Site was updated`);
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
          name="rowsPerPage"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Number de items per page</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Number de items per page"
                  type="number"
                />
              </FormControl>
              <FormDescription>Name will appear on semantic navigation site list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="exactMatch"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Exact Match</FormLabel>
              <FormDescription>Exact match when using double quotes. Will use default field to execute the query.</FormDescription>
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
        <SubPageHeader icon={IconCodeAsterisk} name="Wildcard" feature="Wildcard" description="When use wildcard on search." />
        <FormField
          control={form.control}
          name="wildcardNoResults"
          render={({ field }) => (
            <FormItem>
              <FormLabel>No results</FormLabel>
              <FormDescription>When there are no results, the wildcard will be used at the end of the search term.</FormDescription>
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
          name="wildcardAlways"
          render={({ field }) => (
            <FormItem>
              <FormLabel>In all searches</FormLabel>
              <FormDescription>The wildcard will always be used at the end of the search term.</FormDescription>
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
        <SubPageHeader icon={IconLayoutListFilled} name="Facet" feature="Facet" description="Behavior filter on search page." />
        <FormField
          control={form.control}
          name="facet"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Facet enabled</FormLabel>
              <FormDescription>Use facet in your search.</FormDescription>
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
          name="rowsPerPage"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Number of items per facet</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Number of items per facet"
                  type="number"
                />
              </FormControl>
              <FormDescription>Total of items in facet.</FormDescription>
              <FormMessage />
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
                  <SelectItem key="COUNT" value="COUNT">Count</SelectItem>
                  <SelectItem key="ALPHABETICAL" value="ALPHABETICAL">Alphabetical</SelectItem>
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
              <FormLabel>Operator between facets</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem key="AND" value="AND">And</SelectItem>
                  <SelectItem key="OR" value="OR">Or</SelectItem>
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
              <FormLabel>Operator between facets items</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem key="AND" value="AND">And</SelectItem>
                  <SelectItem key="OR" value="OR">Or</SelectItem>
                </SelectContent>
              </Select>
              <FormDescription>How Turing will join the facet item attributes, using OR or AND.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <SubPageHeader icon={IconHighlight} name="Highlighting" feature="Highlighting" description="Highlighting terms of search on results." />
        <FormField
          control={form.control}
          name="hl"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Highlighting enabled</FormLabel>
              <FormDescription>Use highlighting in your search.</FormDescription>
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
          name="hlPre"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Pre Tag</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Pre Tag"
                  type="text"
                />
              </FormControl>
              <FormDescription>Tag before text in highlighting.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="hlPost"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Post Tag</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Post Tag"
                  type="text"
                />
              </FormControl>
              <FormDescription>Tag after text in highlighting.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <SubPageHeader icon={IconProgressHelp} name="Did you mean?" feature="Did you mean?" description="Corrects the searched term if you have a spelling error. It uses the contents indexed in the Search Engine as a dictionary." />
        <FormField
          control={form.control}
          name="spellCheck"
          render={({ field }) => (
            <FormItem>
              <FormLabel>"Did you mean?" enabled</FormLabel>
              <FormDescription>Use "did you mean?" feature.</FormDescription>
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
          name="spellCheckFixes"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Always show the search with the corrected term</FormLabel>
              <FormDescription>If the term is misspelled, it already shows the search with the corrected term. If disabled, it shows the search with the entered term in the search.</FormDescription>
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
        <SubPageHeader icon={IconCopy} name="MLT" feature="MLT" description="The More Like This Query finds other items that are 'like' a given set of items of search results." />
        <FormField
          control={form.control}
          name="mlt"
          render={({ field }) => (
            <FormItem>
              <FormLabel>"More Like This" enabled</FormLabel>
              <FormDescription>Use "More Like This" feature.</FormDescription>
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
        <SubPageHeader icon={IconCopy} name="Spotlight" feature="Spotlight" description="Show spotlight content based in the term-based search." />
        <FormField
          control={form.control}
          name="spotlightWithResults"
          render={({ field }) => (
            <FormItem>
              <FormLabel>"Spotlight with search results" enabled</FormLabel>
              <FormDescription>Displays the spotlight ones along with the search using its position.</FormDescription>
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
        <SubPageHeader icon={IconListDetails} name="Default Fields" feature="Default Fields" description="What is the fields that will be used on search results display." />
        <FormField
          control={form.control}
          name="exactMatchField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Exact Match</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Exact Match"
                  type="text"
                />
              </FormControl>
              <FormDescription>Exact match field when no field was specified in the query and it uses double quote.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Default</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Default"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default field when no field was specified in the query.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultTitleField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Title</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Title"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default title field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultTextField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Text</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Text"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default text field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultDescriptionField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Description"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default description field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultDateField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Date</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Date"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default date field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultImageField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Date</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Image"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default image field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultURLField"
          render={({ field }) => (
            <FormItem>
              <FormLabel>URL</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="URL"
                  type="text"
                />
              </FormControl>
              <FormDescription>Default url field.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

