# 📘 Life Tracker (AI-Powered Journal)

> 🚧 **Status:** Em desenvolvimento 

## 🎯 Sobre o Projeto

Com a correria do dia a dia — dividindo a atenção entre trabalho, carreira, estudos, hobbies, família e saúde —, frequentemente perdemos a clareza sobre como nossos dias estão passando. *O que aprendi no último mês? O que tem me deixado feliz na última semana? Há quanto tempo uma determinada situação está realmente me incomodando?* Tentar manter um diário tradicional em cadernos quase sempre esbarra no esquecimento e na falta de tempo para revisitar as anotações do passado. O **Life Tracker** nasceu da necessidade de facilitar o acompanhamento da própria vida.

O **Life Tracker** é um diário inteligente e conversacional que interage como um "Amigo Virtual" empático via chat.

O grande diferencial do projeto é o processamento invisível: enquanto o usuário conversa, o sistema utiliza Inteligência Artificial para analisar o diálogo, extrair fatos relevantes (aprendizados, eventos, sentimentos), vetorizar essas informações e armazená-las em um banco de dados semântico. O objetivo final é permitir que o usuário faça perguntas no futuro como *"Quais foram meus maiores desafios no trabalho mês passado?"* e receba respostas precisas baseadas em suas próprias memórias.

Este projeto foi desenvolvido com o foco principal de aprofundar estudos práticos em **Integração de LLMs, Engenharia de Prompt e arquiteturas RAG (Retrieval-Augmented Generation)** utilizando o ecossistema Spring Boot.

---

## 🤖 Conceitos de Inteligência Artificial Aplicados

Para transformar texto livre em dados estruturados e recuperáveis, o projeto explora os seguintes conceitos:

* **Conversational Memory (Memória de Contexto):** Utilização do `InMemoryChatMemory` para manter o estado e o histórico da conversa em tempo real, permitindo que o LLM (Gemini 2.5 Flash) aja de forma contínua e coerente durante o chat diário.
* **Prompt Engineering & Structured Extraction:** Criação de prompts complexos com restrições rígidas para forçar o LLM a atuar como um "Extrator de Dados". O sistema consolida o histórico do chat e obriga a IA a retornar um JSON estrito, mapeando texto livre para Enums e Records no Java (`DailyInfo`).
* **Vectorization (Embeddings):** Transformação dos resumos diários em vetores matemáticos de 768 dimensões utilizando o modelo `gemini-embedding-001`.
* **Vector Database & Semantic Search:** Armazenamento dos embeddings no PostgreSQL utilizando a extensão `pgvector`. Isso prepara o terreno para a recuperação de dados por similaridade de cosseno (busca semântica), muito superior a buscas textuais comuns (LIKE/MATCH).
* **Fundação para RAG (Ingestion Phase):** O pipeline atual implementa a fase crítica de *Ingestão* de uma arquitetura RAG: limpar os dados, gerar metadados de filtro (data, sentimento, categoria), vetorizar e persistir para futura recuperação e injeção de contexto.

---
# 📘Documentação Técnica (Fase 1)

## 🛠 Tech Stack & Infraestrutura

- **Linguagem:** Java 25
- **Framework:** Spring Boot 3.5.10
- **AI Framework:** Spring AI 1.1.2
- **LLM Provider:** Google Vertex AI / Gemini
    - **Chat:** `gemini-2.5-flash`
    - **Embedding:** `gemini-embedding-001` (Dimensão forçada: 768)
- **Banco de Dados:** PostgreSQL 16 + Extensão `pgvector`
- **Containerização:** Docker Compose

---

## 🏗 Arquitetura do Projeto

O projeto adota uma estrutura **Package by Feature** para alta coesão, separando o domínio de "Journaling" (Escrita) de futuras implementações.

### Estrutura de Diretórios

```
com.example.life_tracker
├── api
│   ├── config                # Configurações (VectorStore, AI Client)
│   ├── domain
│   │   ├── common            # Utilitários compartilhados (PromptTemplates)
│   │   ├── domain
│   │   │   ├── mapper        # DailyInfoMapper (DTO -> Document)
│   │   │   ├── model         # DailyInfo (Record/DTO)
│   │   │   └── service       # Lógica de Negócio (JournalingService, IngestionService)
│   │   └── web               # Controllers REST (ChatController)
└── LifeTrackerApplication.java
```

---

## 🔄 Fluxo de Dados: Journaling (Escrita)

O sistema opera em dois estágios: **Interação Síncrona (Chat)** e **Processamento Assíncrono (Persistência)**.

### 1. Interação (Síncrona)

- **Componente:** `JournalingService`
- **Responsabilidade:** Manter a conversa fluida com o usuário ("Conversa de Bar").
- **Memória:** Utiliza `InMemoryChatMemory` para reter o contexto imediato da sessão.
- **Gatilho de Consolidação:** A cada 2 interações completas (configurável), o sistema dispara o processo de ingestão e limpa a memória de curto prazo.

### 2. Ingestão e Persistência (Assíncrona)

- **Componente:** `JournalingIngestionService`
- **Método:** `ingest(String chatHistorySnapshot, UUID userId)`
- **Anotação:** `@Async` (Fire-and-forget).
- **Passos:**
    1. **Extração:** Envia o histórico bruto para o LLM com um prompt estruturado (`INFO_EXTRACTION`).
    2. **Conversão:** O LLM retorna um JSON estrito mapeado para o objeto `DailyInfo`.
    3. **Enriquecimento:** O `DailyInfoMapper` converte os itens em `Document` (Spring AI), injetando metadados críticos.
    4. **Persistência:** Salva os documentos no `VectorStore` (Postgres).

---

## 💾 Modelo de Dados (Banco Vetorial)

A tabela `vector_store` armazena tanto o conteúdo semântico quanto os metadados estruturados para filtragem futura.

### Schema

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | `uuid` | Identificador único do vetor. |
| `content` | `text` | O fato resumido (ex: "Estudou Spring AI pela manhã"). |
| `embedding` | `vector(768)` | Representação vetorial do conteúdo gerada pelo `text-embedding-004`. |
| `metadata` | `jsonb` | Dados estruturados para filtragem (RAG). |

### Estrutura do JSON Metadata

Cada registro possui os seguintes campos indexáveis no JSONB:

```json
{
  "userId": "9cfd9fa2-110e-49a3-8148-65daa18d9c68",
  "date": "2026-01-29",
  "category": "STUDIES",
  "feeling": "GOOD",
  "futureScheduling": false
}
```

---

## 🔒 Segurança & Multi-tenancy

- **Estratégia Atual:** Isolamento Lógico via Metadados.
- **Implementação:** Todos os vetores recebem obrigatoriamente um `userId` nos metadados.
- **Estado Atual:** O `userId` está "hardcoded" (`DEFAULT_USER_ID`) aguardando a implementação do sistema de Autenticação/Login.

---

## 🚀 Próximos Passos

1. Implementar o **Orquestrador de Intenção** (Log vs Search).
2. Criar o fluxo de **Recuperação (RAG)** filtrando por `userId`.
3. Implementar **autenticação real** (Spring Security) para substituir o ID fixo.