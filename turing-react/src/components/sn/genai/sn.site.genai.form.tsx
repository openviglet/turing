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
import React, { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"

const turSNSiteService = new TurSNSiteService();
const turLLMInstanceService = new TurLLMInstanceService();
const turStoreInstanceService = new TurStoreInstanceService();

const DEFAULT_SYSTEM_PROMPT =
    "Using only this rag data. Answer in Portuguese." +
    "You are a helpful assistant that can answer questions about the web site:\n" +
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

    function handleEnabledChange(checked: boolean) {
        form.setValue("enabled", checked);
        if (checked && !form.getValues("systemPrompt")) {
            form.setValue("systemPrompt", DEFAULT_SYSTEM_PROMPT);
        }
    }

    async function onSubmit(data: TurSNSiteGenAi) {
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
                    <>
                        <FormField
                            control={form.control}
                            name="turLLMInstance"
                            rules={{ required: "Language model is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Language Model</FormLabel>
                                    <Select
                                        onValueChange={(val) => {
                                            if (val === "__none__") {
                                                field.onChange(null);
                                            } else {
                                                const instance = llmInstances.find((i) => i.id === val);
                                                field.onChange(instance ?? null);
                                            }
                                        }}
                                        value={field.value?.id || "__none__"}
                                    >
                                        <FormControl>
                                            <SelectTrigger className="w-full">
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
                                    <FormDescription>Language model that will be used.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="turStoreInstance"
                            rules={{ required: "Embedding store is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Embedding Store</FormLabel>
                                    <Select
                                        onValueChange={(val) => {
                                            if (val === "__none__") {
                                                field.onChange(null);
                                            } else {
                                                const instance = storeInstances.find((i) => i.id === val);
                                                field.onChange(instance ?? null);
                                            }
                                        }}
                                        value={field.value?.id || "__none__"}
                                    >
                                        <FormControl>
                                            <SelectTrigger className="w-full">
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
                                    <FormDescription>Embedding store that will be used.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="systemPrompt"
                            rules={{ required: "System prompt is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>System Prompt</FormLabel>
                                    <FormControl>
                                        <Textarea
                                            {...field}
                                            rows={4}
                                            placeholder="System prompt"
                                        />
                                    </FormControl>
                                    <FormDescription>System prompt</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </>
                )}

                <GradientButton type="submit">
                    Save changes
                </GradientButton>
            </form>
        </Form>
    );
};
