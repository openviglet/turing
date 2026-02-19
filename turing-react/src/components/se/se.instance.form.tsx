"use client"
import { ROUTES } from "@/app/routes.const"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import {
  Input
} from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select"
import {
  Textarea
} from "@/components/ui/textarea"
import type { TurSEInstance } from "@/models/se/se-instance.model.ts"
import { TurSEInstanceService } from "@/services/se/se.service"
import { useEffect, useState } from "react"
import {
  useForm
} from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { DialogDelete } from "../dialog.delete"
import { GradientButton } from "../ui/gradient-button"
const turSEInstanceService = new TurSEInstanceService();
interface Props {
  value: TurSEInstance;
  isNew: boolean;
}

export const SEInstanceForm: React.FC<Props> = ({ value, isNew }) => {
  const form = useForm<TurSEInstance>({
    defaultValues: value
  });
  const { control } = form;
  const [open, setOpen] = useState(false);
  const navigate = useNavigate()

  useEffect(() => {
    form.reset(value);
  }, [value])

  async function onSubmit(seInstance: TurSEInstance) {
    try {
      if (isNew) {
        const result = await turSEInstanceService.create(seInstance);
        if (result) {
          toast.success(`The ${seInstance.title} Search Engine was saved`);
          navigate(ROUTES.SE_INSTANCE);
        } else {
          toast.error(`The ${seInstance.title} Search Engine was not saved`);
        }
      }
      else {
        const result = await turSEInstanceService.update(seInstance);
        if (result) {
          toast.success(`The ${seInstance.title} Search Engine was updated`);
        } else {
          toast.error(`The ${seInstance.title} Search Engine was not updated`);
        }
      }
    } catch (error) {
      console.error("Form submission error", error);
      toast.error("Failed to submit the form. Please try again.");
    }
  }

  async function onDelete() {
    console.log("delete");
    try {
      if (await turSEInstanceService.delete(value)) {
        toast.success(`The ${value.title} Search Engine was deleted`);
        navigate(ROUTES.SE_INSTANCE);
      }
      else {
        toast.error(`The ${value.title} Search Engine was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${value.title} Search Engine was not deleted`);
    }
    setOpen(false);
  }
  return (
    <div className="flex min-h-[60vh] h-full w-full items-center justify-center px-4">
      <Card className="mx-auto max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">{isNew && (<span>New</span>)} Search Engine</CardTitle>
          <CardAction>
            {!isNew && <DialogDelete feature="Search Engine" name={value.title} onDelete={onDelete} open={open} setOpen={setOpen} />}
          </CardAction>
          <CardDescription>
            Search engine settings.
          </CardDescription>
        </CardHeader >
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 max-w-3xl mx-auto py-10">
              <FormField
                control={control}
                name="title"
                rules={{ required: "Title is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Title</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="Title"
                        type="text"
                      />
                    </FormControl>
                    <FormDescription>
                      Enter a unique and descriptive name for this search engine instance. This title will be shown in the list of search engines.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Description</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Description"
                        className="resize-none"
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>
                      Provide a brief summary of this search engine instance. This helps users understand its purpose or any special configuration.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="turSEVendor.id"
                rules={{ required: "Vendor is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vendor</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Choose..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem key="SOLR" value="SOLR">Apache Solr</SelectItem>
                        <SelectItem key="LUCENE" value="LUCENE">Apache Lucene</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      Select the backend search engine technology that this instance will use. Choose between Apache Solr or Apache Lucene.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={control}
                name="host"
                rules={{ required: "Host is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Host</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Host"
                        type="text"
                        {...field} />
                    </FormControl>
                    <FormDescription>
                      Specify the hostname or IP address where the search engine server is running. For example: <code>localhost</code> or <code>192.168.1.100</code>.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={control}
                name="port"
                rules={{ required: "Port is required." }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Port</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Port"
                        type="number"
                        {...field} />
                    </FormControl>
                    <FormDescription>
                      Enter the network port number used to connect to the search engine server. Common defaults: <code>8983</code> for Solr, <code>9200</code> for Lucene.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <GradientButton type="submit">Save</GradientButton>
            </form>
          </Form>
        </CardContent>
      </Card >
    </div >
  )
}

