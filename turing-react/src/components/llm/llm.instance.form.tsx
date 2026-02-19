"use client"
import { ROUTES } from "@/app/routes.const"
import { Switch } from "@/components//ui/switch"
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
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts"
import { TurLLMInstanceService } from "@/services/llm/llm.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DialogDelete } from "../dialog.delete"
import { GradientButton } from "../ui/gradient-button"
const turLLMInstanceService = new TurLLMInstanceService();
const urlBase = ROUTES.LLM_INSTANCE
interface Props {
  value: TurLLMInstance;
  isNew: boolean;
}

export const LLMInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurLLMInstance>({
    defaultValues: value
  });

  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  const applyVendorDefaults = (vendorId: string) => {
    if (vendorId === "OLLAMA") {
      form.setValue("url", "http://localhost:8000", { shouldDirty: true });
      form.setValue("modelName", "MISTRAL", { shouldDirty: true });
      form.setValue("temperature", 0.8, { shouldDirty: true });
      form.setValue("topK", 6, { shouldDirty: true });
      form.setValue("supportedCapabilities", "RESPONSE_FORMAT_JSON_SCHEMA", { shouldDirty: true });
      form.setValue("timeout", "PT60S", { shouldDirty: true });
    }
  }

  async function onSubmit(llmInstance: TurLLMInstance) {
    try {
      if (isNew) {
        const result = await turLLMInstanceService.create(llmInstance);
        if (result) {
          toast.success(`The ${llmInstance.title} Language Model was saved`);
          navigate(urlBase);
        } else {
          toast.error(`The ${llmInstance.title} Language Model was not saved`);
        }
      }
      else {
        const result = await turLLMInstanceService.update(llmInstance);
        if (result) {
          toast.success(`The ${llmInstance.title} Language Model was updated`);
        } else {
          toast.error(`The ${llmInstance.title} Language Model was not updated`);
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
      if (await turLLMInstanceService.delete(value)) {
        toast.success(`The ${value.title} Language Model was deleted`);
        navigate(urlBase);
      }
      else {
        toast.error(`The ${value.title} Language Model was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${value.title} Language Model was not deleted`);
    }
    setOpen(false);
  }
  return (
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto ">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Language Model</CardTitle>
          <CardAction>
            {!isNew && <DialogDelete feature="Language Model" name={value.title} onDelete={onDelete} open={open} setOpen={setOpen} />}
          </CardAction>
          <CardDescription>
            Language model settings.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(onSubmit)}
              className="w-full max-w-2xl mx-auto py-8 flex flex-col gap-6"
              autoComplete="off"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <FormField
                  control={form.control}
                  name="title"
                  rules={{ required: "Title is required." }}
                  render={({ field }) => (
                    <FormItem className="col-span-1">
                      <FormLabel>Title</FormLabel>
                      <FormControl>
                        <Input {...field} placeholder="Title" type="text" className="w-full" />
                      </FormControl>
                      <FormDescription>
                        Display name for this language model instance.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="turLLMVendor.id"
                  rules={{ required: "Vendor is required." }}
                  render={({ field }) => (
                    <FormItem className="col-span-1">
                      <FormLabel>Vendor</FormLabel>
                      <Select
                        onValueChange={(nextValue) => {
                          field.onChange(nextValue);
                          applyVendorDefaults(nextValue);
                        }}
                        value={field.value}
                      >
                        <FormControl>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Choose..." />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem key="OLLAMA" value="OLLAMA">Ollama</SelectItem>
                          <SelectItem key="OPENAI" value="OPENAI">OpenAI</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        Provider for this language model.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Description</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Description"
                        className="resize-none w-full"
                        rows={2}
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>
                      Brief summary describing this instance.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <FormField
                  control={form.control}
                  name="url"
                  rules={{ required: "Hostname is required." }}
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Hostname</FormLabel>
                      <FormControl>
                        <Input placeholder="Hostname" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Endpoint URL (e.g., http://localhost:8000).
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="modelName"
                  rules={{ required: "Model Name is required." }}
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Model Name</FormLabel>
                      <FormControl>
                        <Input placeholder="Model Name" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Model identifier (e.g., "MISTRAL", "gpt-3.5-turbo").
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="temperature"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Temperature</FormLabel>
                      <FormControl>
                        <Input placeholder="Temperature" type="number" step="0.01" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Randomness of responses (0.0 - 1.0).
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="topK"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>TopK</FormLabel>
                      <FormControl>
                        <Input placeholder="TopK" type="number" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Top K tokens for generation.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="topP"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>TopP</FormLabel>
                      <FormControl>
                        <Input placeholder="TopP" type="number" step="0.01" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Nucleus sampling (e.g., 0.9).
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="repeatPenalty"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Repeat penalty</FormLabel>
                      <FormControl>
                        <Input placeholder="Repeat penalty" type="number" step="0.01" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Discourages repetition.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="seed"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Seed</FormLabel>
                      <FormControl>
                        <Input placeholder="Seed" type="number" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Random seed for reproducibility.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="numPredict"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Number of predictions</FormLabel>
                      <FormControl>
                        <Input placeholder="Number of predictions" type="number" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Number of tokens to generate.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="stop"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Stop</FormLabel>
                      <FormControl>
                        <Input placeholder="Stop (comma separated)" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Stop generation on these strings.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="responseFormat"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Response format</FormLabel>
                      <FormControl>
                        <Input placeholder="Response format" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Output format, e.g., "TEXT" or "JSON".
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="supportedCapabilities"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Supported capabilities</FormLabel>
                      <FormControl>
                        <Input placeholder="Supported capabilities" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Comma-separated features (e.g., "RESPONSE_FORMAT_JSON_SCHEMA").
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="timeout"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Timeout</FormLabel>
                      <FormControl>
                        <Input placeholder="Timeout (e.g., PT60S)" type="text" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Max wait time for response.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="maxRetries"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Max retries</FormLabel>
                      <FormControl>
                        <Input placeholder="Maximum number of retries" type="number" className="w-full" {...field} />
                      </FormControl>
                      <FormDescription>
                        Retry attempts before failing.
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex items-center justify-between gap-4">
                <FormField
                  control={form.control}
                  name="enabled"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center gap-3 mb-0">
                      <FormLabel className="mb-0">Enabled</FormLabel>
                      <FormControl>
                        <Switch
                          checked={field.value === 1}
                          onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                        />
                      </FormControl>
                      <FormDescription>
                        Enable or disable this instance.
                      </FormDescription>
                    </FormItem>
                  )}
                />
                <GradientButton type="submit" className="ml-auto w-40">
                  Save
                </GradientButton>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}

