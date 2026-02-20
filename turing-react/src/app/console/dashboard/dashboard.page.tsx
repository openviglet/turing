import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { TurSNSiteMetricsService } from "@/services/sn/sn.site.metrics.service";
import { useEffect, useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';



const turSNSiteMetricsService = new TurSNSiteMetricsService();
export default function RealTimeDashboardPage() {
    const [data, setData] = useState<any[]>([]);

    const fetchMetrics = async () => {
        try {
            // Chama o endpoint que traz o acumulado da última hora
            const response = await turSNSiteMetricsService.live('2727a7d5-7450-4a4d-b3dc-01e729103ce4');
            if (Array.isArray(response)) {
                setData(response);
            }
        } catch (error) {
            console.error("Erro ao buscar métricas de 1h", error);
        }
    };

    useEffect(() => {
        fetchMetrics();
        const interval = setInterval(fetchMetrics, 30000); // Atualiza a cada 30s
        return () => clearInterval(interval);
    }, []);

    return (
        <Card className="w-full bg-slate-950 text-white">
            <CardHeader>
                <CardTitle>Acessos (Última 1 Hora)</CardTitle>
                <CardDescription>Acumulado a cada 30 segundos</CardDescription>
            </CardHeader>
            <CardContent>
                <div className="h-[400px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={data}>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#222" />
                            <XAxis
                                dataKey="time"
                                // Mostra o horário a cada 10 minutos para não poluir
                                interval={20}
                                tickFormatter={(str) => new Date(str).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                fontSize={11}
                                tick={{ fill: '#666' }}
                            />
                            <YAxis fontSize={11} tick={{ fill: '#666' }} />
                            <Tooltip
                                labelFormatter={(label) => new Date(label).toLocaleTimeString()}
                                contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #334155' }}
                            />
                            <Line
                                type="stepAfter" // 'stepAfter' é ótimo para dados acumulados/intervalos
                                dataKey="accesses"
                                stroke="#3b82f6"
                                strokeWidth={2}
                                dot={false} // Remove pontos para focar na linha de tendência
                                isAnimationActive={true}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            </CardContent>
        </Card>
    );
}