import { SNSiteFieldForm } from "@/components/sn.site.field.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNSiteService } from "@/services/sn.service";
import { IconAlignBoxCenterStretch } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteFieldPage() {
  const { id, fieldId } = useParams() as { id: string, fieldId: string };
  const [snField, setSnField] = useState<TurSNSiteField>({} as TurSNSiteField);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turSNSiteService.getField(id, fieldId).then(setSnField);
      setIsNew(false);
    }
  }, [id])
  return (
    <>
      <SubPageHeader icon={IconAlignBoxCenterStretch} title={snField.name} description={snField.description} />
      <SNSiteFieldForm snSiteId={id} snField={snField} isNew={isNew} />
    </>
  )
}
