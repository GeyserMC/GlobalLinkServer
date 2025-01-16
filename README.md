# GlobalLinkServer Plugin

> **Warning**
This repo contains the code for the Geyser Global Linking Server plugin. 
If you want to just link your account, join link.geysermc.org on Minecraft Java or Bedrock.

### `server.properties`
```properties
allow-nether=false
generate-structures=false
generator-settings={"biome"\:"minecraft\:the_void","layers"\:[{"block"\:"minecraft\:air","height"\:1}]}
level-type=minecraft\:flat
spawn-protection=200
```

### `spigot.yml`
```yaml
commands:
  send-namespaced: false
```


### `bukkit.yml`
```yaml
settings:
  allow-end: false
```

