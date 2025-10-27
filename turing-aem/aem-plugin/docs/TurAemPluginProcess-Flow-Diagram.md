# TurAemPluginProcess - Diagrama de Fluxo

## Visão Geral do Processo de Indexação do AEM

```mermaid
graph TD
    A[Início da Indexação] --> B{Tipo de Indexação?}
    
    B -->|indexAllByNameAsync| C[Buscar Source por Nome]
    B -->|indexAllByIdAsync| D[Buscar Source por ID]
    B -->|sentToIndexStandalone| E[Indexação Standalone]
    
    C --> F[indexAll]
    D --> F
    E --> G[sentToIndexStandalone]
    
    F --> H[Verificar se Source já está executando]
    H -->|Sim| I[Log Warning e Retornar]
    H -->|Não| J[Adicionar à lista de execução]
    
    J --> K[Criar TurConnectorSession]
    K --> L[getTurAemSourceContext]
    L --> M[getNodesFromJson]
    
    G --> N[Validar payload]
    N -->|Vazio| O[Log Warning]
    N -->|Válido| P[Buscar Source por nome]
    P --> Q[Criar TurConnectorSession]
    Q --> R[Processar cada path]
    
    M --> S[byContentTypeList]
    R --> T[indexContentId]
    
    S --> U{ContentType configurado?}
    U -->|Não| V[Log Debug - Tipo não configurado]
    U -->|Sim| W[byContentType]
    
    W --> X[Obter InfinityJSON do rootPath]
    X --> Y[getNodeFromJson]
    
    T --> Z[getTurAemSourceContext]
    Z --> AA[Obter InfinityJSON do contentId]
    AA -->|Encontrado| Y
    AA -->|Não encontrado| BB[createDeIndexJobAndSendToConnectorQueue]
    
    Y --> CC{Tipo igual ContentType?}
    CC -->|Sim| DD[prepareIndexObject]
    CC -->|Não| EE[Verificar se deve indexar filhos]
    
    DD --> FF{Tipo válido?}
    FF -->|CQ_PAGE| GG[Indexar como Página]
    FF -->|DAM_ASSET + CONTENT_FRAGMENT| HH[Indexar como Content Fragment]
    FF -->|DAM_ASSET + STATIC_FILE| II[Indexar como Static File]
    FF -->|Inválido| JJ[Retornar sem indexar]
    
    GG --> KK[indexObject]
    HH --> LL[Definir dataPath = DATA_MASTER]
    II --> MM[Definir dataPath = METADATA]
    
    LL --> KK
    MM --> KK
    
    KK --> NN[indexingAuthor]
    KK --> OO[indexingPublish]
    
    NN -->|isAuthor = true| PP[indexByEnvironment - AUTHOR]
    OO -->|isPublish = true| QQ{Conteúdo entregue?}
    
    QQ -->|Sim| RR[indexByEnvironment - PUBLISHING]
    QQ -->|Não + Standalone| SS[forcingDeIndex]
    QQ -->|Não + Bulk| TT[ignoringDeIndex]
    
    PP --> UU[Criar TurSNJobItem]
    RR --> UU
    
    UU --> VV[getTargetAttrValueMap]
    VV --> WW[getTurSNJobItem]
    WW --> XX[createIndexJobAndSendToConnectorQueue]
    
    XX --> YY[TurJobItemWithSession]
    YY --> ZZ[turConnectorContext.addJobItem]
    
    EE -->|indexChildren = true| AAA{Reactive Indexing?}
    AAA -->|Sim| BBB[getChildrenFromJsonReactive]
    AAA -->|Não| CCC[getChildrenFromJsonSynchronous]
    
    BBB --> DDD[Flux.fromIterable - Processamento Reativo]
    CCC --> EEE[jsonObject.toMap.forEach - Processamento Síncrono]
    
    DDD --> FFF[Filtrar nós indexáveis]
    EEE --> FFF
    
    FFF --> GGG{É nó indexável?}
    GGG -->|Sim| HHH[Obter InfinityJSON do filho]
    GGG -->|Não| III[Pular nó]
    
    HHH --> Y
    
    SS --> JJJ[deIndexJob]
    BB --> JJJ
    JJJ --> KKK[TurSNJobItem com DELETE]
    KKK --> ZZ
    
    ZZ --> LLL[finished]
    LLL --> MMM[Remover da lista de execução]
    MMM --> NNN[turConnectorContext.finishIndexing]
    
    style A fill:#e1f5fe
    style KK fill:#f3e5f5
    style UU fill:#e8f5e8
    style ZZ fill:#fff3e0
    style LLL fill:#fce4ec
```

## Fluxo de Processamento Reativo vs Síncrono

```mermaid
graph LR
    A[getChildrenFromJson] --> B{reativeIndexing?}
    
    B -->|true| C[getChildrenFromJsonReactive]
    B -->|false| D[getChildrenFromJsonSynchronous]
    
    C --> E[Flux.fromIterable]
    E --> F[filter: isIndexedNode]
    F --> G[flatMap: até 10 concurrent]
    G --> H[turAemReactiveUtils.getInfinityJsonReactive]
    H --> I[getNodeFromJsonReactive]
    
    D --> J[jsonObject.toMap.forEach]
    J --> K[if: isIndexedNode]
    K --> L[TurAemCommonsUtils.getInfinityJson]
    L --> M[getNodeFromJson - recursivo]
    
    I --> N[Mono<Void>]
    M --> O[void]
    
    style C fill:#e3f2fd
    style D fill:#f1f8e9
    style G fill:#fff3e0
```

## Estados de um Job de Indexação

```mermaid
stateDiagram-v2
    [*] --> Criado
    Criado --> Validando: Verificar payload
    Validando --> Rejeitado: Payload inválido
    Validando --> Processando: Payload válido
    Processando --> IndexandoAuthor: isAuthor = true
    Processando --> IndexandoPublish: isPublish = true
    IndexandoAuthor --> CriandoJobAuthor: Ambiente AUTHOR
    IndexandoPublish --> VerificandoEntrega: Verificar se conteúdo foi entregue
    VerificandoEntrega --> CriandoJobPublish: Conteúdo entregue
    VerificandoEntrega --> DeIndex: Conteúdo não entregue
    CriandoJobAuthor --> EnviandoParaFila: TurSNJobItem CREATE
    CriandoJobPublish --> EnviandoParaFila: TurSNJobItem CREATE
    DeIndex --> EnviandoParaFila: TurSNJobItem DELETE
    EnviandoParaFila --> Finalizado: addJobItem
    Rejeitado --> [*]
    Finalizado --> [*]
```

## Tipos de Conteúdo Suportados

```mermaid
graph TD
    A[Content Type] --> B{Tipo do Conteúdo?}
    
    B -->|cq:Page| C[Página AEM]
    B -->|dam:Asset + contentFragment| D[Content Fragment]
    B -->|dam:Asset + staticFile| E[Static File]
    B -->|Outro| F[Não Indexável]
    
    C --> G[Processar diretamente]
    D --> H[dataPath = DATA_MASTER]
    E --> I[dataPath = METADATA]
    
    H --> J[Indexar Fragment]
    I --> K[Indexar Asset]
    G --> L[Indexar Página]
    
    J --> M[Ambientes: Author/Publish]
    K --> M
    L --> M
    
    style C fill:#e8f5e8
    style D fill:#e3f2fd
    style E fill:#fff3e0
    style F fill:#ffebee
```

## Configurações e Dependências

```mermaid
graph TD
    A[@Value Annotations] --> B[turing.url]
    A --> C[turing.apiKey]
    A --> D[turing.connector.dependencies.enabled]
    A --> E[turing.connector.reactive.indexing]
    
    F[Repositories] --> G[TurAemSourceRepository]
    F --> H[TurAemAttributeSpecificationRepository]
    F --> I[TurAemPluginSystemRepository]
    F --> J[TurAemTargetAttributeRepository]
    
    K[Services] --> L[TurAemContentMappingService]
    K --> M[TurAemAttrProcess]
    K --> N[TurAemReactiveUtils]
    
    O[Context] --> P[TurConnectorContext]
    O --> Q[TurAemSourceContext]
    
    style A fill:#e1f5fe
    style F fill:#f3e5f5
    style K fill:#e8f5e8
    style O fill:#fff3e0
```
