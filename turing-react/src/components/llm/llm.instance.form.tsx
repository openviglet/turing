"use client"
import { ROUTES } from "@/app/routes.const"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
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
import { FormItemTwoColumns } from "../ui/form-item-two-columns"
import { GradientButton } from "../ui/gradient-button"
import { GradientSwitch } from "../ui/gradient-switch"
const turLLMInstanceService = new TurLLMInstanceService();
const urlBase = ROUTES.LLM_INSTANCE
interface Props {
  value: TurLLMInstance;
  isNew: boolean;
}

type LlmProviderOptionsDraft = {
  baseUrl: string;
  model: string;
  chatModel: string;
  embeddingModel: string;
  temperature: string;
  topK: string;
  topP: string;
  repeatPenalty: string;
  seed: string;
  numPredict: string;
  maxTokens: string;
  stop: string;
}

const emptyLlmProviderOptionsDraft = (): LlmProviderOptionsDraft => ({
  baseUrl: "",
  model: "",
  chatModel: "",
  embeddingModel: "",
  temperature: "",
  topK: "",
  topP: "",
  repeatPenalty: "",
  seed: "",
  numPredict: "",
  maxTokens: "",
  stop: ""
})

const toText = (value: unknown) => value == null ? "" : String(value)

const parseJsonObject = (jsonValue?: string) => {
  if (!jsonValue?.trim()) {
    return undefined
  }
  try {
    const parsed = JSON.parse(jsonValue) as unknown
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>
    }
  } catch {
    // Keep the draft empty for invalid JSON; validation occurs on submit.
  }
  return undefined
}

const parseLlmProviderOptionsDraft = (vendorId: string | undefined, jsonValue?: string): LlmProviderOptionsDraft => {
  const parsed = parseJsonObject(jsonValue)
  if (!parsed) {
    return emptyLlmProviderOptionsDraft()
  }

  return {
    baseUrl: toText(parsed.baseUrl),
    model: vendorId === "OLLAMA" ? toText(parsed.model) : "",
    chatModel: vendorId === "OPENAI" ? toText(parsed.chatModel ?? parsed.model) : "",
    embeddingModel: toText(parsed.embeddingModel),
    temperature: toText(parsed.temperature),
    topK: toText(parsed.topK),
    topP: toText(parsed.topP),
    repeatPenalty: toText(parsed.repeatPenalty),
    seed: toText(parsed.seed),
    numPredict: toText(parsed.numPredict),
    maxTokens: toText(parsed.maxTokens),
    stop: Array.isArray(parsed.stop) ? parsed.stop.map((item) => String(item)).join(",") : toText(parsed.stop)
  }
}

const parseNumber = (value: string) => {
  const normalized = value.trim()
  if (!normalized) {
    return undefined
  }
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : undefined
}

const buildLlmProviderOptionsFromDraft = (vendorId: string | undefined, draft: LlmProviderOptionsDraft) => {
  const options: Record<string, unknown> = {}
  const putText = (key: string, value: string) => {
    const normalized = value.trim()
    if (normalized) {
      options[key] = normalized
    }
  }
  const putNumber = (key: string, value: string) => {
    const parsed = parseNumber(value)
    if (parsed !== undefined) {
      options[key] = parsed
    }
  }

  putText("baseUrl", draft.baseUrl)
  putText("embeddingModel", draft.embeddingModel)
  putNumber("temperature", draft.temperature)
  putNumber("topP", draft.topP)
  putNumber("seed", draft.seed)

  if (vendorId === "OPENAI") {
    putText("chatModel", draft.chatModel)
    putNumber("maxTokens", draft.maxTokens)
  }

  if (vendorId === "OLLAMA") {
    putText("model", draft.model)
    putNumber("topK", draft.topK)
    putNumber("repeatPenalty", draft.repeatPenalty)
    putNumber("numPredict", draft.numPredict)
    const stopItems = draft.stop
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
    if (stopItems.length > 0) {
      options.stop = stopItems
    }
  }

  return options
}

export const LLMInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurLLMInstance>({
    defaultValues: value
  });

  const [open, setOpen] = useState(false);
  const [llmProviderOptionsDraft, setLlmProviderOptionsDraft] = useState<LlmProviderOptionsDraft>(
    emptyLlmProviderOptionsDraft()
  )
  const selectedVendorId = form.watch("turLLMVendor.id");
  const navigate = useNavigate()

  useEffect(() => {
    form.reset({
      ...value,
      apiKey: "",
      providerOptionsJson: value.providerOptionsJson ?? ""
    });
    setLlmProviderOptionsDraft(
      parseLlmProviderOptionsDraft(value.turLLMVendor?.id, value.providerOptionsJson)
    )
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
    if (vendorId === "OPENAI") {
      form.setValue("url", "https://api.openai.com", { shouldDirty: true });
      form.setValue("modelName", "gpt-4o-mini", { shouldDirty: true });
      form.setValue("timeout", "PT60S", { shouldDirty: true });
    }
  }

  const setLlmDraftValue = (key: keyof LlmProviderOptionsDraft, fieldValue: string) => {
    setLlmProviderOptionsDraft((prev) => ({ ...prev, [key]: fieldValue }))
  }

  const getProviderOptionsPlaceholder = (vendorId?: string) => {
    if (vendorId === "OPENAI") {
      return '{\n  "baseUrl": "https://api.openai.com",\n  "chatModel": "gpt-4o-mini",\n  "embeddingModel": "text-embedding-3-small",\n  "temperature": 0.7,\n  "topP": 0.9,\n  "seed": 42,\n  "maxTokens": 1024\n}'
    }
    return '{\n  "baseUrl": "http://localhost:11434",\n  "model": "mistral",\n  "embeddingModel": "nomic-embed-text",\n  "temperature": 0.8,\n  "topK": 6,\n  "topP": 0.9,\n  "repeatPenalty": 1.1,\n  "seed": 42,\n  "numPredict": 256,\n  "stop": ["END", "STOP"]\n}'
  }

  const normalizeJson = (jsonValue?: string) => {
    const normalized = jsonValue?.trim();
    if (!normalized) {
      return undefined;
    }
    try {
      JSON.parse(normalized);
      return normalized;
    } catch {
      toast.error("Provider Options must be valid JSON.");
      return null;
    }
  }

  async function onSubmit(llmInstance: TurLLMInstance) {
    const rawProviderOptionsJson = normalizeJson(llmInstance.providerOptionsJson)
    if (rawProviderOptionsJson === null) {
      return;
    }

    const visualProviderOptions = buildLlmProviderOptionsFromDraft(selectedVendorId, llmProviderOptionsDraft)
    const rawProviderOptions = parseJsonObject(rawProviderOptionsJson)
    const mergedProviderOptions = {
      ...visualProviderOptions,
      ...(rawProviderOptions ?? {})
    }
    const providerOptionsJson = Object.keys(mergedProviderOptions).length > 0
      ? JSON.stringify(mergedProviderOptions, null, 2)
      : undefined

    const payload: TurLLMInstance = {
      ...llmInstance,
      apiKey: llmInstance.apiKey?.trim() || undefined,
      providerOptionsJson
    }

    try {
      if (isNew) {
        const result = await turLLMInstanceService.create(payload);
        if (result) {
          toast.success(`The ${payload.title} Language Model was saved`);
          navigate(urlBase);
        } else {
          toast.error(`The ${payload.title} Language Model was not saved`);
        }
      }
      else {
        const result = await turLLMInstanceService.update(payload);
        if (result) {
          toast.success(`The ${payload.title} Language Model was updated`);
        } else {
          toast.error(`The ${payload.title} Language Model was not updated`);
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
              className="w-full max-w-2xl mx-auto py-8 flex flex-col gap-8"
              autoComplete="off"
            >
              <Accordion
                type="multiple"
                defaultValue={[
                  "general",
                  "model",
                  "generation",
                  "advanced",
                  "status"
                ]}
                className="w-full space-y-4"
              >
                {/* General Section */}
                <AccordionItem value="general" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">General Information</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    {/* Title */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="title"
                        rules={{ required: "A title is required for this language model instance." }}
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Title</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Enter a clear, descriptive name for this language model instance.
                            </div>
                            <FormControl>
                              <Input {...field} placeholder="Enter a title" type="text" className="w-full" />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Vendor (inline row) */}
                    <div className="w-full flex flex-row justify-between items-center">
                      <div className="flex flex-col">
                        <FormLabel>Vendor</FormLabel>
                        <div className="text-muted-foreground text-sm font-normal mt-1">
                          Choose the provider for this model.
                        </div>
                      </div>
                      <div className="flex-1 ml-8">
                        <FormField
                          control={form.control}
                          name="turLLMVendor.id"
                          rules={{ required: "Vendor is required." }}
                          render={({ field }) => (
                            <FormItem className="w-full">
                              <FormControl>
                                <Select
                                  onValueChange={(nextValue) => {
                                    field.onChange(nextValue);
                                    applyVendorDefaults(nextValue);
                                  }}
                                  value={field.value}
                                >
                                  <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Choose..." />
                                  </SelectTrigger>
                                  <SelectContent>
                                    <SelectItem key="OLLAMA" value="OLLAMA">Ollama</SelectItem>
                                    <SelectItem key="OPENAI" value="OPENAI">OpenAI</SelectItem>
                                  </SelectContent>
                                </Select>
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>
                    </div>
                    {/* Description */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="description"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Description</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Provide a concise summary for this instance.
                            </div>
                            <FormControl>
                              <Textarea
                                placeholder="Add a description"
                                className="resize-none w-full"
                                rows={2}
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

                {/* Model Section */}
                <AccordionItem value="model" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Model Settings</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    {/* URL */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="url"
                        rules={{ required: "Endpoint URL is required." }}
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Endpoint URL</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Specify the model's endpoint, e.g., http://localhost:8000
                            </div>
                            <FormControl>
                              <Input placeholder="Enter the endpoint URL" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Model Name */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="modelName"
                        rules={{ required: "Model Name is required." }}
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Model Name</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Example: "MISTRAL", "gpt-3.5-turbo"
                            </div>
                            <FormControl>
                              <Input placeholder="Specify the model name" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* API Key */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="apiKey"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>API Key</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Optional secret token. Leave blank to keep the existing key when editing.
                            </div>
                            <FormControl>
                              <Input
                                placeholder="Enter API key"
                                type="password"
                                className="w-full"
                                autoComplete="new-password"
                                {...field}
                                value={field.value ?? ""}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Generation Section */}
                <AccordionItem value="generation" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Generation Parameters</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    {/* Temperature */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="temperature"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Temperature</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Controls randomness. Range: 0.0 (deterministic) to 1.0 (creative).
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 0.8" type="number" step="0.01" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Top K */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="topK"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Top K</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Number of top tokens to consider for generation.
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 6" type="number" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Top P */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="topP"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Top P</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Nucleus sampling probability (e.g., 0.9).
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 0.9" type="number" step="0.01" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Repeat Penalty */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="repeatPenalty"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Repeat Penalty</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Discourages repetition in generated output.
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 1.1" type="number" step="0.01" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Seed */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="seed"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Seed</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Random seed for reproducibility.
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 42" type="number" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Number of Predictions */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="numPredict"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Number of Predictions</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Number of tokens to generate per request.
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 128" type="number" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Stop Sequences */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="stop"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Stop Sequences</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Comma-separated stop strings (e.g., END,STOP).
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., END,STOP" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Advanced Section */}
                <AccordionItem value="advanced" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Advanced Options</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    {/* Response Format */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="responseFormat"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Response Format</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Output format, such as "TEXT" or "JSON".
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., JSON" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Supported Capabilities */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="supportedCapabilities"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Supported Capabilities</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Comma-separated features (e.g., "RESPONSE_FORMAT_JSON_SCHEMA").
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., RESPONSE_FORMAT_JSON_SCHEMA" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Timeout */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="timeout"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Timeout</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Maximum wait time for a response (e.g., PT60S).
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., PT60S" type="text" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Max Retries */}
                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="maxRetries"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Max Retries</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Number of retry attempts if the request fails.
                            </div>
                            <FormControl>
                              <Input placeholder="e.g., 3" type="number" className="w-full" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    {/* Provider Options JSON */}
                    <div className="w-full rounded-md border p-4 space-y-4">
                      <div className="text-sm font-medium">Visual Provider Options</div>
                      <div className="text-muted-foreground text-sm">
                        Use these fields for common options. Any value in raw JSON below overrides visual values.
                      </div>
                      {selectedVendorId === "OPENAI" && (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <Input placeholder="Base URL" value={llmProviderOptionsDraft.baseUrl} onChange={(event) => setLlmDraftValue("baseUrl", event.target.value)} />
                          <Input placeholder="Chat Model" value={llmProviderOptionsDraft.chatModel} onChange={(event) => setLlmDraftValue("chatModel", event.target.value)} />
                          <Input placeholder="Embedding Model" value={llmProviderOptionsDraft.embeddingModel} onChange={(event) => setLlmDraftValue("embeddingModel", event.target.value)} />
                          <Input placeholder="Temperature" type="number" step="0.01" value={llmProviderOptionsDraft.temperature} onChange={(event) => setLlmDraftValue("temperature", event.target.value)} />
                          <Input placeholder="Top P" type="number" step="0.01" value={llmProviderOptionsDraft.topP} onChange={(event) => setLlmDraftValue("topP", event.target.value)} />
                          <Input placeholder="Seed" type="number" value={llmProviderOptionsDraft.seed} onChange={(event) => setLlmDraftValue("seed", event.target.value)} />
                          <Input placeholder="Max Tokens" type="number" value={llmProviderOptionsDraft.maxTokens} onChange={(event) => setLlmDraftValue("maxTokens", event.target.value)} />
                        </div>
                      )}
                      {selectedVendorId === "OLLAMA" && (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <Input placeholder="Base URL" value={llmProviderOptionsDraft.baseUrl} onChange={(event) => setLlmDraftValue("baseUrl", event.target.value)} />
                          <Input placeholder="Model" value={llmProviderOptionsDraft.model} onChange={(event) => setLlmDraftValue("model", event.target.value)} />
                          <Input placeholder="Embedding Model" value={llmProviderOptionsDraft.embeddingModel} onChange={(event) => setLlmDraftValue("embeddingModel", event.target.value)} />
                          <Input placeholder="Temperature" type="number" step="0.01" value={llmProviderOptionsDraft.temperature} onChange={(event) => setLlmDraftValue("temperature", event.target.value)} />
                          <Input placeholder="Top K" type="number" value={llmProviderOptionsDraft.topK} onChange={(event) => setLlmDraftValue("topK", event.target.value)} />
                          <Input placeholder="Top P" type="number" step="0.01" value={llmProviderOptionsDraft.topP} onChange={(event) => setLlmDraftValue("topP", event.target.value)} />
                          <Input placeholder="Repeat Penalty" type="number" step="0.01" value={llmProviderOptionsDraft.repeatPenalty} onChange={(event) => setLlmDraftValue("repeatPenalty", event.target.value)} />
                          <Input placeholder="Seed" type="number" value={llmProviderOptionsDraft.seed} onChange={(event) => setLlmDraftValue("seed", event.target.value)} />
                          <Input placeholder="Num Predict" type="number" value={llmProviderOptionsDraft.numPredict} onChange={(event) => setLlmDraftValue("numPredict", event.target.value)} />
                          <Input placeholder="Stop (comma-separated)" value={llmProviderOptionsDraft.stop} onChange={(event) => setLlmDraftValue("stop", event.target.value)} />
                        </div>
                      )}
                    </div>

                    <div className="w-full">
                      <FormField
                        control={form.control}
                        name="providerOptionsJson"
                        render={({ field }) => (
                          <FormItem className="w-full">
                            <FormLabel>Provider Options (JSON)</FormLabel>
                            <div className="text-muted-foreground text-sm font-normal mt-1">
                              Optional provider-specific overrides. Use valid JSON.
                            </div>
                            <FormControl>
                              <Textarea
                                placeholder={getProviderOptionsPlaceholder(selectedVendorId)}
                                className="resize-y w-full min-h-40"
                                rows={10}
                                {...field}
                                value={field.value ?? ""}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Status Section */}
                <AccordionItem value="status" className="border rounded-lg px-6">
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-semibold">Status</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex flex-col gap-6 pt-4">
                    <FormField
                      control={form.control}
                      name="enabled"
                      render={({ field }) => (
                        <FormItemTwoColumns>
                          <FormItemTwoColumns.Left>
                            <FormItemTwoColumns.Label>Enabled</FormItemTwoColumns.Label>
                            <FormItemTwoColumns.Description className="text-sm font-normal mt-1">
                              Toggle to enable or disable this language model instance.
                            </FormItemTwoColumns.Description>
                          </FormItemTwoColumns.Left>
                          <FormItemTwoColumns.Right>
                            <FormControl>
                              <GradientSwitch
                                checked={field.value === 1}
                                onCheckedChange={(checked) => field.onChange(checked ? 1 : 0)}
                              />
                            </FormControl>
                          </FormItemTwoColumns.Right>
                          <FormMessage />
                        </FormItemTwoColumns>
                      )}
                    />
                  </AccordionContent>
                </AccordionItem>
              </Accordion>

              {/* Action Footer */}
              <div className="flex justify-end gap-4 mt-8">
                <GradientButton
                  type="button"
                  variant="outline"
                  className="w-40"
                  onClick={() => navigate(urlBase)}
                >
                  Cancel
                </GradientButton>
                <GradientButton type="submit" className="w-40">
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

