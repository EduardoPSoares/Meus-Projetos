# 🧠 MidgardBrain

**MidgardBrain** é o plugin de proxy (Velocity) responsável pelo gerenciamento de rede do servidor Midgard. Ele atua como a porta de entrada, gerenciando conexões, autenticação e redirecionamento de jogadores.

## 📋 Funcionalidades

- **Gerenciamento de Conexões:** Controle de entrada e saída de jogadores.
- **Integração com LuckPerms:** Sincronização de permissões na rede.
- **Banco de Dados:** Suporte a MySQL e SQLite para dados globais.
- **API de Mensagens:** Sistema de mensagens globais.

## 🛠️ Instalação

1. Compile o projeto:
   ```bash
   ./gradlew shadowJar
   ```
2. Copie o arquivo `.jar` gerado em `build/libs/` para a pasta `plugins/` do seu servidor Velocity.
3. Reinicie o proxy.

## ⚙️ Configuração

O plugin gera um arquivo de configuração padrão em `plugins/MidgardBrain/config.toml` (ou similar) na primeira execução.

## 📦 Dependências

- **Velocity API:** 3.3.0-SNAPSHOT
- **LuckPerms API:** 5.4
- **HikariCP:** Pool de conexões de banco de dados.
