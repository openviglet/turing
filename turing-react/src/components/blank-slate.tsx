import { Button } from "@/components/ui/button.tsx";
import React from "react";
import { NavLink } from "react-router-dom";

interface Props {
    icon: React.ElementType
    title: string;
    description: string;
    buttonText: string;
    urlNew?: string;
}

export const BlankSlate: React.FC<Props> = ({ icon: Icon, title, description, urlNew, buttonText }) => {
    return (
        <div className="space-y-4 text-center mt-8 px-6">
            <Icon className="inline-block" size={48} />
            <h1 className="mb-1">{title}</h1>
            <p className="text-muted-foreground text-sm mt-1">{description}</p>
            {urlNew && (<Button className="mt-4">
                <NavLink to={urlNew}>
                    {buttonText}
                </NavLink></Button>)}
        </div>
    )
}
