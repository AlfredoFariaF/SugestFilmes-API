# SugestFilmes — Relatório de testes e validações

**Escopo:** refatoração do roteamento REST do backend (`SugestFilmes-API`) e separação da camada de acesso HTTP no frontend (`SugestFilmes-Front_API`).  
**Ambiente utilizado:** Windows / PowerShell, Java 17, Maven, Docker Compose, Tomcat 10.1 e MySQL 8.4.  
**Período das evidências fornecidas:** 20 e 21 de novembro de 2026.  
**Natureza:** registro dos comandos e resultados compartilhados durante o desenvolvimento; não é uma suíte automatizada de testes.

## 1. Resumo executivo

O backend foi compilado, empacotado em WAR, executado em Docker e validado com requisições HTTP reais via PowerShell. Foram observados resultados satisfatórios para disponibilidade da API, autenticação, controles básicos de acesso, consultas dos principais recursos e operações de cadastro, consulta, atualização e exclusão de filmes e categorias.

No frontend, o projeto compilou após a introdução dos sete DAOs de comunicação com a API e a atualização dos Services correspondentes. **Não foi apresentada evidência de teste da interface JSP em execução após essa refatoração.**

**Limites desta conclusão:** `BUILD SUCCESS` comprova compilação e empacotamento, não equivalência integral de comportamento. O Maven informou repetidamente `No tests to run`; nenhum conjunto de testes automatizados foi executado nos logs compartilhados. Também não foi apresentado teste completo da integração navegador → frontend JSP → backend após a mudança dos DAOs.

## 2. Compilação e empacotamento

| Projeto / situação | Evidência observada | Resultado |
|---|---|---|
| Backend antes da refatoração | `mvn clean package`; 25 arquivos compilados; `target/api.war` criado | **BUILD SUCCESS** |
| Backend após RouteController, ApiSupport e controllers de recursos | Evolução de 26 até 34 arquivos Java compilados | **BUILD SUCCESS** nas etapas relatadas, após correção pontual de erro de compilação |
| Backend após ativar RouteController como Servlet principal | `mvn clean package`; 34 arquivos compilados; `target/api.war` criado | **BUILD SUCCESS** |
| Frontend após FilmeDAO | `mvn clean package`; 27 arquivos compilados | **BUILD SUCCESS** |
| Frontend após CategoriaDAO | `mvn clean package`; 28 arquivos compilados | **BUILD SUCCESS** |
| Frontend após AvaliacaoDAO e quatro DAOs restantes | `mvn clean package`; 33 arquivos compilados; `target/SugestFilmes-Front-JSP.war` criado | **BUILD SUCCESS** |

**Ocorrência corrigida durante o desenvolvimento:** uma compilação do backend falhou porque o `RouteController` chamava `FilmeController.buscarPorId(HttpServletRequest, HttpServletResponse, Long)` antes de esse método estar presente com essa assinatura. Após correção no `FilmeController`, a compilação voltou a passar.

## 3. Inicialização do ambiente backend

1. A primeira tentativa de `docker compose up --build -d` construiu a imagem, mas falhou na criação do container MySQL porque o nome `sugestfilmes-mysql` já estava em uso por um container antigo.
2. A inspeção identificou containers antigos parados, inclusive um MySQL associado ao volume `sugestfilmes_mysql_data`.
3. Após resolver o conflito de nomes, a execução de `docker compose up --build -d` foi concluída com sucesso.
4. `docker compose ps` mostrou `sugestfilmes-mysql` **healthy** e `sugestfilmes-api` **Up**, com a API publicada em `localhost:8081`.

A existência de um volume antigo foi identificada; os testes seguintes foram realizados na instância local iniciada pelo Compose. **Não foi apresentado teste de persistência após reinicialização**, nem auditoria completa de configuração de produção.

## 4. Testes HTTP realizados no backend

### 4.1 Disponibilidade e autenticação

| Cenário | Comando / rota | Resultado observado | Avaliação |
|---|---|---|---|
| Saúde da API | `GET /api/health` sem JWT | Resposta com `status: ok` e `service: SugestFilmes API` | **Aprovado** |
| Acesso autenticado a filmes | `GET /api/filmes` com token válido | Retorno de 30 filmes cadastrados | **Aprovado** |
| Acesso sem token | `GET /api/filmes` sem `Authorization` | HTTP **401** | **Aprovado** |
| Login de usuário comum | `POST /api/auth/login` com conta de demonstração `maria` | Autenticação bem-sucedida; usuário retornado com ID 3 | **Aprovado** |
| Tentativa de cadastro sem privilégio de administrador | `POST /api/filmes` com token da Maria | HTTP **403** | **Aprovado** |
| Consulta de filme inexistente | `GET /api/filmes/999999` | HTTP **404** | **Aprovado** |

O sucesso da listagem autenticada mostra que um JWT válido foi aceito no fluxo testado. Não foram fornecidas evidências específicas de token adulterado, token expirado ou tentativas de acesso sem credenciais a todos os endpoints.

### 4.2 Consultas dos recursos

| Rota | Resultado observado | Avaliação |
|---|---|---|
| `GET /api/filmes` | 30 filmes retornados antes dos testes de escrita | **Aprovado** |
| `GET /api/categorias` | 10 categorias retornadas | **Aprovado** |
| `GET /api/avaliacoes?filmeId=1` (antes do cadastro de teste) | Nenhuma avaliação | **Requisição concluída; lista vazia** |
| `GET /api/recomendacoes?categorias=1` | 13 filmes recomendados | **Aprovado** |
| `GET /api/usuarios` (administrador) | 3 usuários retornados | **Aprovado** |
| `GET /api/perfis` | 3 perfis retornados | **Aprovado** |

**Observação:** nas respostas de listagem de filmes, o PowerShell exibiu `categorias: {}` e `totalAvaliacoes: 0` para os registros visualizados. Isso não foi investigado até o nível de consistência dos relacionamentos no banco. A listagem de categorias em si retornou dez registros.

### 4.3 CRUD de filmes — registro temporário ID 31

| Ordem | Requisição | Evidência observada | Resultado |
|---|---|---|---|
| 1 | `POST /api/filmes` sem `categoriasIds` válidos | JSON de erro: `Selecione ao menos uma categoria.` | **Validação de negócio acionada corretamente** |
| 2 | `POST /api/filmes` com `categoriasIds: [4]` | `Filme cadastrado.` | **Aprovado** |
| 3 | `GET /api/filmes` e filtro pelo título de teste | Filme temporário identificado com ID **31** | **Aprovado** |
| 4 | `PUT /api/filmes/31` | `Filme atualizado.` | **Aprovado** |
| 5 | `GET /api/filmes/31` | Título atualizado para `TESTE TEMPORARIO ATUALIZADO` e diretor `Teste CRUD` | **Aprovado** |
| 6 | `DELETE /api/filmes/31` | `Filme excluído.` | **Aprovado** |
| 7 | `GET /api/filmes/31` após exclusão | HTTP **404** | **Aprovado** |

**Resultado:** ciclo completo de cadastro, leitura, atualização e exclusão validado. O filme temporário não permaneceu cadastrado após o teste.

### 4.4 CRUD de categorias — registro temporário ID 11

| Ordem | Requisição | Evidência observada | Resultado |
|---|---|---|---|
| 1 | `POST /api/categorias` | `Categoria cadastrada.` | **Aprovado** |
| 2 | `GET /api/categorias` | Categoria temporária identificada com ID **11** | **Aprovado** |
| 3 | `PUT /api/categorias/11` | `Categoria atualizada.` | **Aprovado** |
| 4 | `GET /api/categorias/11` | Nome `TESTE CATEGORIA ATUALIZADA` retornado | **Aprovado** |
| 5 | `DELETE /api/categorias/11` | `Categoria excluída.` | **Aprovado** |
| 6 | `GET /api/categorias/11` após exclusão | HTTP **404** | **Aprovado** |

**Resultado:** CRUD completo validado. A categoria temporária foi excluída.

### 4.5 Avaliações — teste parcial

Foi realizado `POST /api/avaliacoes` com token da usuária de demonstração Maria, filme ID 1, nota 4 e comentário `TESTE TEMPORARIO DA API`. A API respondeu **`Avaliação registrada.`**.

A primeira tentativa de localizar o registro por filtro no PowerShell exibiu campos vazios. Uma consulta posterior a `GET /api/avaliacoes?filmeId=1` retornou um objeto completo:

```json
{
  "id": 1,
  "filmeId": 1,
  "usuarioId": 3,
  "nota": 4,
  "comentario": "TESTE TEMPORARIO DA API",
  "tituloFilme": "Interestelar",
  "nomeUsuario": "Maria Aluna"
}
```

Isso valida o **cadastro e a leitura da avaliação** no cenário realizado e comprova que o registro foi associado ao usuário ID 3. **Não foi apresentada saída confirmando PUT ou DELETE da avaliação**. O registro de teste pode permanecer no banco; é necessário verificar antes de removê-lo.

## 5. O que não foi comprovado pelos testes compartilhados

- Navegação e funcionamento completo do frontend JSP no navegador após a criação dos DAOs.
- Teste end-to-end do frontend refatorado consumindo o backend refatorado.
- CRUD completo de avaliações, usuários e perfis.
- Regra de impossibilidade de alterar avaliação pertencente a outro usuário com um token diferente.
- Respostas e códigos HTTP de todos os cenários inválidos, incluindo JSON malformado, IDs malformados, token expirado e caminhos desconhecidos.
- Equivalência exata entre todos os contratos JSON do `ApiController` antigo e a implementação nova.
- Testes automatizados de regressão; o Maven apresentou `No tests to run`.
- Exclusão definitiva do arquivo `ApiController.java` e eventual deslocamento do `RouteController` para a pasta `controller`, conforme ajuste posterior solicitado por Alfredo.
- Resultado final de merge em `main` ou confirmação de aprovação do Pull Request.

## 6. Relação entre a refatoração e os testes

### Backend

```text
Cliente HTTP
    ↓
JwtFilter / CorsFilter
    ↓
RouteController (Servlet /api/*)
    ↓
Controller do recurso (Filme, Categoria, Auth, Avaliacao...)
    ↓
Service
    ↓
DAO JDBC
    ↓
MySQL
```

O fluxo foi exercitado de ponta a ponta **para os cenários HTTP do backend registrados nas seções 4.1 a 4.5**. O antigo `ApiController` foi mantido no código durante os testes, mas o mapeamento `/api/*` havia sido transferido para `RouteController`.

### Frontend

```text
Navegador / JSP
    ↓
Servlet do frontend
    ↓
Service
    ↓
DAO de integração HTTP
    ↓
ApiClient
    ↓
Backend REST
```

A compilação do frontend com sete DAOs foi bem-sucedida, mas **esse fluxo completo ainda não foi demonstrado por teste de interface** nas evidências disponíveis.

## 7. Comandos de reprodução — sem alterar dados

A partir da pasta do backend:

```powershell
mvn clean package
docker compose ps
Invoke-RestMethod http://localhost:8081/api/health
```

Para verificar a proteção de um endpoint sem JWT:

```powershell
try {
    Invoke-RestMethod http://localhost:8081/api/filmes -Method GET
} catch {
    Write-Host "HTTP:" ([int]$_.Exception.Response.StatusCode)
}
```

A partir da pasta do frontend:

```powershell
mvn clean package
```

**Nota:** a execução do Docker exige daemon ativo e pode depender das portas locais 8081/3306. Evite usar `docker compose down -v`, pois isso remove volumes e pode apagar dados do banco.

## 8. Conclusão para revisão do Alfredo

A evidência disponível sustenta que a refatoração do backend **compilou e funcionou nos endpoints e cenários efetivamente testados**, inclusive com operações reais de leitura/escrita no MySQL e bloqueio HTTP 401/403. A refatoração do frontend **compilou**, mas ainda carece de teste visual e integrado.

Portanto, o estado mais preciso da entrega é: **implementação principal realizada e parcialmente validada, com revisão estrutural final e testes de integração pendentes**. Este documento não certifica cobertura total de testes nem aprovação para merge na branch principal.
