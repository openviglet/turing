import {
    flexRender,
    getCoreRowModel,
    getPaginationRowModel,
    useReactTable,
    type ColumnDef,
} from "@tanstack/react-table";
import { Pencil } from "lucide-react";
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
import type { TurGridItem } from "@/models/ui/grid-item";
import { NavLink } from "react-router-dom";
import { GradientButton } from "./ui/gradient-button";

interface Props {
    gridItemList: TurGridItem[];
}


// 3. Definição das Colunas
export const columns: ColumnDef<TurGridItem>[] = [
    {
        accessorKey: "name",
        header: "Name",
        cell: ({ row }) => <div>{row.getValue("name")}</div>,
    },
    {
        accessorKey: "description",
        header: "Description",
        cell: ({ row }) => <div>{row.getValue("description")}</div>,
    },
    {
        accessorKey: "url",
        header: "Actions",
        cell: ({ row }) => {
            return (
                // Ícone de lápis azul estilo Google
                <GradientButton variant="ghost" size="icon" className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 h-8 w-8">
                    <NavLink to={row.getValue("url")}><Pencil className="h-4 w-4" />
                        <span className="sr-only">Edit</span></NavLink>
                </GradientButton>
            );
        },
    },
];


export const GridList: React.FC<PropsWithChildren<Props>> = ({ gridItemList }) => {
    const [pagination, setPagination] = useState({
        pageIndex: 0, // initial page index
        pageSize: 10, // default page size
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
        <div className="px-6">
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
                                {[10, 20, 30, 40, 50].map((pageSize) => (
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
