"use client"
import { ROUTES } from "@/app/routes.const"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import type { TurSNSiteMergeField } from "@/models/sn/sn-site-merge-field.model"
import type { TurSNSiteMerge } from "@/models/sn/sn-site-merge.model"
import { TurSNSiteMergeService } from "@/services/sn/sn.site.merge.service"
import { PlusCircle, Trash2 } from "lucide-react"
import React, { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"

const turSNSiteMergeService = new TurSNSiteMergeService();

interface Props {
    snSiteId: string;
    value: TurSNSiteMerge;
    isNew: boolean;
}

export const SNSiteMergeForm: React.FC<Props> = ({ snSiteId, value, isNew }) => {
    const form = useForm<TurSNSiteMerge>({
        defaultValues: value,
    });
    const navigate = useNavigate();
    const urlBase = `${ROUTES.SN_INSTANCE}/${snSiteId}/merge-providers`;
    const [addFieldOpen, setAddFieldOpen] = useState(false);
    const [newFieldName, setNewFieldName] = useState("");

    useEffect(() => {
        form.reset(value);
    }, [value]);

    async function onSubmit(data: TurSNSiteMerge) {
        try {
            if (isNew) {
                const result = await turSNSiteMergeService.create(data);
                if (result) {
                    toast.success(`The merge provider was created`);
                    navigate(urlBase);
                } else {
                    toast.error("Failed to create the merge provider. Please try again.");
                }
            } else {
                const result = await turSNSiteMergeService.update(data);
                if (result) {
                    toast.success(`The merge provider was updated`);
                } else {
                    toast.error("Failed to update the merge provider. Please try again.");
                }
            }
        } catch (error) {
            console.error("Form submission error", error);
            toast.error("Failed to submit the form. Please try again.");
        }
    }

    function addOverwrittenField() {
        if (!newFieldName.trim()) return;
        const currentFields = form.getValues("overwrittenFields") || [];
        const newField: TurSNSiteMergeField = { name: newFieldName.trim() };
        form.setValue("overwrittenFields", [...currentFields, newField]);
        setNewFieldName("");
        setAddFieldOpen(false);
    }

    function removeOverwrittenField(index: number) {
        const currentFields = form.getValues("overwrittenFields") || [];
        form.setValue(
            "overwrittenFields",
            currentFields.filter((_, i) => i !== index)
        );
    }

    const overwrittenFields = form.watch("overwrittenFields") || [];

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 py-8 px-6">
                <div>
                    <h3 className="text-lg font-semibold mb-4">Providers</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <FormField
                            control={form.control}
                            name="providerFrom"
                            rules={{ required: "Source provider is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Source</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="Source provider" type="text" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="providerTo"
                            rules={{ required: "Destination provider is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Destination</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="Destination provider" type="text" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </div>
                </div>

                <div>
                    <h3 className="text-lg font-semibold mb-4">Relations</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <FormField
                            control={form.control}
                            name="relationFrom"
                            rules={{ required: "Source relation is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Source</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="Source relation" type="text" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="relationTo"
                            rules={{ required: "Destination relation is required." }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Destination</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="Destination relation" type="text" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </div>
                </div>

                <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Description</FormLabel>
                            <FormControl>
                                <Input {...field} placeholder="Description" type="text" />
                            </FormControl>
                            <FormDescription>More about this merge provider.</FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div>
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold">Overwritten Fields</h3>
                        <Dialog open={addFieldOpen} onOpenChange={setAddFieldOpen}>
                            <DialogTrigger asChild>
                                <GradientButton variant="outline" type="button">
                                    <PlusCircle className="h-4 w-4 mr-2" />
                                    Add field
                                </GradientButton>
                            </DialogTrigger>
                            <DialogContent>
                                <DialogHeader>
                                    <DialogTitle>Add overwritten field</DialogTitle>
                                    <DialogDescription>
                                        This source field will overwrite the destination field.
                                    </DialogDescription>
                                </DialogHeader>
                                <div className="space-y-4 py-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="field-name">Field Name</Label>
                                        <Input
                                            id="field-name"
                                            value={newFieldName}
                                            onChange={(e) => setNewFieldName(e.target.value)}
                                            placeholder="Field name"
                                            onKeyDown={(e) => {
                                                if (e.key === "Enter") {
                                                    e.preventDefault();
                                                    addOverwrittenField();
                                                }
                                            }}
                                        />
                                    </div>
                                </div>
                                <DialogFooter>
                                    <GradientButton type="button" onClick={addOverwrittenField}>
                                        Add
                                    </GradientButton>
                                </DialogFooter>
                            </DialogContent>
                        </Dialog>
                    </div>

                    {overwrittenFields.length > 0 && (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>
                                    <TableHead className="w-24 text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {overwrittenFields.map((field, index) => (
                                    <TableRow key={field.id ?? `new-${index}`}>
                                        <TableCell>{field.name}</TableCell>
                                        <TableCell className="text-right">
                                            <GradientButton
                                                variant="destructive"
                                                size="sm"
                                                type="button"
                                                onClick={() => removeOverwrittenField(index)}
                                            >
                                                <Trash2 className="h-4 w-4 mr-1" />
                                                Delete
                                            </GradientButton>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </div>

                <GradientButton type="submit">
                    {isNew ? "Create merge provider" : "Update merge provider"}
                </GradientButton>
            </form>
        </Form>
    );
};
