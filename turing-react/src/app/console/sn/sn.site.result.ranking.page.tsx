import { SNSiteResultRankingForm } from "@/components/sn/sn.site.result.ranking.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model";
import { TurSNRankingExpressionService } from "@/services/sn/sn.site.result.ranking.service";
import { IconNumber123 } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNRankingExpressionService = new TurSNRankingExpressionService();

export default function SNSiteResultRankingPage() {
    const { id, resultRankingId } = useParams() as { id: string, resultRankingId: string };
    const [resultRanking, setResultRanking] = useState<TurSNRankingExpression>({} as TurSNRankingExpression);
    const [isNew, setIsNew] = useState<boolean>(true);
    useEffect(() => {

        if (resultRankingId !== "new") {
            turSNRankingExpressionService.get(id, resultRankingId).then(setResultRanking);
            setIsNew(false);
        }
    }, [id])
    return (
        <>
            <SubPageHeader icon={IconNumber123} name="Result Ranking" feature="Result Ranking"
                description="Define content that will be featured in the term-based search." />
            <SNSiteResultRankingForm siteId={id} value={resultRanking} isNew={isNew} />
        </>
    )
}