/**
 * The copy of /educacao/: Viglet Turing on the website of a school, college or
 * university, in Brazilian Portuguese.
 *
 * Every feature named here already ships: the WordPress plugin's blocks and its
 * Portuguese admin, PDF indexing, the date field that ends an opening's time in
 * search, the demo school in the plugin's Docker stack, and Viglet Dumont for the
 * sources that are not WordPress. The features that need a language model say so,
 * as the plugin's own feature table does.
 */

export const EDUCACAO_META = {
  title: "Busca com IA para sites de educação | Viglet Turing",
  description:
    "Busca inteligente para sites de escolas, faculdades e universidades: sugestões enquanto se digita, filtros por curso e data, editais e PDFs encontrados pelo texto, respostas com as fontes e um assistente do site. Open source, instalado nos seus servidores, com plugin para WordPress.",
} as const;

export const EDUCACAO_HERO = {
  eyebrow: "Viglet Turing para instituições de ensino",
  title: "A busca que encontra o que alunos, famílias e pesquisadores procuram no seu site",
  description:
    "Notícias, eventos, cursos, editais e publicações acumulados em anos de site, encontrados por quem procura do jeito que procura. Se você chegou aqui pela caixa de busca de um site, quem fez aquela busca foi o Viglet Turing: open source, instalado na infraestrutura da instituição e com plugin para WordPress.",
} as const;

/** What a visitor types on an education site, and what Viglet Turing does with it. */
export const EDUCACAO_EXAMPLES: { query: string; result: string }[] = [
  {
    query: "inscrições abertas",
    result: "Os editais com prazo em aberto; os vencidos já saíram da busca.",
  },
  {
    query: "palestras em março",
    result: "A agenda filtrada pelo mês que a frase cita.",
  },
  {
    query: "bolsa de mestrado",
    result: "O edital em PDF, achado por uma frase do texto dele.",
  },
  {
    query: "como faço a rematrícula?",
    result: "Uma resposta em poucas linhas, com as páginas usadas como fonte.",
  },
];

export const EDUCACAO_PROBLEM = {
  eyebrow: "Por que a busca importa",
  title: "Um site de educação guarda anos de conteúdo, e o visitante quer uma coisa só",
  body: [
    "O candidato procura o processo seletivo, o aluno procura o calendário, a família procura a mensalidade, o pesquisador procura uma publicação. Tudo isso está no site, espalhado entre notícias, páginas de curso, eventos e PDFs anexados.",
    "A busca padrão compara as palavras digitadas com as do título e do texto. Quem escreve “vestibular” não acha a página que diz “processo seletivo”, quem erra um acento não acha nada, e o edital que só existe em PDF nunca aparece. O visitante desiste da busca e liga para a secretaria.",
  ],
} as const;

export type EducacaoBenefit = Readonly<{
  title: string;
  body: string;
  /** True when the feature needs a language model configured in Viglet Turing. */
  ai: boolean;
}>;

export const EDUCACAO_BENEFITS: EducacaoBenefit[] = [
  {
    title: "Sugestões enquanto se digita",
    body: "As sugestões vêm do que o site tem publicado. Um erro de digitação recebe a correção, e os sinônimos cadastrados no Viglet Turing valem para toda busca: “vestibular” também encontra “processo seletivo”.",
    ai: false,
  },
  {
    title: "Filtros que fazem sentido",
    body: "Tipo de conteúdo, curso, área, unidade, data: os campos que você marca como filtro aparecem ao lado dos resultados, com a contagem de cada um para a busca atual.",
    ai: false,
  },
  {
    title: "Agenda que entende datas",
    body: "Eventos têm data, e a agenda mostra os de hoje, da semana e do mês. Com um modelo de linguagem, uma frase como “palestras em março” vira o filtro de data sozinha.",
    ai: true,
  },
  {
    title: "Editais que saem da busca no prazo",
    body: "Uma vaga, um edital ou uma inscrição deixa a busca no dia seguinte ao encerramento, pela data do próprio conteúdo, sem ninguém precisar despublicar a página.",
    ai: false,
  },
  {
    title: "PDFs encontrados pelo texto",
    body: "Editais, regulamentos, capítulos de livros e apostilas anexados às páginas entram no índice pelo conteúdo. Uma frase do PDF encontra a página que o publica.",
    ai: false,
  },
  {
    title: "Respostas com as fontes",
    body: "Uma pergunta em linguagem natural recebe uma resposta curta, escrita a partir do conteúdo do site, com as páginas usadas como fonte para o visitante conferir.",
    ai: true,
  },
  {
    title: "Assistente do site",
    body: "Um botão abre uma conversa sobre o conteúdo do site. As respostas aparecem marcadas como geradas, com links para as páginas citadas, e a conversa continua enquanto o visitante navega.",
    ai: true,
  },
  {
    title: "Uma busca para vários sites",
    body: "Portal, graduação, pós, biblioteca, núcleo de pesquisa: a busca de um site pode incluir os outros da instituição, e o resultado leva para a página certa em cada um.",
    ai: false,
  },
  {
    title: "Leia também",
    body: "Ao fim de uma notícia ou de uma página de curso, o conteúdo relacionado vem do índice, por semelhança de texto, sem lista montada à mão.",
    ai: false,
  },
  {
    title: "O que o público procura",
    body: "As buscas populares e os insights de busca mostram o que candidatos, alunos e famílias procuram no site, e ajudam a decidir que conteúdo falta.",
    ai: false,
  },
];

export const EDUCACAO_PRIVACY = {
  eyebrow: "Dados da instituição",
  title: "Instalado nos seus servidores, com o modelo que você escolher",
  points: [
    {
      title: "Os dados ficam com a instituição",
      body: "O conteúdo indexado, os embeddings e o histórico de buscas ficam nos servidores que a instituição escolher. Não há versão SaaS coletando esses dados nem telemetria enviada ao fabricante.",
    },
    {
      title: "Você decide para onde vai a pergunta",
      body: "A pergunta e os trechos usados na resposta vão só para o provedor de modelo que você configurar: OpenAI, Anthropic, Google Gemini, Mistral, Cohere, AWS Bedrock, Google Vertex AI ou um endpoint compatível com a API da OpenAI. Com um modelo local (Ollama), eles não saem da sua rede.",
    },
    {
      title: "Acesso controlado",
      body: "O painel administrativo pode ficar atrás do login institucional por OIDC (Keycloak), e as chaves de API dos provedores são guardadas criptografadas.",
    },
  ],
} as const;

export const EDUCACAO_COST = {
  eyebrow: "Custo",
  title: "Software livre, sem licença por aluno ou por busca",
  body: "O Viglet Turing e o plugin para WordPress são open source, sob a licença Apache 2.0. O custo é o do servidor onde eles rodam e, se você ligar os recursos de IA, o do provedor de modelo que escolher. Sem um modelo de linguagem, a busca, os filtros, as sugestões e o conteúdo relacionado funcionam do mesmo jeito.",
} as const;

export const EDUCACAO_STEPS: { title: string; body: string }[] = [
  {
    title: "Suba o Viglet Turing",
    body: "Com Docker, num servidor da instituição ou na nuvem que vocês já usam. Escolha Solr, Elasticsearch ou o Lucene embutido como motor de busca.",
  },
  {
    title: "Instale o plugin no WordPress",
    body: "O plugin Viglet Turing Search tem o painel em português. Aponte para o seu Viglet Turing, marque os tipos de conteúdo e os campos que viram filtro, e reindexe.",
  },
  {
    title: "Coloque a busca onde o visitante procura",
    body: "Os blocos de busca, de conteúdo relacionado e do assistente entram nas páginas pelo editor do WordPress, e a caixa de busca do tema passa a abrir a busca do Viglet Turing.",
  },
];

export const EDUCACAO_DEMO = {
  title: "Veja numa escola de demonstração",
  body: "A pilha Docker do plugin traz a Escola Lumen, uma escola fictícia em português com agenda, editais e biblioteca: eventos por período, um mês lido de uma frase, editais que saem da busca no prazo e um livro encontrado por uma frase do PDF.",
} as const;

export const EDUCACAO_BEYOND = {
  title: "Além do WordPress",
  body: "O sistema acadêmico, o repositório da biblioteca e as pastas de documentos não moram no WordPress. O Viglet Dumont indexa bancos de dados, compartilhamentos de arquivos, Adobe Experience Manager e outras fontes REST no mesmo Viglet Turing, para que uma busca encontre tudo junto.",
} as const;

export const EDUCACAO_FAQ: { q: string; a: string }[] = [
  {
    q: "Funciona só com WordPress?",
    a: "Não. Em WordPress, o plugin Viglet Turing Search indexa o site e coloca a busca nas páginas. Para outras fontes, o Viglet Dumont indexa bancos de dados, arquivos, Adobe Experience Manager e APIs REST, e os SDKs em JavaScript e React levam a busca a sites feitos com outras tecnologias.",
  },
  {
    q: "Precisa de inteligência artificial para funcionar?",
    a: "Não. A busca, as sugestões, a correção de digitação, os sinônimos, os filtros, a agenda por período e o conteúdo relacionado funcionam sem modelo de linguagem. As respostas com fontes, o assistente do site e os filtros lidos de uma frase precisam de um modelo configurado no Viglet Turing, e ficam escondidos enquanto não houver um.",
  },
  {
    q: "A IA pode inventar uma resposta?",
    a: "As respostas são escritas a partir das páginas que a busca encontrou no seu site e mostram essas páginas como fonte, marcadas como geradas, para o visitante conferir. Um modelo de linguagem ainda pode errar ao resumir, e por isso a fonte fica sempre à vista.",
  },
  {
    q: "Funciona em português?",
    a: "Sim. O painel do plugin vem em português do Brasil, e a busca, as respostas e o assistente trabalham com o conteúdo em português do seu site.",
  },
  {
    q: "Quanto custa?",
    a: "O software é livre (Apache 2.0): não há licença por aluno, por página nem por busca. O custo é o da infraestrutura onde o Viglet Turing roda e, se você usar um provedor de modelo na nuvem, o consumo desse provedor.",
  },
  {
    q: "Onde ficam os dados dos visitantes?",
    a: "Nos servidores da instituição. O conteúdo indexado, os embeddings e o histórico de buscas ficam onde o Viglet Turing está instalado. Só a pergunta e os trechos usados na resposta vão ao provedor de modelo que você configurar, e nada vai para fora com um modelo local.",
  },
];
