import {
    flexRender,
    getCoreRowModel,
    getPaginationRowModel,
    useReactTable,
    type ColumnDef,
} from "@tanstack/react-table";
import { useState, type PropsWithChildren } from "react";

import { Card } from "@/components/ui/card";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import type { TurLoggingIndexing } from "@/models/logging/logging-indexing.model";
import { GradientButton } from "../ui/gradient-button";

interface Props {
    gridItemList: TurLoggingIndexing[];
}

export const columns: ColumnDef<TurLoggingIndexing>[] = [
    {
        accessorKey: "date",
        header: "Date",
        cell: ({ row }) => <div className="font-mono text-sm">{
            (new Date(row.getValue("date") as string)).toLocaleString(window.navigator.language, {
                day: '2-digit',
                month: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            }).replace(',', '')
        }</div>,
    },
    {
        accessorKey: "source",
        header: "Source",
        cell: ({ row }) => <div className="font-mono text-sm">{row.getValue("source")}</div>,
    },
    {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => <div className="font-mono text-sm">{row.getValue("status")}</div>,
    },
    {
        accessorKey: "resultStatus",
        header: "Result Status",
        cell: ({ row }) => <div className="font-mono text-sm">{row.getValue("resultStatus")}</div>,
    }
    ,
    {
        accessorKey: "url",
        header: "URL",
        cell: ({ row }) => <div className="font-mono text-sm">{row.getValue("url")}</div>,
    },
    {
        accessorKey: "environment",
        header: "Environment",
        cell: ({ row }) => <div className="font-mono text-sm">{row.getValue("environment")}</div>,
    },
    {
        accessorKey: "locale",
        header: "Locale",
        cell: ({ row }) => <div>{row.getValue("locale")}</div>,
    }
    ,
    {
        accessorKey: "sites",
        header: "Sites",
        cell: ({ row }) => <div>{row.getValue("sites")}</div>,
    }
];


export const IndexingLoggingGrid: React.FC<PropsWithChildren<Props>> = ({ gridItemList }) => {
    const [pagination, setPagination] = useState({
        pageIndex: 0, // initial page index
        pageSize: 100, // default page size
    });

    const table = useReactTable({
        data: gridItemList,
        columns,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        onPaginationChange: setPagination,
        state: {
            pagination,
        },
    });

    return (
        <div className="px-4">
            <Card>
                <div className="rounded-md">
                    <Table>
                        <TableHeader >
                            {table.getHeaderGroups().map((headerGroup) => (
                                <TableRow key={headerGroup.id}>
                                    {headerGroup.headers.map((header) => {
                                        return (
                                            <TableHead key={header.id} className="px-5">
                                                {header.isPlaceholder
                                                    ? null
                                                    : flexRender(
                                                        header.column.columnDef.header,
                                                        header.getContext()
                                                    )}
                                            </TableHead>
                                        );
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
                                            <TableCell key={cell.id} className="px-5 py-3 text-sm">
                                                {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))
                            ) : (
                                <TableRow>
                                    <TableCell colSpan={columns.length}>
                                        No results.
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
                <div className="flex items-center justify-end space-x-2 py-4 px-4 border-t">
                    <div className="flex items-center space-x-2">
                        <p >Rows per page</p>
                        <Select
                            value={`${table.getState().pagination.pageSize}`}
                            onValueChange={(value) => {
                                table.setPageSize(Number(value))
                            }}
                        >
                            <SelectTrigger className="h-8 w-17.5">
                                <SelectValue placeholder={table.getState().pagination.pageSize} />
                            </SelectTrigger>
                            <SelectContent side="top">
                                {[10, 20, 30, 40, 50, 100].map((pageSize) => (
                                    <SelectItem key={pageSize} value={`${pageSize}`}>
                                        {pageSize}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="flex w-25 items-center justify-center text-sm font-medium">
                        Page {table.getState().pagination.pageIndex + 1} of{" "}
                        {table.getPageCount()}
                    </div>
                    <div className="flex items-center space-x-2">
                        <GradientButton
                            variant="outline"
                            size="sm"
                            onClick={() => table.previousPage()}
                            disabled={!table.getCanPreviousPage()}
                            className="h-8 w-8 p-0"
                        >
                            <span>&lt;</span>
                        </GradientButton>
                        <GradientButton
                            variant="outline"
                            size="sm"
                            onClick={() => table.nextPage()}
                            disabled={!table.getCanNextPage()}
                            className="h-8 w-8 p-0"
                        >
                            <span>&gt;</span>
                        </GradientButton>
                    </div>
                </div>
            </Card>
        </div >
    );
}