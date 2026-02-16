"use client"

import { Checkbox } from "@/components/ui/checkbox"
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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import type { TurLLMInstance } from "@/models/llm/llm-instance.model"
import type { TurSNSiteGenAi } from "@/models/sn/sn-site-genai.model"
import type { TurSNSite } from "@/models/sn/sn-site.model"
import type { TurStoreInstance } from "@/models/store/store-instance.model"
import { TurLLMInstanceService } from "@/services/llm/llm.service"
import { TurSNSiteService } from "@/services/sn/sn.service"
import { TurStoreInstanceService } from "@/services/store/store.service"
import { Info, PlusCircle } from "lucide-react"
import React, { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"

const turSNSiteService = new TurSNSiteService();
const turLLMInstanceService = new TurLLMInstanceService();
const turStoreInstanceService = new TurStoreInstanceService();

const DEFAULT_SYSTEM_PROMPT =
    "Using only this rag data. Answer in Portuguese. You are a helpful assistant that can answer questions about the web site:\n" +
    "\n" +
    "Question:\n" +
    "{{question}}\n" +
    "\n" +
    "Base your answer on the following information:\n" +
    "{{information}}";

interface Props {
    snSite: TurSNSite;
}

export const SNSiteGenAiForm: React.FC<Props> = ({ snSite }) => {
    const form = useForm<TurSNSiteGenAi>({
        defaultValues: snSite.turSNSiteGenAi,
    });
    const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([]);
    const [storeInstances, setStoreInstances] = useState<TurStoreInstance[]>([]);

    useEffect(() => {
        turLLMInstanceService.query().then(setLlmInstances);
        turStoreInstanceService.query().then(setStoreInstances);
    }, []);

    useEffect(() => {
        form.reset(snSite.turSNSiteGenAi);
    }, [snSite]);

    const enabled = form.watch("enabled");
    const systemPrompt = form.watch("systemPrompt") || "";

    // Tag validation logic
    const hasQuestion = systemPrompt.includes("{{question}}");
    const hasInformation = systemPrompt.includes("{{information}}");

    function handleEnabledChange(checked: boolean) {
        form.setValue("enabled", checked);
        if (checked && !form.getValues("systemPrompt")) {
            form.setValue("systemPrompt", DEFAULT_SYSTEM_PROMPT);
        }
    }

    // Function to insert variable at cursor (or at end)
    const insertVariable = (variable: string) => {
        const currentPrompt = form.getValues("systemPrompt") || "";
        form.setValue("systemPrompt", currentPrompt + variable, { shouldValidate: true });
    };

    async function onSubmit(data: TurSNSiteGenAi) {
        if (data.enabled && (!hasQuestion || !hasInformation)) {
            toast.error("The System Prompt must contain the tags {{question}} and {{information}}.");
            return;
        }

        try {
            const updatedSite = { ...snSite, turSNSiteGenAi: data };
            const result = await turSNSiteService.update(updatedSite);
            if (result) {
                toast.success(`${snSite.name} semantic navigation site was updated.`);
            } else {
                toast.error("Failed to update site. Please try again.");
            }
        } catch (error) {
            console.error("Form submission error", error);
            toast.error("Failed to update site. Please try again.");
        }
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
                {/* Activation Checkbox */}
                <FormField
                    control={form.control}
                    name="enabled"
                    render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                            <FormControl>
                                <Checkbox
                                    checked={field.value}
                                    onCheckedChange={handleEnabledChange}
                                />
                            </FormControl>
                            <div className="space-y-1 leading-none">
                                <FormLabel>Enabled</FormLabel>
                                <FormDescription>Enable Generative AI</FormDescription>
                            </div>
                        </FormItem>
                    )}
                />

                {enabled && (
                    <div className="space-y-6 animate-in fade-in duration-500">
                        {/* Select: Language Model */}
                        <FormField
                            control={form.control}
                            name="turLLMInstance"
                            rules={{ required: enabled && "Language model is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Language Model</FormLabel>
                                    <Select
                                        onValueChange={(val) => {
                                            const instance = llmInstances.find((i) => i.id === val);
                                            field.onChange(instance ?? null);
                                        }}
                                        value={field.value?.id || "__none__"}
                                    >
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="-- No Language Model --" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="__none__">-- No Language Model --</SelectItem>
                                            {llmInstances.map((instance) => (
                                                <SelectItem key={instance.id} value={instance.id}>
                                                    {instance.title}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {/* Select: Embedding Store */}
                        <FormField
                            control={form.control}
                            name="turStoreInstance"
                            rules={{ required: enabled && "Embedding store is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Embedding Store</FormLabel>
                                    <Select
                                        onValueChange={(val) => {
                                            const instance = storeInstances.find((i) => i.id === val);
                                            field.onChange(instance ?? null);
                                        }}
                                        value={field.value?.id || "__none__"}
                                    >
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="-- No Embedding Store --" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="__none__">-- No Embedding Store --</SelectItem>
                                            {storeInstances.map((instance) => (
                                                <SelectItem key={instance.id} value={instance.id}>
                                                    {instance.title}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {/* Textarea: System Prompt with Interactive Variables */}
                        <FormField
                            control={form.control}
                            name="systemPrompt"
                            rules={{ required: enabled && "System prompt is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <div className="flex items-center justify-between mb-2">
                                        <FormLabel className="flex items-center gap-2">
                                            System Prompt
                                        </FormLabel>
                                        <div className="flex gap-2">
                                            <button
                                                type="button"
                                                onClick={() => insertVariable("{{question}}")}
                                                className="text-[10px] flex items-center gap-1 bg-amber-500/10 text-amber-600 border border-amber-500/20 px-2 py-1 rounded hover:bg-amber-500/20 transition-all font-mono"
                                            >
                                                <PlusCircle className="w-3 h-3" /> {"{{question}}"}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => insertVariable("{{information}}")}
                                                className="text-[10px] flex items-center gap-1 bg-amber-500/10 text-amber-600 border border-amber-500/20 px-2 py-1 rounded hover:bg-amber-500/20 transition-all font-mono"
                                            >
                                                <PlusCircle className="w-3 h-3" /> {"{{information}}"}
                                            </button>
                                        </div>
                                    </div>
                                    <FormControl>
                                        <Textarea
                                            {...field}
                                            rows={8}
                                            placeholder="Ex: Use {{information}} to answer {{question}}..."
                                            className="font-mono text-sm leading-relaxed"
                                        />
                                    </FormControl>

                                    {/* Status Indicators for Variables */}
                                    <div className="flex gap-4 mt-2">
                                        <StatusBadge label="question" active={hasQuestion} />
                                        <StatusBadge label="information" active={hasInformation} />
                                    </div>

                                    <div className="flex items-start gap-2 mt-2 p-2 bg-slate-50 rounded-md dark:bg-slate-900 border">
                                        <Info className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                                        <p className="text-[11px] text-muted-foreground">
                                            For RAG to work, the prompt must necessarily contain the tags
                                            <span className="font-bold text-amber-600"> {"{{question}}"}</span> and
                                            <span className="font-bold text-amber-600"> {"{{information}}"}</span>.
                                        </p>
                                    </div>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </div>
                )}

                <GradientButton
                    type="submit"
                    disabled={enabled && (!hasQuestion || !hasInformation)}
                    className="w-full sm:w-auto"
                >
                    Save changes
                </GradientButton>
            </form>
        </Form>
    );
};

// Auxiliary component for Status Badges
function StatusBadge({ label, active }: { label: string; active: boolean }) {
    return (
        <div className={`flex items-center gap-1.5 px-2 py-1 rounded-full border text-[10px] font-bold transition-all ${active
            ? "bg-emerald-500/10 text-emerald-600 border-emerald-500/20"
            : "bg-slate-100 text-slate-400 border-slate-200 opacity-60"
            }`}>
            <div className={`w-1.5 h-1.5 rounded-full ${active ? "bg-emerald-500 animate-pulse" : "bg-slate-300"}`} />
            {label.toUpperCase()}
        </div>
    );
}