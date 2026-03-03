"use client"
import { ROUTES } from "@/app/routes.const"
import { BadgeFieldType } from "@/components/badge-field-type"
import { DialogDelete } from "@/components/dialog.delete"
import { LanguageSelect } from "@/components/language-select"
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { TurLocale } from "@/models/locale/locale.model"
import type {
  TurSNSiteCustomFacet,
  TurSNSiteCustomFacetFieldOption,
  TurSNSiteCustomFacetItem,
} from "@/models/sn/sn-site-custom-facet.model"
import { TurLocaleService } from "@/services/locale/locale.service"
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service"
import {
  closestCenter,
  DndContext,
  type DragEndEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core'
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { GripVertical, PlusCircle, Trash2 } from 'lucide-react'
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { GradientButton } from "../../ui/gradient-button"

const turSNSiteCustomFacetService = new TurSNSiteCustomFacetService();
const turLocaleService = new TurLocaleService();

interface Props {
  snSiteId: string;
  value: TurSNSiteCustomFacet;
  isNew: boolean;
}

const FACET_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

const DATE_FIELD_TYPE = "DATE";

function isDateFieldType(fieldType?: string): boolean {
  return (fieldType ?? "").toUpperCase() === DATE_FIELD_TYPE;
}

function parseRangeValue(value: unknown): number | null {
  if (value === null || value === undefined || value === "") return null;
  const numericValue = Number.parseFloat(String(value).replace(",", "."));
  return Number.isNaN(numericValue) ? null : numericValue;
}

function parseIsoDateValue(value: unknown): string | null {
  if (value === null || value === undefined || value === "") return null;
  const parsedDate = new Date(String(value));
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate.toISOString();
}

function toDateTimeLocalValue(value: string | null | undefined): string {
  if (!value) return "";
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) return "";
  const timezoneOffsetMs = parsedDate.getTimezoneOffset() * 60 * 1000;
  return new Date(parsedDate.getTime() - timezoneOffsetMs).toISOString().slice(0, 16);
}

function normalizeItems(itemsToNormalize: TurSNSiteCustomFacetItem[], fieldType?: string): TurSNSiteCustomFacetItem[] {
  const dateField = isDateFieldType(fieldType);
  return itemsToNormalize.map((item, index) => ({
    ...item,
    position: index + 1,
    rangeStart: dateField ? null : parseRangeValue(item.rangeStart),
    rangeEnd: dateField ? null : parseRangeValue(item.rangeEnd),
    rangeStartDate: dateField ? parseIsoDateValue(item.rangeStartDate) : null,
    rangeEndDate: dateField ? parseIsoDateValue(item.rangeEndDate) : null,
  }));
}

function hasFilledRangeValues(itemsToCheck: TurSNSiteCustomFacetItem[]): boolean {
  return itemsToCheck.some((item) => {
    const hasNumberRange = item.rangeStart !== null && item.rangeStart !== undefined
      || item.rangeEnd !== null && item.rangeEnd !== undefined;
    const hasDateRange = Boolean(item.rangeStartDate) || Boolean(item.rangeEndDate);
    return hasNumberRange || hasDateRange;
  });
}

type ItemRowProps = {
  item: TurSNSiteCustomFacetItem;
  index: number;
  isDateField: boolean;
  onUpdate: (index: number, key: keyof TurSNSiteCustomFacetItem, value: string) => void;
  onRemove: (index: number) => void;
};

const ItemRow: React.FC<ItemRowProps> = ({ item, index, isDateField, onUpdate, onRemove }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id || `item-${index}` });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.8 : 1,
  };

  return (
    <TableRow ref={setNodeRef} style={style}>
      <TableCell className="w-10">
        <button {...attributes} {...listeners} className="p-2 cursor-grab active:cursor-grabbing">
          <GripVertical className="h-4 w-4 text-muted-foreground" />
        </button>
      </TableCell>
      <TableCell className="w-16">{index + 1}</TableCell>
      <TableCell>
        <Input
          value={item.label ?? ""}
          onChange={(event) => onUpdate(index, "label", event.target.value)}
          placeholder="Item label"
        />
      </TableCell>
      <TableCell>
        <Input
          type={isDateField ? "datetime-local" : "number"}
          step={isDateField ? undefined : "any"}
          value={isDateField ? toDateTimeLocalValue(item.rangeStartDate) : (item.rangeStart ?? "")}
          onChange={(event) => onUpdate(index, isDateField ? "rangeStartDate" : "rangeStart", event.target.value)}
          placeholder="Start"
        />
      </TableCell>
      <TableCell>
        <Input
          type={isDateField ? "datetime-local" : "number"}
          step={isDateField ? undefined : "any"}
          value={isDateField ? toDateTimeLocalValue(item.rangeEndDate) : (item.rangeEnd ?? "")}
          onChange={(event) => onUpdate(index, isDateField ? "rangeEndDate" : "rangeEnd", event.target.value)}
          placeholder="End"
        />
      </TableCell>
      <TableCell className="w-24 text-right">
        <Button type="button" variant="outline" onClick={() => onRemove(index)}>Remove</Button>
      </TableCell>
    </TableRow>
  );
};

export const SNSiteCustomFacetForm: React.FC<Props> = ({ snSiteId, value, isNew }) => {
  const form = useForm<TurSNSiteCustomFacet>({
    defaultValues: value
  });
  const { control, watch, setValue } = form;
  const [open, setOpen] = useState(false);
  const [fieldOptions, setFieldOptions] = useState<TurSNSiteCustomFacetFieldOption[]>([]);
  const [availableLocales, setAvailableLocales] = useState<TurLocale[]>([]);
  const [labelEntries, setLabelEntries] = useState<Array<{ locale: string; label: string }>>([]);
  const [openFieldChangeDialog, setOpenFieldChangeDialog] = useState(false);
  const [pendingFieldExtId, setPendingFieldExtId] = useState<string | null>(null);
  const navigate = useNavigate()
  const items = watch("items") ?? [];
  const selectedFieldExtId = watch("fieldExtId") ?? value.fieldExtId;
  const selectedFieldType = fieldOptions.find((field) => field.id === selectedFieldExtId)?.type ?? value.fieldExtType;
  const dateField = isDateFieldType(selectedFieldType);
  const sensors = useSensors(useSensor(PointerSensor));

  useEffect(() => {
    form.reset(value);
    const entries = Object.entries(value.label ?? {}).map(([locale, label]) => ({
      locale,
      label,
    }));
    setLabelEntries(entries);
  }, [value, form])

  useEffect(() => {
    turSNSiteCustomFacetService.getFieldOptions(snSiteId).then(setFieldOptions);
  }, [snSiteId])

  useEffect(() => {
    turLocaleService.query().then(setAvailableLocales);
  }, [])

  function onItemUpdate(index: number, key: keyof TurSNSiteCustomFacetItem, rawValue: string) {
    const updatedItems = [...items];
    if (key === "rangeStart" || key === "rangeEnd") {
      updatedItems[index] = {
        ...updatedItems[index],
        [key]: parseRangeValue(rawValue),
      };
    } else if (key === "rangeStartDate" || key === "rangeEndDate") {
      updatedItems[index] = {
        ...updatedItems[index],
        [key]: parseIsoDateValue(rawValue),
      };
    } else {
      updatedItems[index] = {
        ...updatedItems[index],
        [key]: rawValue,
      };
    }
    setValue("items", normalizeItems(updatedItems, selectedFieldType), { shouldDirty: true });
  }

  function onAddItem() {
    const updatedItems = [...items, { label: "", rangeStart: null, rangeEnd: null, rangeStartDate: null, rangeEndDate: null }];
    setValue("items", normalizeItems(updatedItems, selectedFieldType), { shouldDirty: true });
  }

  function onRemoveItem(index: number) {
    const updatedItems = items.filter((_, currentIndex) => currentIndex !== index);
    setValue("items", normalizeItems(updatedItems, selectedFieldType), { shouldDirty: true });
  }

  function onDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = items.findIndex((item, index) => (item.id || `item-${index}`) === active.id);
    const newIndex = items.findIndex((item, index) => (item.id || `item-${index}`) === over.id);
    if (oldIndex < 0 || newIndex < 0) return;
    const reorderedItems = arrayMove(items, oldIndex, newIndex);
    setValue("items", normalizeItems(reorderedItems, selectedFieldType), { shouldDirty: true });
  }

  function applyFieldChange(newFieldExtId: string) {
    const newFieldType = fieldOptions.find((field) => field.id === newFieldExtId)?.type;
    setValue("fieldExtId", newFieldExtId, { shouldDirty: true });
    setValue("items", normalizeItems(items, newFieldType), { shouldDirty: true });
  }

  function onFieldChange(newFieldExtId: string) {
    if (newFieldExtId === selectedFieldExtId) {
      return;
    }

    if (hasFilledRangeValues(items)) {
      setPendingFieldExtId(newFieldExtId);
      setOpenFieldChangeDialog(true);
      return;
    }

    applyFieldChange(newFieldExtId);
  }

  function onConfirmFieldChange() {
    if (pendingFieldExtId) {
      applyFieldChange(pendingFieldExtId);
    }
    setPendingFieldExtId(null);
    setOpenFieldChangeDialog(false);
  }

  function onCancelFieldChange() {
    setPendingFieldExtId(null);
    setOpenFieldChangeDialog(false);
  }

  function addLabelEntry() {
    setLabelEntries((currentEntries) => [...currentEntries, { locale: "", label: "" }]);
  }

  function removeLabelEntry(index: number) {
    setLabelEntries((currentEntries) => currentEntries.filter((_, currentIndex) => currentIndex !== index));
  }

  function updateLabelEntry(index: number, key: "locale" | "label", inputValue: string) {
    setLabelEntries((currentEntries) => currentEntries.map((entry, currentIndex) => (
      currentIndex === index
        ? { ...entry, [key]: inputValue }
        : entry
    )));
  }

  async function onSubmit(customFacet: TurSNSiteCustomFacet) {
    if (!FACET_NAME_PATTERN.test(customFacet.name ?? "")) {
      toast.error("Facet Name must contain only letters, numbers, underscore (_) or hyphen (-).");
      return;
    }

    const invalidRange = (customFacet.items ?? []).find((item) => {
      if (dateField) {
        const start = parseIsoDateValue(item.rangeStartDate);
        const end = parseIsoDateValue(item.rangeEndDate);
        if (!start || !end) return false;
        return new Date(end).getTime() < new Date(start).getTime();
      }
      const start = parseRangeValue(item.rangeStart);
      const end = parseRangeValue(item.rangeEnd);
      return start !== null && end !== null && end < start;
    });

    if (invalidRange) {
      toast.error(`Invalid range on item "${invalidRange.label || "(without label)"}": Range End must be greater than or equal to Range Start.`);
      return;
    }

    const payload: TurSNSiteCustomFacet = {
      ...customFacet,
      label: labelEntries
        .filter((entry) => entry.locale.trim() && entry.label.trim())
        .reduce<Record<string, string>>((labelsMap, entry) => {
          labelsMap[entry.locale.trim()] = entry.label;
          return labelsMap;
        }, {}),
      items: normalizeItems(customFacet.items ?? [], selectedFieldType),
    };

    try {
      if (isNew) {
        const result = await turSNSiteCustomFacetService.create(snSiteId, payload);
        if (result) {
          toast.success(`The ${payload.name} Custom Facet was saved`);
          navigate(`${ROUTES.SN_INSTANCE}/${snSiteId}/custom-facet`);
        } else {
          toast.error(`The ${payload.name} Custom Facet was not saved`);
        }
      }
      else {
        const result = await turSNSiteCustomFacetService.update(snSiteId, payload);
        if (result) {
          toast.success(`The ${payload.name} Custom Facet was updated`);
        } else {
          toast.error(`The ${payload.name} Custom Facet was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";
      toast.error(`Failed to submit the form: ${errorMessage}`);
    }
  }

  async function onDelete() {
    try {
      if (value.id && await turSNSiteCustomFacetService.delete(snSiteId, value.id)) {
        toast.success(`The ${value.name} Custom Facet was deleted`);
        navigate(`${ROUTES.SN_INSTANCE}/${snSiteId}/custom-facet`);
      }
      else {
        toast.error(`The ${value.name} Custom Facet was not deleted`);
      }

    } catch (error) {
      console.error("Form deletion error", error);
      const errorMessage = error instanceof Error ? error.message : "Unknown error occurred";
      toast.error(`Failed to delete: ${errorMessage}`);
    }
    setOpen(false);
  }

  return (
    <div className="flex h-full w-full items-center justify-center px-4">
      <Dialog open={openFieldChangeDialog} onOpenChange={setOpenFieldChangeDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Change field?</DialogTitle>
            <DialogDescription>
              There are range values already filled in this custom facet. If you continue, current range values will be lost.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCancelFieldChange}>
              Cancel
            </Button>
            <Button type="button" onClick={onConfirmFieldChange}>
              Continue
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Custom Facet</CardTitle>
          <CardAction>
            {!isNew && <DialogDelete feature="Custom Facet" name={value.name} onDelete={onDelete} open={open} setOpen={setOpen} />}
          </CardAction>
          <CardDescription>
            Custom facet settings for creating facets with specific ranges.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 mx-auto py-10">
              <Accordion type="multiple" defaultValue={["basic-info", "labels", "field-config", "facet-items"]} className="space-y-4">
                {/* Basic Information Section */}
                <AccordionItem value="basic-info" className="border rounded-lg px-6">
                  <AccordionTrigger className="text-lg font-semibold">Basic Information</AccordionTrigger>
                  <AccordionContent className="space-y-6 pt-4">
                    <FormField
                      control={control}
                      name="name"
                      rules={{
                        required: "Please enter a facet name.",
                        pattern: {
                          value: FACET_NAME_PATTERN,
                          message: "Use only letters, numbers, underscores (_), or hyphens (-).",
                        },
                      }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-base">Facet Name</FormLabel>
                          <FormDescription className="text-sm mb-2">A unique identifier for this facet. Use lowercase letters, numbers, underscores, or hyphens.</FormDescription>
                          <FormControl>
                            <Input
                              {...field}
                              placeholder="e.g., price_range"
                              type="text"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={control}
                      name="defaultLabel"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-base">Default Label</FormLabel>
                          <FormDescription className="text-sm mb-2">The display name shown when no language-specific label is available.</FormDescription>
                          <FormControl>
                            <Input
                              {...field}
                              value={field.value ?? ""}
                              placeholder="e.g., Price Range"
                              type="text"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </AccordionContent>
                </AccordionItem>

                {/* Localized Labels Section */}
                <AccordionItem value="labels" className="border rounded-lg px-6">
                  <AccordionTrigger className="text-lg font-semibold">Language-Specific Labels</AccordionTrigger>
                  <AccordionContent className="space-y-6 pt-4">
                    <div>
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <h3 className="text-base font-medium">Add Translations</h3>
                          <p className="text-sm text-muted-foreground">Customize labels for different languages and regions.</p>
                        </div>
                        <Button type="button" variant="outline" onClick={addLabelEntry}>
                          <PlusCircle className="h-4 w-4 mr-2" />
                          Add Label
                        </Button>
                      </div>
                      <div className="space-y-3">
                        {labelEntries.map((entry, index) => (
                          <div key={`${entry.locale}-${index}`} className="flex items-center gap-2">
                            <LanguageSelect
                              value={entry.locale}
                              onValueChange={(value) => updateLabelEntry(index, "locale", value)}
                              locales={availableLocales}
                              extraLocaleValues={labelEntries.map((item) => item.locale)}
                            />
                            <Input
                              placeholder="Enter translated label"
                              value={entry.label}
                              onChange={(event) => updateLabelEntry(index, "label", event.target.value)}
                              className="flex-1"
                            />
                            <Button type="button" variant="ghost" size="icon" onClick={() => removeLabelEntry(index)}>
                              <Trash2 className="h-4 w-4 text-red-500" />
                            </Button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </AccordionContent>
                </AccordionItem>

                {/* Field Configuration Section */}
                <AccordionItem value="field-config" className="border rounded-lg px-6">
                  <AccordionTrigger className="text-lg font-semibold">Field Configuration</AccordionTrigger>
                  <AccordionContent className="space-y-6 pt-4">
                    <FormField
                      control={control}
                      name="fieldExtId"
                      render={({ field }) => (
                        <FormItem>
                          <div className="flex gap-4">
                            <div className="flex-1 flex flex-col justify-start">
                              <FormLabel className="text-base">Select a Field</FormLabel>
                              <FormDescription className="text-sm mt-1">Choose the field that will serve as the foundation for this custom facet.</FormDescription>
                            </div>
                            <div className="flex-1">
                              <FormControl>
                                <Select onValueChange={onFieldChange} value={field.value || ""}>
                                  <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select a field" />
                                  </SelectTrigger>
                                  <SelectContent>
                                    {fieldOptions.map((fieldOption) => (
                                      <SelectItem key={fieldOption.id} value={fieldOption.id}>
                                        <div className="flex w-full items-center justify-between gap-3">
                                          <BadgeFieldType type={fieldOption.type} variation="short" />
                                          <span>{fieldOption.name}</span>
                                        </div>
                                      </SelectItem>
                                    ))}
                                  </SelectContent>
                                </Select>
                              </FormControl>
                            </div>
                          </div>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </AccordionContent>
                </AccordionItem>

                {/* Facet Items Section */}
                <AccordionItem value="facet-items" className="border rounded-lg px-6">
                  <AccordionTrigger className="text-lg font-semibold">Facet Range Items</AccordionTrigger>
                  <AccordionContent className="space-y-6 pt-4">
                    <div>
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <h3 className="text-base font-medium">Range Definitions</h3>
                          <p className="text-sm text-muted-foreground">Create custom ranges by defining start and end values. Drag to reorder.</p>
                        </div>
                        <Button type="button" variant="outline" onClick={onAddItem}>
                          <PlusCircle className="h-4 w-4 mr-2" />
                          Add Item
                        </Button>
                      </div>
                      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
                        <div className="rounded-md border overflow-hidden">
                          <Table>
                            <TableHeader>
                              <TableRow className="bg-muted/50">
                                <TableHead className="w-10" />
                                <TableHead className="w-16">Position</TableHead>
                                <TableHead>Label</TableHead>
                                <TableHead>Range Start</TableHead>
                                <TableHead>Range End</TableHead>
                                <TableHead className="w-24 text-right" />
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              <SortableContext
                                items={items.map((item, index) => item.id || `item-${index}`)}
                                strategy={verticalListSortingStrategy}
                              >
                                {items.map((item, index) => (
                                  <ItemRow
                                    key={item.id || `item-${index}`}
                                    item={item}
                                    index={index}
                                    isDateField={dateField}
                                    onUpdate={onItemUpdate}
                                    onRemove={onRemoveItem}
                                  />
                                ))}
                              </SortableContext>
                            </TableBody>
                          </Table>
                        </div>
                      </DndContext>
                    </div>
                  </AccordionContent>
                </AccordionItem>
              </Accordion>

              {/* Action Footer */}
              <div className="flex items-center justify-end gap-3 pt-6 border-t">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate(`${ROUTES.SN_INSTANCE}/${snSiteId}/custom-facet`)}
                >
                  Cancel
                </Button>
                <GradientButton type="submit">
                  {isNew ? "Create Facet" : "Save Changes"}
                </GradientButton>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
