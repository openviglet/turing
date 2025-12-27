import { SubPageHeader } from "@/components/sub.page.header";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { IconNumber123 } from "@tabler/icons-react";
import React from "react";

export default function SNSiteResultRankingPage() {
    const [value, setValue] = React.useState([4]);
    return (
        <>
            <SubPageHeader icon={IconNumber123} title="Result Ranking"
                description="Define content that will be featured in the term-based search." />
            <div>

                <Label htmlFor="weight-slider">
                    Will have its weight changed by
                </Label>

                <div className="flex items-center gap-4 mt-3">
                    <Slider
                        id="weight-slider"
                        value={value}
                        onValueChange={setValue}
                        max={10}
                        min={0}
                        step={1}
                    />
                    <span className="w-10 text-right">
                        + {value[0]}
                    </span>

                </div>
            </div>
        </>
    )
}