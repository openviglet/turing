import { ROUTES } from "@/app/routes.const";
import { Card } from "@/components/ui/card";
import { GradientButton } from "@/components/ui/gradient-button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type ColumnFiltersState,
  type SortingState,
  type VisibilityState,
} from "@tanstack/react-table";
import React, { useState } from "react";
import { useParams } from "react-router-dom";

interface Props {
  items?: TurSNSiteCustomFacet[];
  parentIdName?: string;
}

export const CustomFacetGrid: React.FC<Props> = ({ items, parentIdName }) => {
  const { id } = useParams() as { id: string };
  const data = items ?? [];
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
  const [rowSelection, setRowSelection] = useState({});
  const [globalFilter, setGlobalFilter] = useState<string>("");

  const columns: ColumnDef<TurSNSiteCustomFacet>[] = [
    {
      accessorKey: "label",
      header: "Label",
      cell: ({ row }) => <div>{row.getValue("label")}</div>,
    },
    {
      accessorKey: "range",
      header: "Range",
      cell: ({ row }) => {
        const start = row.original.rangeStart && row.original.rangeStart.length > 0 ? row.original.rangeStart : "*";
        const end = row.original.rangeEnd && row.original.rangeEnd.length > 0 ? row.original.rangeEnd : "*";
        return <div>[{start} TO {end}]</div>;
      },
    },
    {
      accessorKey: "actions",
      header: () => <div className="text-center">Actions</div>,
      cell: ({ row }) => {
        return (
          <div className="text-center">
            <GradientButton variant="outline" to={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${encodeURIComponent(parentIdName ?? "")}/${row.original.id}`}>
              Edit
            </GradientButton>
          </div>
        );
      },
    },
  ];

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      columnFilters,
      columnVisibility,
      rowSelection,
      globalFilter,
    },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onColumnVisibilityChange: setColumnVisibility,
    onRowSelectionChange: setRowSelection,
    initialState: {
      pagination: {
        pageSize: 50,
        pageIndex: 0,
      },
    },
    globalFilterFn: (row, _columnId, filterValue) => {
      if (!filterValue) return true;
      const label: string = row.original.label ?? "";
      return label.toLowerCase().includes(String(filterValue).toLowerCase());
    },
  });

  return (
    <div className="px-6">
      <Card>
        <div className="rounded-md">
          <div className="flex items-center py-4 px-4">
            <Input
              placeholder="Filter by label or attribute..."
              value={globalFilter ?? ""}
              onChange={(event) => {
                setGlobalFilter(event.target.value);
              }}
              className="max-w-sm"
            />
          </div>
          <Table>
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => {
                    return (
                      <TableHead key={header.id} className="px-5">
                        {header.isPlaceholder
                          ? null
                          : flexRender(header.column.columnDef.header, header.getContext())}
                      </TableHead>
                    );
                  })}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows?.length ? (
                table.getRowModel().rows.map((row) => (
                  <TableRow key={row.id} data-state={row.getIsSelected() && "selected"}>
                    {row.getVisibleCells().map((cell) => (
                      <TableCell key={cell.id}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={columns.length} className="h-24 text-center">
                    No results.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </Card>
      <div className="flex items-center justify-end space-x-2 py-4">
        <div className="text-muted-foreground flex-1 text-sm">
          {table.getFilteredSelectedRowModel().rows.length} of {table.getFilteredRowModel().rows.length} row(s)
          selected.
        </div>
        <div className="space-x-2">
          <GradientButton variant="outline" size="sm" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>
            Previous
          </GradientButton>
          <GradientButton variant="outline" size="sm" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
            Next
          </GradientButton>
        </div>
      </div>
    </div>
  );
};
