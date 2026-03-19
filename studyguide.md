# 📚 Guia de Estudo Técnico — Arcturus API

> Este documento explica as decisões de arquitetura, padrões de projeto e anotações Spring Boot utilizadas neste projeto.  
> Foi criado para servir como material de estudo. 🚀

---

## 📌 Sumário

1. [Sobre o Projeto](#sobre-o-projeto)
2. [Stack e Dependências](#stack-e-dependências)
3. [Anotações Spring Boot](#anotações-spring-boot)
    - [Camada Web](#camada-web)
    - [Camada de Segurança](#camada-de-segurança)
    - [Camada de Serviço e Componentes](#camada-de-serviço-e-componentes)
    - [Configuração e Beans](#configuração-e-beans)
    - [Camada de Dados — JPA](#camada-de-dados--jpa)
    - [Lombok](#lombok)
4. [Pilares do POO no Projeto](#pilares-do-poo-no-projeto)
    - [Encapsulamento](#encapsulamento)
    - [Herança](#herança)
    - [Polimorfismo](#polimorfismo)
    - [Abstração](#abstração)
5. [Design Patterns GoF](#design-patterns-gof)
    - [Builder](#builder-criacional)
    - [Singleton](#singleton-criacional)
    - [Template Method](#template-method-comportamental)
    - [Chain of Responsibility](#chain-of-responsibility-comportamental)
    - [Repository Pattern](#repository-pattern)
6. [Segurança — JWT e Spring Security](#segurança--jwt-e-spring-security)
7. [Princípios SOLID](#princípios-solid)
8. [Mapa de Anotações](#mapa-de-anotações)

---

## Sobre o Projeto

O **Arcturus API** é uma API REST para streaming de conteúdo vibracional (áudios com frequências específicas). 
Permite cadastro e autenticação de usuários, busca em biblioteca externa (Jamendo) e gerenciamento de biblioteca pessoal.

**Principais funcionalidades:**
- Autenticação com JWT
- Busca assíncrona em API externa (Jamendo)
- CRUD de conteúdos por usuário autenticado

---

## Stack e Dependências

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5 | Framework principal |
| Spring Security | via Boot | Autenticação e autorização |
| Spring Data JPA | via Boot | ORM e acesso ao banco |
| PostgreSQL | runtime | Banco de dados relacional |
| Hibernate | via JPA | Implementação JPA |
| JWT (jjwt) | 0.11.5 | Geração e validação de tokens |
| Lombok | opcional | Redução de boilerplate |

---

## Anotações Spring Boot

### Camada Web

---

#### `@SpringBootApplication`
**Pacote:** `org.springframework.boot`

Meta-anotação que combina três em uma:
- `@Configuration` — a classe define beans
- `@EnableAutoConfiguration` — Spring Boot configura o contexto automaticamente com base nas dependências do classpath
- `@ComponentScan` — varre o pacote e subpacotes procurando componentes

```java
@SpringBootApplication
public class StreamApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamApiApplication.class, args);
    }
}
```

> 💡 É o ponto de partida de qualquer aplicação Spring Boot. Sem ela, o contexto não inicializa.

---

#### `@RestController`
**Pacote:** `org.springframework.web.bind.annotation`

Combina duas anotações:
- `@Controller` — registra a classe como componente web MVC
- `@ResponseBody` — todo método retorna dados direto no corpo da resposta HTTP (JSON), não uma view HTML

```java
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    // todos os métodos retornam JSON automaticamente
}
```

> 💡 Sem `@RestController`, você precisaria colocar `@ResponseBody` em cada método individualmente.

---

#### `@RequestMapping`
**Pacote:** `org.springframework.web.bind.annotation`

Define o prefixo de URL base para todos os endpoints de um controller.

```java
@RestController
@RequestMapping("/auth")           // todos os endpoints começam com /auth
public class AuthenticationController { ... }

@RestController
@RequestMapping("/v1/contents")    // versionamento de API na URL
public class ContentController { ... }
```

> 💡 Incluir a versão na URL (`/v1/`) é uma boa prática — permite evoluir a API sem quebrar clientes existentes.

---

#### `@GetMapping`, `@PostMapping`, `@DeleteMapping`
**Pacote:** `org.springframework.web.bind.annotation`

Atalhos para `@RequestMapping` com o método HTTP já definido. Cada um mapeia um verbo HTTP:

```java
@PostMapping("/login")    // POST /auth/login
public ResponseEntity login(@RequestBody LoginRequest body) { ... }

@GetMapping               // GET /v1/contents
public List<VibrationalContent> getAllContents(...) { ... }

@GetMapping("/search")    // GET /v1/contents/search?q=meditacao
public List<VibrationalContent> search(...) { ... }

@DeleteMapping("/{id}")   // DELETE /v1/contents/{id}
public ResponseEntity<Void> deleteContent(...) { ... }
```

> 💡 Cada verbo tem semântica HTTP clara: **GET** é idempotente e seguro, **POST** cria recursos, **DELETE** remove.

---

#### `@RequestBody`
**Pacote:** `org.springframework.web.bind.annotation`

Instrui o Spring a desserializar o corpo da requisição HTTP (JSON) e convertê-lo automaticamente para o tipo Java especificado. Usa o **Jackson** por baixo dos panos.

```java
@PostMapping("/login")
public ResponseEntity login(@RequestBody LoginRequest body) {
    // body.username() e body.password() já estão preenchidos pelo Jackson
    User user = repository.findByUsername(body.username())...;
}
```

> 💡 Combina perfeitamente com **Java Records** — o Jackson consegue desserializar diretamente para o record via construtor canônico.

---

#### `@RequestParam`
**Pacote:** `org.springframework.web.bind.annotation`

Extrai parâmetros da query string da URL (após o `?`) ou de formulários multipart.

```java
@PostMapping("/upload")
public ResponseEntity uploadContent(
    @RequestParam("file") MultipartFile file,         // campo do form-data
    @RequestParam("description") String description,  // campo do form-data
    @RequestParam("frequencyHz") int frequencyHz,     // campo do form-data
    @RequestParam("energyType") String energyType) { ... }

@GetMapping("/search")
public List<VibrationalContent> search(
    @RequestParam("q") String query) { ... }  // GET /search?q=meditacao
```

> 💡 **Diferença de `@RequestBody`:** `@RequestBody` lê o corpo JSON inteiro; `@RequestParam` lê parâmetros individuais da URL ou form-data.

---

#### `@PathVariable`
**Pacote:** `org.springframework.web.bind.annotation`

Extrai um trecho dinâmico da URL (definido entre chaves no mapping) e injeta como parâmetro do método.

```java
@DeleteMapping("/{id}")       // {id} é o trecho dinâmico
public ResponseEntity<Void> deleteContent(
    @PathVariable UUID id,    // extrai o {id} da URL
    @AuthenticationPrincipal User user) {

    // DELETE /v1/contents/3fa85f64-5717-4562-b3fc-2c963f66afa6
    var content = contentRepository.findByIdAndUser(id, user);
}
```

> 💡 **Diferença de `@RequestParam`:** `@PathVariable` faz parte do caminho (`/contents/123`); `@RequestParam` vem após o `?` (`/search?q=texto`).

---

### Camada de Segurança

---

#### `@AuthenticationPrincipal`
**Pacote:** `org.springframework.security.core.annotation`

Injeta o objeto do usuário autenticado diretamente como parâmetro do método, extraindo-o do `SecurityContextHolder`. No Arcturus, o `SecurityFilter` coloca o `User` no contexto após validar o JWT — e `@AuthenticationPrincipal` recupera esse objeto automaticamente, sem nova consulta ao banco.

```java
// SecurityFilter coloca o User no contexto:
var authentication = new UsernamePasswordAuthenticationToken(
    user, null, authorities  // principal = o objeto User do domínio
);
SecurityContextHolder.getContext().setAuthentication(authentication);

// ContentController recupera sem precisar buscar no banco de novo:
@GetMapping
public List<VibrationalContent> getAllContents(
        @AuthenticationPrincipal User user) {
    return contentRepository.findByUser(user);
}
```

> 💡 Elimina a necessidade de buscar o usuário no banco em cada endpoint. O `SecurityFilter` já fez isso uma vez por requisição.

---

#### `@EnableWebSecurity`
**Pacote:** `org.springframework.security.config.annotation.web`

Ativa a configuração de segurança web do Spring Security e desabilita as configurações padrão auto-configuradas. Sem ela, o Spring Security aplicaria regras básicas e pouco customizáveis.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)   // APIs REST não usam CSRF
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // sem sessão — JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

> 💡 `STATELESS` é essencial para APIs com JWT — o servidor não guarda sessão do usuário. Cada requisição precisa trazer o token.

---

### Camada de Serviço e Componentes

---

#### `@Service`
**Pacote:** `org.springframework.stereotype`

Marca a classe como um componente de **camada de negócio** no contexto Spring. O Spring cria uma única instância (**Singleton**) e a gerencia no ciclo de vida da aplicação.

```java
@Service
public class TokenService {      // lógica de JWT
    public String generateToken(User user) { ... }
    public String validateToken(String token) { ... }
}

@Service
public class S3Service {         // lógica de upload
    public String uploadFile(MultipartFile file) { ... }
}

@Service
public class ExternalMediaService {  // busca na API Jamendo
    public List<VibrationalContent> searchFreeMusic(String query) { ... }
}
```

> 💡 **Padrão GoF implícito: Singleton.** Cada `@Service` é instanciado uma vez e compartilhado entre todas as requisições.

---

#### `@Component`
**Pacote:** `org.springframework.stereotype`

Versão genérica de `@Service`/`@Repository`. Registra a classe como bean Spring sem semântica específica de camada. No Arcturus, é usado no `SecurityFilter` — que não é serviço nem repositório, mas um componente de infraestrutura.

```java
@Component
public class SecurityFilter extends OncePerRequestFilter {
    // componente de infraestrutura de segurança
}
```

> 💡 **Hierarquia:** `@Component` é a base. `@Service`, `@Repository` e `@Controller` a herdam semanticamente. Todos registram beans, mas comunicam camadas diferentes.

---

#### `@Repository`
**Pacote:** `org.springframework.stereotype`

Marca uma interface como repositório de dados. Além de registrar o bean, habilita tradução automática de exceções do banco (`SQLException` → `DataAccessException` do Spring).

```java
@Repository
public interface ContentRepository extends JpaRepository<VibrationalContent, UUID> {
    Optional<VibrationalContent> findByIdAndUser(UUID id, User user);
    List<VibrationalContent> findByUser(User user);
}
```

> 💡 O Spring Data JPA **gera a implementação em tempo de execução** — você não escreve SQL para `findByUser`. O nome do método **é a query**.

---

### Configuração e Beans

---

#### `@Configuration`
**Pacote:** `org.springframework.context.annotation`

Indica que a classe contém definições de beans (`@Bean`). É processada na inicialização do contexto Spring.

```java
@Configuration
public class S3Config {
    @Bean
    public S3Client s3Client() {
        return S3Client.builder().region(Region.US_EAST_1).build();
    }
}

@Configuration
@EnableWebSecurity
public class SecurityConfig { ... }
```

> 💡 **Diferença de `@Component`:** `@Configuration` garante que chamadas a métodos `@Bean` dentro da mesma classe retornem o **mesmo bean** (proxy CGLIB). `@Component` não tem essa garantia.

---

#### `@Bean`
**Pacote:** `org.springframework.context.annotation`

Declara que o método produz um bean gerenciado pelo Spring. O Spring chama o método **uma vez**, armazena o resultado e o injeta onde necessário.

```java
@Bean
public S3Client s3Client() {               // bean do cliente AWS
    return S3Client.builder()
        .region(Region.US_EAST_1).build();
}

@Bean
public PasswordEncoder passwordEncoder() { // bean do encoder bcrypt
    return new BCryptPasswordEncoder();
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) { ... }
```

> 💡 O `PasswordEncoder` declarado como `@Bean` é injetado automaticamente no `AuthenticationController` pelo construtor — sem o `@Bean`, o Spring não saberia qual implementação usar.

---

#### `@Value`
**Pacote:** `org.springframework.beans.factory.annotation`

Injeta valores de propriedades do `application.yaml` diretamente em campos. Evita hardcoded de segredos no código-fonte.

```java
// TokenService.java
@Value("${api.security.token.secret}")
private String secret;         // vem do yaml — nunca no código-fonte

// S3Service.java
@Value("${aws.s3.bucket}")
private String bucketName;

@Value("${aws.region}")
private String region;

// ExternalMediaService.java
@Value("${jamendo.client-id}")
private String clientId;
```

> 💡 **Boa prática de segurança:** segredos (chaves JWT, IDs de API, nomes de bucket) ficam em variáveis de ambiente ou arquivos não versionados (`.gitignore`). O `@Value` faz a ponte entre o ambiente e o código.

---

### Camada de Dados — JPA

---

#### `@Entity`
**Pacote:** `jakarta.persistence`

Marca a classe como uma entidade JPA — ela representa uma tabela no banco de dados. O Hibernate mapeia cada instância como uma linha.

```java
@Entity
@Table(name = "vibrational_contents")
public class VibrationalContent { ... }

@Entity
@Table(name = "users")
public class User { ... }
```

---

#### `@Table`
**Pacote:** `jakarta.persistence`

Define o nome exato da tabela no banco. Se omitida, o JPA usa o nome da classe.

```java
@Table(name = "users")                  // tabela: users (não "User")
@Table(name = "vibrational_contents")   // convenção snake_case do PostgreSQL
```

---

#### `@Id` e `@GeneratedValue`
**Pacote:** `jakarta.persistence`

`@Id` marca a chave primária. `@GeneratedValue` define a estratégia de geração automática.

```java
// User.java — AUTO deixa o JPA decidir (geralmente SEQUENCE no PostgreSQL)
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private UUID id;

// VibrationalContent.java — UUID gerado diretamente pelo provedor JPA
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

> 💡 `GenerationType.UUID` é mais recente (JPA 3.1) e garante unicidade sem coordenação entre instâncias — ideal para microsserviços e ambientes distribuídos.

---

#### `@Column`
**Pacote:** `jakarta.persistence`

Customiza o mapeamento de um campo para uma coluna do banco — nome, nullabilidade, unicidade, tipo.

```java
@Column(unique = true, nullable = false)
private String username;                      // único e obrigatório

@Column(name = "s3url", columnDefinition = "TEXT")
private String s3Url;                         // nome diferente + tipo TEXT

@Column(name = "energy_type")
private String energyType;                    // snake_case no banco
```

---

#### `@ManyToOne` e `@JoinColumn`
**Pacote:** `jakarta.persistence`

`@ManyToOne` define o lado "muitos" de um relacionamento. `@JoinColumn` especifica a coluna de chave estrangeira.

```java
// Muitos VibrationalContents pertencem a um User:
@ManyToOne
@JoinColumn(name = "user_id")   // coluna user_id no banco
private User user;
```

---

### Lombok

---

#### `@Getter` e `@Setter`
**Pacote:** `lombok`

Geram automaticamente métodos `getX()` e `setX()` para todos os campos. Eliminam boilerplate sem violar encapsulamento — atributos continuam `private`.

```java
@Getter @Setter
@Entity
public class User {
    private UUID id;        // Lombok gera: getId() e setId()
    private String username; // Lombok gera: getUsername() e setUsername()
    private String password; // Lombok gera: getPassword() e setPassword()
}
```

> 💡 **POO — Encapsulamento:** o estado interno continua protegido. O acesso externo é mediado pelos métodos gerados.

---

#### `@Builder`
**Pacote:** `lombok`

Implementa o padrão GoF **Builder** automaticamente — gera uma classe interna `Builder` com métodos fluentes.

```java
@Builder
@Entity
public class VibrationalContent { ... }

// Uso — legível, sem depender da ordem dos parâmetros:
VibrationalContent content = VibrationalContent.builder()
    .title("Frequência 432Hz")
    .description("Meditação profunda")
    .frequencyHz(432.0)
    .energyType("HEALING")
    .s3Url(s3Url)
    .user(user)
    .build();
```

> 💡 **Padrão GoF: Builder (Criacional).** Resolve o problema de construtores com muitos parâmetros.

---

#### `@NoArgsConstructor` e `@AllArgsConstructor`
**Pacote:** `lombok`

`@NoArgsConstructor` gera um construtor sem parâmetros — **obrigatório para o JPA** instanciar entidades via reflexão.  
`@AllArgsConstructor` gera um construtor com todos os campos.

```java
@NoArgsConstructor    // JPA precisa deste para instanciar a entidade
@AllArgsConstructor   // para criação completa em código
public class VibrationalContent { ... }
```

> ⚠️ Sem `@NoArgsConstructor`, o Hibernate não consegue instanciar a entidade ao carregar do banco — erro em runtime.

---

#### `@RequiredArgsConstructor`
**Pacote:** `lombok`

Gera um construtor apenas com os campos `final` ou `@NonNull`. É a forma mais elegante de **Injeção de Dependência pelo construtor** no Spring Boot.

```java
@RestController
@RequiredArgsConstructor   // gera construtor com todos os campos final
public class ContentController {

    private final S3Service s3Service;
    private final ContentRepository contentRepository;
    private final ExternalMediaService externalMediaService;
}
```

> 💡 **Por que é preferido a `@Autowired` no campo:**
> - Campos `final` — imutáveis após construção
> - Dependências explícitas e obrigatórias
> - Facilita testes unitários — basta passar mocks no construtor, sem subir contexto Spring

---

## Pilares do POO no Projeto

### Encapsulamento

Esconder o estado interno de um objeto e controlar o acesso por meio de uma interface pública.

**Onde aparece no Arcturus:**

```java
// User.java — todos os atributos são privados
@Getter @Setter
@Entity
public class User {
    private UUID id;
    private String username;
    private String password;   // hash bcrypt — nunca exposto diretamente
    private String role;
}
```

A decisão mais importante: o `password` nunca aparece na resposta da API. O endpoint de login retorna `LoginResponse(String token)` — não o `User` completo.

```java
// ❌ Errado — expõe password, role, id interno:
return ResponseEntity.ok(user);

// ✅ Correto — expõe apenas o token:
String token = tokenService.generateToken(user);
return ResponseEntity.ok(new LoginResponse(token));
```

No `TokenService`, o método `getSigningKey()` é `private` — nenhuma outra classe sabe como a chave JWT é construída:

```java
@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // chama método privado
            .compact();
    }

    private Key getSigningKey() {   // PRIVADO — detalhe encapsulado
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

---

### Herança

Uma classe filha herda comportamentos da classe pai, podendo estendê-los ou sobrescrevê-los.

**Onde aparece no Arcturus:**

```java
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = recoverToken(request);
        if (token != null) {
            String login = tokenService.validateToken(token);
            // autentica o usuário no SecurityContext
        }
        filterChain.doFilter(request, response);
    }
}
```

`OncePerRequestFilter` garante execução única por requisição — mesmo em redirects internos. Herdamos esse comportamento e sobrescrevemos apenas `doFilterInternal` com a lógica JWT do Arcturus.

> O `@Override` garante em **tempo de compilação** que a assinatura está correta. Se o nome do método for digitado errado, o compilador acusa — em vez de um bug silencioso em runtime.

---

### Polimorfismo

O mesmo método se comporta diferente dependendo do tipo real do objeto em runtime.

**Onde aparece no Arcturus:**

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}

public interface ContentRepository extends JpaRepository<VibrationalContent, UUID> {
    List<VibrationalContent> findByUser(User user);
    Optional<VibrationalContent> findByIdAndUser(UUID id, User user);
}
```

O Spring Data JPA **gera a implementação concreta em runtime**. O `AuthenticationController` chama `findByUsername()` sem saber nada sobre SQL, Hibernate ou PostgreSQL — o polimorfismo garante que a implementação correta será executada.

---

### Abstração

Modelar apenas o que é relevante para o problema, escondendo complexidade desnecessária.

**Onde aparece no Arcturus — Java Records como DTOs:**

```java
// Só o que o cliente precisa enviar para login:
public record LoginRequest(String username, String password) {}

// Só o token na resposta — nunca a entidade User com a senha:
public record LoginResponse(String token) {}

// Para importar conteúdo externo:
public record ImportRequest(
    String description,
    String s3Url,
    String energyType,
    int frequencyHz
) {}
```

Java Records são **imutáveis por natureza** — sem setters. Um `LoginRequest` recebido da API não pode ser alterado após ser criado.

---

## Design Patterns GoF

### Builder (Criacional)

**Problema:** construtor com muitos parâmetros na ordem certa é difícil de ler e propenso a erros.  
**Solução:** construção passo a passo com métodos nomeados.

**No Arcturus — `@Builder` do Lombok:**

```java
// Em vez de: new VibrationalContent(null, "432Hz", "Meditação", url, 432.0, "HEALING", key, user)
VibrationalContent content = VibrationalContent.builder()
    .title("Frequência 432Hz")
    .description("Meditação profunda")
    .frequencyHz(432.0)
    .energyType("HEALING")
    .s3Url(s3Url)
    .user(user)
    .build();
```

**No Arcturus — AWS SDK:**

```java
PutObjectRequest putOb = PutObjectRequest.builder()
    .bucket(bucketName)
    .key(fileName)
    .contentType(file.getContentType())
    .build();
```

**No Arcturus — HttpClient:**

```java
this.httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

---

### Singleton (Criacional)

**Problema:** algumas classes devem ter apenas uma instância compartilhada.  
**Solução:** o Spring gerencia isso automaticamente para todos os beans.

**No Arcturus:**

```java
@Service  // Spring cria UMA instância — compartilhada em todas as requisições
public class TokenService {
    // sem estado mutável entre requisições — thread-safe
}
```

No frontend Angular, o mesmo padrão com `providedIn: 'root'`:

```typescript
@Injectable({ providedIn: 'root' })  // uma instância para toda a app
export class AuthService {
    isLoggedIn = signal<boolean>(this.hasToken());
}
```

---

### Template Method (Comportamental)

**Problema:** um algoritmo tem um esqueleto fixo, mas um passo específico varia conforme a subclasse.  
**Solução:** a superclasse define o esqueleto; a subclasse implementa o passo variável.

**No Arcturus:**

```java
// OncePerRequestFilter (Spring) — define o esqueleto:
public abstract class OncePerRequestFilter {
    public final void doFilter(request, response, chain) {
        // garante execução única por requisição
        doFilterInternal(request, response, chain); // passo em aberto
    }
    protected abstract void doFilterInternal(...); // subclasse implementa
}

// SecurityFilter (Arcturus) — implementa o passo variável:
@Override
protected void doFilterInternal(...) {
    String token = recoverToken(request);
    // valida JWT e autentica no SecurityContext
    filterChain.doFilter(request, response);
}
```

---

### Chain of Responsibility (Comportamental)

**Problema:** uma requisição precisa passar por várias etapas independentes.  
**Solução:** cada etapa é um handler que processa e passa para o próximo.

**No Arcturus — cadeia de filtros do Spring Security:**

```java
// SecurityConfig.java — SecurityFilter é adicionado antes do filtro padrão:
.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)

// A cadeia:
// SecurityFilter → UsernamePasswordAuthenticationFilter → ... → Controller
```

```java
// Cada filtro decide se passa para o próximo:
filterChain.doFilter(request, response); // passa para o próximo elo da cadeia
```

Se o token for inválido, o `SecurityFilter` não autentica — a requisição continua na cadeia sem autenticação, e o Spring Security retorna `401` automaticamente.

---

### Repository Pattern

**Problema:** acoplamento direto entre a lógica de negócio e o banco de dados.  
**Solução:** interface de repositório que abstrai o acesso aos dados.

**No Arcturus:**

```java
// A interface define o contrato:
public interface ContentRepository extends JpaRepository<VibrationalContent, UUID> {
    List<VibrationalContent> findByUser(User user);
    Optional<VibrationalContent> findByIdAndUser(UUID id, User user);
}

// O controller depende da abstração, não da implementação:
@RestController
@RequiredArgsConstructor
public class ContentController {
    private final ContentRepository contentRepository; // interface, não implementação
}
```

Trocar PostgreSQL por outro banco não exige mudar nenhuma linha no controller.

---

## Segurança — JWT e Spring Security

O fluxo de autenticação do Arcturus funciona assim:

```
1. POST /auth/login  →  AuthenticationController
2. Valida credenciais com PasswordEncoder (BCrypt)
3. Gera token JWT com TokenService
4. Retorna LoginResponse(token)

Em requisições subsequentes:
5. Header: Authorization: Bearer <token>
6. SecurityFilter intercepta → valida JWT → autentica no SecurityContext
7. @AuthenticationPrincipal recupera o User autenticado nos endpoints
```

**Por que JWT e não sessão?**

```java
// SecurityConfig.java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Sessão = o servidor guarda estado. JWT = o token carrega todas as informações necessárias. APIs REST devem ser **stateless** — cada requisição é independente e completa em si mesma.

**Por que BCrypt para senhas?**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // hash com salt automático
}

// No registro:
newUser.setPassword(passwordEncoder.encode(body.password())); // nunca salva em texto puro
```

BCrypt aplica um salt aleatório a cada hash — duas senhas iguais geram hashes diferentes. Impossível reverter.

---

## Princípios SOLID

| Princípio | Como aparece no Arcturus |
|---|---|
| **S** — Single Responsibility | Cada classe tem uma responsabilidade: `TokenService` → JWT, `S3Service` → upload, `SecurityFilter` → autenticação |
| **O** — Open/Closed | `SecurityFilter` estende `OncePerRequestFilter` sem modificá-la. Novos filtros = novas classes |
| **L** — Liskov Substitution | `ContentRepository` pode ser substituída por qualquer implementação de `JpaRepository` |
| **I** — Interface Segregation | Interfaces de repositório específicas por entidade: `UserRepository`, `ContentRepository` |
| **D** — Dependency Inversion | Controllers dependem de interfaces (`UserRepository`), não de implementações concretas do Hibernate |

---

## Mapa de Anotações

| Anotação | Origem | Onde no Arcturus | Para que serve |
|---|---|---|---|
| `@SpringBootApplication` | Spring Boot | `StreamApiApplication` | Inicializa a aplicação |
| `@RestController` | Spring Web | `AuthController`, `ContentController` | Controller que retorna JSON |
| `@RequestMapping` | Spring Web | `AuthController`, `ContentController` | Prefixo de URL do controller |
| `@GetMapping` | Spring Web | `ContentController` | Mapeia GET |
| `@PostMapping` | Spring Web | `AuthController`, `ContentController` | Mapeia POST |
| `@DeleteMapping` | Spring Web | `ContentController` | Mapeia DELETE |
| `@RequestBody` | Spring Web | `login()`, `register()`, `importContent()` | Desserializa corpo JSON |
| `@RequestParam` | Spring Web | `uploadContent()`, `search()` | Parâmetros de URL ou form |
| `@PathVariable` | Spring Web | `deleteContent()` | Trecho dinâmico da URL |
| `@AuthenticationPrincipal` | Spring Security | Todos os endpoints protegidos | Injeta usuário autenticado |
| `@EnableWebSecurity` | Spring Security | `SecurityConfig` | Ativa segurança customizada |
| `@Service` | Spring | `TokenService`, `S3Service`, `ExternalMediaService` | Bean de negócio — Singleton |
| `@Component` | Spring | `SecurityFilter` | Bean de infraestrutura |
| `@Repository` | Spring Data | `ContentRepository` | Bean de acesso a dados |
| `@Configuration` | Spring | `SecurityConfig`, `S3Config`, `WebConfig` | Classe de configuração |
| `@Bean` | Spring | `passwordEncoder()`, `s3Client()`, `filterChain()` | Declara bean gerenciado |
| `@Value` | Spring | `TokenService`, `S3Service`, `ExternalMediaService` | Injeta propriedades do yaml |
| `@Entity` | JPA | `User`, `VibrationalContent` | Classe = tabela no banco |
| `@Table` | JPA | `User`, `VibrationalContent` | Nome da tabela |
| `@Id` + `@GeneratedValue` | JPA | `User`, `VibrationalContent` | Chave primária com geração auto |
| `@Column` | JPA | `username`, `s3Url`, `energyType`... | Customiza coluna no banco |
| `@ManyToOne` + `@JoinColumn` | JPA | `VibrationalContent.user` | Relacionamento N:1 |
| `@Getter` / `@Setter` | Lombok | `User`, `VibrationalContent` | Gera getters e setters |
| `@Builder` | Lombok | `VibrationalContent` | Padrão Builder automático |
| `@NoArgsConstructor` | Lombok | `VibrationalContent` | Construtor vazio (JPA precisa) |
| `@AllArgsConstructor` | Lombok | `VibrationalContent` | Construtor completo |
| `@RequiredArgsConstructor` | Lombok | `ContentController`, `S3Service` | DI por construtor — campos final |
| `@Override` | Java Core | `SecurityFilter.doFilterInternal` | Valida sobrescrita em compilação |

---

*Feito com 💜 por [Marianna Rocha](https://github.com/mariannacrocha)*  
*Contribuições e sugestões são bem-vindas!*