# Thindesk — Dashboard administrativo

Painel de acompanhamento de chamados integrado ao Spring Boot 3.4.0, MongoDB, Thymeleaf e à identidade visual existente (Bootstrap, sidebar cinza e `style.css`). A página inicial e o CRUD existente permanecem disponíveis. Acesse `/dashboard` pelo menu após autenticar.

## Documentação da atividade

A [documentação revisada em Word](docs/Documentacao_Thindesk.docx) reúne os requisitos, os contratos da API, as decisões de projeto e os resultados de validação.

Evidências citadas no documento: [respostas reais da API](docs/evidencias/api-respostas.json), [resultados dos testes automatizados](docs/evidencias/testes-automatizados.json) e [observação da interface do dashboard](docs/evidencias/dashboard-observado.txt).

## Vídeo de apresentação

[Assistir ou baixar a apresentação do Thindesk](docs/apresentacao/video-thindesk.mp4). Duração aproximada: **2min32s**, dentro do limite de três minutos.

## Entrega com Gitflow

Repositório: [nickk-eng/DashboardThindesk](https://github.com/nickk-eng/DashboardThindesk).

| Branch | Finalidade |
| --- | --- |
| `main` | Base original; não recebe diretamente a implementação. |
| `develop` | Base de integração, com configuração privada separada e arquivos compilados fora do versionamento. |
| `feature/api-dashboard` | Implementação da API integrada ao MongoDB, dashboard, cadastros de apoio, testes e documentação. |

A branch da entrega é **`feature/api-dashboard`**. O fluxo de revisão é `feature/api-dashboard` → `develop`; uma futura release pode integrar `develop` à `main` após aprovação. Nenhum merge de release é necessário para demonstrar esta implementação.

```powershell
git clone https://github.com/nickk-eng/DashboardThindesk.git
cd DashboardThindesk
git switch feature/api-dashboard
Copy-Item .thindesk-local.properties.example .thindesk-local.properties
```

Preencha a URI do Atlas **somente em `.thindesk-local.properties`**, na raiz. Esse arquivo é importado pelo Spring Boot quando a aplicação é iniciada a partir da pasta do projeto e é ignorado pelo Git. O arquivo de exemplo contém apenas marcadores. Também é possível usar as variáveis de ambiente descritas abaixo, sem criar o arquivo local. Não envie a configuração privada ao GitHub. O histórico inicial já continha credenciais embutidas; movê-las para um arquivo ignorado não as remove de commits anteriores, portanto o responsável pelo Atlas deve substituí-las.

Validação de integração realizada na aplicação em execução em `localhost:8080`, conectada ao Atlas: HTTP 200 com `sucesso=true` em indicadores, evolução mensal, situação, tipos, listagem paginada e indicadores com filtros de status/data. As chamadas foram somente de leitura; nenhuma massa de dados foi adicionada.

## Análise do projeto base e mapeamento

Os arquivos indicados foram inspecionados antes da implementação. `pom.xml` mantém empacotamento WAR e Java release 23. O pacote da aplicação/configurações é `com.pfc`; o domínio e os serviços estão em `com.pfc.thindesk`. A configuração original autentica qualquer página protegida, sem uma distinção de perfis de administrador. O dashboard segue essa política: qualquer usuário autenticado pode consultar os dados.

| Campo original de Chamado | MongoDB / carga original | Uso |
| --- | --- | --- |
| `id: String` | `_id`, ObjectId `675b1fa1d64af05ea59ee7d3` no JSON | Identificação |
| `descricao: String` | `descricao`: Chamado de Teste | Tabela e busca |
| `status: String` | `status`: Aberto | Indicadores, distribuição e filtro |
| `tipo: String` | A carga usava **`Tipo`**: Manutenção | Ranking e filtro; carga corrigida para `tipo` |
| `tecnico: String` | `tecnico`: João | Tabela e busca |
| `usuario: String` | `usuario`: Alberto | Solicitante; não é vínculo com Cliente |

O modelo e a carga não continham datas, considerações técnicas nem vínculo com cliente. Foram adicionados os campos opcionais:

| Campo novo | Tipo Java / MongoDB | Regra |
| --- | --- | --- |
| `dataAbertura` | Instant / BSON Date | Novas criações recebem o instante atual se omitido; não se inventa data histórica |
| `dataConclusao` | Instant / BSON Date | Informação opcional explicitamente fornecida; não inferida pelo status |
| `clienteId` | String | Referência lógica a `Cliente.id`, sem confundir com `usuario` |
| `consideracoesTecnicas` | String | Texto opcional, exibido integralmente na tabela |

`Cliente` possui `id`, `nome`, `telefone`, `setor` e usa a coleção `clientes`. `HorarioAtendimento` possui `id`, `setor`, `diaSemana`, `horarioInicio`, `horarioFim` e usa `horariosAtendimento`; não representa data de abertura de chamado. Os campos foram preservados; os acessores e construtores de horários agora são explícitos, sem depender da geração pelo Lombok para renderizar e preencher formulários em JDKs recentes. `MongoInitConfig` apenas cria coleções ausentes: **não importa o arquivo `chamados.json`**. A propriedade `spring.mongodb.embedded.data` existente não implementa importação neste projeto.

O dashboard usa agregações de leitura, sem migração destrutiva. Na consulta, `tipo` ausente/nulo recorre ao legado `Tipo`. Valores vazios são normalizados para “Não informado” em status/tipo; demais textos vazios são exibidos com indicação de ausência. Datas ausentes ou inválidas viram nulas na consulta. Não existe SLA ou duração média calculável de forma confiável na carga original, portanto essas métricas não são inventadas.

## Execução

Requisitos: JDK 23 para o release configurado, MongoDB 4.2+ (operadores de agregação usados) e acesso à internet para dependências e Chart.js/Bootstrap via CDN. O ambiente desta entrega utilizou JDK 27. O Surefire configura automaticamente `net.bytebuddy.experimental=true` apenas nos testes, para permitir que o Byte Buddy da base execute com JDKs mais recentes, inclusive no “Limpar e Construir” do NetBeans.

A conexão padrão aponta para o MongoDB Atlas `cluster0.p737yt3.mongodb.net`, banco `thindesk`. Defina `MONGODB_USERNAME` e `MONGODB_PASSWORD` no ambiente antes de iniciar o NetBeans ou a aplicação, usando o usuário do banco cadastrado em Database Access (não o login do site do Atlas). As credenciais devem estar codificadas para URL quando contiverem caracteres especiais. Autorize também seu IP em Network Access no Atlas. Reinicie o NetBeans após criar variáveis permanentes do Windows.

Sem o arquivo privado local, `MONGODB_URI`, se definida, tem prioridade sobre `MONGODB_USERNAME` e `MONGODB_PASSWORD` e substitui a conexão completa. Se `.thindesk-local.properties` definir diretamente `spring.data.mongodb.uri`, essa configuração importada prevalece sobre a definição padrão do arquivo principal. Nenhuma senha real é incluída nos novos commits. As credenciais presentes no histórico anterior do repositório devem ser rotacionadas pelo responsável pelo banco.

```powershell
$env:MONGODB_USERNAME = 'SEU_USUARIO_DO_BANCO'
$env:MONGODB_PASSWORD = 'SUA_SENHA_CODIFICADA_PARA_URL'
# Opcional: para utilizar outra conexao, inclusive MongoDB local:
# $env:MONGODB_URI = 'mongodb://localhost:27017/thindesk'
# Para substituir a conta demonstrativa existente (admin / thindesk):
$env:SPRING_SECURITY_USER_NAME = 'admin'
$env:SPRING_SECURITY_USER_PASSWORD = 'uma-senha-local'
.\mvnw.cmd clean spring-boot:run
```

Abra `http://localhost:8080/login`, autentique-se e entre em `http://localhost:8080/dashboard`. A configuração de banco continua em `src/main/resources/application.properties`, database `thindesk`.

Para importar **somente em uma base de desenvolvimento**, com MongoDB Database Tools instalado:

```powershell
mongoimport --uri $env:MONGODB_URI --db thindesk --collection chamados --file src/main/resources/chamados.json
```

Para esse comando de importação, defina primeiro `MONGODB_URI` com a conexão completa: o programa `mongoimport` não lê as propriedades do Spring. A carga contém um único objeto Extended JSON, sem datas. Repetir a importação pode causar conflito do `_id` fixo. Nenhuma importação foi executada automaticamente nesta entrega.

## Requisitos funcionais

| Requisito | Especificação e comportamento verificável |
| --- | --- |
| RF01 — Indicadores gerais | Total, abertos, em andamento, resolvidos, outros, taxa de resolução, sem data e com considerações. Taxa = resolvidos / total × 100, duas casas; total zero retorna 0. |
| RF02 — Chamados por período | Quantidade por data de abertura, agrupada por dia ou mês no fuso America/Sao_Paulo, ordenada cronologicamente. Períodos sem registros não são preenchidos; `semData` informa os excluídos do gráfico. |
| RF03 — Situação | Quantidade e percentual de todos os status presentes, inclusive desconhecidos e não informados. Preserva os rótulos existentes após remover espaços externos. |
| RF04 — Tipos mais atendidos | Ranking de demanda por tipo, quantidade decrescente, desempate por nome. Mede chamados registrados, não apenas encerrados. Para medir os concluídos, aplique o filtro de status. |
| RF05 — Filtros | Datas inclusivas, status, tipo, cliente por ID e busca textual combinados com AND, aplicados no MongoDB para todas as cinco consultas coletivas. |
| RF06 — Atualização assíncrona | Filtros e botão Atualizar Dados consultam a API via fetch sem recarregar a página. Paginação, limpar filtros, loading e cancelamento de requisições anteriores. |
| RF07 — Qualidade e falhas | Normalização de campos ausentes, proteção de divisão por zero, erros HTTP em JSON, mensagens amigáveis, sessão expirada e estado vazio na interface. Considerações são inseridas como texto, sem interpretar HTML. |

Os KPIs agrupam `Aberto`/`Abertos`/`Aberta`; `Em andamento`; `Concluído`, `Concluido`, plurais masculinos, `Concluída`/`Concluida`, `Fechado`/`Fechados`/`Fechada`, ignorando maiúsculas. Outros valores permanecem em `outros` e na distribuição. `dataConclusao` não muda automaticamente quando o status é editado. O filtro de status corresponde ao rótulo específico, não ao grupo do KPI.

## Requisitos não funcionais

| Requisito | Decisão de implementação e limites |
| --- | --- |
| RNF01 — Segurança | API exige autenticação de sessão ou HTTP Basic; ausência de autenticação retorna 401 JSON sem redirecionamento. Páginas MVC mantêm login HTML. Não foi criada hierarquia de perfis inexistente. Usar HTTPS em produção. |
| RNF02 — Compatibilidade e manutenção | Mantém rotas anteriores, coleção `chamados`, serviços/repositórios e contratos do CRUD. API nova isolada em `com.pfc.thindesk.api`, advice restrito a esse pacote e campos novos opcionais. |
| RNF03 — Eficiência | Agrupamentos, filtros e paginação no MongoDB; página de 1 a 100 itens (20 por padrão), ordenação determinística por abertura e ID. Não carrega a coleção inteira na JVM. |
| RNF04 — Usabilidade e acessibilidade | Layout responsivo, labels, títulos, tabela com cabeçalhos, resumos textuais dos gráficos, aria-live/aria-busy, estados de vazio/erro/carregamento e fallback textual se Chart.js não carregar. |
| RNF05 — Confiabilidade e verificabilidade | Testes JUnit 5/MockMvc de contrato, segurança, filtros e falhas; respostas uniformes; erros internos registrados no servidor sem detalhes técnicos na resposta. |

Decisões de MongoDB: campos novos no documento evitam joins para métricas; referência `clienteId` é suficiente para filtragem e não altera o cadastro de clientes. Não há integridade referencial automática ou lookup por nome do cliente. `$addFields` normaliza o legado antes do `$match`; `$group`, `$facet`, `$sort`, `$skip` e `$limit` executam o trabalho no banco. **A normalização antes do filtro pode impedir o uso eficiente de índices**: não se promete latência constante ou desempenho validado em grandes volumes. Uma evolução para grande escala exige medir planos, padronizar documentos persistidos e então projetar índices adequados. Os gráficos carregam as categorias existentes; cardinalidade muito alta também exige limites definidos pelo negócio.

CSRF já estava desabilitado na aplicação original e essa política foi preservada. Os endpoints novos do dashboard são somente GET. Antes de uso produtivo, avaliar CSRF nas rotas legadas de escrita, perfis de acesso e configuração dos segredos. Os testes automatizados não dependem do MongoDB remoto; a verificação manual da aplicação consultou o Atlas apenas para leitura.

## API do dashboard

Base: `/api/dashboard`. Todas as respostas de sucesso têm `Content-Type: application/json` e envelope `{"sucesso":true,"dados":...,"erro":null}`. Chamadas via frontend usam a sessão do login; para curl há HTTP Basic com as mesmas credenciais. A API legada `/api/chamados` mantém seu formato anterior por compatibilidade; a padronização de dados/erros descrita aqui aplica-se à **nova API do dashboard**, e os erros de autenticação JSON aplicam-se a `/api/**`.

| Método e rota | Dados de sucesso | Parâmetros específicos |
| --- | --- | --- |
| GET `/api/dashboard/indicadores` | Objeto de KPIs | — |
| GET `/api/dashboard/por-periodo` | `agrupamento`, `semData`, `periodos` | `agrupamento=dia` (padrão) ou `mes` |
| GET `/api/dashboard/situacao` | Lista `{nome,quantidade,percentual}` de status | — |
| GET `/api/dashboard/tipos` | Lista `{nome,quantidade,percentual}` de tipos | — |
| GET `/api/dashboard/chamados` | `total`, `pagina`, `tamanho`, `itens` | `pagina` baseada em zero (0 a 1000000); `tamanho` de 1 a 100 |
| GET `/api/dashboard/chamados/{id}` | Detalhamento de um chamado | ID hexadecimal ObjectId de 24 caracteres; sem filtros coletivos |

### Parâmetros comuns às cinco consultas coletivas

Todos opcionais, combináveis e URL-encoded quando necessário:

| Parâmetro | Semântica |
| --- | --- |
| `dataInicio` | AAAA-MM-DD, início do dia inclusive no fuso America/Sao_Paulo |
| `dataFim` | AAAA-MM-DD, até o fim desse dia (implementado como início exclusivo do dia seguinte). Deve ser >= dataInicio; ano final menor que 9999. |
| `status` | Correspondência integral, ignorando caixa; espaços externos removidos. Ex.: Aberto. |
| `tipo` | Correspondência integral, ignorando caixa. Ex.: Manutenção. |
| `cliente` | Correspondência integral de `clienteId`, ignorando caixa; não corresponde a nome ou usuario. |
| `busca` | Substring literal em descricao OU tecnico OU usuario OU consideracoesTecnicas, ignorando caixa. Metacaracteres regex são escapados. |

Textos limitados a 200 caracteres. Textos vazios não filtram. Datas ausentes/inválidas nos documentos são excluídas se houver limite de data. Sem filtro de data, participam dos totais e de `semData`. Nomes de atributos da resposta são estáveis e textos ausentes da listagem retornam string vazia, exceto status/tipo com “Não informado”. Datas retornam ISO 8601 ou null. Percentuais são numéricos (não strings com `%`) e podem somar 99,99/100,01 devido ao arredondamento.

### Requests e responses reproduzíveis

Os exemplos abaixo consideram **uma base de desenvolvimento contendo apenas o registro de `chamados.json`**. Não representam uma consulta executada contra o banco remoto. Em PowerShell use `curl.exe`; os comandos pedem a senha do usuário `admin` e não a gravam no histórico.

**RF01 — indicadores**

```powershell
curl.exe -u admin "http://localhost:8080/api/dashboard/indicadores"
```

```json
{"sucesso":true,"dados":{"total":1,"abertos":1,"emAndamento":0,"resolvidos":0,"outros":0,"taxaResolucao":0.0,"semDataAbertura":1,"comConsideracoesTecnicas":0},"erro":null}
```

**RF02 — evolução**

```powershell
curl.exe -u admin "http://localhost:8080/api/dashboard/por-periodo?agrupamento=mes"
```

```json
{"sucesso":true,"dados":{"agrupamento":"mes","semData":1,"periodos":[]},"erro":null}
```

Com dois registros datados em novembro de 2026 e um sem data, a forma completa de resposta seria:

```json
{"sucesso":true,"dados":{"agrupamento":"mes","semData":1,"periodos":[{"periodo":"2026-11","quantidade":2}]},"erro":null}
```

**RF03 — situação**

```powershell
curl.exe -u admin "http://localhost:8080/api/dashboard/situacao"
```

```json
{"sucesso":true,"dados":[{"nome":"Aberto","quantidade":1,"percentual":100.0}],"erro":null}
```

**RF04 — tipos**

```powershell
curl.exe -u admin "http://localhost:8080/api/dashboard/tipos"
```

```json
{"sucesso":true,"dados":[{"nome":"Manutenção","quantidade":1,"percentual":100.0}],"erro":null}
```

**RF05 — listagem filtrada e paginação**

```powershell
curl.exe -u admin --get "http://localhost:8080/api/dashboard/chamados" --data-urlencode "status=Aberto" --data-urlencode "tipo=Manutenção" --data-urlencode "busca=Teste" --data-urlencode "pagina=0" --data-urlencode "tamanho=20"
```

```json
{"sucesso":true,"dados":{"total":1,"pagina":0,"tamanho":20,"itens":[{"id":"675b1fa1d64af05ea59ee7d3","descricao":"Chamado de Teste","status":"Aberto","tipo":"Manutenção","tecnico":"João","usuario":"Alberto","clienteId":"","consideracoesTecnicas":"","dataAbertura":null,"dataConclusao":null}]},"erro":null}
```

**Detalhe**

```powershell
curl.exe -u admin "http://localhost:8080/api/dashboard/chamados/675b1fa1d64af05ea59ee7d3"
```

```json
{"sucesso":true,"dados":{"id":"675b1fa1d64af05ea59ee7d3","descricao":"Chamado de Teste","status":"Aberto","tipo":"Manutenção","tecnico":"João","usuario":"Alberto","clienteId":"","consideracoesTecnicas":"","dataAbertura":null,"dataConclusao":null},"erro":null}
```

**Filtro completo (a carga original não tem data nem cliente e por isso não corresponde)**

```powershell
curl.exe -u admin --get "http://localhost:8080/api/dashboard/chamados" --data-urlencode "dataInicio=2026-11-01" --data-urlencode "dataFim=2026-11-14" --data-urlencode "status=Aberto" --data-urlencode "tipo=Manutenção" --data-urlencode "cliente=cliente-1" --data-urlencode "busca=Teste"
```

```json
{"sucesso":true,"dados":{"total":0,"pagina":0,"tamanho":20,"itens":[]},"erro":null}
```

Para alimentar datas e considerações em novos dados, a rota existente `POST /api/chamados` aceita, por exemplo, este corpo (seu contrato de resposta continua sendo o Chamado sem envelope):

```json
{"descricao":"Revisão de estação","status":"Concluído","tipo":"Manutenção","tecnico":"João","usuario":"Alberto","clienteId":"cliente-1","dataAbertura":"2026-11-14T12:00:00Z","dataConclusao":"2026-11-14T14:00:00Z","consideracoesTecnicas":"Limpeza e atualização concluídas."}
```

### Erros HTTP e evidências

| HTTP | Situação | Comportamento |
| --- | --- | --- |
| 200 | Consulta válida, inclusive sem resultados | Listas vazias e métricas zero |
| 400 | Datas inválidas/invertidas, agrupamento inválido, paginação inválida, texto >200, ID malformado | Envelope com mensagem de parâmetros inválidos |
| 401 | Sessão ausente/expirada ou Basic inválido | JSON, sem `Location` para login |
| 403 | Acesso negado pela segurança | JSON; a política atual permite qualquer usuário autenticado |
| 404 | ObjectId válido sem registro no detalhe | JSON “Chamado não encontrado” |
| 500 | Falha interna ou indisponibilidade do MongoDB | JSON amigável; causa registrada no servidor |

```powershell
# 400
curl.exe -i -u admin "http://localhost:8080/api/dashboard/indicadores?dataInicio=2026-11-15&dataFim=2026-11-01"
# 401
curl.exe -i "http://localhost:8080/api/dashboard/indicadores"
# 404 (considerando que o ID não exista)
curl.exe -i -u admin "http://localhost:8080/api/dashboard/chamados/000000000000000000000000"
```

```json
{"sucesso":false,"dados":null,"erro":{"status":400,"mensagem":"Parâmetros inválidos. Use datas AAAA-MM-DD, agrupamento dia/mês (mes) e limites válidos."}}
```

```json
{"sucesso":false,"dados":null,"erro":{"status":401,"mensagem":"Autenticação necessária. Entre novamente no sistema."}}
```

```json
{"sucesso":false,"dados":null,"erro":{"status":404,"mensagem":"Chamado não encontrado"}}
```

```json
{"sucesso":false,"dados":null,"erro":{"status":500,"mensagem":"Não foi possível consultar os dados. Tente novamente mais tarde."}}
```

O cenário 500 é simulado nos testes; para capturá-lo via curl utilize uma instância exclusivamente de desenvolvimento apontando para um MongoDB indisponível, nunca interrompa o banco compartilhado para gerar evidências. Parâmetros válidos em uma rota desconhecida não constituem o caso 404 de recurso documentado acima.

## Testes e evidências

```powershell
.\mvnw.cmd clean verify
# A compatibilidade do Byte Buddy já está configurada no pom.xml.
node --check src/main/resources/static/js/dashboard.js
```

Validação atual: **44 testes passaram, sem falhas ou erros**, com compilação limpa e geração do WAR concluídas: 26 testes do dashboard e 18 de Configurações em `ConfiguracaoWebTest`. A sintaxe de `dashboard.js` também foi validada.

`DashboardApiTest` utiliza JUnit 5/MockMvc, serviço real e MongoTemplate simulado. Valida os cinco endpoints com filtros, critérios gerados para o MongoDB (inclusive limite final exclusivo e busca literal), normalização legada, dados vazios, percentuais, evolução mensal, paginação/considerações, parâmetros inválidos, 404, 500 e autenticação JSON/MVC, inclusive sessão obtida pelo formulário e HTTP Basic válido/inválido. `DashboardViewTest` renderiza o template Thymeleaf real para verificar o dashboard dentro do layout/sidebar e a preservação da página inicial. Os testes não executam as agregações contra um servidor MongoDB real nem validam visualmente um navegador. Essa validação deve ser complementada em uma base de desenvolvimento com os curl acima e navegação no painel.

Para prints: capture os KPIs, os três gráficos, a tabela com considerações, um filtro aplicado, o estado vazio e as respostas com `-i` para 400/401/404. Chamados históricos sem data devem aparecer na tabela e no contador de qualidade, mas não na curva temporal. Não há screenshots fabricados incluídos.

## Inventário exato de arquivos

Criados em `src/main/`:

```text
src/main/java/com/pfc/thindesk/api/ApiResponse.java
src/main/java/com/pfc/thindesk/api/DashboardApiAdvice.java
src/main/java/com/pfc/thindesk/api/DashboardApiController.java
src/main/java/com/pfc/thindesk/api/DashboardFiltro.java
src/main/java/com/pfc/thindesk/api/DashboardService.java
src/main/java/com/pfc/thindesk/api/RecursoNaoEncontradoException.java
src/main/java/com/pfc/thindesk/controller/ConfiguracaoController.java
src/main/java/com/pfc/thindesk/entity/ConfiguracaoItem.java
src/main/java/com/pfc/thindesk/repository/ConfiguracaoItemRepository.java
src/main/java/com/pfc/thindesk/service/ConfiguracaoService.java
src/main/resources/templates/configuracoes.html
src/main/resources/templates/dashboard.html
src/main/resources/static/js/dashboard.js
```

Alterados em `src/main/`:

```text
src/main/java/com/pfc/SecurityConfig.java
src/main/java/com/pfc/thindesk/controller/HomeController.java
src/main/java/com/pfc/thindesk/controller/HorarioAtendimentoController.java
src/main/java/com/pfc/thindesk/entity/Chamado.java
src/main/java/com/pfc/thindesk/entity/HorarioAtendimento.java
src/main/java/com/pfc/thindesk/service/ChamadoService.java
src/main/java/com/pfc/thindesk/service/HorarioAtendimentoService.java
src/main/resources/application.properties
src/main/resources/chamados.json
src/main/resources/templates/layout.html
src/main/resources/templates/ajustes-horarios.html
src/main/resources/templates/error.html
src/main/resources/templates/fragments/sidebar.html
src/main/resources/static/css/style.css
```

Outros arquivos criados: `README.md`, `.gitignore`, `src/test/java/com/pfc/thindesk/DashboardApiTest.java`, `src/test/java/com/pfc/thindesk/DashboardViewTest.java`, `src/test/java/com/pfc/thindesk/ConfiguracaoWebTest.java`. `pom.xml` foi alterado para dependências de teste, Surefire com JUnit 5 e nomes de parâmetros de métodos disponíveis para o Spring. `MongoInitConfig.java`, `Cliente.java`, `ChamadoController.java`, `home.html` e `script.js` foram analisados e preservados.

## Correção do menu Configurações

As rotas `/tecnicos`, `/tipo-chamado` e `/recursos` agora possuem telas no layout padrão, com listagem, cadastro, edição e exclusão (via POST com confirmação). Os cadastros têm nome obrigatório (até 120 caracteres) e descrição opcional (até 1000), persistidos na coleção `configuracoes`, discriminados por `categoria`. As operações verificam a categoria para evitar editar/excluir um cadastro de outra tela. A coleção é criada pelo MongoDB no primeiro salvamento; nenhuma carga automática é executada. Esses cadastros de apoio não renomeiam ou excluem valores já registrados nos campos textuais de chamados.

A tela `/ajustes-horarios` agora recebe corretamente `horarios` e `novoHorario`, usa o layout/sidebar e possui rotas MVC para salvar e excluir. O fim deve ser posterior ao início no mesmo dia; intervalos que atravessam a meia-noite devem ser cadastrados separadamente. Os caminhos antigos `/api/ajustes-horarios` e `/api/ajustes-horarios/salvar` permanecem como aliases HTML do fluxo MVC; não constituem uma API JSON. A exclusão passa a usar POST `/ajustes-horarios/deletar/{id}` (também disponível sob o prefixo legado `/api`).

A antiga página de erro embutia um link indisponível do Canva e foi substituída por HTML local, com mensagem e links para início/dashboard. Os testes de Configurações usam serviços reais e repositórios simulados para verificar as quatro páginas com dados, salvamento, edição, exclusão, isolamento por categoria, validação de formulários, login e renderização da página de erro em HTML.

Os códigos completos estão na branch `feature/api-dashboard`; arquivos gerados em `target/` e a conexão privada `.thindesk-local.properties` não fazem parte do código versionado. O arquivo `.thindesk-local.properties.example` documenta a configuração sem credenciais reais.
