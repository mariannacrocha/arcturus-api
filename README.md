# 🌌 Arcturus Stream API

![Java CI](https://github.com/mariannacrocha/arcturus-api/actions/workflows/maven.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)

API RESTful robusta desenvolvida para a plataforma de streaming de frequências vibracionais **Arcturus**. O sistema gerencia autenticação, upload de arquivos para nuvem (AWS S3) e integração com APIs externas de música.

## 🚀 Tecnologias e Práticas

* **Core:** Java 21, Spring Boot 3.4.1
* **Segurança:** Spring Security, JWT (JJWT), BCrypt Password Encoder.
* **Banco de Dados:** PostgreSQL, Spring Data JPA.
* **Cloud & Storage:** AWS SDK v2 (S3 Integration).
* **Testes:** JUnit 5, Mockito, Spring Boot Test.
* **DevOps:** Docker, Docker Compose, GitHub Actions (CI/CD Pipeline).
* **Integração:** Consumo de API externa (Jamendo) com `java.net.http.HttpClient`.

## ⚙️ Arquitetura e Destaques

* **Security First:** Implementação de filtro de segurança customizado (`SecurityFilter`) para validação de Tokens Stateless.
* **Hybrid Search:** O sistema busca conteúdos na biblioteca pessoal do usuário (Postgres) e complementa com resultados da API pública do Jamendo, filtrando duplicatas.
* **CI/CD Pipeline:** Workflow automatizado no GitHub Actions que sobe contêineres Docker (Service Containers) para rodar testes de integração contra um banco PostgreSQL real a cada push.

## 🛠️ Como Rodar Localmente

### Pré-requisitos
* Java 21+
* Docker & Docker Compose
* Maven

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone [https://github.com/mariannacrocha/arcturus-api.git](https://github.com/mariannacrocha/arcturus-api.git)
   ```

Configure as Variáveis de Ambiente:
Crie as variáveis no seu IDE ou no terminal (ou edite o application.yaml para dev):
JWT_SECRET: Sua chave secreta para assinatura de tokens.
AWS_ACCESS_KEY_ID: Chave AWS (ou mock para local).
AWS_SECRET_ACCESS_KEY: Segredo AWS.
AWS_S3_BUCKET: Nome do bucket S3.
Suba o Banco de Dados (Docker):
  ```bash
docker-compose up -d
  ```

Execute a aplicação:
  ```bash
./mvnw spring-boot:run
  ```

A API estará disponível em http://localhost:8080.
🧪 Rodando os Testes
O projeto possui testes unitários e de integração cobrindo Controllers, Services e Repositórios.

  ```bash
./mvnw test
  ```


Desenvolvido por Marianna Rocha
