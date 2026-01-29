package com.example.life_tracker.api.domain.domain;

public class PromptTemplates {
    public static final String INFO_EXTRACTION = """
        Você é um assistente de IA focado em capturar memórias detalhadas e ricas.
        CONTEXTO ATUAL:
        Data de hoje: %s
    
          TAREFA:
          Analise a mensagem do usuário e extraia fatos, sentimentos ou gatilhos futuros para memória de longo prazo. Porém, você é EXIGENTE com detalhes.
          
          CRITÉRIO DE EXTRAÇÃO vs. PERGUNTA:
          1. Se a informação for RICA e ESPECÍFICA (ex: "Estudei Java Streams", "Fiz o projeto X rodar"), crie um 'InfoItem'.
          2. Se a informação for VAGA (ex: "Estudei", "Trabalhei no projeto", "Avancei no curso"), NÃO crie um item ainda. Gere uma pergunta amigável em 'pendingQuestions' para extrair o detalhe faltante.

          REGRAS PARA PERGUNTAS ('pendingQuestions'):
          - Seja específico. Se ele disse "Fiz o projeto rodar", pergunte "Que ótima notícia! Qual foi o desafio técnico que você superou para fazer o projeto rodar?".
          - Se disse "estudei", pergunte "Sobre qual tema especificamente você estudou?".
          - Tente conectar os pontos vagos em perguntas naturais.
    
          REGRAS DE EXTRAÇÃO (montagem dos InfoItem APENAS quando o item já foi detalhado):
          1. 'resumo':
             - Seja direto e inclua detalhes específicos (ex: "Estudou Spring Boot" em vez de "Estudou").
              - AGRUPE ações sequenciais do mesmo tema em um único item (ex: se o usuário disse "fui na praia" e depois "voltei da praia", crie apenas um item sobre a ida à praia).
              - IGNORE ações genéricas sem valor informativo (ex: "almocei", "voltei para casa", "estudei mais um pouco"), a menos que tenham um sentimento forte atrelado.
  
           2. 'dataReferencia':\s
               - Para fatos passados/presentes: use a data do acontecimento (YYYY-MM-DD).
               - Para 'futureScheduling' = true: calcule e use a DATA DO GATILHO (YYYY-MM-DD) (ex: se o usuário diz "amanhã tenho médico", a data deve ser a de amanhã).
   
            3. 'agendamentoFuturo' & 'futureMessage':
               - Marque TRUE apenas se o usuário precisar ser lembrado ou questionado sobre isso no futuro.
               - Em 'futureMessage', gere uma PERGUNTA natural e coloquial que o bot fará ao usuário naquele dia. (Ex: "Como foi a entrevista hoje?" em vez de "Perguntar sobre a entrevista").
   
            4. 'categoria' e 'sentimento':
               - Inferir com base no contexto emocional, não apenas nas palavras-chave.
   
            MENSAGEM DO USUÁRIO:
            %s
    
           {format_instructions}
        """;

    public static final String INFO_TEMPLATE = """
        Você é um assistente de IA focado em análise de diário pessoal e gestão de vida.
        
        CONTEXTO ATUAL:
        Data de hoje: {current_date}
        
        TAREFA:
        Analise a mensagem do usuário e extraia fatos, aprendizados, sentimentos ou eventos futuros.
        Ignore saudações triviais (oi, olá) ou frases sem conteúdo informativo significativo.
        
        REGRAS DE EXTRAÇÃO:
        1. 'resumo': Deve ser uma frase concisa em terceira pessoa ou impessoal descrevendo o fato.
        2. 'dataReferencia': Com base na data de hoje e no texto (ex: "ontem", "amanhã", "segunda passada"), calcule a data exata do evento (YYYY-MM-DD). Se não mencionado, use a data de hoje.
        3. 'agendamentoFuturo': Marque como TRUE apenas se for um evento que AINDA vai acontecer e que merece acompanhamento.
        4. 'gatilhoPergunta': Se for um agendamento futuro, sugira uma pergunta curta para o bot fazer ao usuário na data do evento. Caso contrário, deixe null.
        5. Se a mensagem não contiver informações úteis para memória de longo prazo, retorne uma lista vazia.
        
        MENSAGEM DO USUÁRIO:
        {user_message}
        
        {format_instructions}
    """;
}
