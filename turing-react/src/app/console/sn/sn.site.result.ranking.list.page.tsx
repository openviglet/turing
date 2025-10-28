import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate.tsx";
import { SubPageHeader } from "@/components/sub.page.header";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { TurSNRankingExpression } from "@/models/sn/sn-ranking-expression.model.ts";
import { TurSNRankingExpressionService } from "@/services/sn.site.result.ranking.service.ts";
import { IconNumber123 } from "@tabler/icons-react";
import { ArrowUpRight } from "lucide-react";
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNRankingExpressionService = new TurSNRankingExpressionService();

// 1. Definição do tipo de dados (pode ser importado)
export interface SiteInfo {
    id: string;
    name: string;
    description: string;
    url: string;
}

// 2. Definição das props do componente
interface SiteListProps {
    sites: SiteInfo[];
}

export const SiteList = ({ sites }: SiteListProps) => {
    // 3. Verificação para caso a lista esteja vazia
    if (!sites || sites.length === 0) {
        return <p>Nenhum site para exibir.</p>;
    }
}

export default function SNSiteResultRankingListPage() {
    const importantSites: SiteInfo[] = [
        {
            id: 'shadcn-ui',
            name: 'shadcn/ui',
            description: 'Componentes de UI lindamente projetados que você pode copiar e colar em seus aplicativos.',
            url: 'https://ui.shadcn.com/',
        },
        {
            id: 'react-dev',
            name: 'React.dev',
            description: 'A documentação oficial do React. O melhor lugar para aprender e se manter atualizado.',
            url: 'https://react.dev/',
        },
        {
            id: 'vercel',
            name: 'Vercel',
            description: 'Plataforma ideal para deploy de aplicações front-end com integrações perfeitas com Next.js e Vite.',
            url: 'https://vercel.com/',
        },
        {
            id: 'github',
            name: 'GitHub',
            description: 'A maior plataforma de hospedagem de código-fonte e controle de versão usando Git.',
            url: 'https://github.com/',
        },
        {
            id: 'figma',
            name: 'Figma',
            description: 'Ferramenta de design de interface colaborativa para criar, testar e prototipar designs de UI/UX.',
            url: 'https://www.figma.com/',
        },
        {
            id: 'dev-to',
            name: 'DEV Community',
            description: 'Uma comunidade de desenvolvedores de software que compartilham conhecimento e artigos.',
            url: 'https://dev.to/',
        },
    ];
    const { id } = useParams() as { id: string };
    const [rankingList, setRankingList] = useState<TurSNRankingExpression[]>({} as TurSNRankingExpression[]);
    useEffect(() => {
        turSNRankingExpressionService.query(id).then(setRankingList);
    }, [id])
    const [value, setValue] = React.useState([4]);


    return (
        <>
            {rankingList.length > 0 ? (
                <>
                    <SubPageHeader icon={IconNumber123} title="Result Ranking"
                        description="Define content that will be featured in the term-based search." />
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {rankingList.map((site) => (
                            <Card key={site.id} className="flex flex-col justify-between hover:border-primary transition-colors">
                                <CardHeader>
                                    <CardTitle className="text-xl">{site.name}</CardTitle>
                                    <CardDescription>{site.description}</CardDescription>
                                </CardHeader>
                                <CardFooter>
                                    <a
                                        href={"result-ranking/" + site.id}
                                        // Aplica o estilo de botão 'ghost' para um visual limpo
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