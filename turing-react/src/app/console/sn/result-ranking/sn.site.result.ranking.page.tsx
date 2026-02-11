import { ROUTES } from "@/app/routes.const";
import { SNSiteResultRankingForm } from "@/components/sn/result-ranking/sn.site.result.ranking.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model";
import { TurSNRankingExpressionService } from "@/services/sn/sn.site.result.ranking.service";
import { IconNumber123 } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turSNRankingExpressionService = new TurSNRankingExpressionService();

export default function SNSiteResultRankingPage() {
    const navigate = useNavigate();
    const { id, resultRankingId } = useParams() as { id: string, resultRankingId: string };
    const [resultRanking, setResultRanking] = useState<TurSNRankingExpression>({} as TurSNRankingExpression);
    const [isNew, setIsNew] = useState<boolean>(true);

    const [open, setOpen] = useState(false);
    useEffect(() => {

        if (resultRankingId !== "new") {
            turSNRankingExpressionService.get(id, resultRankingId).then(setResultRanking);
            setIsNew(false);
        }
    }, [id])
    async function onDelete() {
        try {
            if (await turSNRankingExpressionService.delete(id, resultRanking)) {
                toast.success(`The ${resultRanking.name} Result Ranking Instance was deleted`);
                navigate(`${ROUTES.SN_INSTANCE}/${id}/result-ranking`);
            } else {
                toast.error(`The ${resultRanking.name} Result Ranking Instance was not deleted`);
            }

        } catch (error) {
            console.error("Form submission error", error);
            toast.error(`The ${resultRanking.name} Result Ranking Instance was not deleted`);
        }
        setOpen(false);
    }
    return (
        <>
            <SubPageHeader icon={IconNumber123} name={resultRanking.name} feature="Result Ranking"
                description="Define content that will be featured in the term-based search."
                onDelete={onDelete}
                open={open}
                setOpen={setOpen} />
            <SNSiteResultRankingForm snSiteId={id} value={resultRanking} isNew={isNew} />
        </>
    )
}