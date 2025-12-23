import { ROUTES } from "@/app/routes.const";
import { SubPage } from "@/components/sub.page";
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model";
import { TurIntegrationInstanceService } from "@/services/integration/integration.service";
import {
    IconGitCommit,
    IconGraph,
    IconSearch,
    IconSettings,
    IconTools
} from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

const turIntegrationInstanceService = new TurIntegrationInstanceService();
const data = {
    navMain: [
        {
            title: "Settings",
            url: "/detail",
            icon: IconSettings,
        },
        {
            title: "Sources",
            url: "/source",
            icon: IconGitCommit,
        },
        {
            title: "Indexing Rules",
            url: "/indexing-rule",
            icon: IconTools,
        },
        {
            title: "Monitoring",
            url: "/monitoring",
            icon: IconGraph,
        },
    ],
}
export default function IntegrationInstancePage() {
    const { id } = useParams() as { id: string };
    const [integration, setIntegration] = useState<TurIntegrationInstance>({} as TurIntegrationInstance);
    const [isNew, setIsNew] = useState<boolean>(true);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate()
    const urlBase = `${ROUTES.INTEGRATION_INSTANCE}/${id}`
    useEffect(() => {
        if (id !== "new") {
            turIntegrationInstanceService.get(id).then(setIntegration);
            setIsNew(false);
        }
    }, [id])

    async function onDelete() {
        console.log("delete");
        try {
            if (await turIntegrationInstanceService.delete(integration)) {
                toast.success(`The ${integration.title} Integration Instance was deleted`);
                navigate(urlBase);
            } else {
                toast.error(`The ${integration.title} Integration Instance was not deleted`);
            }

        } catch (error) {
            console.error("Form submission error", error);
            toast.error(`The ${integration.title} Integration Instance was not deleted`);
        }
        setOpen(false);
    }

    return (
        <SubPage icon={IconSearch} feature={"AEM Integration"} name={integration.title}
            onDelete={onDelete} data={data} isNew={isNew} urlBase={urlBase} open={open} setOpen={setOpen} />
    )
}
