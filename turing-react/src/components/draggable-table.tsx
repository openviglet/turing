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
import React from 'react';

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import type { TurSNSiteField } from '@/models/sn/sn-site-field.model';
import { TurSNFacetedFieldService } from '@/services/sn/sn.faceted.field.service';
import { toast } from 'sonner';

interface DraggableTableRowProps {
    row: TurSNSiteField;
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
            <TableCell className="w-24 font-medium">{row.facetPosition}</TableCell>
            <TableCell>{row.facetName}</TableCell>
            <TableCell className="text-muted-foreground">{row.name}</TableCell>
        </TableRow>
    );
};


interface DraggableTableProps {
    id: string;
    tableData: TurSNSiteField[];
    setTableData: React.Dispatch<React.SetStateAction<TurSNSiteField[]>>;
}
const turSNFacetedFieldService = new TurSNFacetedFieldService();
export const DraggableTable: React.FC<DraggableTableProps> = ({ id, tableData, setTableData }) => {


    const sensors = useSensors(useSensor(PointerSensor));

    const handleDragEnd = async (event: DragEndEvent) => {
        const { active, over } = event;
        if (over && active.id !== over.id) {
            const oldIndex = tableData.findIndex((item) => item.id === active.id);
            const newIndex = tableData.findIndex((item) => item.id === over.id);

            const reorderedItems = arrayMove(tableData, oldIndex, newIndex).map(
                (item, index) => ({
                    ...item,
                    facetPosition: index + 1,
                })
            );

            setTableData(reorderedItems);

            try {
                await turSNFacetedFieldService.saveOrdering(id, reorderedItems);
                toast.success('Facet ordering saved.');
            } catch (error) {
                console.error('Failed to save facet ordering', error);
                toast.error('Failed to save facet ordering.');
            }
        }
    };

    const itemIds = tableData.map(item => item.id);

    return (
        <div className='px-6'>
            <DndContext
                sensors={sensors}
                collisionDetection={closestCenter}
                onDragEnd={handleDragEnd}
            >
                <div className="rounded-md border">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-12"></TableHead>
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
        </div>
    );
};