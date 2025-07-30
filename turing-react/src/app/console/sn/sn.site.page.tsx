import {
    Sidebar,
    SidebarContent,
    SidebarGroup,
    SidebarGroupContent,
    SidebarHeader,
    SidebarInset,
    SidebarMenu,
    SidebarMenuAction,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarProvider,
} from "@/components/ui/sidebar"
import {NavLink, Outlet, useLocation, useNavigate, useParams} from "react-router-dom";
import type {TurSNSite} from "@/models/sn/sn-site.model.ts";
import React, {useEffect, useState} from "react";
import {TurSNSiteService} from "@/services/sn.service";
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
    IconSpeakerphone,
    IconTrash
} from "@tabler/icons-react";
import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog";
import {toast} from "sonner";

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
    const {id} = useParams() as { id: string };
    const [snSite, setSnSite] = useState<TurSNSite>({} as TurSNSite);
    const [isNew, setIsNew] = useState<boolean>(true);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate()
    const location = useLocation();
    const pathname = location.pathname;
    const urlBase = "/console/sn/instance/" + id;
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
        <div className="flex w-full items-center justify-center px-8 py-4">
            <Card className="w-full bg-sidebar py-1">
                <SidebarProvider
                    style={
                        {
                            "--sidebar-width": "calc(var(--spacing) * 72)",
                            "--header-height": "calc(var(--spacing) * 12)",
                        } as React.CSSProperties
                    }
                >
                    <Sidebar collapsible="none" variant="inset" color="black">
                        <SidebarHeader>
                            <SidebarMenu>
                                <SidebarMenuItem>
                                    <SidebarMenuButton
                                        asChild
                                        className="data-[slot=sidebar-menu-button]:!p-1.5">
                                        <NavLink to="/console">
                                            <IconSearch className="!size-5"/>
                                            {isNew ? (<span
                                                className="text-base font-semibold">New Semantic Navigation</span>) : (
                                                <span className="text-base font-semibold">{snSite.name}</span>)}
                                        </NavLink>
                                    </SidebarMenuButton>
                                    <SidebarMenuAction>{!isNew &&
                                        <Dialog open={open} onOpenChange={setOpen}>
                                            <form>
                                                <DialogTrigger asChild>
                                                    <Button variant={"outline"} className="mr-5"><IconTrash/></Button>
                                                </DialogTrigger>
                                                <DialogContent className="sm:max-w-[450px]">
                                                    <DialogHeader>
                                                        <DialogTitle>Are you absolutely sure?</DialogTitle>
                                                        <DialogDescription>
                                                            Unexpected bad things will happen if you don't read this!
                                                        </DialogDescription>
                                                    </DialogHeader>
                                                    <p className="grid gap-4">
                                                        This action cannot be undone. This will permanently delete
                                                        the {snSite.name} search engine.
                                                    </p>
                                                    <DialogFooter>
                                                        <Button onClick={onDelete} variant="destructive">I understand
                                                            the consequences, delete this search engine</Button>
                                                    </DialogFooter>
                                                </DialogContent>
                                            </form>
                                        </Dialog>
                                    }</SidebarMenuAction>
                                </SidebarMenuItem>
                            </SidebarMenu>
                        </SidebarHeader>
                        <SidebarContent>
                            <SidebarGroup>
                                <SidebarGroupContent className="flex flex-col gap-2">
                                    <SidebarMenu>
                                        {data.navMain.map((item) => (
                                            <SidebarMenuItem key={item.title}>
                                                <SidebarMenuButton tooltip={item.title}
                                                                   isActive={pathname.startsWith(urlBase + item.url)}
                                                                   asChild>
                                                    <NavLink to={urlBase + item.url}>
                                                        {item.icon && <item.icon/>}
                                                        <span>{item.title}</span>
                                                    </NavLink>
                                                </SidebarMenuButton>
                                            </SidebarMenuItem>
                                        ))}
                                    </SidebarMenu>
                                </SidebarGroupContent>
                            </SidebarGroup>
                        </SidebarContent>
                    </Sidebar>
                    <SidebarInset className="mr-1 rounded-xl border ">
                        <main className="flex flex-1 flex-col overflow-hidden xl:ml-8 pt-4">
                            <Outlet/>
                        </main>
                    </SidebarInset>
                </SidebarProvider>
            </Card>
        </div>
    )
}
