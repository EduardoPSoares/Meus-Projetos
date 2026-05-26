---
description: Regras obrigatórias do projeto MidgardRPG. Carregar sempre que trabalhar com qualquer arquivo do workspace.
applyTo: '**/*.{java,yml,yaml,xml,properties,json,md}'
---

# MidgardRPG — Regras do Projeto

## Contexto do Projeto

- **Tipo:** Projeto privado, uso exclusivo do autor. Não precisa de compatibilidade pública, APIs abertas para terceiros nem documentação para usuários externos.
- **Plataforma:** Plugin para Minecraft **Folia** (fork do Paper focado em multithreading por região).
- **Versão fixa:** Minecraft **1.21.11** — não há necessidade de suporte multi-versão nem retrocompatibilidade.
- **Java:** 21 (features modernas: records, sealed classes, pattern matching, virtual threads quando aplicável).
- **Build:** Maven multi-módulo.

---

## Princípios de Código

### Qualidade e Clareza
- Código **limpo, legível e autoexplicativo**. Nomes de classes, métodos e variáveis devem comunicar intenção sem precisar de comentários.
- Comentários só quando a lógica não for óbvia. Nunca comentários redundantes como `// retorna o nome` acima de `getName()`.
- Métodos curtos e com responsabilidade única. Se um método faz duas coisas, dividir em dois.
- Sem código morto, TODOs esquecidos, ou imports não utilizados.

### Modularidade
- Respeitar a arquitetura de módulos existente: cada módulo (`RPGModule`) cuida do seu domínio.
- Dependências entre módulos devem passar pelo `midgard-core` (interfaces, eventos, registries). Módulos nunca devem depender diretamente uns dos outros.
- Cada classe tem uma única responsabilidade. Se crescer demais, extrair em classes auxiliares no mesmo pacote.

### Sem Espaguetização
- Sem dependências circulares entre pacotes ou módulos.
- Sem lógica de negócio em listeners — listeners delegam para managers/services.
- Sem god classes. Se uma classe tem mais de ~300 linhas, provavelmente precisa ser dividida.
- Fluxo de dados claro: entrada → processamento → saída. Evitar side-effects escondidos.

### Configurabilidade
- Valores numéricos de gameplay (dano, cooldowns, durações, taxas, fórmulas) devem ser configuráveis via YAML, nunca hardcoded.
- Usar o sistema de `ConfigWrapper` do core para carregar configurações.
- Features opcionais devem ter toggle on/off na config.

---

## Performance

- **Folia-first:** Toda operação que toca o mundo (blocos, entidades, inventários) deve usar o scheduler region-aware do `Task.java`. Nunca usar `Bukkit.getScheduler()` diretamente.
- **Thread-safety:** Usar `ConcurrentHashMap`, `volatile`, ou estruturas thread-safe para dados compartilhados. Nunca acessar coleções mutáveis de threads diferentes sem sincronização.
- **Evitar alocações desnecessárias:** Reutilizar objetos quando possível (pools, caches). Evitar criar objetos novos em hot-paths (ticks, handlers de dano).
- **Operações de IO (banco, Redis, arquivos) sempre assíncronas.** Nunca bloquear a main thread ou region threads.
- **Cache com invalidação:** Usar Caffeine ou cache manual com dirty-flag ao invés de recomputar valores caros toda vez.
- **Lazy initialization** onde fizer sentido — não carregar dados que podem nunca ser usados.
- **Evitar reflection em hot-paths.** Reflection é aceitável na inicialização, nunca em loops de tick/combate.

---

## Convenções de Código

### Nomenclatura
- **Pacotes:** `me.ray.midgard.modules.<modulo>.<subpacote>` ou `me.ray.midgard.core.<subpacote>`
- **Classes:** PascalCase, substantivos (`DamageCalculator`, `ForgeSession`)
- **Métodos:** camelCase, verbos (`calculateDamage()`, `applyModifier()`)
- **Constantes:** UPPER_SNAKE_CASE (`MAX_LEVEL`, `DEFAULT_MANA_REGEN`)
- **Configs YAML:** kebab-case para chaves (`max-health`, `mana-regen-rate`)

### Padrões Obrigatórios
- **Módulos:** Sempre estender `RPGModule` com a prioridade correta.
- **Dados de jogador:** Implementar `ModuleData` e armazenar via `MidgardProfile`.
- **Persistência de definições (raças, classes, spells, items):** Usar `DefinitionRepository`.
- **Sync multi-servidor:** Usar `DefinitionSyncManager` (Redis ou polling).
- **Comandos:** Estender `MidgardCommand` e registrar via `CommandManager` ou `AdminCommandRegistry`.
- **Mensagens:** Usar `LanguageManager` com chaves i18n. Nunca hardcodar strings de UI.
- **Scheduling:** Usar `Task.sync()`, `Task.async()`, etc. Nunca `new BukkitRunnable()`.

### Estilo
- **Sempre usar chaves** em blocos if/else/for, mesmo com uma única linha.
- **Early return** ao invés de blocos if-else aninhados profundamente.
- **Imutabilidade por padrão:** Preferir campos `final`, records para DTOs, coleções imutáveis (`List.of()`, `Map.of()`, `Collections.unmodifiable*()`).
- **Sem `null` em APIs públicas:** Usar `Optional<T>` para retornos que podem não ter valor. Internamente, null é aceitável quando o contexto é claro.

---

## Error Handling e Debugabilidade

### Try-Catch Estratégico
- **Todo ponto de entrada** deve ter try-catch: listeners, comandos, tasks, callbacks de banco/Redis.
- Dentro de um listener, SEMPRE envolver o corpo em try-catch para que um erro não quebre o evento inteiro:
  ```java
  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
      try {
          // lógica
      } catch (Exception e) {
          MidgardLogger.error("Erro ao processar dano", e);
      }
  }
  ```
- **Tasks (sync/async):** Envolver o corpo da task em try-catch. Uma exception não capturada em task mata a task silenciosamente.
- **Operações de IO (banco, Redis, arquivos):** Sempre try-catch com log do erro E do contexto (qual jogador, qual operação, quais dados).
- **Parsing de config/YAML:** Validar valores e capturar `NumberFormatException`, `IllegalArgumentException`, etc. Usar valores default quando o parse falhar, logando um warning.
- **Interações com APIs externas** (MythicMobs, Vault, WorldGuard, Nexo): Sempre try-catch pois plugins externos podem lançar exceptions inesperadas.

### Logging de Erros
- Sempre logar via `MidgardLogger.error(mensagem, exception)` — nunca engolir exceptions silenciosamente.
- Incluir **contexto** na mensagem: qual jogador, qual módulo, qual operação falhou.
- Usar `MidgardLogger.warn()` para situações recuperáveis (config inválida com fallback, dados faltantes).
- Usar `MidgardLogger.debug()` para informações de diagnóstico que só aparecem com debug ativo.
- Nunca logar stack traces com `e.printStackTrace()` — sempre `MidgardLogger.error("msg", e)`.

### Debug Mode
- Cada módulo deve respeitar o debug mode global para logar informações extras quando ativado.
- Logs de debug devem ser úteis: tempos de execução, valores de variáveis importantes, fluxo de decisões.
- Nunca deixar logs de debug hardcoded em nivel INFO/WARN — usar condicionais ou `MidgardLogger.debug()`.

### Validação Defensiva
- Validar parâmetros em métodos públicos: null checks, range checks, estado esperado.
- Usar `Objects.requireNonNull()` para parâmetros que nunca devem ser null.
- Em métodos que recebem `Player`, verificar `player.isOnline()` antes de operar, pois o jogador pode ter saído entre o scheduling e a execução.
- Ao buscar dados do `MidgardProfile`, sempre verificar se o perfil existe antes de acessar `ModuleData`.

---

## Gestão de Recursos

- **Try-with-resources** para tudo que implementa `AutoCloseable`: conexões de banco, streams, readers.
- Nunca deixar conexões de banco abertas — usar o `DatabaseManager.executeAsync()` que já gerencia o pool.
- `JedisPool` — sempre pegar e devolver o recurso via try-with-resources ou o wrapper do `RedisManager`.
- Ao registrar listeners ou tasks no `onEnable()`, sempre desregistrar/cancelar no `onDisable()`.
- Limpar caches e coleções temporárias no `onDisable()` para evitar memory leaks entre reloads.

---

## Tratamento de Dados

### Serialização
- Usar GSON para serialização de perfis (padrão do projeto).
- Ao deserializar, sempre tratar dados corrompidos/incompletos com valores default ao invés de crashar.
- Versionar o formato de dados quando mudar a estrutura — manter backward compatibility na leitura.

### Configurações YAML
- Sempre fornecer um `config.yml` padrão dentro do JAR (`resources/`).
- Validar tipos ao carregar: se espera int e recebe string, logar warning e usar default.
- Documentar as chaves YAML com comentários no arquivo padrão para referência do autor.
- Chaves que controlam features devem ter padrão seguro (toggle off por default para features experimentais).

---

## Boas Práticas Gerais

### Encapsulamento
- Visibilidade mínima: tudo `private` por padrão, `package-private` para classes auxiliares no mesmo pacote, `public` só para API real do módulo.
- Getters sem setters quando possível — preferir imutabilidade.
- Coleções retornadas de métodos públicos devem ser **imutáveis** ou **cópias defensivas**.

### Records e Sealed Classes (Java 21)
- Usar `record` para DTOs, configs carregadas, eventos de dados e qualquer objeto de valor imutável.
- Usar `sealed` para hierarquias fechadas onde os subtipos são conhecidos (tipos de dano, tipos de spell, etc.).
- Usar pattern matching (`instanceof Pattern`, `switch` expressions) onde simplificar o código.

### Enums
- Preferir enums para conjuntos fixos de opções (tipos de dano, prioridades, triggers).
- Enums podem ter métodos e campos — usar ao invés de criar maps de string → comportamento.
- Fornecer método `fromString(String)` que retorna `Optional<T>` ao invés de lançar exception.

### Streams e Coleções
- Usar streams para transformações e filtros — mas NÃO em hot-paths de combate/tick onde um for-loop é mais performático.
- Nunca usar streams paralelos — o scheduling do Folia já é multithreaded.
- Preferir `Map.computeIfAbsent()` ao invés de check-then-put para evitar race conditions.

### Testes
- Código novo deve vir com testes unitários quando a lógica é complexa (fórmulas de dano, parsing de config, cálculos de atributos).
- Usar JUnit 5 e Mockito (já configurados no projeto).
- Testes devem ser independentes entre si — sem estado compartilhado.
- Nomear testes descritivamente: `shouldApplyArmorReduction_whenPhysicalDamage()`.

---

## Regras Específicas Folia

- Nunca assumir que código roda na main thread. Folia não tem main thread global.
- Usar `Task.syncEntity(entity, runnable)` para operar em entidades específicas.
- Usar `Task.syncLocation(location, runnable)` para operar em blocos/regiões.
- Nunca iterar sobre `Bukkit.getOnlinePlayers()` e modificar dados de jogadores sem garantir que está na region thread correta.
- Eventos do Bukkit já são disparados na region thread correta — listeners são seguros, mas qualquer operação em OUTRA região precisa de re-scheduling.

---

## O que NÃO Fazer

- Não adicionar dependências externas sem necessidade real. O projeto já tem as libs necessárias.
- Não criar abstrações prematuras para "flexibilidade futura". Resolver o problema atual.
- Não criar utils genéricos para operações usadas uma ou duas vezes.
- Não usar Lombok, Kotlin, ou qualquer processador de anotações extra.
- Não usar `Bukkit.getScheduler()` — sempre `Task.*`.
- Não usar `System.out.println()` — sempre `MidgardLogger` ou `ConsoleUtils`.
- Não criar arquivos de documentação extras sem que sejam solicitados.
- Não engolir exceptions com catch vazio. Sempre logar.
- Não usar `e.printStackTrace()` — usar `MidgardLogger.error("contexto", e)`.
- Não retornar `null` em métodos públicos sem documentar — preferir `Optional<T>`.
- Não ignorar warnings do compilador — resolver ou suprimir com justificativa.