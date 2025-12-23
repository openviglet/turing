import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteSpotlight } from "@/models/sn/sn-site-spotlight.model.ts";
import { TurSNSiteSpotlightService } from "@/services/sn/sn.site.spotlight.service";
import { IconSpeakerphone } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteSpotlightService = new TurSNSiteSpotlightService();
export default function SNSiteSpotlightPage() {
    const { id } = useParams() as { id: string };
    const [spotlightList, setSpotlightList] = useState<TurSNSiteSpotlight[]>({} as TurSNSiteSpotlight[]);
    useEffect(() => {
        turSNSiteSpotlightService.query(id).then(setSpotlightList);
    }, [id])
    return (
        <>
            {spotlightList.length > 0 ? (
                <SubPageHeader
                    icon={IconSpeakerphone}
                    title="Spotlight"
                    description="Define content that will be featured in the term-based search."
                />
            ) : (
                < BlankSlate
                    icon={IconSpeakerphone}
                    title="You don't seem to have any spotlight."
                    description="Create a new spotlight to define content featured."
                    buttonText="New spotlight"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/spotlight/new`}
                />
            )}
        </>
    )
}
