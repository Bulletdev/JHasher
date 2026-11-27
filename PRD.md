# JHasher v2.0 - Product Requirements Document

**Projeto:** JHasher
**Autor:** Michael Douglas
**Data:** 2026-11-26
**Status:** Draft
**Abordagem:** Caminho B - Hash/Crypto Swiss Army Knife + GUI/TUI

---

## 1. Contexto e Motivacao

O JHasher atual (v1.0) e um toolkit Java 17 com 10 ferramentas standalone de seguranca ofensiva. Uma revisao profunda revelou que o projeto esta **significativamente defasado** e com problemas criticos:

### 1.1 Diagnostico do Estado Atual

| Problema | Impacto |
|---|---|
| 3 dependencias Maven declaradas mas **nenhuma usada** no codigo (jbcrypt 0.4, jjwt 0.9.1, bcprov-jdk15on 1.70) | Build inflado, deps desatualizadas sem proposito |
| 10 classes com `main()` isoladas, sem CLI unificado | UX fragmentada, sem identidade de produto |
| Valores hardcoded em 8 de 10 ferramentas (IPs, URLs, credenciais, hashes) | Ferramentas inutilizaveis sem editar codigo-fonte |
| AES em modo ECB (inseguro por padrao) | Vulnerabilidade de seguranca no proprio toolkit de seguranca |
| PortScanner fixo em `127.0.0.1`, HashIdentifier com hash fixo no codigo | Ferramentas nao funcionam de verdade |
| Zero testes, zero CI/CD | Nenhuma garantia de qualidade |
| README com classpaths errados | Documentacao quebrada |

### 1.2 Sobreposicao com AnarCry e OhmZ

A analise cruzada com os outros dois projetos revelou que **~90% das funcionalidades do JHasher sao cobertas de forma superior** por eles:

| JHasher Tool | Ja resolvido por |
|---|---|
| PortScanner (TCP connect, 1-1024, localhost) | **OhmZ** - 8 tipos de scan, 100K+ pps, service detection, OS fingerprint |
| JwtInspector (decode Base64 only) | **AnarCry** - 8 probes (alg:none, key confusion, JWK injection, x5c, x5u, null sig) |
| XXEDetector (testa parser local) | **AnarCry** - XXE remoto, blind, SSRF-via-XXE |
| SSRFTester (5 payloads concatenados) | **AnarCry** - IP bypass, metadata endpoints, OAST integration |
| SubdomainScanner (9 subdomains) | **AnarCry** - CrawlerModule + SubTakeoverModule com wordlists |
| DirBrute (7 paths) | **AnarCry** - CrawlerModule com wordlists, OpenAPI discovery |
| HttpBruteForcer (3 passwords) | **AnarCry** - FuzzEngine com virtual threads, throttling, session handling |
| ShiroExploit (cookie basico) | **AnarCry** - DeserializationModule completo |
| AesTool (ECB, 128-bit, hardcoded) | Parcialmente por ambos (TLS/crypto modules) |
| HashIdentifier (4 tipos) | **Unico no ecossistema** - mas muito limitado |

### 1.3 Analise de Mercado

Ferramentas existentes cobrem partes isoladas do problema:

**Identificacao:** hashID (Python, 220+ tipos, sem manutencao desde 2015), Name-That-Hash (Python, mantido), haiti (Ruby, mantido)
**Cracking:** Hashcat (GPU, imbativel), John the Ripper (CPU)
**JWT:** jwt_tool (Python, pentest), jwt-hack (Rust, cracker + exploits)
**Encode/Decode:** CyberChef (browser-only), openssl (CLI verboso)

**Nenhuma ferramenta unifica tudo isso.** O fluxo tipico exige 4-6 ferramentas, 3+ runtimes, e nenhuma conversa entre elas. E o CyberChef - a referencia para transform chains - so existe no browser.

### 1.4 Oportunidade

JHasher v2.0 sera o **CyberChef de terminal com esteroides** - um toolkit unico para todo o workflow de hashing e criptografia ofensiva, com:

- **Identificacao contextual** que nenhum concorrente faz (detecta o hash E sugere de onde veio)
- **Transform chains no terminal** (o CyberChef que todo pentester quer mas so existe no browser)
- **GUI/TUI como bancada de trabalho** visual para exploracao manual
- **Integracao nativa** com o ecossistema AnarCry/OhmZ que nenhum concorrente pode copiar
- **Cracking leve** como conveniencia (top passwords, quick checks) sem tentar competir com Hashcat

---

## 2. Visao do Produto

**JHasher v2.0** e um hash/crypto Swiss Army knife para seguranca ofensiva.

**CLI-first, GUI como segundo modo.** O core faz tudo; CLI e GUI sao frontends intercambiaveis sobre a mesma engine.

### 2.1 Papel no Ecossistema

```
+------------------+     +------------------+     +------------------+
|      OhmZ        |     |     JHasher      |     |    AnarCry       |
|  Network Scan    |---->|  Hash & Crypto   |<----|  Web/App Scan    |
|  (Zig, raw perf) |     |  (Java, tools)   |     |  (Java, 44 mods) |
+------------------+     +------------------+     +------------------+
   Encontra hosts    Identifica, transforma,     Encontra hashes em
   e servicos        gera, cracka, analisa       responses, JWTs,
                     hashes e crypto             configs, DBs
```

### 2.2 Modos de Interface

| Modo | Quando usar | Stack |
|---|---|---|
| **CLI** | Automacao, piping, scripting, integracao CI | Picocli 4.x |
| **TUI** | SSH remoto, terminal interativo, exploracao rapida | Lanterna (do AnarCry) |
| **GUI** | Bancada de trabalho visual, transform builder, JWT explorer | Swing + FlatLaf (do AnarCry) |

Lancamento: `jhasher` (CLI), `jhasher --tui` (TUI), `jhasher --gui` (GUI)

---

## 3. Requisitos Funcionais

### 3.1 CLI Unificado

**Prioridade:** P0

Entry point unico com subcomandos:

```
jhasher identify <hash>              # Identifica tipo + contexto de origem
jhasher generate <algo> <input>      # Gera hash
jhasher jwt decode <token>           # Decodifica JWT
jhasher jwt attack <token> <url>     # Ataques JWT
jhasher encrypt <algo> <input>       # Encripta
jhasher decrypt <algo> <input>       # Decripta
jhasher chain "<proc1>|<proc2>"      # Pipeline de transformacoes
jhasher crack <hash> --wordlist <f>  # Quick crack (dictionary only)
jhasher --tui                        # Abre modo TUI
jhasher --gui                        # Abre modo GUI
```

- Flags globais: `--output json|text|table`, `--no-color`, `--verbose`, `--help`
- Leitura de stdin para piping: `echo "abc" | jhasher generate sha256`
- Leitura de arquivo: `jhasher identify --file hashes.txt`
- Composicao Unix: `ohmz -sV target | jhasher identify --stdin`

### 3.2 Hash Identifier com Contexto

**Prioridade:** P0 - Feature diferenciadora principal

O identificador atual detecta 4 tipos. O v2.0 detecta **40+ tipos** E diz **de onde provavelmente veio**.

#### 3.2.1 Algoritmos Suportados

**Categoria 1 - Hashes criptograficos:**
- MD5, MD4, MD2
- SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
- SHA3-224, SHA3-256, SHA3-384, SHA3-512
- RIPEMD-128, RIPEMD-160, RIPEMD-256, RIPEMD-320
- Whirlpool, Tiger, GOST
- BLAKE2b, BLAKE2s, BLAKE3

**Categoria 2 - Hashes de password:**
- BCrypt (`$2a$`, `$2b$`, `$2y$`)
- Argon2 (`$argon2id$`, `$argon2i$`, `$argon2d$`)
- scrypt (`$scrypt$`, `$s0$`)
- PBKDF2 (various prefixes)
- yescrypt (`$y$`)
- PHC format generico

**Categoria 3 - Hashes de aplicacao:**
- NTLM, LM, NTLMv2
- MySQL 3.x/4.x/5.x (`*` prefix)
- PostgreSQL MD5 (`md5` prefix)
- Oracle 10g/11g/12c
- MSSQL (`0x0100`, `0x0200`)
- MongoDB SCRAM
- WordPress (`$P$`), Drupal (`$S$`), Joomla
- Django (PBKDF2, BCrypt, Argon2 wrappers)
- Linux shadow (`$1$`, `$5$`, `$6$`, `$y$`)

**Categoria 4 - Hashes nao-criptograficos:**
- CRC32, CRC16, Adler-32
- MurmurHash3 (inspirado no OhmZ `blackrock.zig`)
- SipHash (inspirado no OhmZ `cookie.zig`)
- xxHash, FNV-1a

#### 3.2.2 Identificacao Contextual (diferencial)

Alem de dizer "isso e SHA-256", o JHasher analisa o contexto e sugere a **origem provavel**:

```
$ jhasher identify '$2b$12$LJ3m4ys3Lg2VHeMsFrEXge...'

  Tipo:       BCrypt (cost 12)
  Confianca:  99% (prefix match + structure)
  Hashcat:    mode 3200
  John:       bcrypt

  Contexto provavel:
    - Ruby on Rails (bcrypt-ruby, cost padrao 12)
    - Node.js bcrypt (cost padrao 10-12)
    - Spring Security (default encoder desde 5.x)

  Forca:  FORTE - cost factor 12 = ~250ms/hash
  Nota:   Resistente a GPU, brute-force impraticavel acima de 8 chars
```

```
$ jhasher identify '0x0100A607BA...'

  Tipo:       MSSQL 2005+ (SHA-1 + salt)
  Confianca:  95% (prefix 0x0100 + length)
  Hashcat:    mode 131

  Contexto provavel:
    - SQL Server 2005-2008 R2 (sys.sql_logins)
    - Dump via SELECT password_hash FROM sys.sql_logins

  Forca:  FRACA - SHA-1 sem iteracoes, GPU-crackable
  Nota:   Hashcat faz ~8 GH/s em RTX 4090
```

**Dados de contexto** organizados em JSON embeddable (inspirado no KnowledgeBase pattern do AnarCry):
- Mapeamento hash_type -> aplicacoes conhecidas
- Default cost/rounds por framework
- Forca estimada por tipo
- Hashcat/John mode numbers
- Notas de ataque (GPU speed estimates, tecnicas conhecidas)

#### 3.2.3 Comportamento

- Retornar **todos os matches possiveis** rankeados por probabilidade
- Detectar por: comprimento, charset, prefixo, regex patterns, estrutura interna
- Suporte a batch: `jhasher identify --file hashes.txt`
- Output `--hashcat-mode` para retornar so o mode number
- Output `--json` para integracao com outras ferramentas

### 3.3 Transform Chain Engine (CyberChef de terminal)

**Prioridade:** P0 - Feature diferenciadora principal

**Reusar do AnarCry:** `PayloadProcessors` - pipeline composavel

#### 3.3.1 Transforms Disponiveis

**Encoding/Decoding:**
- Base64 encode/decode (standard, URL-safe, no-padding)
- URL encode/decode (full, component)
- Hex encode/decode
- HTML entity encode/decode
- Unicode escape/unescape
- ASCII85 / Base32
- Punycode encode/decode

**Hashing:**
- MD5, SHA-1, SHA-256, SHA-384, SHA-512
- SHA3-*, BLAKE2b, BLAKE3
- HMAC-* (com key flag)
- CRC32

**Crypto:**
- AES-GCM encrypt/decrypt
- ChaCha20-Poly1305 encrypt/decrypt
- XOR com key
- ROT13/ROTn

**Text manipulation:**
- Upper/lower/title case
- Reverse
- Regex replace
- Substring/split/join
- Repeat
- Pad (left/right/center)

**Compression:**
- Gzip compress/decompress
- Zlib compress/decompress
- Deflate/inflate

**Analysis:**
- Length
- Entropy calculation
- Frequency analysis
- Detect encoding (auto-detect)

#### 3.3.2 CLI Pipeline

```bash
# Encode chain
jhasher chain "base64encode | urlencode" "hello world"

# Decode chain
jhasher chain "base64decode | hex" "aGVsbG8gd29ybGQ="

# Hash depois de decode
jhasher chain "base64decode | sha256" "aGVsbG8="

# Multi-step decode de payload suspeito
jhasher chain "urldecode | base64decode | gunzip" "%64R..."

# Encrypt com key
jhasher chain "aes-gcm-encrypt --key abc123" "secret data"

# Analise
jhasher chain "entropy" "aGVsbG8gd29ybGQ="

# Pipe com outras tools
ohmz -sV target --json | jq '.services[].banner' | jhasher chain "detect-encoding | auto-decode"
```

#### 3.3.3 GUI - Visual Transform Builder

Na GUI, o Transform Chain vira uma **bancada visual estilo CyberChef**:

```
+-------------------------------------------------------------------+
| JHasher - Transform Workbench                                      |
+-------------------------------------------------------------------+
|                                                                     |
| INPUT                           | RECIPE                           |
| +-----------------------------+ | +-------------------------------+|
| | aGVsbG8gd29ybGQ=            | | | 1. Base64 Decode              ||
| |                             | | | 2. SHA-256                    ||
| |                             | | | 3. Hex Encode                 ||
| +-----------------------------+ | | [+ Add Step]                  ||
|                                 | +-------------------------------+|
| OUTPUT                          |                                   |
| +-----------------------------+ | AVAILABLE TRANSFORMS             |
| | 2cf24dba5fb0a30e26e83b2a... | | | Encoding  | Hashing  | Crypto ||
| |                             | | | Base64    | MD5      | AES    ||
| | Type: hex string            | | | URL       | SHA-1    | ChaCha ||
| | Length: 64 chars             | | | Hex       | SHA-256  | XOR    ||
| +-----------------------------+ | | HTML      | SHA-512  | ROT13  ||
|                                 | | Unicode   | BLAKE2b  |        ||
+-------------------------------------------------------------------+
```

Features visuais:
- Drag-and-drop de transforms na recipe
- Output atualiza em **tempo real** enquanto edita input ou recipe
- Syntax highlighting no output (detecta tipo automaticamente)
- Salvar/carregar recipes como presets
- Favoritar transforms mais usados
- Breakpoints entre steps para inspecionar intermediarios
- Botao "auto-decode" que tenta detectar e decodificar automaticamente

### 3.4 Hash Generator

**Prioridade:** P0

Gerar hashes de qualquer input usando todos os algoritmos suportados.

- Algoritmos: todos os listados em 3.2 Categoria 1 + password hashes
- Input: string (arg), stdin, arquivo (`--file`)
- Output: hash puro, `algo:hash`, JSON com metadata
- Para password hashes: flags `--rounds`, `--salt`, `--cost`
- Modo comparacao: `jhasher generate sha256 "abc" --verify <expected_hash>`
- Modo bulk: `jhasher generate sha256 --file wordlist.txt` (gera hash de cada linha)
- Modo multi-algo: `jhasher generate all "abc"` (gera com todos os algoritmos de uma vez)

### 3.5 JWT Module

**Prioridade:** P1

**Reusar do AnarCry:** `JwtModule` - 8 probes, 21 weak secrets, HMAC-SHA256 signing

#### 3.5.1 Subcomandos

- `jwt decode <token>` - header + payload + signature analysis com syntax highlighting
- `jwt attack <token> --url <endpoint>` - executa probes automaticamente:
  - alg:none bypass
  - RS256-to-HS256 key confusion
  - JKU/x5u injection
  - Null signature
  - Weak secret brute-force (wordlist customizavel)
- `jwt forge --alg <alg> --payload <json> --key <key>` - criar JWT custom
- `jwt compare <token1> <token2>` - diff visual entre dois tokens

#### 3.5.2 GUI - JWT Explorer

```
+-------------------------------------------------------------------+
| JHasher - JWT Explorer                                             |
+-------------------------------------------------------------------+
| TOKEN INPUT                                                        |
| +---------------------------------------------------------------+ |
| | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3O...| |
| +---------------------------------------------------------------+ |
|                                                                     |
| HEADER (ALGO: HS256)     | PAYLOAD              | SIGNATURE        |
| +----------------------+ | +------------------+ | +--------------+ |
| | {                    | | | {                | | | HMAC-SHA256  | |
| |   "alg": "HS256",   | | |   "sub": "123",  | | | Status:      | |
| |   "typ": "JWT"       | | |   "name": "John",| | |  UNVERIFIED  | |
| | }                    | | |   "iat": 1516239 | | |              | |
| +----------------------+ | +------------------+ | | [Verify Key] | |
|                           |                      | +--------------+ |
| TIMESTAMPS                | ATTACKS                                 |
| iat: 2024-01-18 01:30:00 | [Run All Probes]                       |
| exp: 2024-01-19 01:30:00 | [x] alg:none     [ ] JKU injection     |
| nbf: (not set)           | [x] Key confusion [ ] x5u injection     |
|   EXPIRED 2y ago         | [x] Null sig      [ ] Weak secret BF    |
+-------------------------------------------------------------------+
```

### 3.6 Crypto Module

**Prioridade:** P1

Substituir o AesTool (ECB-only, hardcoded) por operacoes criptograficas reais.

**Reusar:** BouncyCastle 1.80 do AnarCry (ja configurado como provider)

**Algoritmos simetricos:**
- AES-128/192/256 em CBC, CTR, GCM (nunca ECB)
- ChaCha20-Poly1305
- 3DES (legado, com warning)

**Operacoes CLI:**
- `encrypt <algo> <input> --key <key> [--iv <iv>]`
- `decrypt <algo> <input> --key <key> [--iv <iv>]`
- `keygen <algo> [--size 128|192|256]` - gerar chave aleatoria
- Key derivation: PBKDF2, HKDF, scrypt

**GUI - Crypto Workbench:**
- Campos input/output lado a lado
- Dropdown de algoritmo + modo
- Key/IV fields com geracao automatica
- Botoes encrypt/decrypt
- Historico de operacoes na sessao

### 3.7 Quick Crack (leve, sem competir com Hashcat)

**Prioridade:** P2

**Escopo deliberadamente limitado** - conveniencia para CTF e quick checks:

- **Dictionary only** - wordlist file como input (sem brute-force incremental, sem rule engine)
- Wordlists bundled: top 1000 passwords, rockyou-top-10k
- Custom wordlist via `--wordlist <file>`
- **Reusar do AnarCry:** `PayloadSource` para lazy streaming (sem carregar arquivo inteiro)
- Virtual threads para paralelismo
- Progress bar com speed (hashes/sec)
- Quando nao encontra: sugere comando Hashcat equivalente

```bash
$ jhasher crack "5f4dcc3b5aa765d61d8327deb882cf99" --wordlist rockyou-10k

  Cracking MD5 (auto-detected) with dictionary...
  [=========>                    ] 3,247/10,000 | 892K h/s

  FOUND: password
  Time: 0.003s

$ jhasher crack "$6$rounds=5000$saltsalt$..." --wordlist big.txt

  Cracking SHA-512 crypt with dictionary...
  [=>                            ] 142/1,000,000 | 28 h/s

  NOT FOUND in wordlist.
  Sugestao: hashcat -m 1800 hash.txt wordlist.txt -r best64.rule
```

### 3.8 Integracao com AnarCry (Plugin/Modulo)

**Prioridade:** P2

JHasher expoe uma **API Java** que o AnarCry pode chamar como dependencia:

```java
// AnarCry chamando JHasher como lib
var result = JHasher.identify("5f4dcc3b...");
// -> HashMatch{type=MD5, confidence=0.95, context=[Linux shadow, Raw MD5], hashcatMode=0}

var decoded = JHasher.chain("base64decode | identify", encodedValue);
// -> pipeline result com identificacao no final

var jwt = JHasher.jwt().decode(tokenFromResponse);
// -> JwtAnalysis{header, payload, expired, attacks[]}
```

Casos de uso no AnarCry:
- Scanner encontra hash em HTTP response -> JHasher identifica automaticamente
- DbExplorer extrai password hashes -> JHasher identifica tipo + sugere ataque
- Finding tem valor encoded -> JHasher tenta auto-decode na chain

---

## 4. Requisitos Nao-Funcionais

### 4.1 Stack Tecnico

| Item | De (v1.0) | Para (v2.0) |
|---|---|---|
| Java | 17 | 21+ (virtual threads, pattern matching, sealed classes) |
| Build | Maven | Gradle + Shadow (alinhado com AnarCry) |
| CLI Framework | Nenhum | Picocli 4.x |
| TUI | Nenhum | Lanterna 3.x (do AnarCry) |
| GUI | Nenhum | Swing + FlatLaf (do AnarCry) |
| Editor/Syntax | Nenhum | RSyntaxTextArea (do AnarCry) |
| Crypto | JDK puro (ECB) | BouncyCastle 1.80 (do AnarCry) |
| JWT | Nenhum (Base64 manual) | Nimbus JOSE + JWT |
| Concorrencia | ExecutorService | Virtual threads (Loom) |
| Output CLI | System.out.println | Jansi (cores) + Jackson (JSON) |
| Settings | Nenhum | AppSettings pattern (do AnarCry) |
| Tests | Nenhum | JUnit 5 + property-based testing |
| CI | Nenhum | GitHub Actions |
| Packaging | Nenhum | jpackage (native), Shadow JAR (portable) |

### 4.2 Performance

- Hash generation: **1M+ hashes/sec** para MD5/SHA em single-thread
- Identificacao: **< 1ms** por hash (regex + prefix matching)
- Transform chain: **< 10ms** para chain de 5 steps em input < 1MB
- GUI startup: **< 2s** (lazy load de paineis nao-visiveis)
- CLI startup: **< 500ms** (evitar reflection pesado no boot)
- Memoria: processar wordlists via streaming, nunca carregar arquivo inteiro
- Transform Builder: output atualiza em **< 100ms** apos edit (debounced)

### 4.3 Arquitetura

**Principio: core separado da interface.** Mesmo pattern do AnarCry.

```
jhasher/
  core/                              # Zero deps de UI - reusavel como lib
    identify/
      HashRegistry.java              # Registry de todos os algoritmos
      HashAlgorithm.java             # Sealed interface: id, regex, generate, verify
      HashIdentifier.java            # Motor de identificacao (multi-match, ranking)
      HashContext.java               # Dados de contexto (origem, forca, hashcat mode)
      ContextDatabase.java           # JSON-backed context data (pattern KnowledgeBase/AnarCry)
    generate/
      HashGenerator.java             # Motor de geracao
    transform/
      Transform.java                 # Functional interface (reusar PayloadProcessor/AnarCry)
      TransformRegistry.java         # Registro de transforms disponiveis
      TransformChain.java            # Pipeline engine: parse "a|b|c" -> execute
      Transforms.java                # Implementacoes built-in
    jwt/
      JwtDecoder.java                # Decode + analise de header/payload/signature
      JwtAttacker.java               # Probes (reusar JwtModule do AnarCry)
      JwtForger.java                 # Criacao de tokens custom
    crypto/
      SymmetricCrypto.java           # AES, ChaCha20 (via BouncyCastle)
      KeyDerivation.java             # PBKDF2, HKDF, scrypt
      KeyGenerator.java              # Geracao de chaves
    crack/
      DictionaryCracker.java         # Dictionary-only cracker (leve)
      WordlistSource.java            # Lazy reader (reusar PayloadSource/AnarCry)
    output/
      OutputFormatter.java           # text, json, table formatters

  cli/                               # Picocli commands - delegam pro core
    JHasherCli.java                  # Entry point unico
    IdentifyCommand.java
    GenerateCommand.java
    ChainCommand.java
    JwtCommand.java
    CryptoCommand.java
    CrackCommand.java

  tui/                               # Lanterna screens - delegam pro core
    JHasherTui.java                  # Entry point TUI
    MainScreen.java                  # Menu principal com tabs
    IdentifyScreen.java              # Identificacao interativa
    ChainScreen.java                 # Transform builder (text-mode)
    JwtScreen.java                   # JWT explorer
    CryptoScreen.java                # Encrypt/decrypt interativo

  gui/                               # Swing + FlatLaf - delegam pro core
    JHasherGui.java                  # Entry point GUI
    MainFrame.java                   # Frame principal com tabs (reusar pattern AnarCry)
    panels/
      IdentifyPanel.java             # Hash identification com contexto visual
      TransformPanel.java            # Visual transform builder (CyberChef-style)
      JwtPanel.java                  # JWT explorer com syntax highlighting
      CryptoPanel.java               # Encrypt/decrypt workbench
      CrackPanel.java                # Quick crack com progress
      GeneratePanel.java             # Hash generation multi-algo

  data/                              # JSON resources embeddables
    hash-contexts.json               # Mapeamento tipo -> aplicacoes, forca, notas
    hashcat-modes.json               # Tipo -> hashcat mode number
    john-formats.json                # Tipo -> john format name
    weak-jwt-secrets.json            # Lista de weak secrets (do AnarCry)
    top-passwords.json               # Top 1000 passwords bundled
```

### 4.4 Qualidade

- Cobertura de testes minima: 80% nas classes `core/`
- Property-based testing para HashIdentifier (gerar hash -> identificar deve retornar o algoritmo correto)
- Property-based testing para TransformChain (encode -> decode == identity)
- Integration tests para JWT e Crypto (roundtrip)
- `gradle check` deve rodar: compile + tests + spotbugs + checkstyle
- Zero warnings em `-Xlint:all`
- GUI testavel via AssertJ-Swing ou similar

---

## 5. O Que Remover

As ferramentas abaixo devem ser **removidas** do JHasher porque sao redundantes com AnarCry/OhmZ:

| Ferramenta | Motivo da remocao | Alternativa |
|---|---|---|
| PortScanner | Inferior ao OhmZ em todos os aspectos | `ohmz -sS <target>` |
| SubdomainScanner | 9 subdomains hardcoded, AnarCry faz melhor | AnarCry CrawlerModule |
| DirBrute | 7 paths hardcoded, AnarCry faz melhor | AnarCry CrawlerModule |
| HttpBruteForcer | 3 passwords, sem features reais | AnarCry FuzzEngine |
| ShiroExploit | Sem gadget generation real | AnarCry DeserializationModule |
| XXEDetector | Testa parser local, nao remoto | AnarCry XxeModule |
| SSRFTester | 5 payloads simples | AnarCry SsrfModule |

**Total: 7 ferramentas removidas, 3 reescritas (HashIdentifier, JwtInspector, AesTool).**

---

## 6. Integracao com AnarCry e OhmZ

### 6.1 Codigo a Reusar do AnarCry

| Componente | Arquivo origem (AnarCry) | Uso no JHasher |
|---|---|---|
| PayloadProcessors | `PayloadProcessors.java` | Transform.java - interface + built-in transforms |
| FuzzEngine pattern | `FuzzEngine.java` | DictionaryCracker - virtual threads, progress |
| PayloadSource | `PayloadSource.java` | WordlistSource - lazy file reader |
| JwtModule probes | `JwtModule.java` | JwtAttacker - 8 attack probes |
| JWT weak secrets | `JwtModule.java` | weak-jwt-secrets.json |
| BouncyCastle config | `build.gradle` | SymmetricCrypto - provider setup |
| KnowledgeBase pattern | `KnowledgeBase.java` | ContextDatabase - JSON-backed hash context data |
| FlatLaf + theme | GUI infrastructure | MainFrame - visual identity consistente |
| RSyntaxTextArea | GUI infrastructure | JwtPanel, TransformPanel - syntax highlighting |
| AppSettings | `AppSettings.java` | Settings persistence (tema, favoritos, recipes) |
| Lanterna screens | TUI infrastructure | TUI screens pattern |

### 6.2 Conceitos a Reusar do OhmZ

| Conceito | Arquivo origem (OhmZ) | Adaptacao no JHasher |
|---|---|---|
| SipHash-2-4 | `cookie.zig` | Adicionar SipHash ao HashRegistry (reimplementar em Java) |
| MurmurHash3 finalizer | `blackrock.zig:86-94` | Adicionar MurmurHash3 ao HashRegistry (reimplementar em Java) |
| Atomic stats + progress | `stats.zig`, `progress.zig` | CrackEngine progress reporting pattern |

### 6.3 JHasher como Dependencia do AnarCry

Publicar `jhasher-core` como artefato Maven/Gradle que o AnarCry consome:

```groovy
// No build.gradle do AnarCry
dependencies {
    implementation project(':jhasher-core')  // ou maven coordinates
}
```

Casos de uso:
- `HashIdentifier.identify()` chamado de qualquer Scanner module do AnarCry
- `TransformChain.execute()` para decode de payloads encontrados
- `JwtDecoder.decode()` para analise de tokens em responses
- Findings do AnarCry ganham campo `hashAnalysis` automaticamente

---

## 7. Fases de Implementacao

### Fase 1 - Fundacao + Core (P0)
**Estimativa: 2-3 semanas**

- [ ] Migrar de Maven para Gradle + Shadow (multi-module: core, cli, tui, gui)
- [ ] Upgrade para Java 21+
- [ ] Remover as 7 ferramentas redundantes
- [ ] Remover dependencias mortas (jbcrypt, jjwt 0.9.1, bcprov-jdk15on)
- [ ] Implementar HashRegistry + HashAlgorithm sealed interface
- [ ] Implementar HashIdentifier com 40+ algoritmos
- [ ] Implementar HashContext + ContextDatabase (JSON-backed)
- [ ] Implementar HashGenerator
- [ ] Setup Picocli: `identify` e `generate` subcomandos
- [ ] Implementar OutputFormatter (text, json, table com cores)
- [ ] Setup JUnit 5 + property-based tests + GitHub Actions CI
- [ ] Corrigir README

### Fase 2 - Transform Chain Engine (P0)
**Estimativa: 2-3 semanas**

- [ ] Implementar Transform interface + TransformRegistry
- [ ] Implementar TransformChain parser + executor ("a|b|c" syntax)
- [ ] Implementar 30+ built-in transforms (encoding, hashing, crypto, text, analysis)
- [ ] Implementar `chain` subcomando CLI
- [ ] Stdin piping: `echo "x" | jhasher chain "base64decode | sha256"`
- [ ] Auto-detect transform: `jhasher chain "detect | auto-decode"`
- [ ] Testes: encode/decode roundtrip, chain composition

### Fase 3 - JWT + Crypto (P1)
**Estimativa: 2 semanas**

- [ ] Integrar BouncyCastle 1.80 + Nimbus JOSE
- [ ] Implementar JwtDecoder, JwtAttacker, JwtForger (reusar probes do AnarCry)
- [ ] Implementar SymmetricCrypto (AES-GCM, ChaCha20)
- [ ] Implementar KeyDerivation e KeyGenerator
- [ ] Implementar `jwt` e `encrypt`/`decrypt` subcomandos CLI
- [ ] Testes: roundtrip, JWT forge-then-verify

### Fase 4 - GUI + TUI (P1)
**Estimativa: 3-4 semanas**

- [ ] Configurar FlatLaf + RSyntaxTextArea (do AnarCry)
- [ ] Implementar MainFrame com tabs
- [ ] Implementar TransformPanel (CyberChef visual builder)
  - [ ] Drag-and-drop recipe builder
  - [ ] Live output update (debounced < 100ms)
  - [ ] Salvar/carregar recipes
  - [ ] Breakpoints entre steps
- [ ] Implementar IdentifyPanel (resultado com contexto visual)
- [ ] Implementar JwtPanel (explorer com syntax highlighting)
- [ ] Implementar CryptoPanel (encrypt/decrypt workbench)
- [ ] Implementar GeneratePanel (multi-algo)
- [ ] Configurar Lanterna (do AnarCry)
- [ ] Implementar TUI screens (identify, chain, jwt, crypto)
- [ ] AppSettings persistence (tema, favoritos, recipes, historico)

### Fase 5 - Quick Crack + Integracao (P2)
**Estimativa: 2 semanas**

- [ ] Implementar DictionaryCracker com virtual threads
- [ ] Implementar WordlistSource (lazy streaming)
- [ ] Bundle top-1000 e rockyou-10k
- [ ] Implementar `crack` subcomando CLI + CrackPanel GUI
- [ ] Sugestao de comando Hashcat quando nao encontra
- [ ] Publicar jhasher-core como artefato
- [ ] Documentar API para integracao com AnarCry
- [ ] Testes: crack de hashes conhecidos

### Fase 6 - Polish + Release
**Estimativa: 1-2 semanas**

- [ ] Cobertura de testes >= 80% em core/
- [ ] jpackage para binario nativo (Linux, macOS, Windows)
- [ ] Shell completions (bash/zsh/fish) via Picocli
- [ ] Man page generation
- [ ] README completo com screenshots da GUI e exemplos CLI
- [ ] Release v2.0

---

## 8. Metricas de Sucesso

| Metrica | Target |
|---|---|
| Algoritmos de hash suportados (identify) | >= 40 |
| Algoritmos com dados de contexto | >= 25 |
| Transforms disponiveis | >= 30 |
| Algoritmos de hash suportados (generate) | >= 20 |
| Tempo de startup CLI | < 500ms |
| Tempo de startup GUI | < 2s |
| Transform chain latency (5 steps) | < 10ms |
| Live update no Transform Builder | < 100ms |
| Cobertura de testes core/ | >= 80% |
| Dependencias com CVE conhecida | 0 |
| Ferramentas redundantes com AnarCry/OhmZ | 0 |
| Modos de interface | 3 (CLI + TUI + GUI) |

---

## 9. Riscos e Mitigacoes

| Risco | Probabilidade | Mitigacao |
|---|---|---|
| GUI adiciona complexidade significativa | Media | Core 100% separado da UI - GUI e "apenas" frontend. Se atrasar, CLI lanca sem GUI |
| Transform builder visual e complexo de implementar | Media | Comecar com lista simples (add/remove/reorder), drag-and-drop e iteracao futura |
| Reuso de codigo do AnarCry exige refactor | Media | Reusar patterns e interfaces, nao copiar codigo. Alinhar packages entre projetos |
| Dados de contexto (hash origins) ficam desatualizados | Alta | Manter como JSON externo, atualizavel sem rebuild. Aceitar PRs de contexto |
| BouncyCastle e pesado para startup | Baixa | Lazy loading - so inicializar provider quando o subcomando precisa |
| Escopo cresce demais | Media | P0 (core + chain) primeiro. GUI pode ser Fase 4 sem bloquear release CLI |

---

## 10. Decisoes de Design

### 10.1 Por que NAO cracking pesado?

Hashcat faz 8+ GH/s em GPU. Um cracker Java em CPU faz ~500K h/s. Investir em rule engine, brute-force incremental, e otimizacao de throughput e tempo jogado fora competindo onde ja perdeu. O JHasher oferece quick crack como conveniencia (top passwords, CTF) e direciona pra Hashcat quando precisa de forca real.

### 10.2 Por que CLI-first + GUI?

- CLI e essencial para automacao, piping, e integracao com AnarCry/OhmZ
- GUI e essencial para o Transform Builder (arrastar blocos e ver output em tempo real e ordens de magnitude melhor que editar string no terminal)
- TUI cobre o caso SSH remoto onde GUI nao roda
- O core nao sabe qual UI ta usando - mesma engine, 3 frontends

### 10.3 Por que identificacao contextual?

hashID e Name-That-Hash dizem "isso e SHA-256". Util, mas o pentester quer saber "isso e SHA-256 e provavelmente veio de um Django app com PBKDF2 wrapper, e o hashcat mode e 10000". Essa segunda parte e o que ninguem faz e e o que transforma uma tool util em uma tool indispensavel.
