import {SubPageHeader} from "@/components/sub.page.header";
import {IconLanguage} from "@tabler/icons-react";
import * as React from "react"
import {useParams} from "react-router-dom"
import type {TurSNSiteLocale} from "@/models/sn/sn-site-locale.model.ts";
import {TurSNSiteLocaleService} from "@/services/sn.site.locale.service";
import {SNSiteMultiLanguageDataTable} from "@/components/sn/locales/sn.site.locale.data.table.tsx";
import {BlankSlate} from "@/components/blank-slate.tsx";

const turSNSiteLocaleService = new TurSNSiteLocaleService();
export default function SNSiteMultiLanguagePage() {
    const {id} = useParams() as { id: string };
    const [data, setData] = React.useState<TurSNSiteLocale[]>({} as TurSNSiteLocale[]);
    React.useEffect(() => {
        turSNSiteLocaleService.query(id).then(setData);
    }, [id])

    return (
        <>
            {data.length > 0 ? (
                <>
                    <SubPageHeader icon={IconLanguage} title="Multi Language" description="Define Multi Languages."
                                   urlNew={"/sn/site/" + id + "/locale/new"}/>
                    <SNSiteMultiLanguageDataTable data={data}/>
                </>
            ) : (
                < BlankSlate
                    icon={IconLanguage}
                    title="You don't seem to have any language."
                    description="Create a new language to semantic navigation search works."
                    buttonText="New language"
                    urlNew={"/console/sn/site/" + id + "/locale/new"}
                />
            )}
        </>
    )
}
