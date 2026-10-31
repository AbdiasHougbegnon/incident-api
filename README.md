# Incident API

API REST de gestion des incidents informatiques développée avec Java et Spring Boot. Elle permet de créer, consulter, modifier et supprimer des incidents, ainsi que de gérer l'inscription et l'authentification des utilisateurs avec des jetons JWT.

## Fonctionnalités

- Gestion des incidents : création, consultation, modification et suppression.
- Authentification par JWT (JSON Web Token).
- Inscription des utilisateurs et hachage des mots de passe avec BCrypt.
- Validation des données entrantes.
- Gestion centralisée des erreurs.
- Migrations de base de données avec Flyway.
- Documentation interactive de l'API avec OpenAPI et Swagger UI.
- Tests automatisés et mesure de couverture avec JaCoCo.

## Technologies

- Java 25
- Spring Boot 4.1.1
- Gradle avec Kotlin DSL
- Spring Web MVC
- Spring Data JPA
- Spring Security et OAuth2 Resource Server
- JWT et BCrypt
- H2 Database
- Flyway
- Lombok
- SpringDoc OpenAPI
- JUnit et Mockito
- JaCoCo

## Prérequis

Avant de lancer le projet, installer :

- Un JDK compatible avec Java 25.
- Git, si le projet est récupéré depuis un dépôt.
- Un terminal PowerShell sous Windows, ou un terminal compatible avec Gradle Wrapper.

## Installation

Cloner le dépôt :

```bash
git clone <URL_DU_DEPOT>
cd incident-api
```

La commande de clonage suppose que le dépôt distant est nommé `incident-api`. Adapter l'URL et le nom du dossier si nécessaire.

## Configuration

L'application utilise une variable d'environnement pour la clé secrète JWT.

La propriété attendue dans `application.properties` est :

```properties
security.jwt.secret=${JWT_SECRET}
```

Sous Windows PowerShell, générer une clé aléatoire de 32 octets encodée en Base64 :

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$rng.Dispose()

$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Cette variable est définie uniquement dans la session PowerShell en cours. Il faut la définir à nouveau dans toute nouvelle session ou configurer le secret dans l'environnement d'exécution.

Ne jamais publier la clé secrète dans le dépôt Git.

## Lancement en développement

Depuis la racine du projet :

```powershell
.\gradlew bootRun
```

L'application utilise actuellement une base H2 en mémoire configurée dans `application.properties`.

Les données de cette base sont temporaires et ne constituent pas une solution de persistance adaptée à une production durable.

## Documentation OpenAPI

Lorsque l'application est démarrée, les interfaces suivantes sont normalement disponibles :

- Swagger UI : `http://localhost:8080/swagger-ui/index.html`
- Document OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Les endpoints de documentation sont configurés pour être accessibles sans authentification.

## Authentification

### Inscription

`POST /api/auth/register`

Exemple de requête :

```json
{
  "email": "utilisateur@example.com",
  "password": "MotDePasseSecurise123!"
}
```

Réponses principales :

- `201 Created` : compte créé.
- `400 Bad Request` : données invalides.
- `409 Conflict` : adresse e-mail déjà utilisée.

L'inscription crée un utilisateur avec le rôle par défaut `ROLE_USER`.

### Connexion

`POST /api/auth/login`

Exemple de requête :

```json
{
  "email": "utilisateur@example.com",
  "password": "MotDePasseSecurise123!"
}
```

Réponses principales :

- `200 OK` : authentification réussie et jeton JWT retourné.
- `400 Bad Request` : données invalides.
- `401 Unauthorized` : identifiants incorrects.

Exemple de réponse :

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer"
}
```

Le jeton doit être envoyé dans l'en-tête HTTP des requêtes protégées :

```http
Authorization: Bearer <JWT>
```

## Gestion des incidents

Toutes les routes suivantes nécessitent une authentification.

| Méthode | Endpoint | Description | Réponses principales |
|---|---|---|---|
| `POST` | `/api/incidents` | Créer un incident | `201`, `400` |
| `GET` | `/api/incidents` | Lister les incidents | `200` |
| `GET` | `/api/incidents/{id}` | Consulter un incident | `200`, `404` |
| `PUT` | `/api/incidents/{id}` | Modifier un incident | `200`, `400`, `404` |
| `DELETE` | `/api/incidents/{id}` | Supprimer un incident | `204`, `404` |

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

Les valeurs de `status` et de `priority` doivent correspondre aux constantes définies dans les énumérations du projet.

### Exemple de modification

`PUT /api/incidents/1`

```json
{
  "title": "Serveur rétabli",
  "description": "Le serveur est de nouveau accessible.",
  "status": "RESOLVED",
  "priority": "HIGH"
}
```

Adapter les valeurs des énumérations aux constantes réellement disponibles dans le code.

### Exemple avec curl

```bash
curl -H "Authorization: Bearer <JWT>" \
     http://localhost:8080/api/incidents
```

## Base de données et migrations

Le projet utilise H2 et Flyway.

Les migrations SQL se trouvent dans :

```text
src/main/resources/db/migration/
```

Migrations présentes :

- `V1__create_incidents_table.sql`
- `V2__create_app_users_table.sql`

La configuration actuelle utilise le mode de création de schéma `validate` de Hibernate : le schéma doit correspondre aux entités et aux migrations appliquées.

## Tests

Exécuter l'ensemble des vérifications :

```powershell
.\gradlew check
```

Exécuter les tests uniquement :

```powershell
.\gradlew test
```

Générer le rapport de couverture :

```powershell
.\gradlew jacocoTestReport
```

Le rapport HTML est généré à cet emplacement :

```text
build/reports/jacoco/test/html/index.html
```

Ouvrir le rapport sous Windows :

```powershell
Start-Process .\build\reports\jacoco\test\html\index.html
```

Le projet possède une règle JaCoCo exigeant au minimum 80 % de couverture des instructions pour la vérification de qualité.

## Construction de l'application

Construire l'application exécutable :

```powershell
.\gradlew bootJar
```

Le JAR exécutable est généré dans le dossier `build/libs/`. Utiliser le fichier `incident-api-0.0.1-SNAPSHOT.jar`, et non le fichier suffixé `-plain.jar`.

Pour exécuter le JAR après avoir défini `JWT_SECRET` :

```powershell
java -jar .\build\libs\incident-api-0.0.1-SNAPSHOT.jar
```

## Sécurité et préparation à la production

Avant un déploiement en production :

- Remplacer H2 en mémoire par une base persistante adaptée au déploiement.
- Stocker les secrets dans les variables d'environnement ou dans le gestionnaire de secrets de la plateforme.
- Utiliser HTTPS.
- Vérifier les règles d'autorisation et la protection de chaque endpoint.
- Configurer les sauvegardes, les journaux et la surveillance.
- Vérifier la configuration des migrations Flyway.
- Exécuter `.\gradlew check` avant chaque livraison.

La configuration actuelle est destinée au développement et aux tests. Le déploiement en production nécessite une configuration complémentaire.

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
│   │   └── service/
│   └── resources/
│       ├── application.properties
│       └── db/migration/
└── test/
    └── java/fr/abdias/incident_api/
```

## Licence

Aucune licence n'est spécifiée dans ce README. Ajouter une licence si le projet doit être distribué publiquement.
