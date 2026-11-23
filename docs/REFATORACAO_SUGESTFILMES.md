# SugestFilmes — Documentação da Refatoração Arquitetural

> **Projetos:** `SugestFilmes-API` (backend) e `SugestFilmes-Front_API` (frontend JSP)  
> **Branch de trabalho do backend:** `refactor/route-controller`  
> **Branch de trabalho do frontend:** `refactor/frontend-dao`  
> **Data do registro:** 21/11/2026  
> **Escopo:** reorganização de responsabilidades, preservando as funcionalidades e os contratos HTTP existentes.

## 1. Solicitação original do Alfredo

O objetivo solicitado foi **organizar a arquitetura**, sem recriar a aplicação nem adicionar frameworks:

1. **Backend:** deixar de concentrar todas as rotas REST, operações e métodos HTTP em um único `ApiController.java`. Criar um `RouteController` responsável por receber as requisições e encaminhá-las aos controllers específicos por recurso (filmes, categorias, avaliações, usuários etc.). Cada controller implementa as operações compatíveis com o respectivo recurso.
2. **Frontend:** retirar as chamadas HTTP feitas diretamente pelos `Services`, criar uma pasta `dao/` e transferir para ela a responsabilidade de acessar a API. Os `Services` devem continuar representando a camada intermediária entre os Servlets de interface e a comunicação externa.
3. **Preservar:** o comportamento existente, a autenticação JWT, as regras de administrador, os DAOs JDBC do backend, as consultas SQL e a compatibilidade entre frontend e backend.

**Não foi solicitada a adoção de Spring, Spring Boot, Hibernate ou JPA.** A implementação permanece em Java 17, Jakarta Servlet/JSP, Gson, JDBC, Maven, Tomcat e Docker.

## 2. Situação anterior

### 2.1 Backend

A classe `br.com.mvc.api.ApiController` era um `HttpServlet` mapeado em `/api/*` e continha, no mesmo arquivo:

- Identificação do recurso, do método HTTP e do ID;
- Rotas de `/api/health` e `/api/auth/login`;
- GET, POST, PUT e DELETE de filmes, categorias, avaliações, usuários e perfis, conforme suportado;
- Consulta de recomendações;
- Autorização de administrador, leitura de claims JWT e criação de respostas JSON;
- Tratamento de erros HTTP.

Os `Services` do backend e os `DAOs` JDBC **já estavam separados** e não precisavam ser reconstruídos.

```text
Cliente HTTP
    |
    v
ApiController (todas as rotas e operações)
    |
    v
Service do recurso
    |
    v
DAO JDBC do backend
    |
    v
MySQL
```

### 2.2 Frontend

O frontend JSP já tinha `Servlet Controllers`, `Services`, modelos e um cliente HTTP reutilizável (`ApiClient`). Contudo, os `Services` chamavam diretamente o `ApiClient`.

```text
Navegador -> JSP / Servlet -> Service -> ApiClient -> API REST
```

Não havia uma camada `dao/` específica para organizar as chamadas à API externa.

## 3. Nova arquitetura do backend

### 3.1 Separação por recurso

A entrada HTTP agora é o `RouteController`, mapeado para `/api/*`. Ele identifica caminho e método e encaminha a operação para o controller apropriado.

```text
Requisição HTTP
       |
       v
CorsFilter -> JwtFilter (exceto rotas públicas)
       |
       v
RouteController  @WebServlet("/api/*")
       |
       +-- /health --------------> resposta de saúde
       +-- /auth/login ----------> AuthController
       +-- /filmes[/{id}] -------> FilmeController
       +-- /categorias[/{id}] ---> CategoriaController
       +-- /avaliacoes[/{id}] ---> AvaliacaoController
       +-- /recomendacoes -------> RecomendacaoController
       +-- /usuarios[/{id}] ----> UsuarioController
       +-- /perfis[/{id}] -------> PerfilController
                                          |
                                          v
                                    Service existente
                                          |
                                          v
                                     DAO JDBC
                                          |
                                          v
                                        MySQL
```

O `ApiSupport` concentra funções compartilhadas de resposta JSON, resposta de erro e verificação de permissões administrativas. O `JwtFilter` passou a usar `ApiSupport.erro()` em vez do método estático de erro do antigo `ApiController`.

### 3.2 Estrutura principal do backend

```text
SugestFilmes-API/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── init.sql
└── src/main/java/br/com/mvc/
    ├── api/
    │   ├── RouteController.java       [NOVO — entrada /api/*]
    │   ├── ApiSupport.java            [NOVO — utilitários HTTP/JSON]
    │   ├── ApiController.java         [ANTIGO — preservado sem @WebServlet]
    │   ├── JwtFilter.java             [ALTERADO]
    │   └── CorsFilter.java            [MANTIDO]
    ├── controller/                    [NOVA PASTA]
    │   ├── AuthController.java
    │   ├── FilmeController.java
    │   ├── CategoriaController.java
    │   ├── AvaliacaoController.java
    │   ├── RecomendacaoController.java
    │   ├── UsuarioController.java
    │   └── PerfilController.java
    ├── service/                       [EXISTENTE]
    ├── dao/                           [EXISTENTE — JDBC/MySQL]
    ├── model/                         [EXISTENTE]
    ├── config/                        [EXISTENTE]
    └── util/                          [EXISTENTE — JWT e senhas]
```

**Importante:** `ApiController.java` foi preservado para comparação e possível reversão; a anotação de mapeamento `/api/*` foi transferida ao `RouteController` para evitar dois Servlets na mesma rota.

### 3.3 Responsabilidades

| Camada | Responsabilidade |
|---|---|
| `CorsFilter` | Tratar origem permitida e preflight CORS. |
| `JwtFilter` | Exigir/validar JWT nas rotas protegidas e disponibilizar claims na requisição. |
| `RouteController` | Identificar recurso, caminho, verbo HTTP e parâmetros de rota; delegar ao controller certo; centralizar erros. |
| Controller específico | Ler entrada HTTP/JSON, verificar autorização cabível, chamar Service e escrever resposta. |
| `ApiSupport` | Respostas JSON/erros e auxiliares de autorização. |
| Service do backend | Validações e regras de negócio. |
| DAO do backend | Persistência via JDBC/SQL no MySQL. |

### 3.4 Endpoints preservados

| Recurso | Métodos e caminhos |
|---|---|
| Saúde | `GET /api/health` (público) |
| Login | `POST /api/auth/login` (público) |
| Filmes | `GET, POST /api/filmes`; `GET, PUT, DELETE /api/filmes/{id}` |
| Categorias | `GET, POST /api/categorias`; `GET, PUT, DELETE /api/categorias/{id}` |
| Avaliações | `GET, POST /api/avaliacoes`; `PUT, DELETE /api/avaliacoes/{id}` |
| Recomendações | `GET /api/recomendacoes?categorias=1,2` |
| Usuários | `GET, POST /api/usuarios`; `GET, PUT, DELETE /api/usuarios/{id}` |
| Perfis | `GET, POST /api/perfis`; `PUT, DELETE /api/perfis/{id}` |

**Proteções preservadas no desenho:** operações administrativas requerem perfil `Administrador`; o ID do autor de uma avaliação é obtido das claims validadas do JWT; respostas públicas de usuário não incluem a senha. O frontend mantém o JWT na sessão do servidor, e não no JavaScript.

## 4. Nova arquitetura do frontend

### 4.1 Mudança principal

**Antes**:

```text
Navegador
   -> Servlet (controller web)
   -> Service
   -> ApiClient (HTTP direto)
   -> SugestFilmes-API
```

**Depois**:

```text
Navegador
   -> Servlet (controller web)
   -> Service
   -> DAO de integração HTTP
   -> ApiClient
   -> SugestFilmes-API
   -> RouteController / Controller de recurso
   -> Service do backend
   -> DAO JDBC
   -> MySQL
```

O termo **DAO no frontend** designa aqui uma **camada de acesso à API**, não uma conexão direta com o banco. O `ApiClient` continua encapsulando `HttpClient`, cabeçalhos, Bearer token, serialização JSON e erros HTTP.

### 4.2 Estrutura principal do frontend

```text
SugestFilmes-Front_API/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── src/main/
    ├── java/br/com/sugestfilmes/
    │   ├── api/
    │   │   ├── ApiClient.java       [EXISTENTE]
    │   │   └── ApiException.java    [EXISTENTE]
    │   ├── controller/             [EXISTENTE — Servlets JSP]
    │   ├── dao/                    [NOVA PASTA]
    │   │   ├── FilmeDAO.java
    │   │   ├── CategoriaDAO.java
    │   │   ├── AvaliacaoDAO.java
    │   │   ├── RecomendacaoDAO.java
    │   │   ├── UsuarioDAO.java
    │   │   ├── PerfilDAO.java
    │   │   └── AuthDAO.java
    │   ├── service/                [ALTERADO]
    │   │   ├── FilmeService.java
    │   │   ├── CategoriaService.java
    │   │   ├── AvaliacaoService.java
    │   │   ├── RecomendacaoService.java
    │   │   ├── UsuarioService.java
    │   │   ├── PerfilService.java
    │   │   └── AuthService.java
    │   ├── filter/                 [EXISTENTE]
    │   ├── config/                 [EXISTENTE]
    │   └── model/                  [EXISTENTE]
    └── webapp/
        ├── WEB-INF/jsp/            [TELAS EXISTENTES]
        └── css/                    [ESTILOS EXISTENTES]
```

### 4.3 O que foi movido

| Recurso | DAO adicionado | Service adaptado | Operações encaminhadas |
|---|---|---|---|
| Filmes | `FilmeDAO` | `FilmeService` | Listar, buscar, criar, atualizar, excluir |
| Categorias | `CategoriaDAO` | `CategoriaService` | Listar, buscar, criar, atualizar, excluir |
| Avaliações | `AvaliacaoDAO` | `AvaliacaoService` | Minhas, por filme, criar, atualizar, excluir |
| Recomendações | `RecomendacaoDAO` | `RecomendacaoService` | Consultar por categorias |
| Usuários | `UsuarioDAO` | `UsuarioService` | Listar, buscar, criar, atualizar, excluir |
| Perfis | `PerfilDAO` | `PerfilService` | Listar, criar, atualizar, excluir |
| Login | `AuthDAO` | `AuthService` | POST de autenticação |

Os métodos públicos dos `Services` foram mantidos com as assinaturas anteriores. Isso evita obrigar os Servlets JSP a mudar seus pontos de chamada.

## 5. Fluxos principais de ponta a ponta

### 5.1 Login

```text
Usuário envia login/senha no formulário JSP
  -> LoginServlet
  -> AuthService (prepara payload)
  -> AuthDAO (POST /auth/login)
  -> ApiClient
  -> RouteController do backend
  -> AuthController
  -> UsuarioService (verifica credenciais)
  -> JwtUtil (emite JWT)
  -> JSON com token e usuário público
  -> LoginServlet guarda JWT na HttpSession
```

### 5.2 Listagem/pesquisa de filmes

```text
GET na tela de filmes (eventual texto de busca)
  -> FilmeServlet
  -> FilmeService (organiza o parâmetro q)
  -> FilmeDAO (GET /filmes?q=...)
  -> ApiClient (envia JWT)
  -> JwtFilter -> RouteController -> FilmeController
  -> FilmeService do backend -> FilmeDAO JDBC -> MySQL
  -> JSON de filmes -> Servlet -> JSP
```

### 5.3 Cadastro ou atualização de filme

```text
Formulário JSP (admin)
  -> FilmeServlet
  -> FilmeService do frontend (monta JSON, categoriasIds)
  -> FilmeDAO (POST /filmes ou PUT /filmes/{id})
  -> ApiClient (Authorization: Bearer ...)
  -> JwtFilter -> RouteController -> FilmeController
  -> validação do perfil Administrador
  -> FilmeService do backend (regras, categoria obrigatória)
  -> DAO JDBC -> MySQL
  -> HTTP 201/200 ou erro JSON
```

### 5.4 Registro de avaliação

```text
Usuário autenticado registra nota/comentário
  -> AvaliacaoServlet -> AvaliacaoService -> AvaliacaoDAO
  -> POST /api/avaliacoes com JWT
  -> JwtFilter -> RouteController -> AvaliacaoController
  -> usuarioId obtido do JWT (não do JSON enviado)
  -> AvaliacaoService do backend -> AvaliacaoDAO JDBC -> MySQL
  -> resposta JSON
```

### 5.5 Tratamento de requisições não autorizadas

```text
Requisição sem Bearer token -> JwtFilter -> HTTP 401
Token válido, mas sem perfil admin em operação restrita
  -> Controller/ApiSupport -> HTTP 403
Recurso inexistente -> HTTP 404 no caso tratado
JSON/ID inválido -> resposta 400 conforme o tratamento da rota
```

## 6. Impactos da mudança

### Benefícios esperados

- **Responsabilidade única:** roteamento separado do tratamento específico de filmes, usuários e outros recursos.
- **Manutenção facilitada:** localizar a regra HTTP de um recurso sem navegar por um `ApiController` monolítico.
- **Extensibilidade:** adicionar um recurso envolve um controller e um encaminhamento explícito de rota.
- **Reuso:** padronização de respostas e verificações de administrador via `ApiSupport`.
- **Separação de integrações no frontend:** Services não precisam conhecer diretamente o `ApiClient` nem os verbos HTTP.
- **Menor impacto nas telas:** os métodos públicos dos Services foram mantidos, evitando mudanças desnecessárias nos Servlets e nas JSPs.

### O que não deveria mudar

- Endereços REST e tipos de requisição utilizados pelo frontend;
- Regras de negócio já implementadas nos Services do backend;
- Persistência JDBC/SQL e esquema do MySQL;
- Autenticação e autorização previstas na API;
- Interface JSP e CSS do frontend;
- Stack técnica sem novos frameworks.

**Atenção:** benefícios de organização não substituem testes de regressão; a compatibilidade completa só pode ser afirmada depois da validação integrada.

## 7. Evidências de compilação e testes realizados

### Backend — confirmado na execução local

- `mvn clean package`: **BUILD SUCCESS**, com **34 arquivos Java** compilados;
- Docker Compose: MySQL **healthy** e Tomcat/API em execução;
- `GET /api/health`: resposta `status=ok`;
- Login da conta de demonstração Maria: autenticado;
- `GET /api/filmes` com JWT: listagem de filmes;
- `GET /api/filmes` sem JWT: **401**;
- `POST /api/filmes` com usuário não administrador: **403**;
- `GET /api/filmes/999999`: **404**;
- Listagens de categorias (**10**), recomendações na categoria 1 (**13**), usuários (**3**) e perfis (**3**);
- Consulta de avaliações por filme e criação de avaliação de teste;
- CRUD de filme temporário **ID 31**: POST, GET, PUT e DELETE realizados; GET posterior retornou **404**;
- CRUD de categoria temporária **ID 11**: POST, GET, PUT e DELETE realizados; GET posterior retornou **404**.

### Frontend — confirmado na compilação local

- Após criação dos **sete DAOs** e adaptação dos Services: `mvn clean package` retornou **BUILD SUCCESS**, com **33 arquivos Java** compilados.

### Ainda não comprovado / pendências de fechamento

- **Teste integrado visual do frontend JSP** rodando no Docker e consumindo o backend refatorado: não foi apresentado resultado na conversa.
- **Testes automatizados:** ambos os builds registraram `No tests to run`; compilação não equivale a cobertura de regressão.
- **CRUD completo de avaliações, usuários e perfis e todos os cenários-limite**: não foram integralmente verificados.
- **Paridade exata** de códigos HTTP, mensagens e serialização de JSON (incluindo nulos e formatos de data): precisa de revisão final.
- **Arquivo antigo do backend:** `ApiController.java` foi mantido no código sem o mapeamento de Servlet; a remoção definitiva fica condicionada à revisão.
- **Avaliação de teste criada pela Maria (ID 1):** a exclusão não foi confirmada na conversa; verificar/limpar caso permaneça no banco local.
- **Push e Pull Requests:** não há confirmação apresentada de publicação das branches ou de merge na `main`.

Portanto, a **implementação estrutural e a compilação das duas frentes estão concluídas**, mas a **entrega funcional definitiva ainda depende de integração, revisão e publicação**.

## 8. Execução local e verificação rápida

### Backend

```powershell
cd "SDP FILMES BACK END"
mvn clean package
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8081/api/health
```

### Frontend

```powershell
cd "SDP FILMES FRONT END"
mvn clean package
docker compose up --build -d
docker compose ps
```

Acessar `http://localhost:8080` e verificar login, listagem, detalhes, categorias, avaliações e navegação. O frontend usa `API_BASE_URL=http://host.docker.internal:8081/api` quando executado no container conforme o Compose original.

> Não usar `docker compose down -v` sem necessidade: remove volumes e pode apagar dados locais. As credenciais embutidas no Compose de desenvolvimento não devem ser utilizadas em ambiente público ou de produção.

## 9. Controle de versão e entrega para revisão

| Repositório | Branch | Propósito |
|---|---|---|
| `AlfredoFariaF/SugestFilmes-API` | `refactor/route-controller` | Revisão do roteamento e dos controllers REST |
| `AlfredoFariaF/SugestFilmes-Front_API` | `refactor/frontend-dao` | Revisão da camada DAO de comunicação HTTP |

Um commit local inicial do backend foi registrado como `f4e8b6a` (`refactor: inicia separacao das rotas e controller de filmes`). O registro desse commit **não confirma a publicação** de todas as modificações posteriores.

**Fluxo recomendado:** verificar `git status` e `git diff --check`, registrar as alterações pendentes, publicar cada branch e abrir um Pull Request específico em cada repositório. Não realizar merge na `main` antes da revisão do Alfredo.

## 10. Resumo executivo para o Alfredo

A refatoração reorganizou o SugestFilmes sem alterar intencionalmente suas funcionalidades ou adicionar frameworks. No **backend**, o antigo controller centralizado foi substituído como entrada HTTP por um `RouteController` que delega a controllers por recurso; Services, DAOs JDBC e regras de negócio foram reaproveitados. No **frontend**, foi introduzida uma camada `dao/` com sete classes para comunicação com a API; os Services passaram a delegar a elas as operações HTTP, mantendo suas interfaces públicas para os Servlets JSP. Os dois projetos **compilaram com sucesso**; o backend teve testes HTTP relevantes aprovados, incluindo autenticação, autorização e CRUD de filmes e categorias. Permanecem a validação integrada do frontend, uma revisão de regressão e a confirmação dos Pull Requests antes da aprovação final.
