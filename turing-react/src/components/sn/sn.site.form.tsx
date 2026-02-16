"use client"
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
import type { TurSEInstance } from "@/models/se/se-instance.model"
import type { TurSNSite } from "@/models/sn/sn-site.model.ts"
import { TurSEInstanceService } from "@/services/se/se.service"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { GradientButton } from "../ui/gradient-button"
import { Skeleton } from "../ui/skeleton"
const turSNSiteService = new TurSNSiteService();
const turSEInstanceService = new TurSEInstanceService();
interface Props {
  value: TurSNSite;
  isNew: boolean;
}

export const SNSiteForm: React.FC<Props> = ({ value, isNew }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [seInstances, setSeInstances] = useState<TurSEInstance[]>([]);
  const form = useForm<TurSNSite>({
    defaultValues: value
  });
  const urlBase = "/admin/sn/instance";
  const navigate = useNavigate()

  useEffect(() => {
    turSEInstanceService.query().then(setSeInstances);
    form.reset(value);
    setIsLoading(false);
  }, [value])

  async function onSubmit(snSite: TurSNSite) {
    setIsLoading(true);
    try {
      if (isNew) {
        const result = await turSNSiteService.create(snSite);
        setIsLoading(false)
        if (result) {
          toast.success(`The ${snSite.name} SN Site was saved`);
          navigate(urlBase);
        } else {
          toast.error(`The ${snSite.name} SN Site was not saved`);
        }
      }
      else {
        const result = await turSNSiteService.update(snSite);
        setIsLoading(false)
        if (result) {
          toast.success(`The ${snSite.name} SN Site was updated`);
        } else {
          toast.error(`The ${snSite.name} SN Site was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
      setIsLoading(false);
    }
  }
  return (
    <>
      {
        isLoading ? (
          <div className="flex w-full max-w-xs flex-col gap-7 px-6">
            <div className="flex flex-col gap-3">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-8 w-full" />
            </div>
            <div className="flex flex-col gap-3">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-full" />
            </div>
            <Skeleton className="h-8 w-24" />
          </div>
        ) : (
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
              <FormField
                control={form.control}
                name="name"
                rules={{
                  required: "Name is required.",
                  pattern: {
                    value: /^[a-zA-Z0-9_-]+$/,
                    message: "Name can only contain letters, numbers, underscores, and hyphens."
                  }
                }}
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
                rules={{ required: "Search engine instance is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Search Engine Instance</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Choose..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {seInstances.map((seInstance) => (
                          <SelectItem key={seInstance.id} value={seInstance.id}>{seInstance.title}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormDescription>Search engine instance that supports semantic navigation site.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <GradientButton type="submit">Save</GradientButton>
            </form>
          </Form>
        )}
    </>
  )
}

