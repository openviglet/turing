import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { TurSNSiteSpotlight } from "@/models/sn/sn-site-spotlight.model";
import { TurSNSiteSpotlightService } from "@/services/sn/sn.site.spotlight.service";
import { IconSpeakerphone } from "@tabler/icons-react";
import { ArrowUpRight } from "lucide-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteSpotlightService = new TurSNSiteSpotlightService();

export default function SNSiteSpotlightListPage() {
    const { id } = useParams() as { id: string };
    const [spotlightList, setSpotlightList] = useState<TurSNSiteSpotlight[]>([]);
    useEffect(() => {
        turSNSiteSpotlightService.query(id).then(setSpotlightList);
    }, [id])
    return (
        <>
            {spotlightList.length > 0 ? (
                <>
                    <SubPageHeader icon={IconSpeakerphone} name="Spotlight" feature="Spotlight"
                        description="Define content that will be featured in the term-based search." />
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 px-6">
                        {spotlightList.map((spotlight) => (
                            <Card key={spotlight.id} className="flex flex-col justify-between hover:border-primary transition-colors">
                                <CardHeader>
                                    <CardTitle className="text-xl">{spotlight.name}</CardTitle>
                                    <CardDescription>{spotlight.description}</CardDescription>
                                </CardHeader>
                                <CardFooter>
                                    <a
                                        href={"spotlight/" + spotlight.id}
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
                    icon={IconSpeakerphone}
                    title="You don’t seem to have any spotlight."
                    description="Create a spotlight component that highlights content triggered by specific search terms."
                    buttonText="New spotlight"
                    urlNew={`${ROUTES.SN_INSTANCE}/${id}/spotlight/new`} />
            )}
        </>
    )
}