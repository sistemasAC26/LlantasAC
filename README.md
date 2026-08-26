# Llantas AC - Sistema de Inspección de Llantas

![Versión](https://img.shields.io/badge/Versi%C3%B3n-1.0-blue)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-green)
![Firebase](https://img.shields.io/badge/Servicios-Firebase-orange)

**Llantas AC** es una aplicación móvil diseñada específicamente para la gestión y supervisión del estado de las llantas en la flota de Autolíneas AC. Permite a los inspectores realizar revisiones detalladas en patio, reportar daños de forma visual y coordinar reparaciones con el equipo de taller en tiempo real.

## 🚀 Características Principales

### 1. Gestión de Inspecciones
*   **Bandeja Inteligente:** Visualización de unidades pendientes de inspección en formatos de Lista o Cuadrícula.
*   **Selector de Tipo:** Inspección diferenciada para **Tractor** (10 puntos clave) y **Caja** (ejes de remolque).
*   **Bloqueo de Seguridad:** Obliga a seleccionar un inspector antes de iniciar cualquier proceso para garantizar la trazabilidad.

### 2. Modo Offline (Persistencia Local)
*   **Trabajo en Patio:** Los inspectores pueden realizar revisiones completas sin conexión a internet.
*   **Sincronización Automática:** Los datos se guardan en la memoria de la tablet y se envían a la nube automáticamente al detectar señal.
*   **Indicador de Conexión:** Semáforo visual (Verde/Rojo) que indica el estado de sincronización en tiempo real.

### 3. Reporte de Daños y Taller
*   **Evidencia Fotográfica:** Captura de fotos directa para cada daño detectado.
*   **Integración con Rastreo:** Ubicación de unidades en tiempo real mediante API de Railway para determinar si están "En Patio" o "En Camino".
*   **Flujo de Taller:** Pestaña dedicada para unidades con daños, permitiendo la asignación de mecánicos y seguimiento de reparaciones.

### 4. Seguridad y Usabilidad
*   **Firebase App Check:** Protección contra accesos no autorizados.
*   **Recordar Credenciales:** Función para facilitar el inicio de sesión diario de los inspectores.
*   **Diseño Limpio:** Interfaz moderna estilo web con divisores elegantes y fácil lectura en condiciones de luz solar.

## 🛠️ Tecnologías Utilizadas

*   **Lenguaje:** Java (Android SDK)
*   **Base de Datos:** Firebase Firestore (con persistencia local habilitada).
*   **Autenticación:** Firebase Auth.
*   **UI/UX:** Material Design Components & ConstraintLayout.
*   **Rastreo Externo:** Integración con API REST para telemetría.

## ⚙️ Configuración del Proyecto

Para ejecutar este proyecto localmente:

1.  Clonar el repositorio: `git clone https://github.com/sistemasAC26/LlantasAC.git`
2.  Importar en **Android Studio**.
3.  Asegurarse de tener el archivo `google-services.json` válido en la carpeta `/app`.
4.  Compilar y desplegar en una tablet con **Android 13 (API 33)** o superior.

---
© 2026 Autolíneas AC - Departamento de Sistemas.
