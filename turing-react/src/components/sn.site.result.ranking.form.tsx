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
  Textarea
} from "@/components/ui/textarea"
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model"
import type { TurSNSite } from "@/models/sn/sn-site.model.ts"
import { TurSNSiteService } from "@/services/sn/sn.service"
import React, { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DynamicResultRankingFields } from "./dynamic-result-ranking-field"
import { Label } from "./ui/label"
import { Slider } from "./ui/slider"
const turSNSiteService = new TurSNSiteService();
interface Props {
  siteId: string
  value: TurSNRankingExpression;
  isNew: boolean;
}

export const SNSiteResultRankingForm: React.FC<Props> = ({ siteId, value, isNew }) => {
  const form = useForm<TurSNSite>();
  const { control, register, setValue } = form;
  const [slideValue, setSlideValue] = React.useState([4]);
  const urlBase = "/admin/sn/instance";
  const navigate = useNavigate()
  useEffect(() => {
    setValue("id", value.id)
    setValue("name", value.name);
    setValue("description", value.description);
  }, [setValue, value]);


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
        <FormItem>
          <FormLabel>Content that matches</FormLabel>
          <FormDescription>Create simple filter expressions to target specific content.</FormDescription>
          <FormControl>
            <DynamicResultRankingFields
              fieldName="facetLocales"
              control={control}
              register={register}
              siteId={siteId}
            />
          </FormControl>

        </FormItem>
        <div>
          <Label htmlFor="weight-slider" className="mt-6">
            Will have its weight changed by
          </Label>
          <div className="flex items-center gap-4 mt-3">

            <Slider
              id="weight-slider"
              value={slideValue}
              onValueChange={setSlideValue}
              max={10}
              min={0}
              step={1}
            />
            <span className="w-10 text-right">
              + {slideValue[0]}
            </span>

          </div>
        </div>
        <Button type="submit">Save</Button>
      </form>
    </Form>
  )
}

