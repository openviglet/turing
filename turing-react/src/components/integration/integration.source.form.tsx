"use client"
import { ROUTES } from "@/app/routes.const"
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"
import {
  Button
} from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
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
  Switch
} from "@/components/ui/switch"
import { useAemSourceService } from "@/contexts/TuringServiceContext"
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model"
import { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DynamicSourceLocales } from "./dynamic.source.locale"

interface Props {
  value: TurIntegrationAemSource;
  isNew: boolean;
  integrationId: string;
}

export const IntegrationSourceForm: React.FC<Props> = ({ value, isNew, integrationId }) => {
  const turIntegrationAemSourceService = useAemSourceService(integrationId);
  const form = useForm<TurIntegrationAemSource>({
    defaultValues: value
  });
  const { control, register } = form;
  const navigate = useNavigate()
  useEffect(() => {
    form.reset(value);
  }, [form, value]);


  async function onSubmit(integrationAemSource: TurIntegrationAemSource) {
    try {
      if (isNew) {
        const result = await turIntegrationAemSourceService.create(integrationAemSource);
        if (result) {
          toast.success(`The ${integrationAemSource.name} Integration Source was saved`);
          navigate(ROUTES.INTEGRATION_INSTANCE);
        }
        else {
          toast.error("Failed to create the integration source. Please try again.");
        }
      }
      else {
        const result = await turIntegrationAemSourceService.update(integrationAemSource);
        if (result) {
          toast.success(`The ${integrationAemSource.name} Integration Source was updated`);
        } else {
          toast.error("Failed to update the integration source. Please try again.");
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-8 pr-8">

        {/* Configurações Gerais */}
        <Card>
          <CardHeader>
            <CardTitle>General Configuration</CardTitle>
            <CardDescription>Basic information and connection settings for the AEM integration source</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Name *</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder="My AEM Source" type="text" />
                  </FormControl>
                  <FormDescription>Unique identifier for this integration source</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="endpoint"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Endpoint *</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder="https://aem.example.com" type="url" />
                  </FormControl>
                  <FormDescription>AEM instance URL to connect</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Username *</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="admin" type="text" />
                    </FormControl>
                    <FormDescription>AEM authentication username</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Password *</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="••••••••" type="password" />
                    </FormControl>
                    <FormDescription>AEM authentication password</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="rootPath"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Root Path *</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder="/content/mysite" type="text" />
                  </FormControl>
                  <FormDescription>Root JCR path to start content extraction</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="contentType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Content Type</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="cq:Page" type="text" />
                    </FormControl>
                    <FormDescription>JCR content type to index</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="subType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Sub Type</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="cq:PageContent" type="text" />
                    </FormControl>
                    <FormDescription>JCR sub-type for content nodes</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="oncePattern"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Once Pattern</FormLabel>
                  <FormControl>
                    <Input {...field} placeholder=".*" type="text" />
                  </FormControl>
                  <FormDescription>Regex pattern for one-time indexing filter</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        {/* Accordion para Author, Publish e Locales */}
        <Accordion type="multiple" defaultValue={["author", "publish", "locales"]} className="space-y-2">

          {/* Author Configuration */}
          <AccordionItem value="author" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">Author Configuration</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="author"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                      <FormLabel className="text-base">Enable Author</FormLabel>
                      <FormDescription>
                        Index content from AEM Author instance
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="authorSNSite"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Author SN Site</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="author-site" type="text" />
                    </FormControl>
                    <FormDescription>Semantic Navigation site ID for author content</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="authorURLPrefix"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Author URL Prefix</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="https://author.example.com" type="url" />
                    </FormControl>
                    <FormDescription>URL prefix for author content links</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>

          {/* Publish Configuration */}
          <AccordionItem value="publish" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">Publish Configuration</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="publish"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                      <FormLabel className="text-base">Enable Publish</FormLabel>
                      <FormDescription>
                        Index content from AEM Publish instance
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="publishSNSite"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Publish SN Site</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="publish-site" type="text" />
                    </FormControl>
                    <FormDescription>Semantic Navigation site ID for published content</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="publishURLPrefix"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Publish URL Prefix</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="https://www.example.com" type="url" />
                    </FormControl>
                    <FormDescription>URL prefix for published content links</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>

          {/* Locales Configuration */}
          <AccordionItem value="locales" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">Locales Configuration</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="space-y-6 pt-4">
              <FormField
                control={form.control}
                name="defaultLocale"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Default Locale</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="en_US" type="text" />
                    </FormControl>
                    <FormDescription>Default locale for content without explicit locale</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="localeClass"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Locale Class</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="com.example.LocaleProvider" type="text" />
                    </FormControl>
                    <FormDescription>Java class for custom locale detection logic</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="deltaClass"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Delta Class</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="com.example.DeltaProvider" type="text" />
                    </FormControl>
                    <FormDescription>Java class for incremental indexing logic</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="rounded-lg border p-4 bg-muted/50">
                <h4 className="font-medium mb-2">Locale Paths</h4>
                <p className="text-sm text-muted-foreground mb-4">
                  Define specific paths for different locales.
                </p>
                <DynamicSourceLocales
                  fieldName="localePaths"
                  control={control}
                  register={register}
                />
              </div>
            </AccordionContent>
          </AccordionItem>
        </Accordion>

        <div className="flex justify-end gap-4 pt-6">
          <Button type="button" variant="outline" onClick={() => navigate(ROUTES.INTEGRATION_INSTANCE)}>
            Cancel
          </Button>
          <Button type="submit">
            {isNew ? 'Create Source' : 'Save Changes'}
          </Button>
        </div>
      </form>
    </Form>
  )
}

