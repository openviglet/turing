import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PlusCircle, Trash2 } from 'lucide-react';
import { useFieldArray, type Control, type UseFormRegister } from 'react-hook-form';

interface DynamicIndexingRuleFieldsProps {
    control: Control<any>;
    register: UseFormRegister<any>;
    fieldName: string;
}

export function DynamicIndexingRuleFields({
    control,
    register,
    fieldName
}: Readonly<DynamicIndexingRuleFieldsProps>) {
    const { fields, append, remove } = useFieldArray({
        control,
        name: fieldName,
    });

    const handleAddField = () => {
        append('');
    };

    return (
        <div className="flex flex-col gap-4 w-full">
            {fields.map((field, index) => (
                <div key={field.id} className="flex items-center gap-2">
                    <Input
                        className="grow"
                        placeholder="Value"
                        {...register(`${fieldName}.${index}`)}
                    />
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => remove(index)}
                        aria-label="Remove the field"
                        type="button"
                    >
                        <Trash2 className="h-4 w-4 text-red-500" />
                    </Button>
                </div>
            ))}

            <div className="mt-2">
                <Button variant="outline" onClick={handleAddField} type="button">
                    <PlusCircle className="h-4 w-4 mr-2" />
                    Add
                </Button>
            </div>
        </div>
    );
}