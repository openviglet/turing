import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteMerge } from "@/models/sn/sn-site-merge.model.ts";
import { TurSNSiteMergeService } from "@/services/sn/sn.site.merge.service";
import { IconGitMerge } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteMergeService = new TurSNSiteMergeService();
export default function SNSiteMergeProvidersPage() {
    const { id } = useParams() as { id: string };
    const [mergeList, setMergeList] = useState<TurSNSiteMerge[]>({} as TurSNSiteMerge[]);
    useEffect(() => {
        turSNSiteMergeService.query(id).then(setMergeList);
    }, [id])
    return (
        <>
            {mergeList.length > 0 ? (
                <SubPageHeader icon={IconGitMerge} name="Merge Providers" feature="Merge Providers"
                    description="Unify different sources contents." />
            )
                : (
                    <BlankSlate
                        icon={IconGitMerge}
                        title="You don’t seem to have any merge provider."
                        description="Create a new merge provider and allow to merge documents from different providers during indexing."
                        buttonText="New merge provider"
                        urlNew={`${ROUTES.SN_INSTANCE}/${id}/merge/new`} />
                )
            }
        </>
    )
}
