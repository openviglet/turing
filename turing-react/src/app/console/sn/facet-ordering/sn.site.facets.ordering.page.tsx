import { ROUTES } from "@/app/routes.const";
import { DraggableTable } from "@/components/draggable-table";
import { LoadProvider } from "@/components/loading-provider";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNFacetedFieldService } from "@/services/sn/sn.faceted.field.service";
import { IconReorder } from "@tabler/icons-react";
import React, { useState } from "react";
import { useParams } from "react-router-dom";

const turSNFacetedFieldService = new TurSNFacetedFieldService();
export default function SNSiteFacetOrderingPage() {
  const { id } = useParams() as { id: string };
  const [error, setError] = useState<string | null>(null);
  const [tableData, setTableData] = React.useState<TurSNSiteField[]>();
  React.useEffect(() => {
    turSNFacetedFieldService.query(id).then(setTableData).catch(() => setError("Connection error or timeout while fetching faceted fields."));
  }, [id])
  return (
    <LoadProvider checkIsNotUndefined={tableData} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/facet-ordering`}>
      <SubPageHeader icon={IconReorder} name="Facet Ordering" feature="Facet Ordering" description="Order the facets of the search." />
      {tableData && <DraggableTable id={id} tableData={tableData} setTableData={setTableData as React.Dispatch<React.SetStateAction<TurSNSiteField[]>>} />}
    </LoadProvider>
  )
}
