"use client"
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
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurSNSite } from "@/models/sn/sn-site.model.ts"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
const turSNSiteService = new TurSNSiteService();
interface Props {
  value: TurSNSite;
  isNew: boolean;
}

export const SNSiteForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSNSite>({
    defaultValues: value
  });
  const urlBase = "/admin/sn/instance";
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(snSite: TurSNSite) {
    try {
      if (isNew) {
        const result = await turSNSiteService.create(snSite);
        if (result) {
          toast.success(`The ${snSite.name} SN Site was saved`);
          navigate(urlBase);
        } else {
          toast.error(`The ${snSite.name} SN Site was not saved`);
        }
      }
      else {
        const result = await turSNSiteService.update(snSite);
        if (result) {
          toast.success(`The ${snSite.name} SN Site was updated`);
        } else {
          toast.error(`The ${snSite.name} SN Site was not updated`);
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
                  placeholder="Title"
                  type="text"
                />
              </FormControl>
              <FormDescription>Name will appear on semantic navigation site list.</FormDescription>
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
              <FormDescription>Description will appear on semantic navigation site list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="turSEInstance.id"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Search Engine</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem key="SOLR" value="SOLR">Solr</SelectItem>
                  <SelectItem key="LUCENE" value="LUCENE">Lucene</SelectItem>
                </SelectContent>
              </Select>
              <FormDescription>Search engine that supports semantic navigation site.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

