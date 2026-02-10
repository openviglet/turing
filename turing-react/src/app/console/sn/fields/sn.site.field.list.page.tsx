import {
    type ColumnDef,
    type ColumnFiltersState,
    flexRender,
    getCoreRowModel,
    getFilteredRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    type SortingState,
    useReactTable,
    type VisibilityState,
} from "@tanstack/react-table"
import {
    AlertTriangle,
    ArrowUpDown,
    CheckCircle2,
    ChevronDown,
    MoreHorizontal,
} from "lucide-react"
import * as React from "react"

import { ROUTES } from "@/app/routes.const"
import { SubPageHeader } from "@/components/sub.page.header"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import type { TurSNFieldCheck } from "@/models/sn/sn-field-check.model.ts"
import type { TurSNStatusFields } from "@/models/sn/sn-field-status.model.ts"
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model.ts"
import { TurSNFieldService } from "@/services/sn/sn.field.service"
import { IconAlignBoxCenterStretch, IconColumns3Filled } from "@tabler/icons-react"
import { useParams } from "react-router-dom"

type StatusFieldDropdownProps = {
    statusField?: TurSNFieldCheck;
};

function StatusFieldDropdown({ statusField }: Readonly<StatusFieldDropdownProps>) {
    if (!statusField) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-6 w-6 p-0">
                    <span className="sr-only">Open field status</span>
                    {statusField.correct ? (
                        <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                    ) : (
                        <AlertTriangle className="h-4 w-4 text-rose-500" />
                    )}
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-80">
                {statusField.correct
                    ? statusField.cores.map((core) => (
                        <DropdownMenuItem
                            key={core.name}
                            className="flex items-center gap-2"
                        >
                            <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                            <span>{core.name}</span>
                        </DropdownMenuItem>
                    ))
                    : statusField.cores.map((core) => (
                        <div key={core.name} className="py-1">
                            {core.correct ? (
                                <DropdownMenuItem className="flex items-center gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                                    <span>{core.name}</span>
                                </DropdownMenuItem>
                            ) : (
                                <>
                                    <div className="flex items-center justify-between px-2 py-1 text-xs font-semibold text-muted-foreground">
                                        <span>{core.name}</span>
                                        <button
                                            type="button"
                                            className="text-xs text-primary hover:underline"
                                        >
                                            Repair All
                                        </button>
                                    </div>
                                    {!core.exists && (
                                        <div className="flex items-center justify-between px-2 py-1 text-sm">
                                            <div className="flex items-center gap-2 text-rose-600">
                                                <AlertTriangle className="h-4 w-4" />
                                                <span>Missing field</span>
                                            </div>
                                            <button
                                                type="button"
                                                className="text-xs text-primary hover:underline"
                                            >
                                                Repair
                                            </button>
                                        </div>
                                    )}
                                    {core.exists && !statusField.facetIsCorrect && (
                                        <div className="flex items-center justify-between px-2 py-1 text-sm">
                                            <div className="flex items-center gap-2 text-rose-600">
                                                <AlertTriangle className="h-4 w-4" />
                                                <span>Facet Type is incorrect</span>
                                            </div>
                                            <button
                                                type="button"
                                                className="text-xs text-primary hover:underline"
                                            >
                                                Repair
                                            </button>
                                        </div>
                                    )}
                                    {core.exists && !core.multiValuedIsCorrect && (
                                        <div className="flex items-center justify-between px-2 py-1 text-sm">
                                            <div className="flex items-center gap-2 text-rose-600">
                                                <AlertTriangle className="h-4 w-4" />
                                                <span>SE MultiValued isn't configured</span>
                                            </div>
                                            <button
                                                type="button"
                                                className="text-xs text-primary hover:underline"
                                            >
                                                Repair
                                            </button>
                                        </div>
                                    )}
                                    {core.exists &&
                                        statusField.facetIsCorrect &&
                                        !core.typeIsCorrect && (
                                            <div className="flex items-center justify-between px-2 py-1 text-sm">
                                                <div className="flex items-center gap-2 text-rose-600">
                                                    <AlertTriangle className="h-4 w-4" />
                                                    <span>Using {core.type}</span>
                                                </div>
                                                <button
                                                    type="button"
                                                    className="text-xs text-primary hover:underline"
                                                >
                                                    Repair
                                                </button>
                                            </div>
                                        )}
                                </>
                            )}
                        </div>
                    ))}
            </DropdownMenuContent>
        </DropdownMenu>
    )
}

type FieldToggleKey = "enabled" | "mlt" | "facet" | "hl";

const buildColumns = (
    statusFieldMap: Map<string, TurSNFieldCheck>,
    onToggle: (fieldId: string, key: FieldToggleKey, checked: boolean) => void,
    isSavingField: (fieldId: string) => boolean
): ColumnDef<TurSNSiteField>[] => [
        {
            id: "select",
            header: ({ table }) => (
                <Checkbox
                    checked={
                        table.getIsAllPageRowsSelected() ||
                        (table.getIsSomePageRowsSelected() && "indeterminate")
                    }
                    onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
                    aria-label="Select all"
                />
            ),
            cell: ({ row }) => (
                <Checkbox
                    checked={row.getIsSelected()}
                    onCheckedChange={(value) => row.toggleSelected(!!value)}
                    aria-label="Select row"
                />
            ),
            enableSorting: false,
            enableHiding: false,
        },
        {
            accessorKey: "name",
            header: ({ column }) => {
                return (
                    <Button
                        variant="ghost"
                        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
                    >
                        Field
                        <ArrowUpDown />
                    </Button>
                )
            },
            cell: ({ row }) => {
                const statusField = statusFieldMap.get(row.original.id);
                return (
                    <div className="flex items-center gap-2">
                        <StatusFieldDropdown statusField={statusField} />
                        <div className="text-left font-medium">{row.getValue("name")}</div>
                    </div>
                )
            },
        },
        {
            accessorKey: "enabled",
            header: () => <div className="text-right">Enabled</div>,
            cell: ({ row }) => {
                const enabled = row.original.enabled;
                const fieldId = row.original.id;
                return (
                    <div className="text-right font-medium">
                        <Switch
                            checked={enabled == 1}
                            onCheckedChange={(checked) =>
                                onToggle(fieldId, "enabled", checked)
                            }
                            disabled={isSavingField(fieldId)}
                        />
                    </div>
                )
            },
        },
        {
            accessorKey: "mlt",
            header: () => <div className="text-right">MLT</div>,
            cell: ({ row }) => {
                const mlt = row.original.mlt;
                const fieldId = row.original.id;
                return (
                    <div className="text-right font-medium">
                        <Switch
                            checked={mlt == 1}
                            onCheckedChange={(checked) =>
                                onToggle(fieldId, "mlt", checked)
                            }
                            disabled={isSavingField(fieldId)}
                        />
                    </div>
                )
            },
        },
        {
            accessorKey: "facet",
            header: () => <div className="text-right">Facet</div>,
            cell: ({ row }) => {
                const facet = row.original.facet;
                const fieldId = row.original.id;
                return (
                    <div className="text-right font-medium">
                        <Switch
                            checked={facet == 1}
                            onCheckedChange={(checked) =>
                                onToggle(fieldId, "facet", checked)
                            }
                            disabled={isSavingField(fieldId)}
                        />
                    </div>
                )
            },
        },
        {
            accessorKey: "hl",
            header: () => <div className="text-right">Highlighting</div>,
            cell: ({ row }) => {
                const hl = row.original.hl;
                const fieldId = row.original.id;
                return (
                    <div className="text-right font-medium">
                        <Switch
                            checked={hl == 1}
                            onCheckedChange={(checked) =>
                                onToggle(fieldId, "hl", checked)
                            }
                            disabled={isSavingField(fieldId)}
                        />
                    </div>
                )
            },
        },
        {
            id: "actions",
            enableHiding: false,
            cell: ({ row }) => {
                return (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                                <span className="sr-only">Open menu</span>
                                <MoreHorizontal />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            <DropdownMenuLabel>Actions</DropdownMenuLabel>
                            <DropdownMenuItem>
                                <a href={"field/" + row.original.id}>Edit</a>
                            </DropdownMenuItem>
                            <DropdownMenuItem>Delete</DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                )
            },
        },
    ]

const turSNFieldService = new TurSNFieldService();
export default function SNSiteFieldListPage() {
    const { id } = useParams() as { id: string };
    const [data, setSnField] = React.useState<TurSNSiteField[]>([]);
    const [statusFields, setStatusFields] = React.useState<TurSNStatusFields | null>(
        null
    );
    const [sorting, setSorting] = React.useState<SortingState>([])
    const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>(
        []
    )
    const [columnVisibility, setColumnVisibility] =
        React.useState<VisibilityState>({})
    const [rowSelection, setRowSelection] = React.useState({})
    const [savingFieldIds, setSavingFieldIds] = React.useState<Set<string>>(
        new Set()
    );

    React.useEffect(() => {
        setStatusFields(null);
        turSNFieldService.query(id).then(setSnField);
        turSNFieldService
            .getStatusFields(id)
            .then(setStatusFields)
            .catch((error) => {
                console.error("Failed to load SN field status", error);
            });
    }, [id])

    const statusFieldMap = React.useMemo(() => {
        const map = new Map<string, TurSNFieldCheck>();
        statusFields?.fields?.forEach((field) => {
            map.set(field.id, field);
        });
        return map;
    }, [statusFields]);

    const setFieldSaving = React.useCallback((fieldId: string, saving: boolean) => {
        setSavingFieldIds((prev) => {
            const next = new Set(prev);
            if (saving) {
                next.add(fieldId);
            } else {
                next.delete(fieldId);
            }
            return next;
        });
    }, []);

    const isSavingField = React.useCallback(
        (fieldId: string) => savingFieldIds.has(fieldId),
        [savingFieldIds]
    );

    const handleToggle = React.useCallback(
        (fieldId: string, key: FieldToggleKey, checked: boolean) => {
            const currentField = data.find((field) => field.id === fieldId);
            if (!currentField) {
                return;
            }

            const updatedField: TurSNSiteField = {
                ...currentField,
                [key]: checked ? 1 : 0,
            };

            setSnField((prev) =>
                prev.map((field) =>
                    field.id === fieldId ? updatedField : field
                )
            );

            setFieldSaving(fieldId, true);
            turSNFieldService
                .update(id, updatedField)
                .catch((error) => {
                    console.error("Failed to update SN field", error);
                    setSnField((prev) =>
                        prev.map((field) =>
                            field.id === fieldId ? currentField : field
                        )
                    );
                })
                .finally(() => {
                    setFieldSaving(fieldId, false);
                });
        },
        [data, id, setFieldSaving]
    );

    const columns = React.useMemo(
        () => buildColumns(statusFieldMap, handleToggle, isSavingField),
        [statusFieldMap, handleToggle, isSavingField]
    );

    const table = useReactTable({
        data,
        columns,
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        onColumnVisibilityChange: setColumnVisibility,
        onRowSelectionChange: setRowSelection,
        state: {
            sorting,
            columnFilters,
            columnVisibility,
            rowSelection,
        },
    })

    return (
        <div className="w-full">
            <SubPageHeader
                icon={IconAlignBoxCenterStretch}
                name="Search Engine Field"
                feature="Search Engine Field"
                description="Custom Search Engine Fields."
                urlNew={`${ROUTES.SN_INSTANCE}/${id}/field/new`} />
            <div className="px-6">
                <div className="flex items-center py-4">
                    <Input
                        placeholder="Filter fields..."
                        value={(table.getColumn("name")?.getFilterValue() as string) ?? ""}
                        onChange={(event) =>
                            table.getColumn("name")?.setFilterValue(event.target.value)
                        }
                        className="max-w-sm"
                    />
                    <div className="ml-auto">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="outline">
                                    <IconColumns3Filled /> Columns <ChevronDown />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {table
                                    .getAllColumns()
                                    .filter((column) => column.getCanHide())
                                    .map((column) => {
                                        return (
                                            <DropdownMenuCheckboxItem
                                                key={column.id}
                                                className="capitalize"
                                                checked={column.getIsVisible()}
                                                onCheckedChange={(value) =>
                                                    column.toggleVisibility(value)
                                                }
                                            >
                                                {column.id}
                                            </DropdownMenuCheckboxItem>
                                        )
                                    })}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
                <div className="overflow-hidden rounded-md border">
                    <Table>
                        <TableHeader>
                            {table.getHeaderGroups().map((headerGroup) => (
                                <TableRow key={headerGroup.id}>
                                    {headerGroup.headers.map((header) => {
                                        return (
                                            <TableHead key={header.id}>
                                                {header.isPlaceholder
                                                    ? null
                                                    : flexRender(
                                                        header.column.columnDef.header,
                                                        header.getContext()
                                                    )}
                                            </TableHead>
                                        )
                                    })}
                                </TableRow>
                            ))}
                        </TableHeader>
                        <TableBody>
                            {table.getRowModel().rows?.length ? (
                                table.getRowModel().rows.map((row) => (
                                    <TableRow
                                        key={row.id}
                                        data-state={row.getIsSelected() && "selected"}
                                    >
                                        {row.getVisibleCells().map((cell) => (
                                            <TableCell key={cell.id}>
                                                {flexRender(
                                                    cell.column.columnDef.cell,
                                                    cell.getContext()
                                                )}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))
                            ) : (
                                <TableRow>
                                    <TableCell
                                        colSpan={columns.length}
                                        className="h-24 text-center"
                                    >
                                        No results.
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
                <div className="flex items-center justify-end space-x-2 py-4">
                    <div className="text-muted-foreground flex-1 text-sm">
                        {table.getFilteredSelectedRowModel().rows.length} of{" "}
                        {table.getFilteredRowModel().rows.length} row(s) selected.
                    </div>
                    <div className="space-x-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => table.previousPage()}
                            disabled={!table.getCanPreviousPage()}
                        >
                            Previous
                        </Button>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => table.nextPage()}
                            disabled={!table.getCanNextPage()}
                        >
                            Next
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}
