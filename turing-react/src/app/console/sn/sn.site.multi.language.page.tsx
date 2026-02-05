import { SNSiteLocaleForm } from "@/components/sn/locales/sn.site.locale.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model";
import { TurSNSiteLocaleService } from "@/services/sn/sn.site.locale.service";
import { IconLanguage } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteLocaleService = new TurSNSiteLocaleService();

export default function SNSiteMultiLanguagePage() {
  const { id, localeId } = useParams() as { id: string, localeId: string };
  const [snLocale, setSnLocale] = useState<TurSNSiteLocale>({} as TurSNSiteLocale);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (localeId !== "new") {
      turSNSiteLocaleService.get(id, localeId).then(setSnLocale);
      setIsNew(false);
    }
  }, [id, localeId])
  return (
    <>
      {isNew && <SubPageHeader icon={IconLanguage} name="Multi Language" feature="Multi Language" description="Define Multi Languages." />}
      {!isNew && <SubPageHeader icon={IconLanguage} name={snLocale.language} feature="Multi Language" description={snLocale.core} />}
      <SNSiteLocaleForm snSiteId={id} snLocale={snLocale} isNew={isNew} />
    </>
  )
}
