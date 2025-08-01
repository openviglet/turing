import {SubPageHeader} from "@/components/sub.page.header";
import {IconGitMerge} from "@tabler/icons-react";
import {BlankSlate} from "@/components/blank-slate.tsx";
import {useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {TurSNSiteMergeService} from "@/services/sn.site.merge.service.ts";
import type {TurSNSiteMerge} from "@/models/sn/sn-site-merge.model.ts";

const turSNSiteMergeService = new TurSNSiteMergeService();
export default function SNSiteMergeProvidersPage() {
    const {id} = useParams() as { id: string };
    const [mergeList, setMergeList] = useState<TurSNSiteMerge[]>({} as TurSNSiteMerge[]);
    useEffect(() => {
        turSNSiteMergeService.query(id).then(setMergeList);
    }, [id])
    return (
        <>
            {mergeList.length > 0 ? (
                <SubPageHeader icon={IconGitMerge} title="Merge Providers"
                               description="Unify different sources contents."/>
            )
            : (
                <BlankSlate
                    icon={IconGitMerge}
                    title="You don’t seem to have any merge provider."
                    description="Create a new merge provider and allow to merge documents from different providers during indexing."
                    buttonText="New merge provider"
                    urlNew={"/admin/sn/site/" + id + "/merge/new"}/>
            )
        }
        </>
    )
}
