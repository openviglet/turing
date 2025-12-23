import type { TurGridItem } from "@/models/ui/grid-item";
import { useMemo } from "react";
type FieldExtractor<T> = keyof T | ((item: T) => string);

interface GridAdapterConfig<T> {
  id?: keyof T | ((item: T) => string | number);
  name: FieldExtractor<T>;
  description: FieldExtractor<T>;
  url: (item: T) => string;
}

export function useGridAdapter<T>(
  data: T[] | undefined | null,
  config: GridAdapterConfig<T>
): TurGridItem[] {
  return useMemo(() => {
    if (!data || data.length === 0) return [];

    const resolveField = (item: T, extractor: FieldExtractor<T>) => {
      if (typeof extractor === "function") {
        return extractor(item);
      }
      return String(item[extractor]);
    };

    return data.map((item) => ({
      id: config.id
        ? typeof config.id === "function"
          ? config.id(item)
          : (item[config.id] as any)
        : (item as any).id,
      name: resolveField(item, config.name),
      description: resolveField(item, config.description),
      url: config.url(item),
    }));
  }, [data]);
}
