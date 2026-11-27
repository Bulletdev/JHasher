```
     +---------------------------------------------------------+
     |                                                         |
     |      ___   _   _            _                           |
     |      | |  | | | | __ _ ___| |__   ___ _ __              |
     |   _  | |  | |_| |/ _` / __| '_ \ / _ \ '__|             |
     |  | |_| |  |  _  | (_| \__ \ | | |  __/ |                |
     |   \___/   |_| |_|\__,_|___/_| |_|\___|_|                |
     |                                                         |
     |          Hash & Crypto Swiss Army Knife                 |
     |                                                         |
     +---------------------------------------------------------+
```

<div align="center">

![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Build](https://img.shields.io/badge/Build-Gradle_9-02303A?style=flat-square&logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-AGPL--3.0-blue?style=flat-square)
![Version](https://img.shields.io/badge/Version-2.0.0-green?style=flat-square)

**Identify, generate, transform, crack, and attack hashes and crypto .**

Part of the **effront** offensive security ecosystem alongside [AnarCry](https://github.com/effront/anarcry) and [OhmZ](https://github.com/effront/ohmz).

</div>

---

```
+===========================================================================+
|                                                                           |
|   JHasher is a hash and cryptography toolkit for offensive security.      |
|                                                                           |
|   - Identifies 40+ hash types with contextual analysis                    |
|     (where the hash came from, how strong it is, how to attack it)        |
|                                                                           |
|   - Transform chains in the terminal (CyberChef without the browser)      |
|                                                                           |
|   - JWT decode, forge, and 6 attack probes (alg:none, HMAC confusion,     |
|     null signature, weak secret brute-force, JKU/KID injection)           |
|                                                                           |
|   - Modern encryption: AES-GCM/CBC/CTR, ChaCha20-Poly1305                 |
|                                                                           |
|   - Designed to complement AnarCry (web/app scanning) and                 |
|     OhmZ (network scanning) - the hash/crypto piece they don't cover.     |
|                                                                           |
+===========================================================================+
```

---

```
  Table of Contents

  01 - Quick Start
  02 - Features
  03 - Usage Examples
  04 - Technology Stack
  05 - Architecture
  06 - Ecosystem
  07 - Building from Source
  08 - Ethical Use
```

---

## 01 - Quick Start

```bash
# Build
./gradlew shadowJar

# Run
java -jar build/libs/jhasher-2.0.0-SNAPSHOT-all.jar <command>

# Or create an alias
alias jhasher='java -jar /path/to/jhasher-2.0.0-SNAPSHOT-all.jar'
```

```bash
# Identify a hash - tells you what it is, where it came from, and how to crack it
jhasher identify '5f4dcc3b5aa765d61d8327deb882cf99'

# Generate a hash
jhasher generate sha256 "hello world"

# Transform chain (CyberChef in your terminal)
jhasher chain "base64encode|sha256|upper" "secret payload"

# Decode a JWT
jhasher jwt decode 'eyJhbGciOiJIUzI1...'

# Encrypt data
jhasher encrypt aes-256-gcm "sensitive data" --key "strongpassword"
```

---

## 02 - Features

<details open>
<summary><kbd>Hash Identification with Context</kbd></summary>

Identifies 40+ hash types and tells you **where it probably came from**, not just what algorithm it is.

```
$ jhasher identify '$2b$12$WApznUPhDubN0oeveSXHp.P/NfcrMFEiMQ2LYjl...'

[+] bcrypt - Confidence: 99%
    Hashcat mode: 3200
    John format:  bcrypt
    Sources:      Ruby on Rails (bcrypt-ruby), Node.js bcrypt, Spring Security 5.x+, Laravel
    Strength:     STRONG - Adaptive cost, memory-hard, GPU-resistant
    Attacks:      Hashcat: ~105 KH/s on RTX 4090 (cost 12); Brute-force impractical above 8 chars
```

```
+==========================+=============+===================================+
| Category                 | Hash Types  |            Examples               |
+==========================+=============+===================================+
| Cryptographic            | 13          | MD5, SHA-1, SHA-256, SHA-512,     |
|                          |             | SHA3-256, SHA3-512, RIPEMD-160,   |
|                          |             | Whirlpool, BLAKE2b-256/512        |
+--------------------------+-------------+-----------------------------------+
| Password                 | 8           | BCrypt, Argon2, scrypt, yescrypt, |
|                          |             | PBKDF2, Linux SHA-512/256/MD5     |
+--------------------------+-------------+-----------------------------------+
| Application              | 10          | NTLM, MySQL 5.x, PostgreSQL MD5,  |
|                          |             | MSSQL 2005/2012, WordPress,       |
|                          |             | Drupal, Django PBKDF2, Oracle     |
+--------------------------+-------------+-----------------------------------+
| Checksum                 | 3           | CRC32, CRC16, Adler-32            |
+--------------------------+-------------+-----------------------------------+
```

Contextual data includes:
- Common real-world sources (which apps/frameworks produce this hash)
- Strength rating (VERY_WEAK to VERY_STRONG)
- Attack notes (GPU cracking speeds, known weaknesses)
- Hashcat mode and John the Ripper format numbers

</details>

<details>
<summary><kbd>Transform Chains (CyberChef in Your Terminal)</kbd></summary>

Pipe-separated transform chains that compose like Unix pipes.

```bash
# Encode then hash
jhasher chain "base64encode|sha256" "hello world"

# Decode a suspicious payload
jhasher chain "urldecode|base64decode" "%64R..."

# Multi-step transform with uppercase output
jhasher chain "base64encode|sha256|upper" "password123"
# Result: 7EA37D0251E86C0334B82D8F2AF4BE5D888FD947A847B1E327D0DEF96D442F57

# URL encode special characters
jhasher chain "urlencode" "hello world&foo=bar"
# Result: hello+world%26foo%3Dbar

# Show intermediate results at each step
jhasher chain "base64encode|sha256" "test" --show-steps
```

```
+=====================+==============================================+
| Category            | Available Transforms                         |
+=====================+==============================================+
| Encoding            | base64encode, base64decode, base64url-encode |
|                     | base64url-decode, hex-encode, hex-decode     |
|                     | urlencode, urldecode, html-encode            |
|                     | html-decode, unicode-escape, unicode-unescape|
+---------------------+----------------------------------------------+
| Hashing             | md5, sha1, sha256, sha384, sha512, crc32     |
|                     | hmac-sha256 (--key required)                 |
+---------------------+----------------------------------------------+
| Crypto              | aes-gcm-encrypt, aes-gcm-decrypt             |
|                     | xor, rot13, rotn                             |
+---------------------+----------------------------------------------+
| Text                | upper, lower, reverse, length, repeat        |
|                     | replace (--from, --to)                       |
+---------------------+----------------------------------------------+
| Compression         | gzip, gunzip                                 |
+---------------------+----------------------------------------------+
| Analysis            | entropy, freq                                |
+---------------------+----------------------------------------------+
```

</details>

<details>
<summary><kbd>JWT Decode, Forge, and Attack</kbd></summary>

Full JWT analysis and 6 attack probes.

```bash
# Decode a JWT - shows header, payload, timestamps, and warnings
jhasher jwt decode 'eyJhbGciOiJIUzI1NiIs...'

# Run all attack probes against a JWT
jhasher jwt attack 'eyJhbGciOiJIUzI1NiIs...'

# Forge a custom JWT
jhasher jwt forge --alg HS256 --payload '{"sub":"admin","role":"superuser"}' --key "secret"
```

```
$ jhasher jwt decode 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'

[JWT Analysis]
  Algorithm: HS256

  Header:
    alg: HS256
    typ: JWT

  Payload:
    sub: 1234567890
    name: John Doe
    iat: 1.516239022E9

  Timestamps:
    Issued At:  2018-01-18T01:30:22Z

  Warnings:
    ! Missing expiration claim
    ! Weak algorithm: HS256 may be vulnerable to brute-force if secret is weak
```

Attack probes:
- **alg:none** - 4 case variants (none, None, NONE, nOnE) with empty signature
- **HMAC confusion** - RS256-to-HS256 key confusion with public key
- **Null signature** - keep algorithm, strip signature
- **Weak secret brute-force** - 30 common secrets dictionary
- **JKU injection** - inject attacker-controlled JWKS URL
- **KID SQL injection** - SQL payload in kid header

</details>

<details>
<summary><kbd>Encryption and Key Generation</kbd></summary>

Modern symmetric encryption with no insecure defaults. ECB mode is not available.

```bash
# Encrypt with AES-256-GCM (default, recommended)
jhasher encrypt aes-256-gcm "sensitive data" --key "strongpassword"

# Decrypt
jhasher decrypt aes-256-gcm "lUslLBCBEABh..." --key "strongpassword"

# ChaCha20-Poly1305
jhasher encrypt chacha20-poly1305 "data" --key "password"

# Generate a 256-bit random key
jhasher keygen --size 256

# Generate a random password
jhasher keygen --password --length 32
```

```
+=======================+============+===========================+
| Algorithm             | Key Sizes  | Notes                     |
+=======================+============+===========================+
| AES-GCM               | 128/192/256| Recommended default       |
| AES-CBC               | 128/192/256| PKCS5 padding             |
| AES-CTR               | 128/192/256| Stream mode               |
| ChaCha20-Poly1305     | 256        | Modern alternative to AES |
+=======================+============+==========================+
```

Key derivation: PBKDF2 (600K iterations), scrypt, HKDF-SHA256.

</details>

<details>
<summary><kbd>Hash Generation</kbd></summary>

Generate hashes with any supported algorithm, including password hashes.

```bash
# Single algorithm
jhasher generate sha256 "hello world"
# b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9

# All algorithms at once
jhasher generate sha256 "test" --all
# md5: 098f6bcd4621d373cade4e832627b4f6
# sha1: a94a8fe5ccb19ba61c4c0873d391e987982fbbd3
# sha256: 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
# sha512: ee26b0dd4af7e749aa1a8ee3c10ae9923f618980...
# bcrypt: $2y$12$...
# ... (17 algorithms total)

# Verify a hash
jhasher generate sha256 "password" --verify "5e884898da..."

# From stdin
echo "hello world" | jhasher generate sha256
```

</details>

---

## 03 - Usage Examples

```bash
# Identify a hash dumped from a database
jhasher identify '*6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9'
# [+] MySQL 5.x - Confidence: 99%
#     Hashcat mode: 300
#     Sources: MySQL 5.x+ (mysql.user table), MariaDB

# Identify a hash from /etc/shadow
jhasher identify '$6$rounds=5000$saltsalt$aBcDeFg...'
# [+] Linux SHA-512 crypt - Confidence: 99%
#     Hashcat mode: 1800
#     Sources: Modern Linux /etc/shadow, Ubuntu/Debian/RHEL default

# JSON output for scripting
jhasher -o JSON identify '5f4dcc3b5aa765d61d8327deb882cf99'

# Output only the hashcat mode number
jhasher identify '5f4dcc3b5aa765d61d8327deb882cf99' --hashcat-mode

# Pipe with other tools
echo "some encoded payload" | jhasher chain "base64decode|sha256"

# JWT attack - find weak secrets
jhasher jwt attack 'eyJhbGciOiJIUzI1NiIs...'
# [OK] alg-none(none): Set algorithm to 'none' and removed signature
# [OK] null-signature: Kept original algorithm but set signature to empty
# [ERROR] weak-secret: Tried 30 secrets - none matched
# [OK] jku-injection: Added jku header pointing to attacker URL
```

---

## 04 - Technology Stack

```
+=======================+==============================+
| Component             | Technology                   |
+=======================+==============================+
| Language              | Java 21+ (virtual threads,   |
|                       | records, sealed classes)     |
+-----------------------+------------------------------+
| Build                 | Gradle 9.x + Shadow JAR      |
+-----------------------+------------------------------+
| CLI Framework         | Picocli 4.7                  |
+-----------------------+------------------------------+
| Cryptography          | BouncyCastle 1.80            |
|                       | (bcprov-jdk18on)             |
+-----------------------+------------------------------+
| JWT                   | Nimbus JOSE + JWT 10.3       |
+-----------------------+------------------------------+
| Terminal Colors       | Jansi 2.4                    |
+-----------------------+------------------------------+
| Serialization         | Gson 2.13                    |
+-----------------------+------------------------------+
| Testing               | JUnit 5.11                   |
+-----------------------+------------------------------+
| CI                    | GitHub Actions               |
+-----------------------+------------------------------+
```

---

## 05 - Architecture

```
+-----------------------------------------------------------------------+
|                            jhasher                                     |
+-----------------------------------------------------------------------+
|                                                                        |
|  cli/                          core/                                   |
|  +------------------------+    +------------------------------------+  |
|  | JHasherCli (Picocli)   |    | identify/                          |  |
|  | IdentifyCommand        |--->|   HashAlgorithm (interface)        |  |
|  | GenerateCommand        |    |   HashRegistry (40+ algorithms)    |  |
|  | ChainCommand           |    |   HashIdentifier (multi-match)     |  |
|  | JwtCommand             |    |   ContextDatabase (JSON-backed)    |  |
|  | EncryptCommand         |    |                                    |  |
|  | DecryptCommand         |    | generate/                          |  |
|  | KeygenCommand          |    |   HashGenerator                    |  |
|  +------------------------+    |                                    |  |
|                                | transform/                         |  |
|                                |   Transform (interface)            |  |
|                                |   TransformChain (parser+runner)   |  |
|                                |   Transforms (30+ built-in)        |  |
|                                |                                    |  |
|                                | jwt/                               |  |
|                                |   JwtDecoder                       |  |
|                                |   JwtAttacker (6 probes)           |  |
|                                |   JwtForger                        |  |
|                                |                                    |  |
|                                | crypto/                            |  |
|                                |   SymmetricCrypto                  |  |
|                                |   KeyDerivation                    |  |
|                                |   CryptoKeyGenerator               |  |
|                                |                                    |  |
|                                | output/                            |  |
|                                |  OutputFormatter (TEXT/JSON/TABLE) |  |
|                                +------------------------------------+  |
|                                                                        |
+-----------------------------------------------------------------------+
```

The core has zero UI dependencies and can be consumed as a library.

---

## 06 - Ecosystem

JHasher is one of three complementary tools in the effront offensive security toolkit:

```
+------------------+     +------------------+     +------------------+
|      OhmZ        |     |     JHasher      |     |    AnarCry       |
|  Network Scan    |---->|  Hash & Crypto   |<----|  Web/App Scan    |
|  (Zig)           |     |  (Java)          |     |  (Java)          |
+------------------+     +------------------+     +------------------+
  Finds hosts and         Identifies hashes,       Finds hashes in
  services at              generates, transforms,   HTTP responses,
  100K+ packets/sec        decodes JWTs,            JWTs, configs,
                           encrypts/decrypts        and databases
```

```
+-----------+----------------------------------------------------+
| Project   | Role                                               |
+-----------+----------------------------------------------------+
| OhmZ      | Async stateless network scanner (SYN, UDP, 8 scan  |
|           | types, service detection, OS fingerprinting)       |
+-----------+----------------------------------------------------+
| AnarCry   | Bug bounty scanner (44 attack modules, AI          |
|           | assistant, CVSS scoring, knowledge base)           |
+-----------+----------------------------------------------------+
| JHasher   | Hash/crypto toolkit (identification with context,  |
|           | transform chains, JWT attacks, encryption)         |
+-----------+----------------------------------------------------+
```

---

## 07 - Building from Source

```bash
# Requirements: Java 21+, Gradle (wrapper included)

# Clone
git clone https://github.com/effront/jhasher.git
cd jhasher

# Build fat JAR (includes all dependencies)
./gradlew shadowJar

# Run
java -jar build/libs/jhasher-2.0.0-SNAPSHOT-all.jar --help

# Run tests
./gradlew test

# Build only (no fat JAR)
./gradlew build
```

---

## 08 - Ethical Use

```
+===========================================================================+
|                                                                           |
|   JHasher is built for authorized security testing, CTF competitions,     |
|   and educational purposes.                                               |
|                                                                           |
|   - Only use against systems you own or have written authorization to     |
|     test                                                                  |
|   - Respect all applicable laws and regulations                           |
|   - Do not use for unauthorized access, credential theft, or any          |
|     malicious purpose                                                     |
|   - The authors are not responsible for misuse of this tool               |
|                                                                           |
+===========================================================================+
```

---

<div align="center">

```

          ⢀⣴⣿⣿⣿⣿⣿⣶⣶⣶⣿⣿⣶⣶⣶⣶⣶⣿⡿⣿⣾⣷⣶⣶⣾⣿⠀        
        ⣠⣿⣿⢿⣿⣯⠀⢹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⣿⡇⣿⣿⣿⣿⣿⡇                                                                                                 
     ⠀⣰⣿⣿⣷⡟⠤⠟⠁⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢸⡇⣿⣿⣿⣿⣿⡇     
     ⠀⣿⣿⣿⣿⣿⣷⣶⣿⣿⡟⠁⣮⡻⣿⣿⣿⣿⣿⣿⣿⣿⢸⡇⣿⣿⣿⣿⣿⡇   
     ⠘⣿⣿⣿⣿⣿⣿⣿⣿⠏⠀⠀⣿⣿⣹⣿⣿⣿⣿⣿⣿⡿⢸⡇⣿⣿⣿⣿⣿⡇   
     ⠀⠙⢿⣿⣿⣿⡿⠟⠁⣿⣿⣶⣿⠟⢻⣿⣿⣿⣿⣿⣿⡇⣼⡇⣿⣿⣿⣿⣿⠇  
     ⠀⠀⠈⠋⠉⠁⣶⣶⣶⣿⣿⣿⣿⢀⣿⣿⣿⣿⣿⣿⣿⣇⣿⢰⣿⣿⣿⣿⣿⠀   
     ⠀⠀⠀⠀⠀⠙⠿⣿⣿⣿⡄⢀⣠⣾⣿⣿⣿⣿⣿⣿⣿⣽⣿⣼⣿⣿⣿⣿⠇⠀   
     ⠀⠀⠀⠀⠀⠀⠀⠈⠉⠒⠚⠿⠿⠿⠿⠿⠿⠿⠿⠿⠿⠛⠿⠿⠿⠿⠿⠋⠀⠀   
     ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ 
     ⠀⠀⠀⣿⣙⡆⠀⠀⡇⠀⢸ ⠀⢸⡇ ⠀⢸⡇  ⢸⣏⡉  ⠙⡏⠁⠀ 
     ⠀⠀⠀⣿⣉⡷⠀⠀⢧⣀⣼ ⠀⢸⣇⡀ ⢸⣇⡀ ⢸⣏⣁   ⡇⠀ 

```

**powered by @effront**

Built by [@effront](https://github.com/effront)

AGPL-3.0 License

</div>
