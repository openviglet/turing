import { GradientButton } from '@/components/ui/gradient-button';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import type { TurSNRankingExpression } from '@/models/sn/sn-ranking-expression.model';
import type { TurSNSiteField } from '@/models/sn/sn-site-field.model';
import { TurSNFieldService } from '@/services/sn/sn.field.service';
import { PlusCircle, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Controller, useFieldArray, type Control, type UseFormRegister } from 'react-hook-form';

interface DynamicResultRankingFieldsProps {
    control: Control<any>;
    register: UseFormRegister<any>;
    fieldName: keyof TurSNRankingExpression;
    snSiteId: string;
}
const turSNFieldService = new TurSNFieldService();

export function DynamicResultRankingFields({ control, register, fieldName, snSiteId }: Readonly<DynamicResultRankingFieldsProps>) {
    const { fields, append, remove } = useFieldArray({
        control,
        name: fieldName,
    });
    const [snFields, setSNFields] = useState<TurSNSiteField[]>([]);
    useEffect(() => {
        turSNFieldService.query(snSiteId).then(setSNFields)
    }, [snSiteId]);

    const handleAddField = () => {
        append({ attribute: '', condition: '', value: '' });
    };

    return (
        <div className="flex flex-col gap-4 w-full max-w-2xl">
            {fields.map((field, index) => (
                <div key={field.id} className="flex items-center gap-2">
                    <Controller
                        control={control}
                        name={`${fieldName}.${index}.attribute`}
                        render={({ field: controllerField }) => (
                            <Select
                                onValueChange={controllerField.onChange}
                                value={controllerField.value === null ? '' : String(controllerField.value)}
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
                    <Controller
                        control={control}
                        name={`${fieldName}.${index}.condition`}
                        render={({ field: controllerField }) => (
                            <Select
                                onValueChange={(value) => controllerField.onChange(Number(value))}
                                value={controllerField.value === null ? '' : String(controllerField.value)}
                            >
                                <SelectTrigger>
                                    <SelectValue placeholder="Choose..." />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem key="1" value="1">Is</SelectItem>
                                    <SelectItem key="2" value="2">Is not</SelectItem>
                                </SelectContent>
                            </Select>
                        )}
                    />
                    <Input
                        className="grow"
                        placeholder="Value"
                        {...register(`${fieldName}.${index}.value`)}
                    />
                    <GradientButton
                        variant="ghost"
                        size="icon"
                        onClick={() => remove(index)}
                        aria-label="Remove the field"
                        type="button"
                    >
                        <Trash2 className="h-4 w-4 text-red-500" />
                    </GradientButton>
                </div>
            ))}

            <div className="mt-2">
                <GradientButton variant="outline" onClick={handleAddField} type="button">
                    <PlusCircle className="h-4 w-4 mr-2" />
                    Add
                </GradientButton>
            </div>
        </div>
    );
}