"use client"
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
            </AccordionContent>
          </AccordionItem>
        </Accordion>
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

