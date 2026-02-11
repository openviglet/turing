import { ROUTES } from "@/app/routes.const";
import { SNSiteLocaleForm } from "@/components/sn/locales/sn.site.locale.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model";
import { TurSNSiteLocaleService } from "@/services/sn/sn.site.locale.service";
import { IconLanguage } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turSNSiteLocaleService = new TurSNSiteLocaleService();

export default function SNSiteMultiLanguagePage() {
  const navigate = useNavigate();
  const { id, localeId } = useParams() as { id: string, localeId: string };
  const [snLocale, setSnLocale] = useState<TurSNSiteLocale>({} as TurSNSiteLocale);
  const [isNew, setIsNew] = useState<boolean>(true);
  const [open, setOpen] = useState(false);
  useEffect(() => {
    if (localeId !== "new") {
      turSNSiteLocaleService.get(id, localeId).then(setSnLocale);
      setIsNew(false);
    }
  }, [id, localeId])
  async function onDelete() {
    try {
      if (await turSNSiteLocaleService.delete(snLocale)) {
        toast.success(`The ${snLocale.language} Language was deleted`);
        navigate(`${ROUTES.SN_INSTANCE}/${id}/locale`);
      } else {
        toast.error(`The ${snLocale.language} Language was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${snLocale.language} Language was not deleted`);
    }
    setOpen(false);
  }
  return (
    <>
      {isNew && <SubPageHeader icon={IconLanguage} name="Multi Language" feature="Multi Language" description="Define Multi Languages." />}
      {!isNew && <SubPageHeader
        icon={IconLanguage}
        name={snLocale.language}
        feature="Multi Language"
        description={snLocale.core}
        onDelete={onDelete}
        open={open}
        setOpen={setOpen} />}
      <SNSiteLocaleForm
        snSiteId={id}
        snLocale={snLocale}
        isNew={isNew}
      />
    </>
  )
}
