# Seven Deadly Sins (Fabric, Minecraft 1.21.1, Java 21)

Craft a Sigil: 8 gold ingots around a center item.
Wrath=blaze rod, Greed=diamond, Sloth=white bed, Envy=eye of ender,
Gluttony=golden apple, Lust=rose bush, Pride=emerald block.

- Sneak + right-click a Sigil: embrace that sin (again to cast it off). One sin at a time.
- Passive buffs/debuffs apply automatically while you hold a sin.
- Right-click the matching Sigil: use the sin's ability (has a cooldown).
- /sin <wrath|greed|sloth|envy|gluttony|lust|pride|none> [player]  (op only, for testing)

## Build
This zip has no Gradle wrapper jar. Easiest: take the official Fabric example mod
(https://github.com/FabricMC/fabric-example-mod, 1.21 branch), copy its gradlew,
gradlew.bat and gradle/ folder into this folder, then run:
    ./gradlew build
The jar appears in build/libs/. Put it in your mods folder with Fabric API.
