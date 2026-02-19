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
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-8 py-8 px-6"
      >
        <Accordion
          type="multiple"
          defaultValue={[
            "general",
            "wildcard",
            "facet",
            "highlighting",
            "didYouMean",
            "mlt",
            "spotlight",
            "defaultFields",
          ]}
          className="space-y-2"
        >
          {/* General */}
          <AccordionItem value="general" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">General</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="rowsPerPage"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Results per Page</FormLabel>
                    <FormDescription>
                      Set the number of search results displayed per page.
                    </FormDescription>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g. 10"
                        type="number"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="exactMatch"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Exact Match</FormLabel>
                        <FormDescription>
                          Enable precise search for quoted terms using the default field.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Wildcard */}
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
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Add Wildcard on No Results</FormLabel>
                        <FormDescription>
                          If no results are found, automatically append a wildcard to the search term.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="wildcardAlways"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Always Add Wildcard</FormLabel>
                        <FormDescription>
                          Always append a wildcard to each search term.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Facet */}
          <AccordionItem value="facet" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconLayoutListFilled />
                <span className="text-lg font-semibold">Facets</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="facet"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Enable Facets</FormLabel>
                        <FormDescription>
                          Allow filtering results by categories or attributes.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="itemsPerFacet"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Items per Facet</FormLabel>
                    <FormDescription>
                      Maximum number of items displayed per facet category.
                    </FormDescription>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g. 5"
                        type="number"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="facetSort"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Facet Sorting</FormLabel>
                        <FormDescription>
                          Choose how facet values are sorted.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select..." />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem key="COUNT" value="COUNT">
                              By Count
                            </SelectItem>
                            <SelectItem key="ALPHABETICAL" value="ALPHABETICAL">
                              Alphabetical
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </FormControl>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="facetType"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Facet Operator</FormLabel>
                        <FormDescription>
                          Logical operator (AND/OR) between different facets.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select..." />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem key="AND" value="AND">
                              AND
                            </SelectItem>
                            <SelectItem key="OR" value="OR">
                              OR
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </FormControl>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="facetItemType"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Facet Item Operator</FormLabel>
                        <FormDescription>
                          Logical operator (AND/OR) between values within the same facet.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select..." />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem key="AND" value="AND">
                              AND
                            </SelectItem>
                            <SelectItem key="OR" value="OR">
                              OR
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </FormControl>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Highlighting */}
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
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Enable Highlighting</FormLabel>
                        <FormDescription>
                          Highlight search terms in the results.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="hlPre"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Highlight Start Tag</FormLabel>
                    <FormDescription>
                      HTML/text tag inserted before the highlighted term.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. <mark>" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="hlPost"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Highlight End Tag</FormLabel>
                    <FormDescription>
                      HTML/text tag inserted after the highlighted term.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. </mark>" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Did you mean */}
          <AccordionItem value="didYouMean" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconProgressHelp />
                <span className="text-lg font-semibold">Spelling Suggestions</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="spellCheck"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Enable Suggestions</FormLabel>
                        <FormDescription>
                          Suggest spelling corrections for user queries.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="spellCheckFixes"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Always Show Corrected Term</FormLabel>
                        <FormDescription>
                          Automatically display results for the corrected term if a typo is detected.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* MLT */}
          <AccordionItem value="mlt" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <IconCopy />
                <span className="text-lg font-semibold">More Like This (MLT)</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="mlt"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Enable Similar Recommendations</FormLabel>
                        <FormDescription>
                          Recommend documents similar to the current result.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Spotlight */}
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
                    <div className="flex flex-row items-center justify-between gap-4">
                      <div className="flex flex-col flex-1">
                        <FormLabel>Show Spotlight with Results</FormLabel>
                        <FormDescription>
                          Display featured content alongside search results.
                        </FormDescription>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => {
                            field.onChange(checked ? 1 : 0);
                          }}
                        />
                      </FormControl>
                    </div>
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
          {/* Default Fields */}
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
                    <FormLabel>Exact Match Field</FormLabel>
                    <FormDescription>
                      Field used for quoted searches without a specified field.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. title" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Default Field</FormLabel>
                    <FormDescription>
                      Field used for general searches without a specified field.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. content" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultTitleField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Title Field</FormLabel>
                    <FormDescription>
                      Field used to display the document title.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. title" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultTextField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Text Field</FormLabel>
                    <FormDescription>
                      Field used for the main document content.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. body" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultDescriptionField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Description Field</FormLabel>
                    <FormDescription>
                      Field used for the document description.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. description" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultDateField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Date Field</FormLabel>
                    <FormDescription>
                      Field used for the document date.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. date" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultImageField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Image Field</FormLabel>
                    <FormDescription>
                      Field used for document images.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. image" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="defaultURLField"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>URL Field</FormLabel>
                    <FormDescription>
                      Field used for document URLs.
                    </FormDescription>
                    <FormControl>
                      <Input {...field} placeholder="e.g. url" type="text" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>
        </Accordion>
        <div className="flex justify-end gap-4 pt-8">
          <GradientButton type="button" variant="outline" onClick={() => navigate(urlBase)}>
            Cancel
          </GradientButton>
          <GradientButton type="submit">
            Save Changes
          </GradientButton>
        </div>
      </form>
    </Form>
  )
}
