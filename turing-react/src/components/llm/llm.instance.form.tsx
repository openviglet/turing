"use client"
import { ROUTES } from "@/app/routes.const"
import { Switch } from "@/components//ui/switch"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
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
      <Card className="mx-auto max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Language Model</CardTitle>
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
                      This action cannot be undone. This will permanently delete the {value.title} language model.
                    </p>
                    <DialogFooter>
                      <GradientButton onClick={onDelete} variant="destructive">I understand the consequences, delete this language model</GradientButton>
                    </DialogFooter>
                  </DialogContent>
                </form>
              </Dialog>
            }
          </CardAction>
          <CardDescription>
            Language model settings.
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
                    <FormDescription>Language model instance title will appear on list.</FormDescription>
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
                    <FormDescription>Language model instance description will appear on list.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="turLLMVendor.id"
                rules={{ required: "Vendor is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vendor</FormLabel>
                    <Select
                      onValueChange={(nextValue) => {
                        field.onChange(nextValue);
                        applyVendorDefaults(nextValue);
                      }}
                      value={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Choose..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem key="OLLAMA" value="OLLAMA">Ollama</SelectItem>
                        <SelectItem key="OPENAI" value="OPENAI">OpenAI</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription>Language model vendor that will be used.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="url"
                rules={{ required: "Hostname is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Hostname</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Hostname"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Language model instance URL will be connected.</FormDescription>
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
                      <Input
                        placeholder="Model Name"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>The name of the model to use from server.</FormDescription>
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
                      <Input
                        placeholder="Temperature"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Controls the randomness of the generated responses. Higher values (e.g., 1.0) result in more diverse output, while lower values (e.g., 0.2) produce more deterministic responses.</FormDescription>
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
                      <Input
                        placeholder="TopK"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Specifies the number of highest probability tokens to consider for each step during generation.</FormDescription>
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
                      <Input
                        placeholder="TopP"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Controls the diversity of the generated responses by setting a threshold for the cumulative probability of top tokens.</FormDescription>
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
                      <Input
                        placeholder="Repeat penalty"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Penalizes the model for repeating similar tokens in the generated output.</FormDescription>
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
                      <Input
                        placeholder="Seed"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Sets the random seed for reproducibility of generated responses.</FormDescription>
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
                      <Input
                        placeholder="Number of predictions"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>The number of predictions to generate for each input prompt.</FormDescription>
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
                      <Input
                        placeholder="Stop"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>A list of strings that, if generated, will mark the end of the response.</FormDescription>
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
                      <Input
                        placeholder="Response format"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>The desired format for the generated output. TEXT or JSON with optional JSON Schema definition.</FormDescription>
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
                      <Input
                        placeholder="Supported capabilities"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>Set of model capabilities.</FormDescription>
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
                      <Input
                        placeholder="Timeout"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>The maximum time allowed for the API call to complete.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="maxRetries"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Maximum number of retries</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Maximum number of retries"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>The maximum number of retries in case of API call failure.</FormDescription>
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

