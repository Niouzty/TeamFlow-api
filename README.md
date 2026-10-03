# TeamFlow API

Backend Spring Boot minimal de TeamFlow.

## PostgreSQL

Démarrer PostgreSQL avec Docker:

```bash
docker compose up -d postgres
```

Puis lancer l’API:

```bash
# Générer une fois et conserver la valeur dans votre environnement local
export JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run
```

`JWT_SECRET` doit être une clé Base64 représentant au moins 32 octets. Garde cette valeur privée et stable entre les redémarrages ; ne la commite pas. Les paramètres de la base peuvent être remplacés avec `DATABASE_URL`, `DATABASE_USERNAME` et `DATABASE_PASSWORD`.

L’API est disponible sur `http://localhost:8080`.