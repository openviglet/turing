import { Badge } from "@/components/ui/badge";
import { Globe } from "lucide-react";
import React, { useState } from "react";

interface BadgeLocaleProps {
    locale: string;
    className?: string;
}

const OVERRIDES: Record<string, string> = {
    EN: "US",
    PT: "BR",
    ES: "ES",
    JA: "JP",
    KO: "KR",
};

export const BadgeLocale: React.FC<BadgeLocaleProps> = ({ locale, className }) => {
    const [imgError, setImgError] = useState(false);

    const getCountryCode = (): string => {
        if (!locale) return "";

        let code = locale;
        if (locale.includes("_")) code = locale.split("_")[1];
        else if (locale.includes("-")) code = locale.split("-")[1];

        const upperCode = code.toUpperCase();
        return (OVERRIDES[upperCode] || upperCode).toLowerCase();
    };

    const countryCode = getCountryCode();

    return (
        <Badge
            variant="secondary"
            className={`font-mono gap-2 py-1 pl-1 pr-2 w-fit ${className}`}
        >
            {!imgError && countryCode ? (
                <img
                    src={`https://flagcdn.com/w40/${countryCode}.png`}
                    alt={countryCode}
                    onError={() => setImgError(true)}
                    className="w-5 h-3.5 object-cover rounded-sm shadow-sm"
                />
            ) : (
                <Globe className="w-3.5 h-3.5 text-muted-foreground" />
            )}
            <span className="text-xs font-bold uppercase tracking-tight leading-none">
                {locale}
            </span>
        </Badge>
    );
};