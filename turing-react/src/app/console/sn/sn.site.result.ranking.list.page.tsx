import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model.ts";
import { TurSNRankingExpressionService } from "@/services/sn/sn.site.result.ranking.service";
import { IconNumber123 } from "@tabler/icons-react";
import { ArrowUpRight } from "lucide-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNRankingExpressionService = new TurSNRankingExpressionService();

export default function SNSiteResultRankingListPage() {
    const { id } = useParams() as { id: string };
    const [rankingList, setRankingList] = useState<TurSNRankingExpression[]>([]);
    useEffect(() => {
        turSNRankingExpressionService.query(id).then(setRankingList);
    }, [id])
    return (
        <>
            {rankingList.length > 0 ? (
                <>
                    <SubPageHeader icon={IconNumber123} title="Result Ranking"
                        description="Define content that will be featured in the term-based search." />
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {rankingList.map((ranking) => (
                            <Card key={ranking.id} className="flex flex-col justify-between hover:border-primary transition-colors">
                                <CardHeader>
                                    <CardTitle className="text-xl">{ranking.name}</CardTitle>
                                    <CardDescription>{ranking.description}</CardDescription>
                                </CardHeader>
                                <CardFooter>
                                    <a
                                        href={"result-ranking/" + ranking.id}
                                        className={buttonVariants({ variant: "ghost" })}
                                    >
                                        Edit
                                        <ArrowUpRight className="h-4 w-4 ml-2" />
                                    </a>
                                </CardFooter>
                            </Card>
                        ))}
                    </div>
                </>
            ) : (
                <BlankSlate
                    icon={IconNumber123}
                    title="You don’t seem to have any result ranking."
                    description="Create a new result ranking to define relevance rules."
                    buttonText="New result ranking"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/result-ranking/new`} />
            )}
        </>
    )
}