import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { SNSiteFacetedFieldForm } from "@/components/sn/facet/sn.site.faceted.field.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNFieldService } from "@/services/sn/sn.field.service";
import { IconAlignBoxCenterStretch } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turSNFieldService = new TurSNFieldService();

export default function SNSiteFacetedFieldPage() {
  const navigate = useNavigate();
  const { id, facetedFieldId } = useParams() as { id: string, facetedFieldId: string };
  const [snField, setSnField] = useState<TurSNSiteField>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (facetedFieldId === "new") {
      turSNFieldService.query(id).then(() => setSnField({} as TurSNSiteField)).catch(() => setError("Connection error or timeout while fetching faceted fields."));
    } else {
      turSNFieldService.get(id, facetedFieldId).then(setSnField).catch(() => setError("Connection error or timeout while fetching faceted field details."));
      setIsNew(false);
    }
  }, [id, facetedFieldId])
  async function onDelete() {
    if (!snField) return;
    try {
      if (await turSNFieldService.delete(id, snField)) {
        toast.success("The faceted field was deleted");
        navigate(`${ROUTES.SN_INSTANCE}/${id}/field`);
      } else {
        toast.error("The faceted field was not deleted");
      }
    } catch (error) {
      console.error("Delete error", error);
      toast.error("The faceted field was not deleted");
    }
    setOpen(false);
  }

  return (
    <LoadProvider checkIsNotUndefined={snField} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/field`}>
      {snField && (<> <SubPageHeader
        icon={IconAlignBoxCenterStretch}
        name={snField.name}
        feature="Faceted Field"
        description={snField.description}
        onDelete={isNew ? undefined : onDelete}
        open={open}
        setOpen={setOpen} />
        <SNSiteFacetedFieldForm snSiteId={id} snField={snField} isNew={isNew} />
      </>)}
    </LoadProvider>
  )
}
