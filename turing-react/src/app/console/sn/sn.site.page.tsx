import { useNavigate, useParams } from "react-router-dom";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { useEffect, useState } from "react";
import { TurSNSiteService } from "@/services/sn.service";
import {
    IconAlignBoxCenterStretch,
    IconCpu2,
    IconDashboard,
    IconGitMerge,
    IconLanguage,
    IconNumber123,
    IconReorder,
    IconScale,
    IconSearch,
    IconSettings,
    IconSpeakerphone
} from "@tabler/icons-react";
import { toast } from "sonner";
import { SubPage } from "@/components/sub.page";
import { ROUTES } from "@/app/routes.const";

const turSNSiteService = new TurSNSiteService();
const data = {
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
}
export default function SNSitePage() {
    const { id } = useParams() as { id: string };
    const [snSite, setSnSite] = useState<TurSNSite>({} as TurSNSite);
    const [isNew, setIsNew] = useState<boolean>(true);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate()
    const urlBase = `${ROUTES.SN_INSTANCE}/${id}`;
    useEffect(() => {
        if (id !== "new") {
            turSNSiteService.get(id).then(setSnSite);
            setIsNew(false);
        }
    }, [id])

    async function onDelete() {
        console.log("delete");
        try {
            if (await turSNSiteService.delete(snSite)) {
                toast.success(`The ${snSite.name} Search Engine was deleted`);
                navigate(urlBase);
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
