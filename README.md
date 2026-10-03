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

## Profil utilisateur

Les routes suivantes nécessitent un JWT dans l’en-tête `Authorization: Bearer <token>`:

- `GET /api/users/me`: consulter le profil du compte connecté.
- `PATCH /api/users/me`: modifier le nom d’utilisateur et l’email du compte connecté.
- `PUT /api/users/me/password`: changer le mot de passe en fournissant l’ancien et le nouveau.

Le compte est identifié à partir du JWT. Le rôle ne peut pas être modifié par ces routes. Après un changement d’email, reconnectez-vous avec la nouvelle adresse.