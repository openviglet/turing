import { ROUTES } from "@/app/routes.const"
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
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../ui/accordion"
import { GradientButton } from "../ui/gradient-button"
import { Switch } from "../ui/switch"

const turSNSiteService = new TurSNSiteService();
interface Props {
  value: TurSNSite;
  isNew: boolean;
}

export const SNSiteBehaviorForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSNSite>({
    defaultValues: value
  });
  const urlBase = ROUTES.SN_INSTANCE;
  const navigate = useNavigate()
  useEffect(() => {
    form.reset(value);
  }, [value]);

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
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
        <Accordion type="multiple" defaultValue={["general", "wildcard", "facet", "highlighting", "didYouMean", "mlt", "spotlight", "defaultFields"]} className="space-y-2">
          <AccordionItem value="general" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">General Configuration</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="rowsPerPage"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Number of items per page</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="Number of items per page"
                        type="number"
                      />
                    </FormControl>
                    <FormDescription>
                      Sets the maximum number of search results displayed per page.
                    </FormDescription>
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
                    <FormDescription>
                      Enables strict matching for queries wrapped in double quotes. Uses the default field for precise searches.
                    </FormDescription>
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
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="wildcard" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconCodeAsterisk />
                <span className="text-lg font-semibold">Wildcard</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="wildcardNoResults"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>No results</FormLabel>
                    <FormDescription>
                      If a search returns no results, automatically append a wildcard to the end of the search term to broaden the query.
                    </FormDescription>
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
                    <FormDescription>
                      Always append a wildcard to the end of every search term, increasing the flexibility of search results.
                    </FormDescription>
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
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="facet" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconLayoutListFilled />
                <span className="text-lg font-semibold">Facet</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="facet"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Facet enabled</FormLabel>
                    <FormDescription>
                      Activates faceted search, allowing users to filter results by categories or attributes.
                    </FormDescription>
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
                name="itemsPerFacet"
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
                    <FormDescription>
                      Specifies how many items are shown for each facet category.
                    </FormDescription>
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
                    <FormDescription>
                      Determines how facet values are ordered: by count or alphabetically.
                    </FormDescription>
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
                    <FormDescription>
                      Sets the logical operator (AND/OR) used to combine multiple facet filters.
                    </FormDescription>
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
                    <FormDescription>
                      Sets the logical operator (AND/OR) for combining multiple values within a single facet.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="highlighting" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconHighlight />
                <span className="text-lg font-semibold">Highlighting</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="hl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Highlighting enabled</FormLabel>
                    <FormDescription>
                      Highlights matching terms in search results for better visibility.
                    </FormDescription>
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
                    <FormDescription>
                      HTML or text tag inserted before highlighted terms in results.
                    </FormDescription>
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
                    <FormDescription>
                      HTML or text tag inserted after highlighted terms in results.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="didYouMean" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconProgressHelp />
                <span className="text-lg font-semibold">Did you mean?</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="spellCheck"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>"Did you mean?" enabled</FormLabel>
                    <FormDescription>
                      Activates spelling correction suggestions for user queries.
                    </FormDescription>
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
                    <FormDescription>
                      If enabled, automatically displays results for the corrected term when a misspelling is detected. If disabled, shows results for the original term.
                    </FormDescription>
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
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="mlt" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconCopy />
                <span className="text-lg font-semibold">MLT</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="mlt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>"More Like This" enabled</FormLabel>
                    <FormDescription>
                      Enables recommendations for documents similar to the current search result.
                    </FormDescription>
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
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="spotlight" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconCopy />
                <span className="text-lg font-semibold">Spotlight</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="spotlightWithResults"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>"Spotlight with search results" enabled</FormLabel>
                    <FormDescription>
                      Displays spotlighted content alongside search results, using its configured position.
                    </FormDescription>
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
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="defaultFields" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconListDetails />
                <span className="text-lg font-semibold">Default Fields</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
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
                    <FormDescription>
                      Field used for exact match queries when no field is specified and double quotes are used.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for general queries when no field is specified.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for document titles in search results.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for main document content in search results.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for document descriptions in search results.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for document dates in search results.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultImageField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Image</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="Image"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>
                      Field used for document images in search results.
                    </FormDescription>
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
                    <FormDescription>
                      Field used for document URLs in search results.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
        </Accordion>
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}
