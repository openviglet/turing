import {SubPageHeader} from "@/components/sub.page.header";
import {IconNumber123} from "@tabler/icons-react";
import {BlankSlate} from "@/components/blank-slate.tsx";
import {useParams} from "react-router-dom";
import {TurSNRankingExpressionService} from "@/services/sn.site.result.ranking.service.ts";
import {useEffect, useState} from "react";
import type {TurSNRankingExpression} from "@/models/sn/sn-ranking-expression.model.ts";
import { ROUTES } from "@/app/routes.const";

const turSNRankingExpressionService = new TurSNRankingExpressionService();
export default function SNSiteResultRankingPage() {
    const {id} = useParams() as { id: string };
    const [rankingList, setRankingList] = useState<TurSNRankingExpression[]>({} as TurSNRankingExpression[]);
    useEffect(() => {
        turSNRankingExpressionService.query(id).then(setRankingList);
    }, [id])
    return (
        <>
            {rankingList.length > 0 ? (
                <SubPageHeader icon={IconNumber123} title="Result Ranking"
                               description="Define content that will be featured in the term-based search."/>
            ) : (
                <BlankSlate
                    icon={IconNumber123}
                    title="You don’t seem to have any result ranking."
                    description="Create a new result ranking to define relevance rules."
                    buttonText="New result ranking"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/result-ranking/new`}/>
            )}
        </>
    )
}