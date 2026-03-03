"use client"
import { ROUTES } from "@/app/routes.const"
import { DialogDelete } from "@/components/dialog.delete"
import { LanguageSelect } from "@/components/language-select"
import { Button } from "@/components/ui/button"
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

function normalizeItems(itemsToNormalize: TurSNSiteCustomFacetItem[]): TurSNSiteCustomFacetItem[] {
  return itemsToNormalize.map((item, index) => ({
    ...item,
    position: index + 1,
    rangeStart: item.rangeStart === null || item.rangeStart === undefined || item.rangeStart === ("" as any)
      ? null
      : Number(item.rangeStart),
    rangeEnd: item.rangeEnd === null || item.rangeEnd === undefined || item.rangeEnd === ("" as any)
      ? null
      : Number(item.rangeEnd),
  }));
}

type ItemRowProps = {
  item: TurSNSiteCustomFacetItem;
  index: number;
  onUpdate: (index: number, key: keyof TurSNSiteCustomFacetItem, value: string) => void;
  onRemove: (index: number) => void;
};

const ItemRow: React.FC<ItemRowProps> = ({ item, index, onUpdate, onRemove }) => {
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
          type="number"
          value={item.rangeStart ?? ""}
          onChange={(event) => onUpdate(index, "rangeStart", event.target.value)}
          placeholder="Start"
        />
      </TableCell>
      <TableCell>
        <Input
          type="number"
          value={item.rangeEnd ?? ""}
          onChange={(event) => onUpdate(index, "rangeEnd", event.target.value)}
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
  const navigate = useNavigate()
  const items = watch("items") ?? [];
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
        [key]: rawValue === "" ? null : Number(rawValue),
      };
    } else {
      updatedItems[index] = {
        ...updatedItems[index],
        [key]: rawValue,
      };
    }
    setValue("items", normalizeItems(updatedItems), { shouldDirty: true });
  }

  function onAddItem() {
    const updatedItems = [...items, { label: "", rangeStart: null, rangeEnd: null }];
    setValue("items", normalizeItems(updatedItems), { shouldDirty: true });
  }

  function onRemoveItem(index: number) {
    const updatedItems = items.filter((_, currentIndex) => currentIndex !== index);
    setValue("items", normalizeItems(updatedItems), { shouldDirty: true });
  }

  function onDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = items.findIndex((item, index) => (item.id || `item-${index}`) === active.id);
    const newIndex = items.findIndex((item, index) => (item.id || `item-${index}`) === over.id);
    if (oldIndex < 0 || newIndex < 0) return;
    const reorderedItems = arrayMove(items, oldIndex, newIndex);
    setValue("items", normalizeItems(reorderedItems), { shouldDirty: true });
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
    const payload: TurSNSiteCustomFacet = {
      ...customFacet,
      label: labelEntries
        .filter((entry) => entry.locale.trim() && entry.label.trim())
        .reduce<Record<string, string>>((labelsMap, entry) => {
          labelsMap[entry.locale.trim()] = entry.label;
          return labelsMap;
        }, {}),
      items: normalizeItems(customFacet.items ?? []),
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
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto max-w-md">
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
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
              <FormField
                control={control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Facet Name</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="e.g., price_range"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>Unique name used as the facet key.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="defaultLabel"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Default Label</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        value={field.value ?? ""}
                        placeholder="e.g., Price Range"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>Fallback label when no localized label matches the request locale.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-medium">Localized Labels</h3>
                  <Button type="button" variant="outline" onClick={addLabelEntry}>
                    <PlusCircle className="h-4 w-4 mr-2" />
                    Add Label
                  </Button>
                </div>
                <div className="space-y-2">
                  {labelEntries.map((entry, index) => (
                    <div key={`${entry.locale}-${index}`} className="flex items-center gap-2">
                      <LanguageSelect
                        value={entry.locale}
                        onValueChange={(value) => updateLabelEntry(index, "locale", value)}
                        locales={availableLocales}
                        extraLocaleValues={labelEntries.map((item) => item.locale)}
                      />
                      <Input
                        placeholder="Localized label"
                        value={entry.label}
                        onChange={(event) => updateLabelEntry(index, "label", event.target.value)}
                      />
                      <Button type="button" variant="ghost" size="icon" onClick={() => removeLabelEntry(index)}>
                        <Trash2 className="h-4 w-4 text-red-500" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>

              <FormField
                control={control}
                name="fieldExtId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Field Selection</FormLabel>
                    <FormControl>
                      <Select onValueChange={field.onChange} value={field.value || ""}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a field" />
                        </SelectTrigger>
                        <SelectContent>
                          {fieldOptions.map((fieldOption) => (
                            <SelectItem key={fieldOption.id} value={fieldOption.id}>
                              {fieldOption.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormDescription>The field used as the base for this custom facet.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-medium">Facet Items</h3>
                  <Button type="button" variant="outline" onClick={onAddItem}>Add Item</Button>
                </div>
                <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-10" />
                          <TableHead className="w-16">Pos</TableHead>
                          <TableHead>Label</TableHead>
                          <TableHead>Range Start</TableHead>
                          <TableHead>Range End</TableHead>
                          <TableHead className="w-24" />
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

              <GradientButton type="submit">Save</GradientButton>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
