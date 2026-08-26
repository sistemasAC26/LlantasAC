# Plan para diagnosticar por qué no se ven unidades en el móvil

El problema principal es que el código actual falla silenciosamente si hay un error en Firestore y utiliza un proveedor de depuración de App Check que requiere configuración adicional en dispositivos físicos.

## Cambios Propuestos

### Componente: Diagnóstico de Firestore

#### [MODIFY] [BandejaActivity.java](file:///C:/Users/mario/AndroidStudioProjects/LlantasAC/app/src/main/java/com/example/llantasac/BandejaActivity.java)
- Agregar un `Toast` y un `Log` para capturar errores en el `addSnapshotListener`.
- Agregar un `Log` para ver cuántos documentos se están recibiendo antes de los filtros.

## Plan de Verificación

### Verificación Manual
1. Ejecutar la aplicación en el dispositivo móvil.
2. Observar si aparece un `Toast` con un mensaje de error (ej. "Permission Denied" o "App Check fails").
3. Revisar el Logcat en Android Studio filtrando por "FIRESTORE".
