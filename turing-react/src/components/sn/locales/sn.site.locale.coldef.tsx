import { ROUTES } from "@/app/routes.const";
import { Button } from "@/components/ui/button.tsx";
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model.ts";
import { Checkbox } from "@radix-ui/react-checkbox";
import type { ColumnDef } from "@tanstack/react-table";
import { ArrowUpDown } from "lucide-react";

export const columns: ColumnDef<TurSNSiteLocale>[] = [
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
        accessorKey: "language",
        header: ({ column }) => {
            return (
                <Button
                    variant="ghost"
                    onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
                >
                    Language
                    <ArrowUpDown />
                </Button>
            )
        },
        cell: ({ row }) => (
            <div className="text-left font-medium">{row.getValue("language")}</div>
        ),
    },
    {
        accessorKey: "core",
        header: () => <div className="text-left">Core</div>,
        cell: ({ row }) => {
            return <div className="text-left font-medium">{row.getValue("core")}</div>
        },
    },
    {
        accessorKey: "action",
        header: () => <div className="text-right">Action</div>,
        cell: ({ row }) => {
            const locale = row.original;
            return (
                <div className="flex justify-end gap-2">
                    <Button asChild variant="outline">
                        <a href={`${ROUTES.SN_INSTANCE}/${locale.turSNSite.id}/locale/${locale.id}`}>
                            Edit
                        </a>
                    </Button>
                    <Button asChild>
                        <a href={`/sn/${locale.turSNSite.id}?_setlocale=${locale.language}`}>
                            Open Search
                        </a>
                    </Button>
                </div>
            )
        },
    }
]