import { ROUTES } from "@/app/routes.const";
import { SNSiteSpotlightForm } from "@/components/sn/spotlight/sn.site.spotlight.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteSpotlight } from "@/models/sn/sn-site-spotlight.model";
import { TurSNSiteSpotlightService } from "@/services/sn/sn.site.spotlight.service";
import { IconSpeakerphone } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turSNSiteSpotlightService = new TurSNSiteSpotlightService();

export default function SNSiteSpotlightPage() {
    const navigate = useNavigate();
    const { id, spotlightId } = useParams() as { id: string; spotlightId: string };
    const [spotlight, setSpotlight] = useState<TurSNSiteSpotlight>({} as TurSNSiteSpotlight);
    const [isNew, setIsNew] = useState<boolean>(true);
    const [open, setOpen] = useState(false);

    useEffect(() => {
        if (spotlightId === "new") {
            turSNSiteSpotlightService.getStructure(id).then(setSpotlight);
        } else {
            turSNSiteSpotlightService.get(id, spotlightId).then(setSpotlight);
            setIsNew(false);
        }
    }, [id, spotlightId]);

    async function onDelete() {
        try {
            if (await turSNSiteSpotlightService.delete(spotlight)) {
                toast.success("The spotlight was deleted");
                navigate(`${ROUTES.SN_INSTANCE}/${id}/spotlight`);
            } else {
                toast.error("The spotlight was not deleted");
            }
        } catch (error) {
            console.error("Delete error", error);
            toast.error("The spotlight was not deleted");
        }
        setOpen(false);
    }

    const name = spotlight.name || "New Spotlight";

    return (
        <>
            <SubPageHeader
                icon={IconSpeakerphone}
                name={name}
                feature="Spotlight"
                description="Define content that will be featured in the term-based search."
                onDelete={isNew ? undefined : onDelete}
                open={open}
                setOpen={setOpen}
            />
            <SNSiteSpotlightForm snSiteId={id} value={spotlight} isNew={isNew} />
        </>
    );
}
