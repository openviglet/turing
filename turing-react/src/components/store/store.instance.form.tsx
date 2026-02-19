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
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../ui/accordion"
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
              <Accordion
                type="multiple"
                defaultValue={["general", "vendor", "endpoint", "status"]}
                className="w-full space-y-4"
              >
                {/* General Information Section */}
                <AccordionItem value="general" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">General Information</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="title"
                        rules={{ required: "A title is required for this embedding store." }}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Title</FormLabel>
                            <FormDescription>
                              Provide a unique, descriptive name for this embedding store instance.
                            </FormDescription>
                            <FormControl>
                              <Input
                                {...field}
                                placeholder="Enter store title"
                                type="text"
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="description"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Description</FormLabel>
                            <FormDescription>
                              Summarize the purpose or intended use of this embedding store.
                            </FormDescription>
                            <FormControl>
                              <Textarea
                                placeholder="Enter a brief description"
                                className="resize-none"
                                {...field}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Vendor Section - Strict Inline */}
                <AccordionItem value="vendor" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Vendor</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    <div className="w-full flex flex-row justify-between items-center gap-4">
                      <div className="flex flex-col">
                        <FormLabel>Vendor</FormLabel>
                        <FormDescription>
                          Choose the backend technology powering this embedding store.
                        </FormDescription>
                      </div>
                      <div className="flex-1 max-w-xs">
                        <FormField
                          control={form.control}
                          name="turStoreVendor.id"
                          rules={{ required: "Please select a vendor." }}
                          render={({ field }) => (
                            <FormItem className="mb-0">
                              <Select
                                onValueChange={(nextValue) => {
                                  field.onChange(nextValue);
                                  applyVendorDefaults(nextValue);
                                }}
                                value={field.value}
                              >
                                <FormControl>
                                  <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select vendor..." />
                                  </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                  <SelectItem key="CHROMA" value="CHROMA">
                                    Chroma
                                  </SelectItem>
                                  <SelectItem key="MILVUS" value="MILVUS">
                                    Milvus
                                  </SelectItem>
                                </SelectContent>
                              </Select>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Endpoint Section */}
                <AccordionItem value="endpoint" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Endpoint</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="url"
                        rules={{ required: "Endpoint URL is required." }}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Endpoint</FormLabel>
                            <FormDescription>
                              Enter the base URL where this embedding store is accessible. Example: http://localhost:8000
                            </FormDescription>
                            <FormControl>
                              <Input
                                placeholder="Enter endpoint URL"
                                type="text"
                                {...field}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Status Section - Strict Inline */}
                <AccordionItem value="status" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Status</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    <div className="w-full flex flex-row justify-between items-center gap-4">
                      <div className="flex flex-col">
                        <FormLabel>Enabled</FormLabel>
                        <FormDescription>
                          Toggle to activate or deactivate this embedding store instance.
                        </FormDescription>
                      </div>
                      <div>
                        <FormField
                          control={form.control}
                          name="enabled"
                          render={({ field }) => (
                            <FormItem className="mb-0">
                              <FormControl>
                                <Switch
                                  checked={field.value === 1}
                                  onCheckedChange={(checked) => {
                                    field.onChange(checked ? 1 : 0);
                                  }}
                                />
                              </FormControl>
                            </FormItem>
                          )}
                        />
                      </div>
                    </div>
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
              <div className="flex justify-end gap-3 pt-4">
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
        </CardContent>
      </Card>
    </div>
  )
}

