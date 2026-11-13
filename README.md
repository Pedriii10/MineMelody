# MineMelody (Fabric · Minecraft 26.3)

<img width="1250" height="1250" alt="MineMelody-Logo" src="https://github.com/user-attachments/assets/1142936d-0ff6-4875-9544-534602cc0c63" />



**Crea tu propia playlist con la música de Minecraft.** Se acabaron los 20 minutos de silencio entre canciones.

- **Botón en Opciones → Música y sonidos → "Playlist de música..."**, o tecla **M** dentro del juego.
- Modos: en orden, aleatorio (sin repetir hasta pasar por todas) y repetir una.
- Pausa configurable entre canciones (0 s a 60 s).
- **Discos en survival**: sólo suenan si los tuviste en el inventario en ese mundo. Se puede desactivar.
- Buscador (sin importar mayúsculas ni tildes) y favoritos ★.
- Exportar/importar la playlist como texto para compartirla.
- Banner de "Sonando ahora" en la esquina que elijas, y botón para pre-escuchar cualquier canción.
- **Varias playlists con nombre** ("Para minar", "Para construir"...), y cada una puede atarse a una dimensión: al entrar al Nether suena tu playlist del Nether.
- Banner de "Sonando ahora" configurable: nunca, al empezar cada canción o siempre visible con barra de progreso; en la esquina y del tamaño que quieras (50%-200%), con vista previa.
- Duración de cada canción y barra de progreso (leídas del propio archivo .ogg).
- Teclas opcionales (sin asignar por defecto: Opciones → Controles → MineMelody): siguiente canción, siguiente playlist, pausar/reanudar, subir/bajar volumen.
- 100% cliente: funciona aunque el servidor no tenga el mod.

## Requisitos para desarrollar

1. **JDK 25** (Minecraft 26.x lo exige). Por ejemplo, Temurin 25: https://adoptium.net
2. **IntelliJ IDEA Community** con el plugin *Minecraft Development* (opcional, pero ayuda muchísimo con los mixins).

## Cómo ejecutarlo

```bash
./gradlew runClient     # abre Minecraft con el mod cargado
./gradlew test          # tests de la lógica de la playlist (no abre el juego)
./gradlew build         # genera el .jar en build/libs/
```

El `.jar` de `build/libs/` (el que NO termina en `-sources`) se copia a `.minecraft/mods/`, junto con Fabric Loader y Fabric API.

La primera vez Gradle descarga Minecraft y lo prepara: tarda varios minutos, es normal.

## Cómo funciona por dentro

Minecraft tiene un "DJ", el `MusicManager`. Cada tick:

1. pregunta qué música corresponde (`getSituationalMusic`: bioma, Nether, bajo el agua...),
2. si la canción terminó, espera `nextSongDelay` ticks (¡minutos!),
3. llama a `startPlaying(music)`, y el `SoundManager` elige **una canción al azar** de ese evento.

El mod se mete con **Mixins** en tres puntos:

| Dónde | Qué hace |
|---|---|
| `startPlaying` (HEAD, cancelable) | En vez de una al azar, reproduce la siguiente de tu playlist |
| `tick` (HEAD) | Cuando termina una canción, espera tu pausa en vez de minutos |
| `getCurrentMusicTranslationKey` | Para que el aviso "Sonando ahora" muestre bien el nombre de los discos |

### Estructura

```
com.pedro.minemelody
├── core/        Lógica pura, SIN Minecraft → testeable con JUnit
│   ├── PlaylistQueue    decide la siguiente canción (orden / aleatorio / repetir)
│   └── PlaybackMode
├── config/      PlaylistConfig → config/minemelody.json
├── track/       Track, TrackGroup, TrackCatalog (descubre todas las canciones cargadas)
├── unlock/      DiscUnlockTracker (regla de discos en survival)
├── playback/    PlaylistPlayer (une todo) + PlaylistSoundInstance (fuerza una canción concreta)
├── gui/         PlaylistScreen + TrackListWidget
└── mixin/       los puntos de enganche con el código de Minecraft
```

La regla de oro: **cuanta menos lógica viva dentro de los mixins, mejor.** Los mixins son frágiles (si Mojang
cambia un método, se rompen), así que sólo "enganchan" y delegan en clases normales.

## Ideas para seguir aprendiendo

- Publicarlo en Modrinth (licencia, changelog, capturas).
- Seek: saltar a un punto de la canción haciendo clic en la barra de progreso.
- Más formatos de config compartibles (modo, pausa...) en el texto exportado.
