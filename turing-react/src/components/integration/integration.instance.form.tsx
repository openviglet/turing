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
  const form = useForm<TurIntegrationInstance>({
    defaultValues: value
  });
  const navigate = useNavigate()
  useEffect(() => {
    form.reset(value);
  }, [value]);


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
          rules={{ required: "Title is required." }}
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
                Enter a unique, descriptive name for this integration instance. This title will be shown in the integration list and used to identify the instance.
              </FormDescription>
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
              <FormDescription>
                Provide a brief summary of the integration instance’s purpose or functionality. This helps users understand its role at a glance.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="vendor"
          rules={{ required: "Vendor is required." }}
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
              <FormDescription>
                Select the integration vendor or technology this instance connects to, such as Adobe AEM or a web crawler.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="endpoint"
          rules={{ required: "Endpoint is required." }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Endpoint</FormLabel>
              <FormControl>
                <Input
                  placeholder="URL"
                  type="url"
                  {...field} />
              </FormControl>
              <FormDescription>
                Specify the base URL or endpoint address for the integration. This is where the platform will connect to access the external system.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

