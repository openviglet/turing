import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import type { PropsWithChildren } from "react";
import { NavLink } from "react-router-dom";

interface Props {
  items: TurSNSite[] | undefined;
}

export const SNCardList: React.FC<PropsWithChildren<Props>>= ({ items }) => {
    return <div className="*:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card grid grid-cols-1 gap-4 px-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:shadow-xs lg:px-6 @xl/main:grid-cols-2 @5xl/main:grid-cols-4">
        {items?.map((item) => (
            <Card key={item.id} className="@container/card">
                <CardHeader>
                    <CardTitle>{item.name}</CardTitle>
                    <CardDescription>{item.description}</CardDescription>
                </CardHeader>
                <CardContent>
                    <p>{item.turSEInstance.title}</p>
                </CardContent>
                <CardFooter className="flex-col gap-2">
                    <Button type="submit" className="w-full" asChild>
                        <NavLink to={"/admin/sn/instance/" + item.id}>Edit</NavLink>
                    </Button>
                </CardFooter>
            </Card>
        ))}
    </div>;
}