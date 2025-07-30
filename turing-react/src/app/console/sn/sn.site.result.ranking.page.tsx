import {SubPageHeader} from "@/components/sub.page.header";
import {IconNumber123} from "@tabler/icons-react";
import {BlankSlate} from "@/components/blank-slate.tsx";
import {useParams} from "react-router-dom";

export default function SNSiteResultRankingPage() {
    const {id} = useParams() as { id: string };
    return (
        <>
            <SubPageHeader icon={IconNumber123} title="Result Ranking"
                           description="Define content that will be featured in the term-based search."/>
            <BlankSlate
                icon={IconNumber123}
                title="You don’t seem to have any result ranking."
                description="Create a new result ranking to define relevance rules."
                buttonText="New result ranking"
                urlNew={"/console/sn/site/" + id + "/result-ranking/new"}/>
        </>
    )
}