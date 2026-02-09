import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import type { TurLocale } from '@/models/locale/locale.model';
import type { TurSNSiteField } from '@/models/sn/sn-site-field.model';
import { TurLocaleService } from '@/services/locale/locale.service';
import { PlusCircle, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Controller, useFieldArray, type Control, type UseFormRegister } from 'react-hook-form';

interface DynamicLanguageFieldsProps {
    control: Control<any>;
    register: UseFormRegister<any>;
    fieldName: keyof TurSNSiteField;
}
const turLocaleService = new TurLocaleService();
export function DynamicLanguageFields({ control, register, fieldName }: Readonly<DynamicLanguageFieldsProps>) {
    const { fields, append, remove } = useFieldArray({
        control,
        name: fieldName,
    });
    const [locales, setLocales] = useState<TurLocale[]>([]);
    useEffect(() => {
        turLocaleService.query().then(setLocales)
    }, []);

    const handleAddField = () => {
        append({ id: '', locale: '', label: '' });
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
                                    <SelectValue placeholder="Choose the language" />
                                </SelectTrigger>
                                <SelectContent>
                                    {locales.map((option) => (
                                        <SelectItem key={option.initials} value={option.initials}>
                                            {option.en} ({option.initials})
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        )}
                    />

                    <Input
                        className="grow"
                        placeholder="Facet Name..."
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