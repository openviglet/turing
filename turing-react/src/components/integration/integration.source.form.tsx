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
import { useAemSourceService } from "@/contexts/TuringServiceContext"
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model"
import { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"

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
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 pr-8">
        <FormField
          control={form.control}
          name="name"
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
              <FormDescription>Integration source name will appear on list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="rootPath"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Root Path</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Root Path"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source root path will appear on list.</FormDescription>
              <FormMessage />
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
                <Input
                  {...field}
                  placeholder="Author SN Site"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source author SN Site will appear on list.</FormDescription>
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
                <Input
                  {...field}
                  placeholder="Author URL Prefix"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source author URL Prefix will appear on list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Username</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Username"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source username will appear on list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Password</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Password"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source password will appear on list.</FormDescription>
              <FormMessage />
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
                <Input
                  {...field}
                  placeholder="Publish SN Site"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source publish SN Site will appear on list.</FormDescription>
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
                <Input
                  {...field}
                  placeholder="Publish URL Prefix"
                  type="text"
                />
              </FormControl>
              <FormDescription>Integration source publish URL Prefix will appear on list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="endpoint"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Endpoint</FormLabel>
              <FormControl>
                <Input
                  placeholder="URL"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance hostname will be connected.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="contentType"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Content Type</FormLabel>
              <FormControl>
                <Input
                  placeholder="Content Type"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance content type will be connected.</FormDescription>
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
                <Input
                  placeholder="Sub Type"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance sub type will be connected.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="defaultLocale"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Default Locale</FormLabel>
              <FormControl>
                <Input
                  placeholder="Default Locale"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance default locale will be connected.</FormDescription>
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
                <Input
                  placeholder="Delta Class"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance delta class will be connected.</FormDescription>
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
                <Input
                  placeholder="Locale Class"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance locale class will be connected.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="oncePattern"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Once Pattern</FormLabel>
              <FormControl>
                <Input
                  placeholder="Once Pattern"
                  type="text"
                  {...field} />
              </FormControl>
              <FormDescription>Integration instance once pattern will be connected.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

