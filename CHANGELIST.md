# Changelist:

## Calio

* Renamed `SerializableDataTypes.VANILLA_INGREDIENT` to `INGREDIENT_NONEMPTY`: That's what it always was.
* Removed `SerializableDataTypes.EITHER_INGREDIENTS`: No longer necessary as neo redefines `CODEC` and `CODEC_NONEMPTY`.
* Marked `SerializableDataTypes.INGREDIENT_ENTRY` and `INGREDIENT_ENTRIES` as deprecated: Use `INGREDIENT` OR `INGREDIENT_NONEMPTY` instead.
* `SerializableDataTypes.RECIPE` now supports serialization.
* Remove `CriteriaRegistryInvoker`: It was supposed to be a legacy method, and with 1.20.4 the calling convention changed, meaning it no longer needs to exist.
* Renamed `IAbilityHolder` to `AbilityHolder`: Following the interface naming convention used by Neo and Mojang.
* Removed `ApoliRegistries.codec()`: Fully replaced by Registry.byNameCodec.
* `AbilityHolder.get` returns a nullable instance of `AbilityHolder`: Inline with the new capability system.

## Apoli

* Registries are no longer hidden behind a supplier
* All occurrences of `Supplier<@NotNull T>` have been replaced by `Supplier<@NotNull T>` or `Supplier<T>` when the former isn't applicable.
* Removed `ModifierOperation.STRICT_CODEC`: Behaviour is handled in `CODEC` using registry aliasing.
* Renamed `IPowerContainer` to `PowerContainer`: Following the interface naming convention used by Neo and Mojang.
* Renamed `PowerContainer` to `PowerContainerImpl`: Following the interface naming convention used by Neo and Mojang.
* Removed `IPowerDataCache`: Replaced by `ApoliAttachments.DAMAGE_CACHE`
* `PowerContainer.get` returns a nullable instance of `PowerContainer`