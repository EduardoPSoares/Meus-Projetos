plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.0.0"
}

group = "me.ray"
version = "1.0-SNAPSHOT"

typewriter {
    namespace = "midgard"

    extension {
        name = "MidgardDungeon"
        shortDescription = "Sistema completo de dungeons para servidores RPG"
        description = "Um motor completo de dungeons com waves, bosses, grupos, loot e gerenciamento de salas — tudo configurável pelo painel web do Typewriter."
        engineVersion = "0.9.0-beta-171"

        paper {
            dependency("WorldEdit")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

configurations.all {
    exclude(group = "me.tofaa.entitylib", module = "spigot")
}
