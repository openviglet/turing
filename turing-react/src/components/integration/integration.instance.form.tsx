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
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model.ts"
import { TurIntegrationInstanceService } from "@/services/integration/integration.service"
import { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { GradientButton } from "../ui/gradient-button"
const turIntegrationInstanceService = new TurIntegrationInstanceService();
interface Props {
  value: TurIntegrationInstance;
  isNew: boolean;
}

export const IntegrationInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurIntegrationInstance>();
  const { setValue } = form;
  const navigate = useNavigate()
  useEffect(() => {
    setValue("id", value.id)
    setValue("title", value.title);
    setValue("description", value.description);
    setValue("vendor", value.vendor);
    setValue("endpoint", value.endpoint);
    setValue("enabled", value.enabled);
  }, [setValue, value]);


  function onSubmit(integrationInstance: TurIntegrationInstance) {
    try {
      if (isNew) {
        turIntegrationInstanceService.create(integrationInstance);
        toast.success(`The ${integrationInstance.title} Integration Instance was saved`);
        navigate(ROUTES.INTEGRATION_INSTANCE);
      }
      else {
        turIntegrationInstanceService.update(integrationInstance);
        toast.success(`The ${integrationInstance.title} Integration Instance was updated`);
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
          name="title"
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
              <FormDescription>Integration instance title will appear on list.</FormDescription>
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
              <FormDescription>Integration instance description will appear on list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="vendor"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Vendor</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem key="AEM" value="AEM">AEM</SelectItem>
                  <SelectItem key="WEB_CRAWLER" value="WEB_CRAWLER">Web Crawler</SelectItem>
                </SelectContent>
              </Select>
              <FormDescription>Integration vendor that will be used.</FormDescription>
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
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

