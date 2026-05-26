# MidgardCommands Module

## Descrição

O módulo `midgard-commands` é responsável por centralizar o gerenciamento de todos os comandos do MidgardRPG. Ele resolve problemas comuns como:

- Comandos mostrados no `/help` que não existem
- Comandos registrados sem documentação
- Conflitos de aliases entre módulos
- Falta de consistência no sistema de permissões

## Funcionalidades

### 1. Registro Centralizado de Comandos

Todos os comandos do projeto podem ser registrados através do `CentralCommandRegistry`, que mantém controle de:
- Metadados completos de cada comando
- Módulo de origem
- Categoria (PLAYER, MODERATOR, ADMIN)
- Estado de habilitação
- Conflitos e duplicações

### 2. Validação de Comandos

O `CommandValidator` verifica a integridade do sistema de comandos:
- Detecta comandos no help que não existem
- Detecta comandos sem documentação
- Verifica padrões de permissão
- Identifica conflitos de aliases
- Localiza comandos "órfãos"

### 3. Comandos Administrativos

O módulo adiciona o subcomando `/rpg admin commands` com as seguintes opções:

```
/rpg admin commands list [módulo]   - Lista todos os comandos
/rpg admin commands info <comando>  - Informações detalhadas de um comando
/rpg admin commands validate        - Valida a consistência do sistema
/rpg admin commands modules         - Lista módulos com comandos registrados
/rpg admin commands search <termo>  - Busca comandos por nome/descrição
```

## Uso em Outros Módulos

### Registrar um Comando

```java
import me.ray.midgard.modules.commands.registry.CommandRegistrationHelper;

// Registrar comando de jogador
CommandRegistrationHelper.registerPlayerCommand(myCommand, "mymodule");

// Registrar comando admin
CommandRegistrationHelper.registerAdminCommand(myCommand, "mymodule");

// Registrar comando de moderador
CommandRegistrationHelper.registerModeratorCommand(myCommand, "mymodule");

// Registrar comando standalone (fora do /rpg)
CommandRegistrationHelper.registerStandaloneCommand(myCommand, "mymodule");
```

### Verificar se um Comando Existe

```java
boolean exists = CommandRegistrationHelper.isRegistered("mycommand");
```

### Obter Informações de um Comando

```java
CommandDescriptor desc = CommandRegistrationHelper.getDescriptor("mycommand");
if (desc != null) {
    System.out.println("Módulo: " + desc.getModule());
    System.out.println("Permissão: " + desc.getPermission());
}
```

## Configuração

O módulo pode ser configurado através do arquivo `modules/commands/config.yml`:

```yaml
# Habilitar validação automática na inicialização
auto-validate: true

# Habilitar logging detalhado de registro de comandos
verbose-logging: false

# Habilitar detecção de comandos duplicados
detect-duplicates: true

# Habilitar sincronização automática com o sistema de help
sync-help-system: true

# Comandos desabilitados
disabled-commands: []
```

## Permissões

| Permissão | Descrição |
|-----------|-----------|
| `midgard.admin.commands` | Acesso ao `/rpg admin commands` |

## Estrutura de Arquivos

```
midgard-commands/
├── src/main/java/me/ray/midgard/modules/commands/
│   ├── CommandsModule.java              # Módulo principal
│   ├── admin/
│   │   └── CommandsAdminCommand.java    # Comando /rpg admin commands
│   ├── registry/
│   │   ├── CentralCommandRegistry.java  # Registro centralizado
│   │   ├── CommandDescriptor.java       # Metadados de comando
│   │   └── CommandRegistrationHelper.java # Utilitário de registro
│   └── validator/
│       └── CommandValidator.java        # Validador de comandos
└── src/main/resources/
    └── modules/commands/
        └── config.yml                   # Configuração
```

## Changelog

### v1.0.0
- Implementação inicial do módulo
- Registro centralizado de comandos
- Validação de consistência
- Comando administrativo `/rpg admin commands`
