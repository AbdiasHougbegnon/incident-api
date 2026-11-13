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
- Déconnexion (`/api/auth/logout`) avec révocation effective du jeton courant.
- Migrations de base de données avec Flyway (PostgreSQL).
- Documentation interactive de l'API avec OpenAPI et Swagger UI (désactivable en production).
- Observabilité : endpoints Spring Actuator sur un port de gestion isolé, supervision Prometheus + Grafana (tableau de bord + alerte de démonstration), logs structurés en JSON.
- Sauvegardes automatiques et planifiées de la base PostgreSQL.
- Secrets gérés via HashiCorp Vault (mode développement).
- Déploiement visé : VM Oracle Cloud Always Free (gratuite à vie, pas de mise en veille).
- Scripts de test de charge (k6) : vérification rapide et scénario de montée en charge progressive.
- Tests automatisés et mesure de couverture avec JaCoCo.
- Formatage de code automatique et vérifié en CI (Spotless).
- Analyse de qualité de code statique (scaffold SonarCloud).
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
- Spring Boot Actuator / Micrometer (Prometheus)
- Prometheus et Grafana
- HashiCorp Vault
- k6 (tests de charge)
- Spotless (formateur Eclipse/JDT)
- SonarCloud (scaffold)
- Docker / docker-compose

## Démarrage rapide avec Docker

C'est la méthode recommandée pour lancer le projet en local.

```bash
cp .env.example .env
# Éditer .env : définir DB_PASSWORD, JWT_SECRET, VAULT_DEV_ROOT_TOKEN,
# GRAFANA_ADMIN_PASSWORD, et optionnellement ADMIN_EMAIL/ADMIN_PASSWORD
docker-compose up --build
```

L'application démarre sur `http://localhost:8080` avec une base PostgreSQL persistée dans un volume Docker.

Au démarrage, la stack lance aussi :
- **`vault`** : un serveur HashiCorp Vault en mode développement, qui stocke les secrets (`DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`).
- **`vault-init`** : un conteneur qui s'exécute une seule fois pour écrire ces secrets dans Vault (`secret/incident-api`), à partir de ton fichier `.env`.
- **`postgres-backup`** : un conteneur qui sauvegarde la base PostgreSQL toutes les 24h.
- **`prometheus`** : scrute les métriques de l'application toutes les 15s, consultable sur `http://localhost:9090`.
- **`grafana`** : tableau de bord de supervision, consultable sur `http://localhost:3000` (identifiant `admin`, mot de passe = `GRAFANA_ADMIN_PASSWORD` dans `.env`).

Le service `app` ne lit **plus** `DB_PASSWORD`/`JWT_SECRET`/`ADMIN_EMAIL`/`ADMIN_PASSWORD` directement dans son environnement : il les récupère auprès de Vault au démarrage (voir [Gestion des secrets](#gestion-des-secrets-avec-hashicorp-vault)). `docker compose up` attend donc que `vault-init` ait terminé avant de démarrer `app`.

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

Sans `VAULT_ADDR`/`VAULT_TOKEN` définis, l'application ignore totalement Vault et lit les secrets directement dans les variables d'environnement, comme ci-dessus — pratique pour développer sans lancer toute la stack Docker.

## Gestion des secrets avec HashiCorp Vault

En conteneur (`docker-compose`), les secrets ne sont plus transmis en clair à l'application : ils sont stockés dans Vault, et l'application va les chercher elle-même au tout début de son démarrage.

**Comment ça fonctionne** : une classe `VaultEnvironmentPostProcessor` (`EnvironmentPostProcessor` Spring Boot) s'exécute avant la création de tout bean. Si `VAULT_ADDR` et `VAULT_TOKEN` sont présents dans l'environnement, elle appelle `GET {VAULT_ADDR}/v1/secret/data/incident-api` (API HTTP de Vault, via `java.net.http.HttpClient` — aucune dépendance supplémentaire) et injecte le résultat comme source de propriétés prioritaire, sous les mêmes noms que les variables d'environnement habituelles (`DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`). Si Vault est injoignable ou que ces variables sont absentes, l'application se comporte exactement comme avant (fallback silencieux, avec un simple avertissement dans les logs).

**Inspecter les secrets stockés** (utile pour déboguer) :

```bash
docker compose exec vault vault kv get secret/incident-api
```

### ⚠️ Le mode "dev" de Vault n'est pas sécurisé pour une vraie production

Le conteneur `vault` tourne en **mode développement** (`hashicorp/vault` avec `VAULT_DEV_ROOT_TOKEN_ID`) :
- **stockage en mémoire** — tout disparaît si le conteneur redémarre (pas un problème ici puisque `vault-init` réécrit les secrets à chaque démarrage de la stack, mais inacceptable en vraie prod) ;
- **déverrouillage automatique** — en prod, Vault doit être "unsealed" manuellement (ou via un auto-unseal KMS cloud) avec des parts de clé Shamir, justement pour qu'aucune personne seule ne puisse déverrouiller le coffre ;
- **jeton root fixe, sans TLS** — en prod, l'authentification doit utiliser une méthode à privilège limité (AppRole, Kubernetes auth...) plutôt qu'un jeton root permanent, et toutes les communications doivent être chiffrées (TLS).

Pour une vraie mise en production : un backend de stockage persistant (Raft intégré, ou Consul), un vrai processus de scellement/déverrouillage, l'authentification AppRole, et TLS de bout en bout. C'est un projet à part entière, pas une simple option à activer.

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

Les routes `/api/auth/**` sont limitées à ~10 requêtes par minute et par adresse IP (protection anti brute-force). Au-delà, l'API répond `429 Too Many Requests`. Cette limitation est en mémoire et par instance : avec une seule instance de l'application (le cas aujourd'hui), c'est suffisant. **Si l'application est un jour déployée sur plusieurs instances** derrière un load balancer, ce filtre doit être remplacé par un compteur partagé (ex. Redis avec `INCR`/`EXPIRE`), sinon chaque instance a sa propre limite et un attaquant peut la contourner en répartissant ses requêtes.

### Déconnexion

`POST /api/auth/logout` (authentification requise)

Révoque le jeton JWT utilisé pour cet appel : son identifiant unique (`jti`) est enregistré dans une liste de révocation en base. Toute requête ultérieure avec ce même jeton est rejetée (`401`), même s'il n'est pas encore expiré. Les entrées expirées sont purgées automatiquement chaque nuit à 3h.

```bash
curl -X POST http://localhost:8080/api/auth/logout \
     -H "Authorization: Bearer <JWT>"
```

Réponse : `204 No Content`.

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

## Observabilité

### Endpoints Actuator sur un port isolé

L'application expose des endpoints [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) (`health`, `info`, `metrics`, `prometheus`) sur un **port de gestion séparé** (`management.server.port=9001`, voir `application.properties`), distinct du port applicatif (8080).

Pourquoi ce choix plutôt qu'une protection par rôle JWT (comme pour le reste de l'API) : un scraper Prometheus interroge l'endpoint toutes les 15 secondes en continu — il ne peut pas "se reconnecter" avec un jeton JWT qui expire au bout d'une heure. La solution standard Spring Boot pour ce cas précis est d'isoler ces endpoints sur un port dédié, puis de ne **jamais le publier publiquement**. Dans `docker-compose.yml`, le port 9001 n'est pas mappé vers l'hôte : seuls les conteneurs du même réseau Docker (ex. `prometheus`) peuvent l'atteindre. C'est une isolation réseau, strictement plus robuste qu'un contrôle applicatif par rôle — inatteignable depuis l'extérieur quels que soient les identifiants présentés.

Conséquence pratique : `curl http://localhost:8080/actuator/health` ne fonctionne plus (ce chemin n'existe plus sur ce port). Pour consulter la santé de l'app depuis ta machine en debug, utiliser `docker compose exec app curl http://localhost:9001/actuator/health` (depuis l'intérieur du conteneur).

### Prometheus et Grafana

- **Prometheus** (`http://localhost:9090`) scrute `app:9001/actuator/prometheus` toutes les 15s (config : `monitoring/prometheus.yml`). Vérifier que la cible est `UP` : `http://localhost:9090/targets`.
- **Grafana** (`http://localhost:3000`, identifiant `admin` / mot de passe `GRAFANA_ADMIN_PASSWORD`) affiche un tableau de bord préconfiguré (`monitoring/grafana/provisioning/dashboards/json/incident-api.json`) : uptime, mémoire JVM, débit et latence HTTP, disponibilité.
- Une **règle d'alerte de démonstration** est provisionnée (`monitoring/grafana/provisioning/alerting/`) : se déclenche si l'application ne répond plus pendant 1 minute. ⚠️ Son point de contact est un webhook factice (`https://example.invalid/...`) — **aucune vraie notification ne sera envoyée** tant qu'il n'est pas remplacé par un vrai webhook Slack/Teams ou un serveur SMTP réel dans Grafana (`Alerting > Contact points`).

Les logs sont émis en JSON structuré sur la sortie standard (`logging.structured.format.console=logstash`), directement exploitables par un outil centralisé (ELK, Loki, Datadog...) sans configuration supplémentaire.

## Sauvegardes de la base de données

Le service `postgres-backup` (voir `docker-compose.yml`) exécute `pg_dump` toutes les 24h et conserve les sauvegardes 7 jours (variable `RETENTION_DAYS`, voir `backup/backup.sh`), dans le volume Docker `pgbackups`.

Lister les sauvegardes disponibles :

```bash
docker compose exec postgres-backup ls -la /backups
```

Restaurer une sauvegarde :

```bash
docker compose exec -T postgres pg_restore -U incident -d incidentdb --clean < nom_du_fichier.dump
```

C'est une solution minimale adaptée à un environnement de démo/développement. Une vraie production s'appuierait plutôt sur les sauvegardes automatiques du service managé (ex. RDS, Cloud SQL) ou un outil dédié (WAL-G, pgBackRest) avec stockage hors-site (S3...).

## Tests de charge

Deux scripts [k6](https://k6.io/), partageant la logique commune de `loadtest/common.js` :

- **`loadtest/scenario.js`** — vérification rapide : 10 utilisateurs simultanés pendant 30s, enchaînant inscription, connexion, création d'incident, liste paginée et consultation (chaque itération s'authentifie, comme un smoke test de bout en bout).
- **`loadtest/scenario-extended.js`** — montée en charge progressive : de 0 à 20 utilisateurs virtuels sur plusieurs minutes (`ramping-vus`). Ce scénario s'authentifie **une seule fois** (dans `setup()`, pas à chaque itération) et sollicite ensuite uniquement les endpoints métier (`/api/incidents`) — comme un vrai client qui réutilise son jeton, et sans se heurter inutilement au rate limiting de `/api/auth/**`.

```bash
# Verification rapide
k6 run loadtest/scenario.js

# Montee en charge progressive
k6 run loadtest/scenario-extended.js

# Ou via l'image Docker officielle, sans rien installer
# (host.docker.internal fonctionne sur Docker Desktop Windows/Mac ;
#  sous Docker natif sur Linux, remplacer par --network host)
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < loadtest/scenario.js
```

⚠️ Le rate limiting sur `/api/auth/**` (10 req/min/IP) s'applique au scénario rapide : avec 10 utilisateurs virtuels qui s'inscrivent et se connectent en boucle depuis la même machine (donc la même IP), tu verras rapidement des `429` sur `register`/`login` — c'est le rate limiter qui fonctionne correctement, pas un bug du script. C'est justement pour éviter ce faux négatif que le scénario étendu ne s'authentifie qu'une seule fois.

## Base de données et migrations

Le projet utilise PostgreSQL en exécution et Flyway pour les migrations.

Les tests (`@DataJpaTest`, `@SpringBootTest`) s'exécutent sur une base H2 embarquée, remplacée automatiquement par Spring Boot — H2 n'est présent que dans les dépendances de test.

Les migrations SQL se trouvent dans `src/main/resources/db/migration/` :

- `V1__create_incidents_table.sql`
- `V2__create_app_users_table.sql`
- `V3__add_incident_reporter.sql`
- `V4__normalize_app_user_roles.sql`
- `V5__create_revoked_tokens_table.sql`

La configuration utilise le mode `validate` de Hibernate : le schéma doit correspondre aux entités et aux migrations appliquées.

## Tests

```powershell
.\gradlew check       # tests + vérifications qualité (couverture Jacoco ≥ 80%)
.\gradlew test        # tests uniquement
.\gradlew jacocoTestReport
Start-Process .\build\reports\jacoco\test\html\index.html
```

## Qualité de code : formatage et analyse statique

### Spotless (formatage automatique)

Le code Java est formaté selon le style **Eclipse/JDT** (via le plugin [Spotless](https://github.com/diffplug/spotless)). `spotlessCheck` est intégré à `.\gradlew check` : un code mal formaté fait échouer la CI.

```powershell
.\gradlew spotlessCheck   # vérifie sans modifier (ce que fait la CI)
.\gradlew spotlessApply   # reformate automatiquement
```

Pourquoi Eclipse plutôt que `google-java-format` (le choix le plus courant) : ce dernier s'appuie sur des API internes du compilateur Java qui ne suivent pas encore Java 25 (version utilisée par ce projet) — le formateur Eclipse est indépendant du JDK hôte.

### SonarCloud (scaffold, inactif)

Le plugin Gradle `org.sonarqube` est configuré (`build.gradle.kts`), ainsi qu'une étape conditionnelle dans la CI (`.github/workflows/ci.yml`) — mais **l'analyse ne s'exécute pas** tant que le dépôt n'a pas été importé sur SonarCloud.

**Pour activer :**

1. Va sur [sonarcloud.io](https://sonarcloud.io/), connecte-toi avec ton compte GitHub (gratuit pour les dépôts publics).
2. **"+" > Analyze new project"**, choisis ce dépôt GitHub.
3. Note la **clé d'organisation** et la **clé de projet** générées — remplace les valeurs `sonar.projectKey`/`sonar.organization` dans `build.gradle.kts` si elles diffèrent de celles déjà renseignées (`AbdiasHougbegnon_incident-api` / `abdiashougbegnon`, des valeurs plausibles mais à vérifier après l'import).
4. Génère un jeton : **My Account > Security > Generate Token**.
5. Sur GitHub : `Settings > Secrets and variables > Actions` → ajoute un secret `SONAR_TOKEN` avec ce jeton.
6. Au prochain push, la CI exécutera `./gradlew sonar` et le tableau de bord SonarCloud affichera les résultats (bugs, vulnérabilités, "code smells", couverture).

Une fois actif, ajouter le badge de qualité (fourni par SonarCloud après le premier scan) en haut de ce README — c'est un signal appréciable sur un portfolio.

## Construction de l'application

```powershell
.\gradlew bootJar
java -jar .\build\libs\incident-api-0.0.1-SNAPSHOT.jar
```

## Revue de code (action à faire toi-même)

Ce dépôt n'a, pour l'instant, aucune barrière empêchant de pousser directement sur `master` sans relecture — c'est d'ailleurs arrivé plusieurs fois pendant le développement de ce projet. Ça ne peut pas être automatisé depuis ce dépôt (c'est un réglage GitHub, pas du code) :

1. Sur GitHub : `Settings > Branches > Add branch protection rule`.
2. Branche ciblée : `master`.
3. Activer *"Require a pull request before merging"* et *"Require approvals"* (au moins 1).
4. Optionnel mais recommandé : *"Require status checks to pass before merging"* → sélectionner le job `Build and test` de la CI.

Une fois actif, même un push direct sur `master` sera refusé par GitHub — toute modification devra passer par une Pull Request.

## Environnements (staging / production)

- **`application-staging.properties`** : mêmes garde-fous que la prod (voir plus bas), mais Swagger reste actif pour permettre l'exploration de l'API avant promotion.
- **`application-prod.properties`** : Swagger désactivé.

Le principe suivi est *"build once, promote"* : la même image Docker est déployée d'abord en staging, puis — après validation manuelle — promue en prod sans être reconstruite. Voir la section suivante.

## Déploiement en production : VM Oracle Cloud Always Free

Choix retenu après un essai infructueux sur Fly.io (base Postgres "unmanaged" qui s'endort de façon imprévisible sur le tier gratuit, peu adapté à un lien de portfolio qui doit toujours répondre) : une **VM Oracle Cloud Ampere A1 (ARM)**, gratuite à vie (jusqu'à 4 OCPU / 24 Go de RAM), sans "scale to zero" — elle ne s'éteint jamais toute seule.

Le déploiement reste **manuel** pour l'instant (pas de pipeline CI/CD automatisé vers cette VM) :

1. Créer la VM (console OCI : `Compute > Instances > Create Instance`, image Ubuntu, forme `VM.Standard.A1.Flex`, clé SSH téléchargée).
2. Ouvrir le port `8080` (et `3000` pour Grafana si besoin) dans la liste de sécurité du sous-réseau.
3. Se connecter en SSH, installer Docker + Docker Compose.
4. Copier ce dépôt (ou au minimum `Dockerfile`, `docker-compose.yml`, `backup/`, `vault/`, `monitoring/`, `.env`) sur la VM.
5. `docker compose up -d` — exactement la même commande qu'en local.

Rien n'empêche d'automatiser cette étape plus tard (ex. une action GitHub qui se connecte en SSH et relance `docker compose pull && docker compose up -d` après chaque push sur `master`).

## Haute disponibilité de la base de données

Volontairement **non implémenté** ici (documentation seulement) : une seule instance PostgreSQL tourne, sans réplication ni bascule automatique — un point de défaillance unique. C'est un choix délibéré : le chantier est lourd pour un gain limité sur un projet sans trafic réel à protéger aujourd'hui.

Ce qu'il faudrait pour une vraie haute disponibilité :
- **Réplication en streaming** : une ou plusieurs instances PostgreSQL "replica" recevant en continu les changements du primaire (lecture seule, utile aussi pour répartir la charge de lecture).
- **Bascule automatique (failover)** : un outil dédié comme [Patroni](https://patroni.readthedocs.io/) ou `repmgr`, qui détecte la panne du primaire et promeut automatiquement une replica — ce n'est *pas* quelque chose qu'on improvise soi-même de façon fiable.
- **Alternative plus simple en pratique** : un service managé avec HA intégrée (AWS RDS Multi-AZ, Google Cloud SQL avec haute disponibilité...) qui gère tout ça pour toi, au prix d'un abonnement.

## Sécurité et préparation à la production

- Les secrets (`JWT_SECRET`, `DB_PASSWORD`, `ADMIN_PASSWORD`) sont gérés via HashiCorp Vault (voir [plus haut](#gestion-des-secrets-avec-hashicorp-vault)) — mais en **mode développement**, pas prêt pour une vraie prod (stockage en mémoire, jeton root, pas de TLS). Pour une vraie mise en production, prévoir : un backend de stockage persistant, l'authentification AppRole, et TLS de bout en bout.
- Activer le profil `prod` pour désactiver la documentation Swagger publique.
- **HTTPS** : non géré par l'application elle-même, et volontairement pas implémenté dans ce dépôt — il n'existe aucun moyen d'obtenir un certificat de confiance (Let's Encrypt) pour `localhost` sans nom de domaine public réel. En production, placer un reverse proxy/load balancer devant l'application qui termine le TLS (ex. Nginx ou Traefik avec Let's Encrypt/Certbot, ou la terminaison TLS du load balancer d'un fournisseur cloud) ; l'application continue de parler HTTP en clair uniquement sur le réseau interne, derrière ce proxy.
- **Rate limiting distribué** : le filtre actuel est en mémoire, par instance — suffisant pour une seule instance. Avant de déployer sur plusieurs instances derrière un load balancer, le remplacer par un compteur partagé (ex. Redis, `INCR`/`EXPIRE` par IP), sinon chaque instance a sa propre limite.
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
│       ├── db/migration/
│       └── META-INF/spring.factories
└── test/
    └── java/fr/abdias/incident_api/

backup/        # script de sauvegarde PostgreSQL planifiée
vault/         # script d'initialisation des secrets dans Vault
loadtest/      # scripts de test de charge k6
monitoring/    # config Prometheus + provisioning Grafana (datasource, dashboard, alerte)
```

## Licence

Aucune licence n'est spécifiée dans ce README. Ajouter une licence si le projet doit être distribué publiquement.
