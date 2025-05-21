# 🛡️ JHasher

### Ferramentas e utilitárias para Red Team e entusiastas de segurança ofensiva, escritas em **Java 17**. 

## ✨ Funcionalidades

| Ferramenta / Exploit          | Descrição                                                                 |
|------------------------------|---------------------------------------------------------------------------|
| 🔐 JWT Inspector              | Decodifica tokens JWT e exibe o header e payload                         |
| 🔎 Port Scanner               | Scanner multithread para detectar portas abertas (1-1024)                |
| 🧪 AES Tool                   | Ferramenta para criptografar e descriptografar usando AES                |
| 🛠️ HTTP Brute Forcer         | Força bruta de login HTTP com credenciais conhecidas                     |
| 🧬 Hash Identifier            | Identifica possíveis algoritmos com base no padrão do hash               |
| ⚠️ Shiro RememberMe Exploit   | Estrutura básica para exploração do rememberMe do Apache Shiro           |
| ⚠️ XXE Detector               | Testa vulnerabilidades XXE em parsing XML                                |
| 🔍 Subdomain Scanner          | Busca subdomínios comuns para um domínio alvo                            |
| 🕵️ Directory Bruteforcer     | Testa diretórios comuns via HTTP para encontrar pontos acessíveis        |
| ⚠️ SSRF Tester                | Testa endpoints vulneráveis a SSRF com payloads internos conhecidos      |

## 🧰 Requisitos

- Java 17 ou superior  
- Maven 3 ou superior  

## ⚙️ Build

```bash
mvn clean package
````

## 🚀 Execução

Exemplo de execução das ferramentas:

```bash
java -cp target/classes tools.jwt.JwtInspector
java -cp target/classes tools.scan.PortScanner
java -cp target/classes tools.crypto.AESTool
java -cp target/classes tools.http.HttpBruteForcer
java -cp target/classes tools.hash.HashIdentifier
java -cp target/classes exploits.shiro.ShiroRememberMeExploit
java -cp target/classes exploits.xxe.XXEDetector
java -cp target/classes scanner.SubdomainScanner dominio.com
java -cp target/classes scanner.DirBrute http://alvo.com/
java -cp target/classes exploits.ssrf.SSRFTester http://vulneravel.com/fetch?url=
```


## 📜 Licença

Código aberto para fins educacionais e de segurança ofensiva. **Use com responsabilidade.**

## Autor 


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
         ⠀⠀⠀⣿⣙⡆⠀⠀⡇⠀⢸⠀⠀⢸⠀⠀ ⢸⡇⠀⠀⢸⣏⡉  ⠙⡏⠁⠀ 
         ⠀⠀⠀⣿⣉⡷⠀⠀⢧⣀⣼ ⠀⢸⣀  ⢸⣇⡀ ⢸⣏⣁⠀ ⠀⡇⠀ 