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
import type { TurSNSiteCustomFacetGroup } from "@/models/sn/sn-site-custom-facet-group.model";
import React, { useState } from "react";
import { useParams } from "react-router-dom";

interface Props {
  items?: TurSNSiteCustomFacetGroup[];
}

export const CustomFacetParentGrid: React.FC<Props> = ({ items }) => {
  const { id } = useParams() as { id: string };
  const data = items ?? [];
  const [globalFilter, setGlobalFilter] = useState<string>("");
  const filtered = data.filter((g) => {
    if (!globalFilter) return true;
    return (
      (g.idName ?? "").toLowerCase().includes(globalFilter.toLowerCase())
    );
  });

  return (
    <div className="px-6">
      <Card>
        <div className="rounded-md">
          <div className="flex items-center py-4 px-4">
            <Input
              placeholder="Filter by NameID..."
              value={globalFilter ?? ""}
              onChange={(event) => setGlobalFilter(event.target.value)}
              className="max-w-sm"
            />
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="px-5">NameID</TableHead>
                <TableHead className="px-5">Items</TableHead>
                <TableHead className="px-5 text-center">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.length ? (
                filtered.map((row) => (
                  <TableRow key={row.idName}>
                    <TableCell className="px-5">{row.idName}</TableCell>
                    <TableCell className="px-5">{row.count}</TableCell>
                    <TableCell className="px-5 text-center">
                      <div className="flex justify-center gap-2">
                        <GradientButton to={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${row.idName}`} variant="outline">
                          Open
                        </GradientButton>
                        <GradientButton to={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${row.idName}/edit`} variant="outline">
                          Edit
                        </GradientButton>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="h-24 text-center">
                    No results.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </Card>
    </div>
  );
};
