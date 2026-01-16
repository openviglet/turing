import { ROUTES } from '@/app/routes.const';
import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormLabel } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { cn } from "@/lib/utils.ts";
import type { TurRestInfo } from '@/models/auth/rest-info';
import { TurAuthorizationService } from '@/services/auth/authorization.service';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';


export function LoginForm({
    className,
    ...props
}: React.ComponentProps<"form">) {
    const [searchParams] = useSearchParams();
    const returnUrl = searchParams.get('returnUrl') || ROUTES.CONSOLE;
    const form = useForm<TurRestInfo>();
    const [error, setError] = useState('');
    const authorization = new TurAuthorizationService()
    function onSubmit(turRestInfo: TurRestInfo) {
        setError('');
        try {
            if (!turRestInfo.username || !turRestInfo.password) {
                throw new Error('Please enter both username and password');
            }
            authorization.login(turRestInfo.username, turRestInfo.password).then((response) => {
                response.authdata = window.btoa(turRestInfo.username + ':' + turRestInfo.password);
                localStorage.setItem("restInfo", JSON.stringify(response));
                window.location.href = returnUrl;

            });
        } catch (err) {
            if (err instanceof Error) {
                setError(err.message || 'Login failed');
            } else {
                setError('Login failed');
            }
        }
    };
    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className={cn("flex flex-col gap-6", className)} {...props}>
                <div className="flex flex-col items-center gap-2 text-center">
                    <h1 className="text-2xl font-bold">Login to your account</h1>
                    <p className="text-muted-foreground text-sm text-balance">
                        Enter your username below to login to your account
                    </p>
                </div>
                <div className="grid gap-6">
                    <div className="grid gap-3">
                        <FormField
                            control={form.control}
                            name="username"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Username</FormLabel>
                                    <FormControl>
                                        <Input
                                            {...field}
                                            placeholder="Username"
                                            type="text"
                                        />
                                    </FormControl>
                                </FormItem>
                            )}
                        />
                    </div>
                    <div className="grid gap-3">
                        <FormField
                            control={form.control}
                            name="password"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Password</FormLabel>
                                    <FormControl>
                                        <Input
                                            {...field}
                                            placeholder="Password"
                                            type="password"
                                        />
                                    </FormControl>
                                </FormItem>
                            )}
                        />
                    </div>
                    <Button type="submit" className="w-full">
                        Login
                    </Button>
                </div>
            </form>
            <div>{error}</div>
        </Form>
    );
}