
import { createContext, type ReactNode, useCallback, useContext, useMemo, useState } from "react";
export type BreadcrumbItem = {
    label: string;
    href?: string;
};

interface BreadcrumbContextType {
    items: BreadcrumbItem[];
    setItems: (items: BreadcrumbItem[]) => void;
    pushItem: (item: BreadcrumbItem) => void;
    popItem: () => void;
    resetBreadcrumb: (initialItems?: BreadcrumbItem[]) => void;
}

const BreadcrumbContext = createContext<BreadcrumbContextType | undefined>(undefined);
export function BreadcrumbProvider({ children }: { readonly children: ReactNode }) {
    const [items, setItems] = useState<BreadcrumbItem[]>([]);

    // No seu context
    const pushItem = useCallback((newItem: BreadcrumbItem) => {
        setItems((prev) => {
            // Se o último item já tem o mesmo label, não adiciona de novo
            if (prev.length > 0 && prev[prev.length - 1].label === newItem.label) {
                return prev;
            }
            return [...prev, newItem];
        });
    }, []);
    const popItem = () => {
        setItems((prev) => prev.slice(0, -1));
    };

    const resetBreadcrumb = (initialItems: BreadcrumbItem[] = []) => {
        setItems(initialItems);
    };

    const contextValue = useMemo(
        () => ({
            items,
            setItems,
            pushItem,
            popItem,
            resetBreadcrumb,
        }),
        [items]
    );

    return (
        <BreadcrumbContext.Provider value={contextValue}>
            {children}
        </BreadcrumbContext.Provider>
    );
}

export const useBreadcrumb = () => {
    const context = useContext(BreadcrumbContext);
    if (!context) {
        // É AQUI que o erro é gerado
        throw new Error("useBreadcrumb must be used within Provider");
    }
    return context;
};