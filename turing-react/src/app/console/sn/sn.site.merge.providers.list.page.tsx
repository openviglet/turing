import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { TurSNSiteMerge } from "@/models/sn/sn-site-merge.model";
import { TurSNSiteMergeService } from "@/services/sn.site.merge.service";
import { IconGitMerge } from "@tabler/icons-react";
import { ArrowUpRight } from "lucide-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteMergeService = new TurSNSiteMergeService();

export default function SNSiteMergeProvidersListPage() {
    const { id } = useParams() as { id: string };
    const [mergeProviderList, setMergeProviderList] = useState<TurSNSiteMerge[]>([]);
    useEffect(() => {
        turSNSiteMergeService.query(id).then(setMergeProviderList);
    }, [id])
    return (
        <>
            {mergeProviderList.length > 0 ? (
                <>
                    <SubPageHeader icon={IconGitMerge} title="Merge Providers"
                        description="Unify different sources contents." />
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {mergeProviderList.map((merge) => (
                            <Card key={merge.id} className="flex flex-col justify-between hover:border-primary transition-colors">
                                <CardHeader>
                                    <CardTitle className="text-xl">{merge.providerFrom} &gt; {merge.providerTo}</CardTitle>
                                    <CardDescription>{merge.description}</CardDescription>
                                </CardHeader>
                                <CardFooter>
                                    <a
                                        href={"merge-providers/" + merge.id}
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
                    icon={IconGitMerge}
                    title="You don’t seem to have any merge provider."
                    description="Create a new merge provider and allow to merge documents from different providers during indexing."
                    buttonText="New merge provider"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/merge-providers/new`} />
            )}
        </>
    )
}