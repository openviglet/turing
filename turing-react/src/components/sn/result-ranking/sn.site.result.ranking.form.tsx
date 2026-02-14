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
import { GradientButton } from "@/components/ui/gradient-button"
import {
  Input
} from "@/components/ui/input"
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model"
import { TurSNRankingExpressionService } from "@/services/sn/sn.site.result.ranking.service"
import React, { useEffect } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Label } from "../../ui/label"
import { Slider } from "../../ui/slider"
import { DynamicResultRankingFields } from "./dynamic-result-ranking-field"
const turSNRankingExpressionService = new TurSNRankingExpressionService();
interface Props {
  snSiteId: string
  value: TurSNRankingExpression;
  isNew: boolean;
}

export const SNSiteResultRankingForm: React.FC<Props> = ({ snSiteId, value, isNew }) => {
  const form = useForm<TurSNRankingExpression>({
    defaultValues: value
  });
  const { control, register } = form;
  const [slideValue, setSlideValue] = React.useState([4]);
  const urlBase = `${ROUTES.SN_INSTANCE}/${snSiteId}/result-ranking`;;
  const navigate = useNavigate()
  useEffect(() => {
    const nextValue = isNew
      ? { ...value, weight: value.weight ?? 4 }
      : value;

    form.reset(nextValue);
    setSlideValue([nextValue.weight ?? 4]);
  }, [value, isNew]);


  async function onSubmit(snRankingExpression: TurSNRankingExpression) {
    try {
      if (isNew) {
        const result = await turSNRankingExpressionService.create(snSiteId, snRankingExpression);
        if (result) {
          toast.success(`The ${snRankingExpression.name} Ranking Expression was created`);
          navigate(urlBase);
        } else {
          toast.error("Failed to create the Ranking Expression. Please try again.");
        }
      }
      else {
        const result = await turSNRankingExpressionService.update(snSiteId, snRankingExpression);
        if (result) {
          toast.success(`The ${snRankingExpression.name} Ranking Expression was updated`);
        } else {
          toast.error("Failed to update the Ranking Expression. Please try again.");
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
              fieldName="turSNRankingConditions"
              control={control}
              register={register}
              snSiteId={snSiteId}
            />
          </FormControl>

        </FormItem>
        <div>
          <Label htmlFor="weight-slider" className="mt-6">
            Will have its weight changed by
          </Label>
          <div className="flex items-center gap-4 mt-3">
            <FormField
              control={form.control}
              name="weight"
              render={({ field }) => (
                <>
                  <Slider
                    id="weight-slider"
                    value={[field.value ?? slideValue[0]]}
                    onValueChange={(value) => {
                      setSlideValue(value);
                      field.onChange(value[0]);
                    }}
                    max={10}
                    min={0}
                    step={1}
                  />
                  <span className="w-10 text-right">
                    + {slideValue[0]}
                  </span>
                </>
              )}
            />
          </div>
        </div>
        <GradientButton type="submit">Save</GradientButton>
      </form>
    </Form>
  )
}

