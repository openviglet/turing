import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import type { TurSNSiteField } from '@/models/sn/sn-site-field.model';
import { TurSNFieldService } from '@/services/sn.field.service';
import { PlusCircle, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Controller, useFieldArray, type Control, type UseFormRegister } from 'react-hook-form';

interface DynamicResultRankingFieldsProps {
    control: Control<any>;
    register: UseFormRegister<any>;
    fieldName: keyof TurSNSiteField;
    siteId: string;
}
const turSNFieldService = new TurSNFieldService();

export function DynamicResultRankingFields({ control, register, fieldName, siteId }: DynamicResultRankingFieldsProps) {
    const { fields, append, remove } = useFieldArray({
        control,
        name: fieldName,
    });
    const [snFields, setSNFields] = useState<TurSNSiteField[]>([]);
    useEffect(() => {
        turSNFieldService.query(siteId).then(setSNFields)
    }, []);

    const handleAddField = () => {
        append({ id: '', name: '' });
    };

    return (
        <div className="flex flex-col gap-4 w-full max-w-2xl">
            {fields.map((field, index) => (
                <div key={field.id} className="flex items-center gap-2">
                    <Controller
                        control={control}
                        name={`${fieldName}.${index}.locale`}
                        render={({ field: controllerField }) => (
                            <Select
                                onValueChange={controllerField.onChange}
                                defaultValue={controllerField.value}
                            >
                                <SelectTrigger>
                                    <SelectValue placeholder="Choose the field" />
                                </SelectTrigger>
                                <SelectContent>
                                    {snFields.map((option) => (
                                        <SelectItem key={option.id} value={option.id}>
                                            {option.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        )}
                    />

                    <Select>
                        <SelectTrigger>
                            <SelectValue placeholder="Choose..." />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem key="IS" value="IS">Is</SelectItem>
                            <SelectItem key="IS_NOT" value="IS_NOT">Is not</SelectItem>
                        </SelectContent>
                    </Select>
                    <Input
                        className="flex-grow"
                        placeholder="Value"
                        {...register(`${fieldName}.${index}.label`)}
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