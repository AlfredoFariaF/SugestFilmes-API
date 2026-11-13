# SugestFilmes API

API REST do projeto SugestFilmes, executada em Apache Tomcat 10.1 na porta **8081** do host. Implementada em Java 17 com Jakarta Servlet (API nativa do container), JDBC e Gson. Não utiliza Spring, Spring Boot, Hibernate, JPA, Jersey ou JAX-RS.

## Arquitetura

- `api/`: Servlet REST e filtro JWT (camada HTTP).
- `service/`: regras de negócio e validações reaproveitadas do projeto original.
- `dao/`: persistência JDBC.
- `model/`: entidades.
- `config/`: conexão MySQL.
- `util/`: JSON auxiliar, JWT HS256 e hash PBKDF2 de senhas.

## Requisitos

Docker Engine e Docker Compose. Para compilar fora do Docker: JDK 17 e Maven 3.9+.

## Executar

1. Copie `.env.example` para `.env`.
2. Altere obrigatoriamente `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD` e `JWT_SECRET` (a chave JWT deve ter pelo menos 32 caracteres aleatórios).
3. Execute `docker compose up --build -d`.
4. Verifique `http://localhost:8081/api/health`.

O MySQL fica disponível na porta 3306 do host e o Tomcat recebe na porta 8081, encaminhando para 8080 dentro do container. O script `init.sql` só é executado automaticamente quando o volume `mysql_data` é criado pela primeira vez. Para recriar o banco e perder os dados locais: `docker compose down -v` e depois `docker compose up --build -d`.

## Autenticação

`POST /api/auth/login` não exige token. Todas as outras rotas, exceto `/api/health`, exigem `Authorization: Bearer <token>`. O JWT HS256 expira em uma hora. Senhas novas são armazenadas com PBKDF2-HMAC-SHA256; as senhas legadas em texto puro do banco inicial são verificadas e migradas para hash no primeiro login válido.

O `init.sql` contém contas de demonstração herdadas do projeto original. Em instalação local recém-criada:

| Perfil | Login | Senha inicial |
|---|---|---|
| Administrador | `admin` | `123456` |
| Professor | `joao` | `123456` |
| Aluno | `maria` | `123456` |

Troque/remova essas credenciais antes de qualquer exposição externa. A senha do usuário é migrada para hash após autenticação bem-sucedida.

### Login

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{"login":"admin","senha":"123456"}
```

Resposta contém `token`, `tipo`, `expiraEm` e dados públicos do usuário (nunca a senha).

## Endpoints

Todas as rotas abaixo, exceto login e health, exigem JWT.

| Método | Rota | Acesso |
|---|---|---|
| GET | `/api/health` | Público |
| POST | `/api/auth/login` | Público |
| GET | `/api/filmes` | Autenticado; aceita `?q=termo` |
| GET | `/api/filmes/{id}` | Autenticado |
| POST | `/api/filmes` | Administrador |
| PUT | `/api/filmes/{id}` | Administrador |
| DELETE | `/api/filmes/{id}` | Administrador |
| GET | `/api/categorias` | Autenticado |
| GET | `/api/categorias/{id}` | Autenticado |
| POST/PUT/DELETE | `/api/categorias[/{id}]` | Administrador |
| GET | `/api/recomendacoes?categorias=1,2` | Autenticado |
| GET | `/api/avaliacoes?filmeId={id}` | Autenticado |
| GET | `/api/avaliacoes` | Próprias avaliações; administrador pode filtrar `usuarioId` |
| POST | `/api/avaliacoes` | Autenticado; usuário vem do token |
| PUT/DELETE | `/api/avaliacoes/{id}` | Proprietário; administrador pode excluir qualquer avaliação |
| GET/POST | `/api/usuarios` | Administrador |
| GET/PUT/DELETE | `/api/usuarios/{id}` | Administrador |
| GET | `/api/perfis` | Autenticado |
| POST/PUT/DELETE | `/api/perfis[/{id}]` | Administrador |

### Exemplo: cadastrar filme

```http
POST /api/filmes
Authorization: Bearer SEU_TOKEN
Content-Type: application/json

{
  "titulo": "Exemplo",
  "descricao": "Descrição do filme",
  "genero": "Drama",
  "ano": 2024,
  "diretor": "Diretor Exemplo",
  "categoriasIds": [1, 4]
}
```

### Exemplo: avaliar filme

```http
POST /api/avaliacoes
Authorization: Bearer SEU_TOKEN
Content-Type: application/json

{"filmeId": 1, "nota": 5, "comentario": "Excelente filme"}
```

## Observações

- As permissões administrativas são verificadas no controller com base no papel assinado no JWT.
- As avaliações são vinculadas ao ID do usuário extraído do token, não ao ID enviado pelo cliente.
- A API retorna JSON e códigos HTTP; não há JSP, HTML, CSS ou sessão MVC.
- Para produção, configure HTTPS, segredos fortes e backup do banco. Revise ainda limites de requisição, política de CORS e rotação/revogação de tokens conforme o ambiente.
