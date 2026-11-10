# AudioZen - Gestor de Perfiles de Audio, Widgets y Automatización

Aplicación Android nativa moderna, ligera y de alto rendimiento en Jetpack Compose para gestionar perfiles de sonido (Normal, Vibración, Silencio y No Molestar), con widgets interactivos de 1 toque para la pantalla de inicio, automatización por horario y geovallas de bajo consumo, y sincronización/respaldo seguro sin servidores.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> A continuación se resumen las decisiones confirmadas y los estándares de arquitectura definidos para garantizar el mínimo consumo de batería y la máxima fluidez:

- **Sincronización y Respaldo Seguro (Confirmado)**: Sistema de exportación e importación local cifrado en formato JSON (con hash de integridad y protección opcional de clave), garantizando funcionamiento 100% offline, privacidad absoluta y cero consumo de recursos en segundo plano.
- **Detección de Ubicación Eficiente (Confirmado)**: Automatización mediante Geovallas (`Geofence` API con Play Services Location) que se gestionan a nivel del sistema operativo. No se ejecutan servicios continuos de GPS en segundo plano; el sistema solo despierta la app cuando se cruza la frontera de la zona definida (ej. Oficina, Casa, Gimnasio).
- **Widgets de Pantalla de Inicio (Confirmado)**: Implementación de dos widgets independientes:
  1. *Widget Barra 4x1*: Muestra el perfil activo y 4 botones de acceso directo para cambiar inmediatamente con un toque.
  2. *Widget Compacto 2x1*: Selector cíclico táctil que cambia entre Normal → Vibración → Silencio → No Molestar con indicador visual de estado.
- **Permisos y Políticas de Sonido de Android**:
  - `AudioManager` para alternar modo Normal y Vibración (`RINGER_MODE_NORMAL`, `RINGER_MODE_VIBRATE`).
  - Para Silencio Total / No Molestar (`RINGER_MODE_SILENT` o InterruptionFilter Priority/None), Android requiere `ACCESS_NOTIFICATION_POLICY`. La app incluye una tarjeta instructiva intuitiva que guía al usuario a conceder el permiso en los Ajustes del sistema de forma transparente.

---

### 1. Overview & Core Concept

**AudioZen** permite a los usuarios tomar el control total del sonido de su smartphone con transiciones instantáneas y sin distracciones. Ofrece:
1. **Control Inmediato**: Cambio rápido entre Normal (timbre + vibración), Solo Vibración, Silencio y No Molestar (DND total).
2. **Widgets de Inicio**: Cambio de perfil en 1 toque directo desde el escritorio sin necesidad de abrir la aplicación.
3. **Automatización Inteligente**:
   - *Por Horario*: Reglas temporales por días de la semana y horas (ej. Noches de 23:00 a 07:00 en Silencio, Trabajo de Lun-Vie de 09:00 a 18:00 en Vibración).
   - *Por Ubicación*: Zonas geográficas (radio personalizable) que activan perfiles al entrar o salir.
4. **Eficiencia Extrema & Modo Oscuro**: Arquitectura reactiva con Kotlin Coroutines y Room, animaciones spring fluidas (menos de 250ms), adaptación automática al tema oscuro/claro del sistema y nulo impacto en la batería.

---

### 2. User Experience & Visual Design

#### Dirección Estética y Paleta
- **Tema**: *Midnight Slate & Sonic Teal* — Minimalismo nórdico con superficies oscuras OLED profundas (`#0F141A`), tarjetas tonales en azul grisáceo (`#1A222C`) y acentos en verde azulado brillante (`#2DD4BF` / `#0D9488`) para estados activos. En modo claro, utiliza blancos cerámicos limpios (`#F8FAFC`) con grises neutros suaves y acentos cian.
- **Tipografía**: Jerarquía Material 3 limpia, con números grandes y claros para temporizadores/volúmenes y etiquetas con peso `FontWeight.SemiBold`.
- **Micro-interacciones y Haptics**: Respuesta háptica táctil al pulsar perfiles o botones del widget. Transición animada entre estados con `AnimatedContent` y elevación tonal sutil.

#### Pantallas Principales
1. **Pantalla Principal (Dashboard)**:
   - Tarjeta heroica con el estado actual del dispositivo (icono grande, etiqueta, niveles de volumen para llamadas, notificaciones y multimedia).
   - Selector cuadrante de perfiles rápidos (Normal, Vibración, Silencio, No Molestar).
   - Temporizador de silencio rápido ("Silenciar durante 30m, 1h, 2h o hasta mañana").
   - Banner informativo de permisos (solo si falta el permiso de política DND).
2. **Pantalla de Automatizaciones (Reglas)**:
   - Pestañas entre "Horarios" y "Ubicaciones".
   - Lista interactiva de reglas con switches para activar/desactivar al instante.
   - Diálogos modernos para crear/editar reglas horarias (selector de días de la semana, hora inicio/fin, perfil deseado).
   - Creador de zonas de ubicación (coordenadas, nombre de lugar, radio en metros, perfil al entrar/salir).
3. **Pantalla de Ajustes y Sincronización**:
   - Ajustes de volumen por perfil predeterminado.
   - Sistema de exportación e importación JSON con clave de seguridad y firma de integridad.
   - Historial de cambios automáticos recientes (registro ligero de auditoría).
   - Guía visual para añadir los dos tipos de widgets a la pantalla de inicio.

---

### 3. Key Product Decisions & Trade-Offs

- **Room Persistence vs Cloud**:
  - *Decisión*: Room local con exportación/importación JSON cifrada.
  - *Razón*: Se alinea exactamente con la respuesta del usuario (privacidad, compatibilidad 100% offline, cero consumo de datos o cuentas de usuario).
- **AlarmManager Exacto vs WorkManager**:
  - *Decisión*: `AlarmManager` con `setExactAndAllowWhileIdle` y `BroadcastReceiver` ligero para los cambios de hora programados.
  - *Razón*: Máxima precisión al minuto sin mantener un proceso en segundo plano activo.
- **Geofencing API de Google Play Services**:
  - *Decisión*: `GeofencingClient` del sistema en lugar de un servicio en primer plano con GPS constante.
  - *Razón*: El sistema operativo agrupa las comprobaciones de ubicación con torres de telefonía y redes Wi-Fi, garantizando un consumo prácticamente imperceptible de batería (<0.5% al día).
- **Widgets con RemoteViews Estándar**:
  - *Decisión*: `AppWidgetProvider` con `RemoteViews` y diseño adaptable.
  - *Razón*: Compatibilidad universal en cualquier launcher de Android, respuesta instantánea al toque con `PendingIntent` directo al `BroadcastReceiver` de audio sin latencias.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       UI LAYER (M3)                         │
│  MainActivity ─► AudioZenNavHost                            │
│  ├─ DashboardScreen (Quick Profiles, Volume Sliders, Timer) │
│  ├─ SchedulesScreen (Time-based triggers)                   │
│  ├─ LocationsScreen (Geofence zones)                        │
│  └─ SettingsScreen (Export/Import Sync, DND Permission)     │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes Flow / StateFlow
┌──────────────────────────────▼──────────────────────────────┐
│                    VIEWMODEL & REPOSITORY                   │
│  AudioViewModel ◄───► AudioRepository                       │
│  ├─ AudioProfileManager (AudioManager + NotificationManager)│
│  └─ SyncManager (Encrypted JSON Export/Import)              │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                   PERSISTENCE LAYER (Room)                  │
│  AppDatabase ──► ScheduleRuleDao, GeofenceZoneDao, LogDao    │
└─────────────────────────────────────────────────────────────┘
                               ▲
┌──────────────────────────────┴──────────────────────────────┐
│                  BACKGROUND & OS INTEGRATION                │
│  ├─ AudioWidgetProvider (4x1 Quick Bar & 2x1 Cycle Toggle)  │
│  ├─ ScheduleReceiver (Triggered via AlarmManager)           │
│  └─ GeofenceBroadcastReceiver (Play Services Geofence event)│
└─────────────────────────────────────────────────────────────┘
```

#### Entidades de Base de Datos
- `AudioProfile`: Modo de timbre (`NORMAL`, `VIBRATE`, `SILENT`, `DND`), volúmenes asociados.
- `ScheduleRule`: Id, nombre, días de la semana activos (bitmask/set), hora inicio, hora fin, perfil asignado, habilitado.
- `GeofenceZone`: Id, nombre, latitud, longitud, radio (metros), perfil al entrar, perfil al salir, habilitado.
- `AudioLog`: Registro temporal de los últimos cambios de perfil (origen: manual, widget, horario, ubicación).

#### Mapeo de Acciones del Widget
- Al pulsar un botón en el widget, un `PendingIntent` lanza `AudioActionReceiver`, que invoca `AudioProfileManager`, actualiza el modo en el hardware del teléfono y notifica a `AppWidgetManager` para refrescar los estados visuales inmediatamente.

---

### Verification & Testing Plan

1. **Compilación y Construcción**:
   - Ejecutar `compile_applet` tras implementar el código para verificar la compatibilidad de Kotlin DSL, dependencias y KSP con Room.
2. **Pruebas de Componentes de Audio y Permisos**:
   - Comprobar la respuesta al cambio entre modo Normal, Vibración y Silencio/DND.
   - Validar que si no se dispone del permiso DND, la app degrade graciosamente a modo Silencio/Vibración sin crash y muestre el botón para activarlo.
3. **Pruebas de Widgets y Receivers**:
   - Verificar la definición en `AndroidManifest.xml` de los receptores `AudioWidgetProvider`, `ScheduleReceiver`, `GeofenceBroadcastReceiver`.
   - Validar las vistas XML de `RemoteViews` para asegurar soporte en diferentes resoluciones y temas oscuros.
4. **Pruebas de Exportación/Importación**:
   - Validar que el archivo JSON de configuración se genera con checksum, se importa correctamente y actualiza las reglas en Room.
