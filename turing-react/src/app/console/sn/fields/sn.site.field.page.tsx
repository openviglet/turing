import { ROUTES } from "@/app/routes.const";
import { SNSiteFieldForm } from "@/components/sn/fields/sn.site.field.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNFieldService } from "@/services/sn/sn.field.service";
import { IconAlignBoxCenterStretch } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turSNFieldService = new TurSNFieldService();

export default function SNSiteFieldPage() {
  const navigate = useNavigate();
  const { id, fieldId } = useParams() as { id: string, fieldId: string };
  const [snField, setSnField] = useState<TurSNSiteField>({} as TurSNSiteField);
  const [isNew, setIsNew] = useState<boolean>(true);
  const [open, setOpen] = useState(false);
  useEffect(() => {
    if (fieldId !== "new") {
      turSNFieldService.get(id, fieldId).then(setSnField);
      setIsNew(false);
    }
  }, [fieldId])
  async function onDelete() {
    try {
      if (await turSNFieldService.delete(id, snField)) {
        toast.success("The field was deleted");
        navigate(`${ROUTES.SN_INSTANCE}/${id}/field`);
      } else {
        toast.error("The field was not deleted");
      }
    } catch (error) {
      console.error("Delete error", error);
      toast.error("The field was not deleted");
    }
    setOpen(false);
  }

  return (
    <>
      <SubPageHeader
        icon={IconAlignBoxCenterStretch}
        name={snField.name}
        feature="Field"
        description={snField.description}
        onDelete={isNew ? undefined : onDelete}
        open={open}
        setOpen={setOpen} />
      <SNSiteFieldForm snSiteId={id} snField={snField} isNew={isNew} />
    </>
  )
}
