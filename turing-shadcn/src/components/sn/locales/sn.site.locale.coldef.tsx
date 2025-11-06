import type {ColumnDef} from "@tanstack/react-table";
import type {TurSNSiteLocale} from "@/models/sn/sn-site-locale.model.ts";
import {Checkbox} from "@radix-ui/react-checkbox";
import {Button} from "@/components/ui/button.tsx";
import {ArrowUpDown} from "lucide-react";

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
            return <div className="text-right font-medium">
                <Button asChild>
                    <a href={"/sn/" + row.getValue("turSNSite.id") + "?_setlocale=" + row.getValue("language")}>
                        Open Search
                    </a>
                </Button>
            </div>
        },
    }
]