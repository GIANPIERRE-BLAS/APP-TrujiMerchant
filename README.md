# 🚀 TrujiMerchant

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin%2FJava-orange?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Database-Firebase-yellow?style=for-the-badge&logo=firebase" alt="Firebase">
  <img src="https://img.shields.io/badge/API-21+-lightblue?style=for-the-badge&logo=android" alt="API">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

<p align="center">
  <strong>Una app para emprendedores que gestiona negocios y productos vinculados con la app de delivery Trujidelivery</strong>
</p>

<p align="center">
  <a href="#características">Características</a> •
  <a href="#tecnologías">Tecnologías</a> •
  <a href="#instalación">Instalación</a> •
  <a href="#arquitectura">Arquitectura</a> •
  <a href="#contribución">Contribución</a>
</p>

---

## 📖 Descripción

**TrujiMerchant** es una aplicación Android que permite a comerciantes y emprendedores crear, configurar y administrar sus negocios de manera sencilla. La información gestionada en esta app se sincroniza automáticamente con la app de delivery asociada: [**Trujidelivery**](https://github.com/GIANPIERRE-BLAS/Trujidelivery.git), garantizando una experiencia completa tanto para comerciantes como para clientes.

### 🎯 Objetivo

Brindar una plataforma de gestión ágil y accesible para negocios locales, enfocada en potenciar sus ventas mediante una integración directa con la plataforma de delivery.

---

## ✨ Características

### 🏪 Gestión de Negocios

* Crear y personalizar el perfil del negocio (nombre, logo, descripción, dirección).
* Seleccionar tipo de negocio (restaurante, licorería, supermercado, etc.).
* Activar o desactivar el negocio según disponibilidad.

### 📦 Administración de Productos

* Agregar, editar y eliminar productos fácilmente.
* Subir imágenes desde galería o cámara.
* Establecer precios, descripciones y descuentos promocionales.
* Productos organizados por categoría.

### 🧾 Sincronización en Tiempo Real

* Cambios reflejados automáticamente en la app **Trujidelivery**.
* Actualización instantánea de productos, precios y estado del negocio.
* Notificaciones sobre estado de sincronización y éxito de publicación.

### 🧑‍💻 Interfaz Adaptada al Emprendedor

* UI moderna, amigable e intuitiva.
* Dashboard de gestión rápida con accesos directos.
* Visualización del estado de productos y promociones activas.

---

## 🛠️ Tecnologías

### **Frontend Mobile**

```
• Android SDK 21+
• Kotlin 1.8 / Java 8
• Android Jetpack (ViewModel, LiveData, Navigation)
• Material Design 3
• ViewBinding
```

### **Backend y Servicios Cloud**

```
• Firebase:
  ├── Firestore (Base de datos NoSQL)
  ├── Firebase Auth (Autenticación de usuarios)
  ├── Firebase Storage (Subida de imágenes)
  ├── Cloud Functions (Lógica compartida con Trujidelivery)
```

### **APIs y Librerías Adicionales**

```
• Glide (Carga de imágenes)
• Google Services (Location, Maps)
• Hilt (Inyección de dependencias)
```
## 📦 Instalación para Vendedores

### 🛍️ **¿Eres emprendedor? Instala TrujiMerchant y publica tu negocio**

**TrujiMerchant** es la app oficial para comerciantes de Trujidelivery. Con ella puedes:

- 🏪 Crear tu negocio y registrar tu tienda desde el celular.
- 🧾 Subir productos con nombre, precio, descripción e imagen.
- 🎯 Crear ofertas, promociones y descuentos por producto.
- 🖼️ Editar datos de tu negocio como logo, dirección y horario.
- 📲 Hacer que tus productos y promociones aparezcan automáticamente en la app de clientes (Trujidelivery).

---

### ✅ **Requisitos mínimos**
- 📱 Dispositivo Android 5.0 (Lollipop) o superior  
- 🌐 Conexión a Internet estable  
- 💾 Al menos 50 MB de almacenamiento disponible  
- 🔓 Permitir instalación desde fuentes desconocidas (solo la primera vez)

---

### 📥 **Pasos para instalar la aplicación**

1. **⬇️ Descargar la APK**
   👉 [Haz clic aquí para descargar TrujiMerchant.apk](https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant/blob/main/apk/TrujiMerchant.apk)

2. **📂 Abrir el archivo en tu celular**
   - Ve a la carpeta **Descargas** y toca el archivo `TrujiMerchant.apk`.

3. **🔐 Permitir instalación desde fuentes desconocidas**
   - Si es la primera vez que instalas una APK, Android te pedirá permiso.
   - Actívalo desde:
     ```
     Ajustes > Seguridad > Fuentes desconocidas
     ```

4. **🚀 Usar la aplicación**
   - Abre **TrujiMerchant** desde tu menú principal.
   - Regístrate como vendedor.
   - Crea tu negocio, agrega productos, activa ofertas ¡y empieza a vender hoy mismo!
---

## 📦 Instalación para desarrolladores

### Prerrequisitos

* Android Studio Arctic Fox o superior
* JDK 8+
* Firebase configurado con Firestore y Auth
* Conexión a internet
* Dispositivo o emulador Android 5.0+

### Pasos

1. **Clonar el Repositorio**

   ```bash
   git clone https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant.git
   cd APP-TrujiMerchant
   ```

2. **Configurar Firebase**

   ```bash
   # Crear proyecto en Firebase
   # Habilitar Firestore, Auth y Storage
   # Descargar google-services.json
   cp google-services.json app/
   ```

3. **Abrir y Ejecutar en Android Studio**

   ```bash
   # Abrir proyecto
   # Sincronizar Gradle
   # Ejecutar en dispositivo o emulador
   ```

---

## 🏗️ Arquitectura

### Estructura del Proyecto

```
TrujiMerchant/
├── 📁 activities/
├── 📁 adapters/
├── 📁 fragments/
├── 📁 models/
├── 📁 repositories/
├── 📁 utils/
├── 📁 viewmodels/
└── 📁 res/ (layouts, drawables, values)
```

### Patrón MVVM

```
┌────────────┐    ┌──────────────┐    ┌────────────┐
│   View     │───▶│ ViewModel    │───▶│ Repository │
│ (UI Layer) │    │ Logic Layer  │    │ Data Layer │
└────────────┘    └──────────────┘    └────────────┘
```

---

## ✅ Funcionalidades

### Implementadas

* [x] Crear negocio con logo e información
* [x] Agregar productos por categoría
* [x] Editar y eliminar productos
* [x] Activar/desactivar negocio
* [x] Sincronización con Trujidelivery
* [x] Autenticación de usuarios comerciantes

### En desarrollo (Futuro)

* [ ] Dashboard de ventas y estadísticas
* [ ] Notificaciones push para nuevos pedidos
* [ ] Historial de pedidos del negocio
* [ ] Configuración de horarios de atención

---

## 🧪 Testing

```bash
# Pruebas unitarias y de integración
./gradlew test
./gradlew connectedAndroidTest
```

---

## 🤝 Contribución

1. 🍴 Haz un fork del repositorio
2. 🌱 Crea una nueva rama: `feature/nueva-funcionalidad`
3. ✅ Asegúrate que todos los tests pasen
4. 📝 Haz commit con un mensaje claro
5. 🚀 Haz push y crea un Pull Request

---

## 📄 Licencia

Este proyecto está bajo licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## 👨‍💻 Desarrolladores

<p align="center">
  <img src="https://github.com/GIANPIERRE-BLAS.png?size=140" alt="Gianpierre Blas Flores" width="140"/><br>
  <strong>Gianpierre Blas Flores</strong><br>
  <em>Desarrollador Android | Computación e Informática</em><br><br>
  
  <a href="https://github.com/GIANPIERRE-BLAS">
    <img src="https://img.shields.io/badge/GitHub-@GIANPIERRE--BLAS-black?style=flat-square&logo=github" alt="GitHub" />
  </a>
  <a href="https://www.linkedin.com/in/justo-gianpierre-blas-flores-5ba671302/">
    <img src="https://img.shields.io/badge/LinkedIn-Gianpierre%20Blas-blue?style=flat-square&logo=linkedin" alt="LinkedIn" />
  </a>
  <a href="mailto:gianpierreblasflores235@gmail.com">
    <img src="https://img.shields.io/badge/Email-gianpierreblasflores235@gmail.com-red?style=flat-square&logo=gmail" alt="Email" />
  </a>
</p>

<p align="center">
  <strong>Colaboradores:</strong><br>
  👩‍💻 Anita Benites Venturo<br>
  👨‍💻 Aldo Chávez Blas
</p>


---

## 📞 Soporte y Contacto

### ¿Necesitas Ayuda?

- 🐛 **Reportar Bug**: [Crea un Issue](https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant/issues)
- 💡 **Sugerir Feature**: [Participa en Discussions](https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant/discussions)
- 📧 **Contacto Directo**: [gianpierreblasflores235@gmail.com](mailto:gianpierreblasflores235@gmail.com)
- 📖 **Documentación**: Visita la [Wiki del Proyecto](https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant/wiki)

### ⏱️ Tiempos de Respuesta

| Tipo de solicitud         | Tiempo estimado      |
|---------------------------|----------------------|
| 🔥 **Bugs críticos**       | 24–48 horas          |
| 🐛 **Bugs menores**        | 3–5 días hábiles     |
| 💡 **Nuevas Features**     | 1–2 semanas          |
| ❓ **Consultas generales** | 1–3 días hábiles     |

---

<div align="center">

### 🌟 ¡Dale una estrella si te gustó el proyecto! ⭐

[![Stargazers](https://img.shields.io/github/stars/GIANPIERRE-BLAS/APP-TrujiMerchant?style=social)](https://github.com/GIANPIERRE-BLAS/APP-TrujiMerchant/stargazers)

**Hecho con ❤️ en Trujillo, Perú**

</div>

