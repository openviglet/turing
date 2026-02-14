import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import * as React from "react"

import { cn } from "@/lib/utils"

const gradientButtonVariants = cva(
    "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all duration-200 cursor-pointer disabled:cursor-not-allowed disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:ring-[3px]",
    {
        variants: {
            variant: {
                default: [
                    "bg-gradient-to-r from-violet-600 to-indigo-600 text-white",
                    "shadow-md shadow-violet-500/25",
                    "hover:from-violet-700 hover:to-indigo-700",
                    "hover:shadow-lg hover:shadow-violet-500/30",
                    "focus-visible:ring-violet-500/50",
                    "dark:from-violet-500 dark:to-indigo-500",
                    "dark:shadow-violet-500/20",
                    "dark:hover:from-violet-600 dark:hover:to-indigo-600",
                    "dark:hover:shadow-violet-500/25",
                    "dark:focus-visible:ring-violet-400/50",
                ].join(" "),
                secondary: [
                    "bg-gradient-to-r from-slate-600 to-slate-700 text-white",
                    "shadow-md shadow-slate-500/25",
                    "hover:from-slate-700 hover:to-slate-800",
                    "hover:shadow-lg hover:shadow-slate-500/30",
                    "focus-visible:ring-slate-500/50",
                    "dark:from-slate-500 dark:to-slate-600",
                    "dark:shadow-slate-500/20",
                    "dark:hover:from-slate-600 dark:hover:to-slate-700",
                    "dark:hover:shadow-slate-500/25",
                    "dark:focus-visible:ring-slate-400/50",
                ].join(" "),
                destructive: [
                    "bg-gradient-to-r from-red-600 to-rose-600 text-white",
                    "shadow-md shadow-red-500/25",
                    "hover:from-red-700 hover:to-rose-700",
                    "hover:shadow-lg hover:shadow-red-500/30",
                    "focus-visible:ring-red-500/50",
                    "dark:from-red-500 dark:to-rose-500",
                    "dark:shadow-red-500/20",
                    "dark:hover:from-red-600 dark:hover:to-rose-600",
                    "dark:hover:shadow-red-500/25",
                    "dark:focus-visible:ring-red-400/50",
                ].join(" "),
                success: [
                    "bg-gradient-to-r from-emerald-600 to-teal-600 text-white",
                    "shadow-md shadow-emerald-500/25",
                    "hover:from-emerald-700 hover:to-teal-700",
                    "hover:shadow-lg hover:shadow-emerald-500/30",
                    "focus-visible:ring-emerald-500/50",
                    "dark:from-emerald-500 dark:to-teal-500",
                    "dark:shadow-emerald-500/20",
                    "dark:hover:from-emerald-600 dark:hover:to-teal-600",
                    "dark:hover:shadow-emerald-500/25",
                    "dark:focus-visible:ring-emerald-400/50",
                ].join(" "),
                outline: [
                    "border-2 border-violet-600 text-violet-600 bg-transparent",
                    "hover:bg-violet-600 hover:text-white",
                    "shadow-sm hover:shadow-md hover:shadow-violet-500/25",
                    "focus-visible:ring-violet-500/50",
                    "dark:border-violet-400 dark:text-violet-400",
                    "dark:hover:bg-violet-500 dark:hover:text-white",
                    "dark:hover:shadow-violet-500/20",
                    "dark:focus-visible:ring-violet-400/50",
                ].join(" "),
                ghost: [
                    "bg-transparent text-violet-600",
                    "hover:bg-violet-500/10 hover:text-violet-700",
                    "focus-visible:ring-violet-500/50",
                    "dark:text-violet-400",
                    "dark:hover:bg-violet-400/10 dark:hover:text-violet-300",
                    "dark:focus-visible:ring-violet-400/50",
                ].join(" "),
            },
            size: {
                default: "h-11 px-5 py-2 has-[>svg]:px-4",
                sm: "h-9 rounded-md gap-1.5 px-4 has-[>svg]:px-3 text-xs",
                lg: "h-12 rounded-md px-8 has-[>svg]:px-6 text-base",
                icon: "size-11",
                "icon-sm": "size-9",
                "icon-lg": "size-12",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
        },
    }
)

function GradientButton({
    className,
    variant,
    size,
    asChild = false,
    ...props
}: React.ComponentProps<"button"> &
    VariantProps<typeof gradientButtonVariants> & {
        asChild?: boolean
    }) {
    const Comp = asChild ? Slot : "button"

    return (
        <Comp
            data-slot="gradient-button"
            className={cn(gradientButtonVariants({ variant, size, className }))}
            {...props}
        />
    )
}

export { GradientButton, gradientButtonVariants }

