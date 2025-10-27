import {
    closestCenter,
    DndContext,
    type DragEndEvent,
    PointerSensor,
    useSensor,
    useSensors,
} from '@dnd-kit/core';
import {
    arrayMove,
    SortableContext,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical } from 'lucide-react';
import React, { useState } from 'react';

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';

// 1. Defina o tipo para os itens da sua tabela
export interface Facet {
    id: string; // ID único para o dnd-kit
    position: number;
    facetName: string;
    fieldName: string;
}

// 2. Crie um componente para a linha da tabela que será arrastável
interface DraggableTableRowProps {
    row: Facet;
}

const DraggableTableRow: React.FC<DraggableTableRowProps> = ({ row }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
        isDragging,
    } = useSortable({ id: row.id });

    const style: React.CSSProperties = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.8 : 1,
        zIndex: isDragging ? 1 : 0,
        position: 'relative',
    };

    return (
        <TableRow ref={setNodeRef} style={style}>
            <TableCell className="w-12">
                <button {...attributes} {...listeners} className="p-2 cursor-grab active:cursor-grabbing">
                    <GripVertical className="h-5 w-5 text-muted-foreground" />
                </button>
            </TableCell>
            <TableCell className="w-24 font-medium">{row.position}</TableCell>
            <TableCell>{row.facetName}</TableCell>
            <TableCell className="text-muted-foreground">{row.fieldName}</TableCell>
        </TableRow>
    );
};


// 3. Crie o componente principal da tabela
interface DraggableTableProps {
    initialData: Facet[];
}

export const DraggableTable: React.FC<DraggableTableProps> = ({ initialData }) => {
    const [tableData, setTableData] = useState<Facet[]>(initialData);
    const sensors = useSensors(useSensor(PointerSensor));

    const handleDragEnd = (event: DragEndEvent) => {
        const { active, over } = event;

        if (over && active.id !== over.id) {
            setTableData((items) => {
                const oldIndex = items.findIndex((item) => item.id === active.id);
                const newIndex = items.findIndex((item) => item.id === over.id);

                // Reordena o array
                const reorderedItems = arrayMove(items, oldIndex, newIndex);

                // Atualiza a propriedade 'position' de cada item
                return reorderedItems.map((item, index) => ({
                    ...item,
                    position: index + 1,
                }));
            });
        }
    };

    // Extrai os IDs para o SortableContext
    const itemIds = tableData.map(item => item.id);

    return (
        <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
        >
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead className="w-12"></TableHead> {/* Para o ícone de arrastar */}
                            <TableHead className="w-24">Position</TableHead>
                            <TableHead>Facet Name</TableHead>
                            <TableHead>Field Name</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        <SortableContext
                            items={itemIds}
                            strategy={verticalListSortingStrategy}
                        >
                            {tableData.map((row) => (
                                <DraggableTableRow key={row.id} row={row} />
                            ))}
                        </SortableContext>
                    </TableBody>
                </Table>
            </div>
        </DndContext>
    );
};