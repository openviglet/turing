"use client"
import { ROUTES } from "@/app/routes.const"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
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
import type { TurSEInstance } from "@/models/se/se-instance.model.ts"
import { TurSEInstanceService } from "@/services/se/se.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../ui/dialog"
import { GradientButton } from "../ui/gradient-button"
const turSEInstanceService = new TurSEInstanceService();
interface Props {
  value: TurSEInstance;
  isNew: boolean;
}

export const SEInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSEInstance>({
    defaultValues: value
  });
  const { control } = form;
  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(seInstance: TurSEInstance) {
    try {
      if (isNew) {
        const result = await turSEInstanceService.create(seInstance);
        if (result) {
          toast.success(`The ${seInstance.title} Search Engine was saved`);
          navigate(ROUTES.SE_INSTANCE);
        } else {
          toast.error(`The ${seInstance.title} Search Engine was not saved`);
        }
      }
      else {
        const result = await turSEInstanceService.update(seInstance);
        if (result) {
          toast.success(`The ${seInstance.title} Search Engine was updated`);
        } else {
          toast.error(`The ${seInstance.title} Search Engine was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  async function onDelete() {
    console.log("delete");
    try {
      if (await turSEInstanceService.delete(value)) {
        toast.success(`The ${value.title} Search Engine was deleted`);
        navigate(ROUTES.SE_INSTANCE);
      }
      else {
        toast.error(`The ${value.title} Search Engine was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${value.title} Search Engine was not deleted`);
    }
    setOpen(false);
  }
  return (
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Search Engine</CardTitle>
          <CardAction>
            {!isNew &&
              <Dialog open={open} onOpenChange={setOpen}>
                <form>
                  <DialogTrigger asChild>
                    <GradientButton variant={"outline"}>Delete</GradientButton>
                  </DialogTrigger>
                  <DialogContent className="sm:max-w-112.5">
                    <DialogHeader>
                      <DialogTitle>Are you absolutely sure?</DialogTitle>
                      <DialogDescription>
                        Unexpected bad things will happen if you don't read this!
                      </DialogDescription>
                    </DialogHeader>
                    <p className="grid gap-4">
                      This action cannot be undone. This will permanently delete the {value.title} search engine.
                    </p>
                    <DialogFooter>
                      <GradientButton onClick={onDelete} variant="destructive">I understand the consequences, delete this search engine</GradientButton>
                    </DialogFooter>
                  </DialogContent>
                </form>
              </Dialog>
            }
          </CardAction>
          <CardDescription>
            Search engine settings.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
              <FormField
                control={control}
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
                    <FormDescription>Search engine instance title will appear on list.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
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
                    <FormDescription>Search engine instance description will appear on list.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="turSEVendor.id"
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
                        <SelectItem key="SOLR" value="SOLR">Apache Solr</SelectItem>
                        <SelectItem key="LUCENE" value="LUCENE">Apache Lucene</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription>Search engine vendor that will be used.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="host"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Host</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Host"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Search engine instance host will be connected.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={control}
                name="port"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Port</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Port"
                        type="number"
                        {...field} />
                    </FormControl>
                    <FormDescription>Search engine instance port will be connected.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <GradientButton type="submit">Save</GradientButton>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}

