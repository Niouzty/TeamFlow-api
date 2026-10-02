# TeamFlow API

Backend Spring Boot minimal de TeamFlow.

## PostgreSQL

Démarrer PostgreSQL avec Docker:

```bash
docker compose up -d postgres
```

Puis lancer l’API:

```bash
mvn spring-boot:run
```

Les valeurs peuvent être remplacées avec `DATABASE_URL`, `DATABASE_USERNAME` et `DATABASE_PASSWORD`.

## Lancer le projet

```bash
mvn spring-boot:run
```

L’API est disponible sur `http://localhost:8080`.

Vérification de l’état:

```bash
curl http://localhost:8080/api/health
```

Réponse attendue: `{"status":"UP"}`.