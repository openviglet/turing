import { SNSiteFieldForm } from "@/components/sn/fields/sn.site.field.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNFieldService } from "@/services/sn/sn.field.service";
import { IconAlignBoxCenterStretch } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNFieldService = new TurSNFieldService();

export default function SNSiteFieldPage() {
  const { id, fieldId } = useParams() as { id: string, fieldId: string };
  const [snField, setSnField] = useState<TurSNSiteField>({} as TurSNSiteField);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (fieldId !== "new") {
      turSNFieldService.get(id, fieldId).then(setSnField);
      setIsNew(false);
    }
  }, [fieldId])
  return (
    <>
      <SubPageHeader icon={IconAlignBoxCenterStretch} name={snField.name} feature="Field" description={snField.description} />
      <SNSiteFieldForm snSiteId={id} snField={snField} isNew={isNew} />
    </>
  )
}
