# TeamFlow API

API backend de TeamFlow, une application de gestion collaborative de projets et de tâches.

## Fonctionnalités

- Authentification avec JWT et gestion des profils utilisateur.
- Gestion des projets, de leurs membres et des tâches.
- Dashboard avec aperçu des projets et des tâches.
- Notifications liées aux affectations de tâches.
- Rôles utilisateur et administrateur avec contrôle d’accès.

## Technologies

- Java 17 et Spring Boot 3.
- Spring Data JPA et PostgreSQL.
- JWT et Spring Security.
- Swagger UI / OpenAPI.
- Maven.

## Démarrage local

Prérequis : Java 17, Maven et Docker avec Docker Compose.

1. Démarrer PostgreSQL :

   ```bash
   docker compose up -d postgres
   ```

2. Définir un secret JWT local :

   ```bash
   export JWT_SECRET="$(openssl rand -base64 32)"
   ```

   Conservez cette valeur dans votre environnement local et ne la commitez pas.

3. Démarrer l’API :

   ```bash
   mvn spring-boot:run
   ```

L’API est disponible sur `http://localhost:8080`. La configuration PostgreSQL de développement est définie dans `docker-compose.yml`. Pour utiliser une autre base de données, configurez les variables d’environnement de connexion prises en charge par l’application.

## Documentation de l’API

Avec l’application démarrée :

- Swagger UI : <http://localhost:8080/swagger-ui/index.html>
- Spécification OpenAPI JSON : <http://localhost:8080/v3/api-docs>

Swagger UI décrit les routes, les paramètres, les réponses et l’authentification. Pour essayer les routes protégées, connectez-vous depuis l’API, puis utilisez le bouton **Authorize** avec le jeton JWT reçu.

## Tests

Lancer les tests et la vérification Maven :

```bash
mvn verify
```

Le workflow GitHub Actions exécute également `mvn verify` sur les pull requests et les pushs vers `develop`.
