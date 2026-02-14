"use client"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { GradientButton } from "@/components/ui/gradient-button"
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
  const [useCustomCore, setUseCustomCore] = useState(isNew && !!snLocale.core);
  const form = useForm<TurSNSiteLocale>({
    defaultValues: snLocale
  });
  const urlBase = `/admin/sn/instance/${snSiteId}/locale`;
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(snLocale);
    setUseCustomCore(isNew && !!snLocale.core);
  }, [form, isNew, snLocale]);

  useEffect(() => {
    const fetchLocales = async () => {
      const result = await new TurLocaleService().query();
      setLocales(result);
    };
    fetchLocales();
  }, []);

  async function onSubmit(snLocale: TurSNSiteLocale) {
    const payload = {
      ...snLocale,
      core: isNew && !useCustomCore ? "" : snLocale.core
    };
    try {
      if (isNew) {
        const result = await turSNSiteLocaleService.create(snSiteId, payload);
        if (result) {
          toast.success(`The ${snLocale.language} SN Locale was created`);
          navigate(urlBase);
        }
        else {
          toast.error("Failed to create the SN Locale. Please try again.");
        }
      }
      else {
        const result = await turSNSiteLocaleService.update(snSiteId, payload);
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
          rules={{ required: "Language is required." }}
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

        {isNew && (
          <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4 shadow">
            <FormControl>
              <Checkbox
                checked={useCustomCore}
                onCheckedChange={(value) => {
                  const nextValue = !!value;
                  setUseCustomCore(nextValue);
                  if (!nextValue) {
                    form.setValue("core", "", { shouldDirty: true });
                  }
                }}
              />
            </FormControl>
            <div className="space-y-1 leading-none">
              <FormLabel>Define core manually</FormLabel>
              <FormDescription>
                Enable to inform the core; otherwise it will be created automatically.
              </FormDescription>
            </div>
          </FormItem>
        )}

        {(!isNew || useCustomCore) && (
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
        )}
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

