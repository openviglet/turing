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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { TurLocale } from "@/models/locale/locale.model"
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model"
import { TurLocaleService } from "@/services/locale/locale.service"
import { TurSNSiteLocaleService } from "@/services/sn/sn.site.locale.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"


interface Props {
  snSiteId: string;
  snLocale: TurSNSiteLocale;
  isNew: boolean;
}
const turSNSiteLocaleService = new TurSNSiteLocaleService();

export const SNSiteLocaleForm: React.FC<Props> = ({ snSiteId, snLocale, isNew }) => {
  const [locales, setLocales] = useState<TurLocale[]>([]);
  const form = useForm<TurSNSiteLocale>({
    defaultValues: snLocale
  });
  const urlBase = `/admin/sn/instance/${snSiteId}/locale`;
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(snLocale);
  }, [form, snLocale]);

  useEffect(() => {
    const fetchLocales = async () => {
      const result = await new TurLocaleService().query();
      setLocales(result);
    };
    fetchLocales();
  }, []);

  async function onSubmit(snLocale: TurSNSiteLocale) {
    try {
      if (isNew) {
        const result = await turSNSiteLocaleService.create(snSiteId, snLocale);
        if (result) {
          toast.success(`The ${snLocale.language} SN Locale was created`);
          navigate(urlBase);
        }
        else {
          toast.error("Failed to create the SN Locale. Please try again.");
        }
      }
      else {
        const result = await turSNSiteLocaleService.update(snSiteId, snLocale);
        if (result) {
          toast.success(`The ${snLocale.language} SN Locale was updated`);
        }
        else {
          toast.error("Failed to update the SN Locale. Please try again.");
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
          name="language"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Language</FormLabel>
              <Select onValueChange={field.onChange} value={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Choose..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {locales.map((locale) => (
                    <SelectItem key={locale.initials} value={locale.initials}>
                      {locale.en} ({locale.initials})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                Select the language code used for semantic search and content indexing.
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="core"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Core</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Core"
                  type="text"
                />
              </FormControl>
              <FormDescription>Core will appear on semantic navigation site locale list.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

