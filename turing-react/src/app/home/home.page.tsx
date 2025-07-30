import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

const HomePage = () => {
    return (
    <main className="min-h-screen bg-gradient-to-b from-white to-gray-100">
      <header className="w-full border-b bg-white">
        <nav className="container mx-auto flex items-center justify-between py-6">
          <span className="text-2xl font-bold text-primary">Viglet</span>
          <Button variant="outline">Contato</Button>
        </nav>
      </header>

      <section className="container mx-auto flex flex-col items-center justify-center gap-8 py-20">
        <h1 className="text-5xl font-bold text-center">
          Plataforma <span className="text-primary">Inteligente</span> para Desenvolvedores
        </h1>
        <p className="text-lg text-center text-gray-700 max-w-xl">
          Soluções inovadoras, APIs, open source e serviços para acelerar seu desenvolvimento.
        </p>
        <Button size="lg">Comece agora</Button>
      </section>

      <section className="container mx-auto grid grid-cols-1 md:grid-cols-3 gap-8 py-16">
        <Card>
          <CardHeader>
            <CardTitle>APIs Modernas</CardTitle>
          </CardHeader>
          <CardContent>
            <p>Consuma APIs robustas para voz, texto, tradução e muito mais.</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Open Source</CardTitle>
          </CardHeader>
          <CardContent>
            <p>Projetos abertos para a comunidade evoluir juntos.</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Fácil Integração</CardTitle>
          </CardHeader>
          <CardContent>
            <p>SDKs e exemplos práticos para você começar em minutos.</p>
          </CardContent>
        </Card>
      </section>

      <footer className="w-full border-t bg-white py-8 mt-12">
        <div className="container mx-auto flex flex-col md:flex-row justify-between items-center">
          <span className="text-gray-600">&copy; {new Date().getFullYear()} Viglet</span>
          <span className="text-gray-600">Feito com ♥ por Viglet</span>
        </div>
      </footer>
    </main>
    )
}

export default HomePage