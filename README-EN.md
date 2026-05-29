# GauzyNote
> Light as gauze, secure as a fortress

An open-source, self-hosted Web-based Markdown note-taking tool designed for developers and knowledge workers who value data autonomy.
* ✨ Lightweight Web experience: No client install required, browser access, smooth editing
* 🔒 Self-hosted deployment: Supports local server / private cloud, full data control, no public cloud dependency
* 📝 Native Markdown support: Full MD syntax compatibility, supports source editing, preview, and live preview modes
* 🆓 Open-source & customizable: MIT licensed, developers can extend features as needed

|           | GauzyNote                                      |
|-----------|------------------------------------------------|
| Pricing   | Free forever                                   |
| Customization | Fully open-source, MIT licensed             |
| Privacy   | Self-hosted, local or private cloud, data isolated per account |

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0-green.svg)]()
[![Java](https://img.shields.io/badge/Java-8-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3.5-4fc08d.svg)]()

[中文](./README.md)

## 📌 Table of Contents
- Quick Start
- Technology Stack
- Project Progress
- Features
- File Structure
- Development Guide
- Production Deployment
- Contributing
- FAQ
- License
- Screenshots

## Quick Start

> Switch the backend language by modifying `Constants.DEFAULT_LOCALE` (supports Chinese/English). See the i18n directory.

### Development Requirements
* Backend: JDK 8, Maven 3.9+, MySQL 5.7, Redis 6.0+
* Frontend: Node.js 16+, npm 7+
* OS: Windows / macOS / Linux (any OS supporting self-hosted deployment)

### Deployment Requirements
* Backend: JDK 8, MySQL 5.7, Redis 6.0+
* Nginx or other tools
* OS: Windows / macOS / Linux (any OS supporting self-hosted deployment)

### Backend Setup
1. Clone the repo: `git clone https://github.com/guan-x-l/gauzynote.git`
2. Enter the project root and load Maven dependencies (the IDE can auto-load; CLI: `mvn clean install -Dmaven.test.skip=true`)
3. Enter the backend directory: `cd gauzynote/gauzynote-application`
4. Configure database: edit `src/main/resources/application-dev.yml`, set MySQL and Redis connection info (see Essential Configuration below)
5. Start the service:
    - CLI: `mvn spring-boot:run`
    - IDE: run `GauzyNoteApplication.java` directly
6. After successful startup, visit `http://localhost:8989` — a 404 page is expected (no static pages; deploy the frontend next)

### Frontend Setup
1. Enter the frontend directory: `cd gauzynote/gauzynote-web`
2. Install dependencies: `npm install`
   > It is strongly recommended not to use cnpm directly, as it may cause various unexpected bugs. You can solve slow npm installs by changing the registry.
    - Default registry: `npm install`
    - Chinese mirror (recommended): `npm install --registry=https://registry.npmmirror.com`
3. Start the dev server: `npm run dev`
4. Access the frontend: open `http://localhost:8988` in browser, log in with `admin` / `1234567` (default credentials; see project initialization data for details)

> [!WARNING]
> This project uses a fully decoupled frontend/backend architecture. Both services must be running to use the application.

### Essential Configuration (Required)
Set the active profile by editing `gauzynote-application/src/main/resources/application.yml`:
```yaml
# Spring config
spring:
  profiles:
    active: dev # Run environment; can also be set via IDE run configuration with a dev profile argument
token:
   secret: wwssadadbaba # Token secret
```

Configure database and Redis by editing `gauzynote-application/src/main/resources/application-dev.yml`:
```yaml
# Data source config
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gauzy_note?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8&createDatabaseIfNotExist=true
    username: root  # your database username
    password: 123456  # your database password
  redis:
    host: localhost  # Redis host
    port: 6379       # Redis port
    password:        # Redis password (leave empty if none)
    database: 0      # Redis database index
```

Database initialization config:
```yaml
spring:
  sql:
    init:
      mode: ALWAYS  # ALWAYS: always initialize (dev); NEVER: do not initialize (recommended for production)
      schema-locations: classpath:sql/schema.sql  # Schema init script
      data-locations: classpath:sql/data.sql      # Seed data script
```

### Optional Configuration
Edit `gauzynote-application/src/main/resources/application-dev.yml`:
```yaml
app:
  cors:
    allowed-origins: # CORS config; keep "*" for local dev, set specific domains for prod
    - "*"
    - "https://www.example.com"
    - "https://example.com"
    allow-credentials: true # Whether to allow cookies
  upload:
    dir: /Users/{username}/home/gauzynote/uploads/ # Upload path for local dev (macOS example)
    #dir: C:/gauzynote/uploads/ # Windows example
```

> Barring any surprises, you should now be able to use the application locally.
>
> The author's local dev environment: macOS Sequoia 15.7.2, jdk1.8.0_462, mysql5.7, maven3.9, redis7.2.0, nodejs18.17.1, npm9.6.7

## Technology Stack

### Frontend
| Technology    | Version | Purpose                                |
|---------------|---------|----------------------------------------|
| Vue           | 3.5.x   | Frontend framework                     |
| Vite          | 7.x     | Build tool                             |
| CodeMirror 6  | 6.x     | Markdown editor                        |
| markdown-it   | 14.x    | Markdown parsing & read-only preview   |
| highlight.js  | 11.x    | Code syntax highlighting (read-only & live preview) |

### Backend
| Technology   | Version | Purpose                   |
|--------------|---------|---------------------------|
| Java         | 8       | Development language      |
| Spring Boot  | 2.7.6   | Backend framework         |
| MySQL        | 5.7     | Database                  |
| Redis        | 6.0+    | Cache & token storage     |
| Maven        | 3.9+    | Build & dependency management |

## Project Progress

### Current Version
**v1.0** (core features available, under active iteration)

### Phase Goals
* ✅ **Phase 1**: Basic editing features
  * Markdown editing/preview, resource management, image upload, account system
* ✅ **Phase 2**: Experience optimization
  * Live preview (tables, task lists, code syntax highlighting), theme switching (light/dark), i18n
* 📋 **Phase 3**: Advanced features (planned)
  * Bidirectional links, knowledge graph, sharing

## Features

### Editing & Preview
* [x] Markdown source editing
* [x] Image embedding & upload
* [x] Markdown read-only preview (highlight.js code syntax highlighting)
* [x] Live preview (WYSIWYG style, what you see is what you get)
* [x] Image embedding & upload (drag-drop/paste/toolbar)

### Resource Management
* [x] Resource manager (create, edit, delete, move, rename files/folders)
* [ ] Recycle bin (planned)

### System Features
* [x] Account system (register, login, change password)
* [x] Multi-language support (Chinese / English)
* [x] Theme switching (light / dark, follow system)
* [x] WebAuthn / Passkey login

### Advanced Features (Planned)
* [ ] Bidirectional links & knowledge graph
* [ ] Resource/article sharing
* [ ] Flowcharts & mind maps

## File Structure

### Backend Structure
```
├── gauzynote-application      // Main service module (entry point & REST controllers)
├── gauzynote-common           // Common module (domain classes, enums, utilities, annotations, constants)
├── gauzynote-framework        // Framework module (Spring Security, JWT, AOP, interceptors, global exception handler)
├── gauzynote-system           // Business module (MyBatis mappers, services, entities/VOs)
└── gauzynote-web              // Frontend module (Vue 3 / Vite, independent of Maven build)
```

### Frontend Structure
```
├── public                      // Public static assets
├── src                         // Source code
│   ├── api                     // API request modules
│   ├── assets                  // Styles, images, etc.
│   ├── biz                     // Business logic modules
│   ├── components              // Shared components (globally reusable)
│   ├── constants               // Global constants
│   ├── enum                    // Enum definitions
│   ├── hooks                   // Vue 3 hooks
│   ├── layout                  // Layout components
│   ├── locales                 // i18n language packs
│   ├── plugins                 // Plugins (editor, UI, etc.)
│   ├── router                  // Route config
│   ├── store                   // State management (Vue 3 Composables)
│   ├── utils                   // Global utility functions
│   ├── views                   // Page components
│   ├── App.vue                 // Root component
│   ├── context.js              // App context
│   ├── jsdoc.js                // JSDoc config
│   ├── main.js                 // Entry file
│   └── storage-compatibility.js// Browser storage compatibility
├── .env.development            // Dev environment config
├── .env.production             // Production environment config
├── .gitignore                  // Git ignore rules
├── index.html                  // HTML entry
├── jsconfig.js                 // JavaScript config
├── package.json                // Dependencies & scripts
└── vite.config.js              // Vite build config
```

## Development Guide

This project is fully open-source under the MIT license. No fees.

Code comments are in Chinese.

### Local Development Notes
- Frontend-backend integration: ensure `VITE_API_BASE_URL` in `.env.development` matches the backend address
- Dependency install: if frontend install fails, try deleting `node_modules` and `package-lock.json`, then reinstall
- Database migration: if you modify data models, sync `schema.sql` and reinitialize the database

## 🚢 Production Deployment

After verifying in dev, follow these steps to deploy to production (Linux server example):

### 1. Backend Package & Deploy
1. Package: from the project root, run `mvn clean package -Dmaven.test.skip=true`, or build in IDE; the JAR is generated at `gauzynote-application/target/gauzynote-application-{version}.jar`
2. Set environment: in `application.yml`, change profile to prod (`spring.profiles.active=prod`)
3. Configure production: edit `application-prod.yml`, set database, Redis, CORS, upload path, etc. (recommended: set `spring.sql.init.mode=NEVER`)
4. Start the service:
   * Background (recommended): `nohup java -jar gauzynote-application-{version}.jar > /dev/null 2>&1 &`
   * Verify: check the `app.log` file

### 2. Frontend Package & Deploy
1. Package: enter `gauzynote-web`, run `npm run build`; output in `dist/`
2. Deploy to Nginx:
   * Copy `dist/` contents to the Nginx static directory (e.g. `/usr/share/nginx/html/gauzynote`)
   * Configure Nginx reverse proxy (to handle CORS):
    ```
    server {
        listen 80;
        server_name your-domain.com;  # your domain

        location / {
            root /usr/share/nginx/html/gauzynote;
            index index.html;
            try_files $uri $uri/ /index.html;  # Fix Vue router refresh 404
        }

        # Reverse proxy backend APIs
        location /service/gauzynote {
            proxy_pass http://localhost:8989;  # Backend address
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
    ```
    * Restart Nginx: `nginx -s reload`

### 3. Production Recommendations
- Database: enable MySQL slow query logging, back up data regularly
- Security: set up firewall, only expose necessary ports (80/443); configure HTTPS (via Nginx SSL certificate)
- **Regular database backups**: recommended daily automatic MySQL backups, keep the last 7 days
- **Disable SQL auto-init**: set `spring.sql.init.mode=NEVER`
- **Nginx gzip compression**: enable static resource compression to reduce transfer size
- See [SECURITY.md](SECURITY.md) for details

## 🤝 Contributing

Contributions are welcome via:
1. Submit Issues: report bugs or suggest features on the [Issues](https://github.com/guan-x-l/gauzynote/issues) page

## ❓ FAQ

1. Q: File upload fails on macOS?
   A: Check that `app.upload.dir` in the yml file uses an absolute path, and ensure the directory has read/write permissions (`chmod 755`).

2. Q: Frontend can't connect to backend after startup?
   A: Verify the backend is running (visit `http://<backend-host>:8989`); check that `VITE_API_BASE_URL` in `.env.development` is correct; check that the backend CORS config includes the frontend address.

3. Q: Maven dependency loading fails?
   A: Check network, consider configuring a Chinese Maven mirror (Alibaba); run `mvn clean install -U` to force update dependencies.

If you encounter other issues, please report them on [Issues](https://github.com/guan-x-l/gauzynote/issues). Responses are provided periodically.

## 📚 Acknowledgments

This project's logic and design drew inspiration from the following excellent projects:
* **Obsidian**: Excellent UI layout and smooth interactions
* **Arco Design Vue**: UI components and design specifications
* **RuoYi-Vue**: Frontend-backend separation architecture design

## ⚠️ Disclaimer

This project was developed by an individual developer during learning and practice. Features and stability may have limitations. Please evaluate risks based on your actual needs. The author assumes no liability for any losses arising from the use of this project.

This is a Web-based Markdown editor. The UI layout and interaction logic reference industry-standard design conventions. Visual and interaction details were designed with reference to certain browsers and open-source projects, independently completed, with no affiliation to any third-party app/software, and no use of any third-party protected design elements or code.

This project is released under the MIT open-source license and includes only independently developed code and designs. Any infringement disputes arising from the use of this project are the responsibility of the user.

Regarding non-independently developed code and designs sourced from the web or AI, please contact the author for removal if any infringement exists.

## 📜 License

This project is open-sourced under the [MIT License](https://opensource.org/licenses/MIT). See the `LICENSE` file for details.

## Screenshots

|         | |
|---------|-|
| Login    | ![Login](/.image%2F%E7%99%BB%E9%99%86.png)|
| Preview  | ![Preview](/.image%2F%E9%A2%84%E8%A7%88%E7%AC%94%E8%AE%B0.png)|
| Change Password | ![Password](/.image%2F%E5%AF%86%E7%A0%81.png)|
| Dark Theme | ![Dark Theme](/.image%2F%E6%B7%B1%E8%89%B2%E4%B8%BB%E9%A2%98.png)|
| Editing  | ![Editing](/.image%2F%E7%BC%96%E8%BE%91%E4%B8%8E%E5%88%9B%E5%BB%BA.png)|
| TOC      | ![TOC](/.image%2F%E7%9B%AE%E5%BD%95.png)|
| Language | ![Language](/.image%2F%E8%AF%AD%E8%A8%80.png)|
| Rename & Tabs | ![Tabs](/.image%2F%E9%87%8D%E5%91%BD%E5%90%8D%E4%B8%8E%E6%A0%87%E7%AD%BE%E6%A0%8F.png) |
