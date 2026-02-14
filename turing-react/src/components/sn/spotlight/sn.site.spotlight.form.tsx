"use client"
import { ROUTES } from "@/app/routes.const"
import {
    Dialog,
    DialogContent,
    DialogDescription,
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model"
import type { TurSNSiteSpotlightDocument } from "@/models/sn/sn-site-spotlight-document.model"
import type { TurSNSiteSpotlightTerm } from "@/models/sn/sn-site-spotlight-term.model"
import type { TurSNSiteSpotlight } from "@/models/sn/sn-site-spotlight.model"
import type { TurSNSearchDocument } from "@/search/models/sn-search-document.model"
import type { TurSNSearch } from "@/search/models/sn-search.model"
import { TurSNSearchService } from "@/search/services/sn-search.service"
import { TurSNSiteLocaleService } from "@/services/sn/sn.site.locale.service"
import { TurSNSiteSpotlightService } from "@/services/sn/sn.site.spotlight.service"
import { PlusCircle, Search, Trash2 } from "lucide-react"
import React, { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"

const turSNSiteSpotlightService = new TurSNSiteSpotlightService();
const turSNSiteLocaleService = new TurSNSiteLocaleService();

interface Props {
    snSiteId: string;
    value: TurSNSiteSpotlight;
    isNew: boolean;
}

export const SNSiteSpotlightForm: React.FC<Props> = ({ snSiteId, value, isNew }) => {
    const form = useForm<TurSNSiteSpotlight>({
        defaultValues: value,
    });
    const navigate = useNavigate();
    const urlBase = `${ROUTES.SN_INSTANCE}/${snSiteId}/spotlight`;
    const [locales, setLocales] = useState<TurSNSiteLocale[]>([]);
    const [searchDialogOpen, setSearchDialogOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const [searchResult, setSearchResult] = useState<TurSNSearch | null>(null);
    const [termsError, setTermsError] = useState<string | null>(null);
    const [documentsError, setDocumentsError] = useState<string | null>(null);

    useEffect(() => {
        turSNSiteLocaleService.query(snSiteId).then(setLocales);
    }, [snSiteId]);

    useEffect(() => {
        form.reset(value);
    }, [value]);

    async function onSubmit(data: TurSNSiteSpotlight) {
        let hasError = false;
        if (!data.turSNSiteSpotlightTerms || data.turSNSiteSpotlightTerms.length === 0) {
            setTermsError("At least one term is required.");
            hasError = true;
        } else {
            setTermsError(null);
        }
        if (!data.turSNSiteSpotlightDocuments || data.turSNSiteSpotlightDocuments.length === 0) {
            setDocumentsError("At least one document is required.");
            hasError = true;
        } else {
            setDocumentsError(null);
        }
        if (hasError) return;
        try {
            if (isNew) {
                const result = await turSNSiteSpotlightService.create(data);
                if (result) {
                    toast.success(`The ${data.name} spotlight was created`);
                    navigate(urlBase);
                } else {
                    toast.error("Failed to create the spotlight. Please try again.");
                }
            } else {
                const result = await turSNSiteSpotlightService.update(data);
                if (result) {
                    toast.success(`The ${data.name} spotlight was updated`);
                } else {
                    toast.error("Failed to update the spotlight. Please try again.");
                }
            }
        } catch (error) {
            console.error("Form submission error", error);
            toast.error("Failed to submit the form. Please try again.");
        }
    }

    function addTerm() {
        const currentTerms = form.getValues("turSNSiteSpotlightTerms") || [];
        const newTerm: TurSNSiteSpotlightTerm = { name: "" };
        form.setValue("turSNSiteSpotlightTerms", [...currentTerms, newTerm]);
        setTermsError(null);
    }

    function removeTerm(index: number) {
        const currentTerms = form.getValues("turSNSiteSpotlightTerms") || [];
        form.setValue(
            "turSNSiteSpotlightTerms",
            currentTerms.filter((_, i) => i !== index)
        );
    }

    function removeDocument(index: number) {
        const currentDocs = form.getValues("turSNSiteSpotlightDocuments") || [];
        form.setValue(
            "turSNSiteSpotlightDocuments",
            currentDocs.filter((_, i) => i !== index)
        );
    }

    function updateTermName(index: number, name: string) {
        const currentTerms = form.getValues("turSNSiteSpotlightTerms") || [];
        const updated = [...currentTerms];
        updated[index] = { ...updated[index], name };
        form.setValue("turSNSiteSpotlightTerms", updated);
    }

    function updateDocumentPosition(index: number, position: number) {
        const currentDocs = form.getValues("turSNSiteSpotlightDocuments") || [];
        const updated = [...currentDocs];
        updated[index] = { ...updated[index], position };
        form.setValue("turSNSiteSpotlightDocuments", updated);
    }

    async function searchDocument(page: number) {
        const siteName = value.turSNSite?.name;
        const language = form.getValues("language");
        if (!siteName || !searchQuery) return;
        try {
            const result = await TurSNSearchService.query(
                siteName,
                {
                    q: searchQuery,
                    p: page.toString(),
                    _setlocale: language || "en-US",
                    sort: "title:desc",
                    fq: [],
                    tr: [],
                    nfpr: "",
                }
            );
            setSearchResult(result);
        } catch (error) {
            console.error("Search error", error);
            toast.error("Failed to search documents.");
        }
    }

    function addDocument(searchDoc: TurSNSearchDocument) {
        const currentDocs = form.getValues("turSNSiteSpotlightDocuments") || [];
        const newDoc: TurSNSiteSpotlightDocument = {
            title: searchDoc.fields.title || "",
            type: searchDoc.fields.type || "",
            position: 1,
            link: searchDoc.fields.url || "",
            referenceId: searchDoc.fields.id || "",
        };
        form.setValue("turSNSiteSpotlightDocuments", [...currentDocs, newDoc]);
        setDocumentsError(null);
        setSearchDialogOpen(false);
        setSearchQuery("");
        setSearchResult(null);
    }

    const terms = form.watch("turSNSiteSpotlightTerms") || [];
    const documents = form.watch("turSNSiteSpotlightDocuments") || [];
    const selectedLanguage = form.watch("language");

    function handleFormSubmit(e: React.BaseSyntheticEvent) {
        const currentTerms = form.getValues("turSNSiteSpotlightTerms") || [];
        if (currentTerms.length === 0) {
            setTermsError("At least one term is required.");
        } else {
            setTermsError(null);
        }
        if (selectedLanguage) {
            const currentDocs = form.getValues("turSNSiteSpotlightDocuments") || [];
            if (currentDocs.length === 0) {
                setDocumentsError("At least one document is required.");
            } else {
                setDocumentsError(null);
            }
        }
        form.handleSubmit(onSubmit)(e);
    }

    return (
        <Form {...form}>
            <form onSubmit={handleFormSubmit} className="space-y-8 py-8 px-6">
                <FormField
                    control={form.control}
                    name="name"
                    rules={{ required: "Name is required." }}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>
                            <FormControl>
                                <Input {...field} placeholder="Name" type="text" />
                            </FormControl>
                            <FormDescription>Name will appear on spotlight list.</FormDescription>
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
                                <Input {...field} placeholder="Description" type="text" />
                            </FormControl>
                            <FormDescription>Description will appear on spotlight list.</FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="language"
                    rules={{ required: "Language is required." }}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Language</FormLabel>
                            <Select onValueChange={field.onChange} value={field.value || ""}>
                                <FormControl>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="Select a language" />
                                    </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                    {locales.map((locale) => (
                                        <SelectItem key={locale.id} value={locale.language}>
                                            {locale.language}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <FormDescription>Language of semantic navigation site.</FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div>
                    <h3 className="text-lg font-semibold mb-2">When searching for the following terms, ...</h3>
                    <p className="text-sm text-muted-foreground mb-4">
                        If any of these terms are searched for, this will trigger documents to display as spotlights.
                    </p>

                    <div className="space-y-2">
                        {terms.map((term, index) => (
                            <div key={term.id || `term-${index}`} className="flex items-center gap-1.5">
                                <Input
                                    value={term.name}
                                    onChange={(e) => updateTermName(index, e.target.value)}
                                    placeholder="Search term"
                                    className="grow"
                                />
                                <GradientButton
                                    variant="ghost"
                                    size="icon"
                                    type="button"
                                    onClick={() => removeTerm(index)}
                                    aria-label="Remove term"
                                >
                                    <Trash2 className="h-4 w-4 text-red-500" />
                                </GradientButton>
                            </div>
                        ))}
                    </div>

                    <div className="mt-4">
                        <GradientButton variant="outline" type="button" onClick={addTerm}>
                            <PlusCircle className="h-4 w-4 mr-2" />
                            Add
                        </GradientButton>
                    </div>
                    {termsError && (
                        <p className="text-sm font-medium text-destructive mt-2">{termsError}</p>
                    )}
                </div>

                {selectedLanguage && (
                    <div>
                        <h3 className="text-lg font-semibold mb-2">... then will be showing the following documents as spotlight.</h3>
                        <p className="text-sm text-muted-foreground mb-4">
                            These documents will display as spotlights when there are search terms.
                        </p>

                        <div className="mb-4">
                            <Dialog open={searchDialogOpen} onOpenChange={(open) => {
                                setSearchDialogOpen(open);
                                if (!open) {
                                    setSearchQuery("");
                                    setSearchResult(null);
                                }
                            }}>
                                <DialogTrigger asChild>
                                    <GradientButton variant="outline" type="button">
                                        <PlusCircle className="h-4 w-4 mr-2" />
                                        Add Document
                                    </GradientButton>
                                </DialogTrigger>
                                <DialogContent className="sm:max-w-2xl">
                                    <DialogHeader>
                                        <DialogTitle>Search Document</DialogTitle>
                                        <DialogDescription>
                                            Search for documents to add as spotlight.
                                        </DialogDescription>
                                    </DialogHeader>
                                    <div className="flex items-center gap-2">
                                        <Input
                                            placeholder="Search..."
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            onKeyDown={(e) => {
                                                if (e.key === "Enter") {
                                                    e.preventDefault();
                                                    searchDocument(1);
                                                }
                                            }}
                                            className="grow"
                                        />
                                        <GradientButton
                                            type="button"
                                            variant="outline"
                                            size="icon"
                                            onClick={() => searchDocument(1)}
                                        >
                                            <Search className="h-4 w-4" />
                                        </GradientButton>
                                    </div>
                                    {searchResult?.results?.document && searchResult.results.document.length > 0 && (
                                        <div className="max-h-80 overflow-y-auto">
                                            <Table>
                                                <TableHeader>
                                                    <TableRow>
                                                        <TableHead>Title</TableHead>
                                                        <TableHead>Type</TableHead>
                                                    </TableRow>
                                                </TableHeader>
                                                <TableBody>
                                                    {searchResult.results.document.map((searchDoc, index) => (
                                                        <TableRow
                                                            key={searchDoc.fields.id || `search-${index}`}
                                                            className="cursor-pointer"
                                                            onClick={() => addDocument(searchDoc)}
                                                        >
                                                            <TableCell>{searchDoc.fields.title}</TableCell>
                                                            <TableCell>{searchDoc.fields.type}</TableCell>
                                                        </TableRow>
                                                    ))}
                                                </TableBody>
                                            </Table>
                                        </div>
                                    )}
                                    {searchResult?.pagination && searchResult.pagination.length > 0 && (
                                        <div className="flex items-center gap-1 pt-2">
                                            {searchResult.pagination.map((page) => (
                                                <GradientButton
                                                    key={page.page}
                                                    type="button"
                                                    variant={page.type === "current" ? "default" : "outline"}
                                                    size="sm"
                                                    onClick={() => searchDocument(page.page)}
                                                >
                                                    {page.text}
                                                </GradientButton>
                                            ))}
                                        </div>
                                    )}
                                </DialogContent>
                            </Dialog>
                        </div>

                        {documents.length > 0 && (
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="w-24">Position</TableHead>
                                        <TableHead>Title</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead className="w-24 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {documents.map((doc, index) => (
                                        <TableRow key={doc.id || `doc-${index}`}>
                                            <TableCell>
                                                <Input
                                                    type="number"
                                                    value={doc.position}
                                                    onChange={(e) => updateDocumentPosition(index, Number(e.target.value))}
                                                    className="w-20"
                                                />
                                            </TableCell>
                                            <TableCell>{doc.title}</TableCell>
                                            <TableCell>{doc.type}</TableCell>
                                            <TableCell className="text-right">
                                                <GradientButton
                                                    variant="destructive"
                                                    size="sm"
                                                    type="button"
                                                    onClick={() => removeDocument(index)}
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
                        {documentsError && (
                            <p className="text-sm font-medium text-destructive mt-2">{documentsError}</p>
                        )}
                    </div>
                )}

                <GradientButton type="submit">
                    {isNew ? "Create spotlight" : "Update spotlight"}
                </GradientButton>
            </form>
        </Form>
    );
};
