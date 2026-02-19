"use client"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
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
import { Switch } from "@/components/ui/switch"
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
        <Accordion
          type="multiple"
          defaultValue={["section-language", "section-core"]}
          className="w-full space-y-4"
        >
          {/* Language Section */}
          <AccordionItem value="section-language" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">Language Settings</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="flex flex-col gap-6 pt-4">
              <FormField
                control={form.control}
                name="language"
                rules={{ required: "Language is required." }}
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-row justify-between items-center w-full">
                      <div className="flex flex-col">
                        <FormLabel>Language</FormLabel>
                        <FormDescription>
                          Select the language code for semantic search and content indexing.
                        </FormDescription>
                      </div>
                      <div className="flex-1 max-w-xs">
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger className="w-full">
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
                      </div>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </AccordionContent>
          </AccordionItem>

          {/* Core Section */}
          <AccordionItem value="section-core" className="border rounded-lg px-6">
            <AccordionTrigger className="hover:no-underline">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold">Core Configuration</span>
              </div>
            </AccordionTrigger>
            <AccordionContent className="flex flex-col gap-6 pt-4">
              {isNew && (
                <FormItem>
                  <div className="flex flex-row justify-between items-center w-full">
                    <div className="flex flex-col">
                      <FormLabel>Define Core Manually</FormLabel>
                      <FormDescription>
                        Enable this option to manually specify the core. If disabled, the core will be created automatically.
                      </FormDescription>
                    </div>
                    <Switch
                      checked={useCustomCore}
                      onCheckedChange={(value) => {
                        const nextValue = !!value;
                        setUseCustomCore(nextValue);
                        if (!nextValue) {
                          form.setValue("core", "", { shouldDirty: true });
                        }
                      }}
                    />
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
                      <FormDescription>
                        The core name will appear in the semantic navigation site locale list.
                      </FormDescription>
                      <FormControl>
                        <Input
                          {...field}
                          placeholder="Enter core name"
                          type="text"
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </AccordionContent>
          </AccordionItem>
        </Accordion>

        {/* Action Footer */}
        <div className="flex justify-end gap-4 pt-4">
          <GradientButton
            type="button"
            variant="outline"
            onClick={() => navigate(urlBase)}
          >
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

