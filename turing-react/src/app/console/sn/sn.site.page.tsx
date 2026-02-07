import { ROUTES } from "@/app/routes.const";
import { SubPage } from "@/components/sub.page";
import type { TurSNSiteStatus } from "@/models/sn/sn-site-monitoring.model.ts";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { TurSNSiteService } from "@/services/sn/sn.service";
import {
    IconAlignBoxCenterStretch,
    IconCpu2,
    IconDashboard,
    IconDatabase,
    IconGitMerge,
    IconInbox,
    IconLanguage,
    IconNumber123,
    IconReorder,
    IconScale,
    IconSearch,
    IconSettings,
    IconSpeakerphone
} from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
const turSNSiteService = new TurSNSiteService();

export default function SNSitePage() {
    const { id } = useParams() as { id: string };
    const [snSite, setSnSite] = useState<TurSNSite>({} as TurSNSite);
    const [snStatus, setSnStatus] = useState<TurSNSiteStatus | null>(null);
    const [isNew, setIsNew] = useState<boolean>(true);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate()
    const urlBase = `${ROUTES.SN_INSTANCE}/${id}`;
    const data = useMemo(() => {
        if (isNew) {
            return {
                navMain: [
                    {
                        title: "Settings",
                        url: "/detail",
                        icon: IconSettings,
                    },
                ],
            };
        }

        return {
            navMain: [
                {
                    title: "Settings",
                    url: "/detail",
                    icon: IconSettings,
                },
                {
                    title: "Multi Languages",
                    url: "/locale",
                    icon: IconLanguage,
                },
                {
                    title: "Fields",
                    url: "/field",
                    icon: IconAlignBoxCenterStretch,
                },
                {
                    title: "Behavior",
                    url: "/behavior",
                    icon: IconScale,
                },
                {
                    title: "Facet Ordering",
                    url: "/facet-ordering",
                    icon: IconReorder,
                },
                {
                    title: "Generative AI",
                    url: "/ai",
                    icon: IconCpu2,
                },
                {
                    title: "Result Ranking",
                    url: "/result-ranking",
                    icon: IconNumber123,
                },
                {
                    title: "Merge Providers",
                    url: "/merge-providers",
                    icon: IconGitMerge,
                },
                {
                    title: "Spotlight",
                    url: "/spotlight",
                    icon: IconSpeakerphone,
                },
                {
                    title: "Top Search Terms",
                    url: "/top-terms",
                    icon: IconDashboard,
                },
            ],
            counts: [
                {
                    title: "Queue",
                    icon: IconInbox,
                    count: snStatus?.queue ?? 0,
                },
                {
                    title: "Indexed",
                    icon: IconDatabase,
                    count: snStatus?.documents ?? 0,
                },
            ],
        };
    }, [isNew, snStatus]);
    useEffect(() => {
        if (id === "new") {
            setSnSite({} as TurSNSite);
            setSnStatus(null);
            setIsNew(true);
        } else {
            turSNSiteService.get(id).then(setSnSite);
            turSNSiteService.getStatus(id)
                .then(setSnStatus)
                .catch((error) => {
                    console.error("Failed to load SN site monitoring status", error);
                });
            setIsNew(false);
        }

    }, [id])

    async function onDelete() {
        try {
            if (await turSNSiteService.delete(snSite)) {
                toast.success(`The ${snSite.name} Search Engine was deleted`);
                navigate(`${ROUTES.SN_INSTANCE}`);
            } else {
                toast.error(`The ${snSite.name} Search Engine was not deleted`);
            }

        } catch (error) {
            console.error("Form submission error", error);
            toast.error(`The ${snSite.name} Search Engine was not deleted`);
        }
        setOpen(false);
    }

    return (
        <SubPage icon={IconSearch} feature={"Semantic Navigation"} name={snSite.name}
            onDelete={onDelete} data={data} isNew={isNew} urlBase={urlBase} open={open} setOpen={setOpen} />
    )
}
