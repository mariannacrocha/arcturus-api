# 🌌 Arcturus Stream API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![AWS S3](https://img.shields.io/badge/AWS_S3-Integrated-yellow)

API robusta e escalável que gerencia o ecossistema de conteúdos vibracionais do **Arcturus Stream**. Desenvolvida com **Java 21** e **Spring Boot**, a aplicação foca em performance, segurança e integração eficiente com serviços de cloud.

---
## ⚙️ Funcionalidades Técnicas
* **Segurança Stateless:** Autenticação e autorização implementadas com Spring Security e tokens JWT.
* **Armazenamento em Nuvem:** Integração nativa com AWS S3 para upload e streaming de arquivos de áudio.
* **Consumo de APIs Externas:** Serviço de busca assíncrona integrado à API do Jamendo para descoberta de músicas gratuitas.
* **Persistência Híbrida:** Gerenciamento de usuários e conteúdos salvos utilizando PostgreSQL e Spring Data JPA.
---
## 🛠️ Stack Tecnológica
* **Linguagem:** Java 21 (LTS).
* **Framework:** Spring Boot 3.x com Spring Security e Hibernate.
* **Banco de Dados:** PostgreSQL 15 rodando em ambiente containerizado.
* **DevOps:** Orquestração completa via Docker Compose, com limites de recursos otimizados para ambientes de nuvem (Oracle Cloud).
---
## 🚀 Configuração de Deploy

A aplicação está configurada para rodar em containers, otimizada para instâncias com recursos limitados (1GB RAM) através de configurações específicas da JVM:

```bash
# Iniciar todo o ecossistema (API + DB)
docker-compose up -d --build
```
---

## Variáveis de Ambiente Necessárias:
Crie as variáveis no seu IDE ou no terminal (ou edite o application.yaml para dev):
JWT_SECRET: Sua chave secreta para assinatura de tokens.
AWS_ACCESS_KEY_ID: Chave AWS (ou mock para local).
AWS_SECRET_ACCESS_KEY: Segredo AWS.
AWS_S3_BUCKET: Nome do bucket S3.

---
Desenvolvido por Marianna Rocha — Software Developer focada em arquiteturas modernas e escaláveis.
