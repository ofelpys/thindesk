# Thindesk

Sistema de chamados técnicos com dashboard administrativo funcional.

## Tecnologias

- Java e Spring Boot no backend
- Thymeleaf, HTML, CSS e JavaScript no frontend
- MongoDB Atlas para armazenar os chamados
- Maven Wrapper para executar o projeto

## Objetivo do dashboard

O painel permite acompanhar indicadores gerais, chamados atendidos por período, distribuição por status, tipos mais atendidos e filtros de consulta. Os dados são fornecidos por endpoints da própria aplicação.

## Execução local

1. Abra a pasta raiz do projeto.
2. Configure `MONGODB_URI`, `THINDESK_LOGIN_USER` e `THINDESK_LOGIN_PASSWORD` na configuração de execução.
3. Execute `com.pfc.ThindeskApplication`.
4. Acesse `http://localhost:8080/login`.

As credenciais não devem ser colocadas nos arquivos enviados ao GitHub.

## Documentação

[Documentação, API e evidências de testes](documentacao-thindesk-dashboard.pdf)

## Gitflow

- `main`: versão estável
- `develop`: integração das funcionalidades
- `feature/base-thindesk`: inclusão do sistema existente e atualização do escopo
- `feature/api-dashboard`: endpoints de indicadores e filtros
- `feature/dashboard`: painel Thymeleaf integrado à API
- `feature/testes-documentacao`: documentação e evidências de testes
