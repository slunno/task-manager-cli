# Resumo das Alterações e Motivações

Este documento descreve as alterações realizadas no projeto Task Manager CLI para sanar bugs funcionais, falhas de segurança e melhorar a qualidade do código.

---

## 🔒 1. Autenticação, Autorização e Segurança (Spring Security + JWT)
- **O que foi feito**: 
  - Adicionadas as dependências do `spring-boot-starter-security` e biblioteca JWT (JJWT) no `pom.xml`.
  - Criados os arquivos `SecurityConfig.java` (configuração do Spring Security), `JwtUtil.java` (utilitário para gerar e validar tokens) e `JwtAuthenticationFilter.java` (filtro para interceptar requisições e validar o token Bearer).
  - Rotas públicas definidas: apenas `POST /users` (cadastro) e `POST /users/login` (login) são permitidos sem autenticação. Todos os outros endpoints exigem token JWT válido.
- **Motivo**: Anteriormente, qualquer endpoint da API (inclusive listagem e exclusão de dados) podia ser acessado por qualquer pessoa sem autenticação.

---

## 🔑 2. Proteção de Credenciais de Banco de Dados
- **O que foi feito**: 
  - Modificado o arquivo `DatabaseConfig.java` para ler as variáveis de ambiente `DATABASE_URL` e `DATABASE_USER`.
- **Motivo**: O host do banco de dados (Supabase) e o usuário estavam fixos (hardcoded) no código, o que expõe informações de infraestrutura em repositórios públicos.

---

## 🚫 3. Correção do Fluxo de Login
- **O que foi feito**:
  - Ajustado o método `loginUser` em `UserService.java` para chamar `verifyLoginUsers` do repositório (que busca o usuário e valida o hash da senha com `BCrypt.checkpw`).
  - Removido o método inutilizado `findUserByEmail` que sempre retornava `null`.
- **Motivo**: O login estava quebrado porque chamava uma função mockada que retornava `null` para qualquer tentativa de login.

---

## 🛡️ 4. Ocultação do Hash de Senha nas Respostas da API
- **O que foi feito**:
  - Criado o DTO `UserResponse.java` contendo apenas `idUser`, `name` e `email`.
  - Modificado o `UserController.java` e `UserService.java` para que os endpoints de criação, busca e listagem de usuários retornem `UserResponse` em vez de `UserModel`.
- **Motivo**: A API retornava a entidade `UserModel` completa, o que expunha publicamente o hash BCrypt da senha de todos os usuários nas requisições HTTP.

---

## ⚙️ 5. Tratamento de Exceções e Erros Robustos
- **O que foi feito**:
  - Ajustada a captura de exceções em `UserRepository.java` e `TaskRepository.java` de `Exception` genérica para `SQLException`, relançando-as como `RuntimeException`.
  - Criado o `GlobalExceptionHandler.java` para interceptar e padronizar as respostas de erros de validação (`400 Bad Request` para `UserException`/`TaskException`) e erros de banco/servidor (`500 Internal Server Error`).
  - Removido o bloco `try-catch` em `TaskService.createTask` para que os erros de validação da tarefa cheguem ao handler global em vez de retornar um corpo nulo com status 201 Created.
- **Motivo**: Evitar mascarar bugs de programação como erros de banco e garantir que requisições inválidas recebam as respostas HTTP apropriadas com as mensagens de erro detalhadas.

---

## 📝 6. Validação de Dados de Usuários
- **O que foi feito**:
  - Adicionado regex para validação de e-mail em `UsersValidator.java`.
  - Adicionada validação de tamanho mínimo de senha (mínimo 8 caracteres) no cadastro.
- **Motivo**: A validação anterior de e-mail aceitava e-mails inválidos (ex: `a@b..`) e não havia verificação do tamanho mínimo ou presença da senha.
