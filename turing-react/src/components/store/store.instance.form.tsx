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
import type { TurStoreInstance } from "@/models/store/store-instance.model.ts"
import { TurStoreInstanceService } from "@/services/store/store.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DialogDelete } from "../dialog.delete"
import { GradientButton } from "../ui/gradient-button"
import { Switch } from "../ui/switch"
const turStoreInstanceService = new TurStoreInstanceService();
const urlBase = ROUTES.STORE_INSTANCE
interface Props {
  value: TurStoreInstance;
  isNew: boolean;
}

export const StoreInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurStoreInstance>({
    defaultValues: value
  });
  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])
  const applyVendorDefaults = (vendorId: string) => {
    if (vendorId === "CHROMA") {
      form.setValue("url", "http://localhost:8000", { shouldDirty: true });
    }
  }
  function onSubmit(storeInstance: TurStoreInstance) {
    try {
      if (isNew) {
        turStoreInstanceService.create(storeInstance);
        toast.success(`The ${storeInstance.title} Embedding Store was saved`);
        navigate(urlBase);
      }
      else {
        turStoreInstanceService.update(storeInstance);
        toast.success(`The ${storeInstance.title} Embedding Store was updated`);
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  async function onDelete() {
    console.log("delete");
    try {
      if (await turStoreInstanceService.delete(value)) {
        toast.success(`The ${value.title} Embedding Store was deleted`);
        navigate(urlBase);
      }
      else {
        toast.error(`The ${value.title} Embedding Store was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${value.title} Embedding Store was not deleted`);
    }
    setOpen(false);
  }
  return (
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Embedding Store</CardTitle>
          <CardAction>
            {!isNew && <DialogDelete feature="embedding store" name={value.title} onDelete={onDelete} open={open} setOpen={setOpen} />}
          </CardAction>
          <CardDescription>
            Embedding store settings.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
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
                      Enter a unique and descriptive name for this embedding store instance. This title will be shown in the list of available stores.
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
                      Provide a brief summary of the embedding store’s purpose or usage. This helps users understand its role at a glance.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="turStoreVendor.id"
                rules={{ required: "Vendor is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vendor</FormLabel>
                    <Select onValueChange={(nextValue) => {
                      field.onChange(nextValue);
                      applyVendorDefaults(nextValue);
                    }} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Choose..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem key="CHROMA" value="CHROMA">Chroma</SelectItem>
                        <SelectItem key="MILVUS" value="MILVUS">Milvus</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      Select the backend technology for this embedding store. The vendor determines how vectors are stored and retrieved.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="url"
                rules={{ required: "Endpoint is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Endpoint</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Endpoint"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>
                      Specify the base URL or endpoint where the embedding store service is accessible. Example: http://localhost:8000
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="enabled"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Enabled</FormLabel>
                    <FormControl>
                      <Switch checked={field.value === 1}
                        onCheckedChange={(checked) => {
                          field.onChange(checked ? 1 : 0);
                        }}
                      />
                    </FormControl>
                    <FormDescription>
                      Toggle to activate or deactivate this embedding store instance. Disabled stores will not be available for use.
                    </FormDescription>
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

