import { SNSiteGenAiForm } from "@/components/sn/genai/sn.site.genai.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSite } from "@/models/sn/sn-site.model";
import { TurSNSiteService } from "@/services/sn/sn.service";
import { IconCpu2 } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteGenAIPage() {
  const { id } = useParams() as { id: string };
  const [snSite, setSnSite] = useState<TurSNSite | null>(null);

  useEffect(() => {
    turSNSiteService.get(id).then(setSnSite);
  }, [id]);

  if (!snSite) return null;

  return (
    <>
      <SubPageHeader
        icon={IconCpu2}
        name="Generative AI"
        feature="Generative AI"
        description="Use Assistant in your search."
      />
      <SNSiteGenAiForm snSite={snSite} />
    </>
  );
}
