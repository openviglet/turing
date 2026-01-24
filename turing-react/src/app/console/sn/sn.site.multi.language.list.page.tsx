import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SNSiteMultiLanguageDataTable } from "@/components/sn/locales/sn.site.locale.data.table.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model.ts";
import { TurSNSiteLocaleService } from "@/services/sn/sn.site.locale.service";
import { IconLanguage } from "@tabler/icons-react";
import * as React from "react";
import { useParams } from "react-router-dom";

const turSNSiteLocaleService = new TurSNSiteLocaleService();
export default function SNSiteMultiLanguageListPage() {
    const { id } = useParams() as { id: string };
    const [data, setData] = React.useState<TurSNSiteLocale[]>({} as TurSNSiteLocale[]);
    React.useEffect(() => {
        turSNSiteLocaleService.query(id).then(setData);
    }, [id])

    return (
        <>
            {data.length > 0 ? (
                <>
                    <SubPageHeader icon={IconLanguage} title="Multi Language" description="Define Multi Languages."
                        urlNew={`${ROUTES.SN_INSTANCE}/${id}/locale/new`} />
                    <SNSiteMultiLanguageDataTable data={data} />
                </>
            ) : (
                < BlankSlate
                    icon={IconLanguage}
                    title="You don't seem to have any language."
                    description="Create a new language to semantic navigation search works."
                    buttonText="New language"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/locale/new`}
                />
            )}
        </>
    )
}
