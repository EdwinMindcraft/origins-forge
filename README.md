# Origins (Forge)

This is the repository that is used to build Origins Forge.

## Building

To build this repository, first clone it, preferably with the `--recurse-submodules` flag.

Then run `gradlew build` in the root directory. The build output is the `unified` jar file.

## Creating addons

### 1.5.0.2
Origins Forge now uses the MerchantPug maven to host its artifacts. To use this, add the following to your gradle build script:
```gradle
repositories {
    ...
    maven {
        url "https://maven.merchantpug.net"
    }
}

dependencies {
    ...
    implementation fg.deobf("io.github.edwinmindcraft:calio-forge:${calio_forge_version}")
    implementation fg.deobf("io.github.edwinmindcraft:apoli-forge:${apoli_forge_version}")
    implementation fg.deobf("io.github.edwinmindcraft:origins-forge:${origins_forge_version}")
}
```

You can find each individual version by looking at the [Reposilite maven page](https://maven.merchantpug.net/#/releases/io/github/edwinmindcraft).
Alternatively, you can look at the released JARs on the GitHub releases page of [EdwinMindcraft/origins-architectury](https://github.com/EdwinMindcraft/origins-architectury).

### Versions prior to 1.7.1.1
The simplest way to load Origins in a dev environment is currently to use
[Curse Maven](https://www.cursemaven.com/). To do so, add the flowing to your
gradle build script:
```gradle
repositories {
    ...
    maven {
        url "https://cursemaven.com"
        content {
            includeGroup "curse.maven"
        }
    }
}

dependencies {
    ...
    implementation fg.deobf("curse.maven:origins-474438:<fileid>")
}
```

You can find file ids on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/origins-forge/files).

### Backup Mavens
Viable alternatives if one of these two methods don't work are the [Modrinth Maven](https://docs.modrinth.com/docs/tutorials/maven/) or [JitPack](https://jitpack.io/#EdwinMindcraft/origins-forge) (using a commit hash).

### Changes from fabric
Apoli for forge is partial rewrite of the fabric version, as such many compatiblity features
are currently missing. Furthermore, due to a mistake during the porting process, powers for
forge take Entities as argument instead of LivingEntities.

To register a power on forge, you'll need to use the regular forge process, either
with `RegistryEvent.Register` or `DeferredRegister`.

The registry types apoli forge uses are all defined in the package `io.github.edwinmindcraft.apoli.api.power.factory`.
If you want to use `DeferredRegister`, as classes are generics, consider using types
provided in `ApoliRegistries`.

While defining a power, action or condition the same way as fabric is possible,
if you are developing a pure forge addon, I would recommend using Mojang's `Codec`
system as well as `records` for static data storage as the system currently has builtin
support for error logging on those.

The system currently implemented is similar to the vanilla's Feature/ConfiguredFeature system, which
means most data will not change from one entity to another. If you do need to change data internally,
forge provides `ConfiguredPower.getPowerData` that will store any **mutable** data structure on the entity.

Other important changes:
* `ActionFactory` and `ConditionFactory` were split for each given subtype.
* When fabric used `Predicate` or `Consumer`, you'll need to use the matching `ConfiguredCondition` or `ConfiguredAction`
* Codecs are provided for most types, either in the class itself, or in the same places
as fabric `SerializableDataTypes` and `ApoliDataTypes`.
* Prefer using `CalioCodecHelper.optionalField` over `Codec.optionalFieldOf` 
  since those field will properly handle error logging.
* Prefer using `CalioCodecHelper.listOf` over `Codec.listOf` since those lists
  support the calio format of list.
* `SerializableDataTypes` can be used as `Codecs` of the same type.
* `SerializableData` can be used a `MapCodecs` of the same type.
* `PowerFactories` can by default be conditioned.

### Defining a new content

To define a new power, action or condition, you'll need a configuration which
implements `IDynamicFeatureConfiguration`. While this class doesn't have any
abstract methods, you can still override the methods provided to display additional
information in the logs for people defining datapacks using your content.