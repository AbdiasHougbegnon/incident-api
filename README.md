# Incident API

API REST de gestion des incidents informatiques développée avec Java et Spring Boot. Elle permet de créer, consulter, modifier et supprimer des incidents, avec authentification JWT, gestion des rôles et propriété des incidents.

## Fonctionnalités

- Gestion des incidents : création, consultation (paginée et filtrable), modification et suppression.
- Authentification par JWT (JSON Web Token), rôles `ADMIN` / `USER`.
- Propriété des incidents : seul le créateur (`reporter`) d'un incident ou un `ADMIN` peut le modifier ou le supprimer. La lecture reste ouverte à tout utilisateur authentifié.
- Inscription des utilisateurs et hachage des mots de passe avec BCrypt.
- Limitation de débit (rate limiting) sur les routes d'authentification.
- Validation des données entrantes.
- Gestion centralisée des erreurs au format `ProblemDetail` (RFC 7807).
- Migrations de base de données avec Flyway (PostgreSQL).
- Documentation interactive de l'API avec OpenAPI et Swagger UI (désactivable en production).
- Tests automatisés et mesure de couverture avec JaCoCo.
- Conteneurisation avec Docker et docker-compose.

## Technologies

- Java 25
- Spring Boot 4.1.1
- Gradle avec Kotlin DSL
- Spring Web MVC
- Spring Data JPA
- Spring Security et OAuth2 Resource Server
- JWT et BCrypt
- PostgreSQL (H2 en mémoire pour les tests uniquement)
- Flyway
- Lombok
- SpringDoc OpenAPI
- JUnit et Mockito
- JaCoCo
- Docker / docker-compose

## Démarrage rapide avec Docker

C'est la méthode recommandée pour lancer le projet en local.

```bash
cp .env.example .env
# Éditer .env : définir DB_PASSWORD, JWT_SECRET, et optionnellement ADMIN_EMAIL/ADMIN_PASSWORD
docker-compose up --build
```

L'application démarre sur `http://localhost:8080` avec une base PostgreSQL persistée dans un volume Docker.

Générer une clé `JWT_SECRET` valide (32 octets minimum, encodée en Base64) :

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

### Compte administrateur initial

Si les variables d'environnement `ADMIN_EMAIL` et `ADMIN_PASSWORD` sont définies et qu'aucun compte `ADMIN` n'existe encore, un compte administrateur est créé automatiquement au démarrage de l'application (une seule fois, de façon idempotente).

## Démarrage sans Docker

Nécessite un JDK 25 et une instance PostgreSQL accessible.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/incidentdb"
$env:DB_USERNAME = "incident"
$env:DB_PASSWORD = "..."
$env:JWT_SECRET = "..."

.\gradlew bootRun
```

Ces variables ne sont définies que dans la session PowerShell en cours. Ne jamais publier de secret dans le dépôt Git (voir `.env.example`).

## Documentation OpenAPI

Lorsque l'application est démarrée (hors profil `prod`) :

- Swagger UI : `http://localhost:8080/swagger-ui/index.html`
- Document OpenAPI JSON : `http://localhost:8080/v3/api-docs`

En production, activer le profil `prod` (`SPRING_PROFILES_ACTIVE=prod`) désactive ces endpoints pour ne pas exposer publiquement la structure de l'API.

## Authentification

### Inscription

`POST /api/auth/register`

```json
{
  "email": "utilisateur@example.com",
  "password": "MotDePasseSecurise123!"
}
```

Réponses principales : `201 Created`, `400 Bad Request`, `409 Conflict` (e-mail déjà utilisé).

L'inscription crée un utilisateur avec le rôle par défaut `USER`.

### Connexion

`POST /api/auth/login`

```json
{
  "email": "utilisateur@example.com",
  "password": "MotDePasseSecurise123!"
}
```

Réponses principales : `200 OK` (jeton JWT retourné), `400 Bad Request`, `401 Unauthorized`.

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer"
}
```

```http
Authorization: Bearer <JWT>
```

Les routes `/api/auth/**` sont limitées à ~10 requêtes par minute et par adresse IP (protection anti brute-force). Au-delà, l'API répond `429 Too Many Requests`. Cette limitation est en mémoire et par instance : un déploiement multi-instance nécessiterait un store partagé (ex. Redis).

## Gestion des incidents

Toutes les routes suivantes nécessitent une authentification.

| Méthode | Endpoint | Description | Autorisation |
|---|---|---|---|
| `POST` | `/api/incidents` | Créer un incident (le créateur devient le `reporter`) | Tout utilisateur authentifié |
| `GET` | `/api/incidents` | Lister les incidents (paginé, filtrable) | Tout utilisateur authentifié |
| `GET` | `/api/incidents/{id}` | Consulter un incident | Tout utilisateur authentifié |
| `PUT` | `/api/incidents/{id}` | Modifier un incident | Propriétaire (`reporter`) ou `ADMIN` |
| `DELETE` | `/api/incidents/{id}` | Supprimer un incident | Propriétaire (`reporter`) ou `ADMIN` |

Une tentative de modification/suppression par un utilisateur non autorisé renvoie `403 Forbidden`.

### Pagination et filtrage

`GET /api/incidents` accepte les paramètres :

- `page`, `size`, `sort` (standard Spring Data, défaut : `size=20`, tri par `createdAt` décroissant).
- `status` (optionnel, ex. `OPEN`).
- `priority` (optionnel, ex. `CRITICAL`).

```bash
curl -H "Authorization: Bearer <JWT>" \
     "http://localhost:8080/api/incidents?page=0&size=10&status=OPEN"
```

La réponse est un objet paginé Spring Data (`content`, `page.size`, `page.totalElements`, etc.).

### Exemple de création d'incident

`POST /api/incidents`

```json
{
  "title": "Serveur inaccessible",
  "description": "Le serveur de production ne répond plus.",
  "status": "OPEN",
  "priority": "CRITICAL"
}
```

Les valeurs de `status` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) et de `priority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) doivent correspondre aux énumérations du projet.

## Base de données et migrations

Le projet utilise PostgreSQL en exécution et Flyway pour les migrations.

Les tests (`@DataJpaTest`, `@SpringBootTest`) s'exécutent sur une base H2 embarquée, remplacée automatiquement par Spring Boot — H2 n'est présent que dans les dépendances de test.

Les migrations SQL se trouvent dans `src/main/resources/db/migration/` :

- `V1__create_incidents_table.sql`
- `V2__create_app_users_table.sql`
- `V3__add_incident_reporter.sql`
- `V4__normalize_app_user_roles.sql`

La configuration utilise le mode `validate` de Hibernate : le schéma doit correspondre aux entités et aux migrations appliquées.

## Tests

```powershell
.\gradlew check       # tests + vérifications qualité (couverture Jacoco ≥ 80%)
.\gradlew test        # tests uniquement
.\gradlew jacocoTestReport
Start-Process .\build\reports\jacoco\test\html\index.html
```

## Construction de l'application

```powershell
.\gradlew bootJar
java -jar .\build\libs\incident-api-0.0.1-SNAPSHOT.jar
```

## Sécurité et préparation à la production

- Les secrets (`JWT_SECRET`, `DB_PASSWORD`, `ADMIN_PASSWORD`) doivent être injectés via variables d'environnement ou un gestionnaire de secrets — jamais commités.
- Activer le profil `prod` pour désactiver la documentation Swagger publique.
- Utiliser HTTPS devant l'application (reverse proxy / load balancer).
- Le rate limiting intégré protège une seule instance ; prévoir un store partagé pour un déploiement distribué.
- Exécuter `.\gradlew check` avant chaque livraison.

## Structure du projet

```text
src/
├── main/
│   ├── java/fr/abdias/incident_api/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── mapper/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   └── resources/
│       ├── application.properties
│       ├── application-prod.properties
│       └── db/migration/
└── test/
    └── java/fr/abdias/incident_api/
```

## Licence

Aucune licence n'est spécifiée dans ce README. Ajouter une licence si le projet doit être distribué publiquement.
