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

## Projets

Toutes les routes projet nécessitent un JWT. Un utilisateur ne peut consulter, modifier ou supprimer que les projets dont il est propriétaire.

- `POST /api/projects`: créer un projet (`name`, `description`).
- `GET /api/projects`: lister ses projets.
- `GET /api/projects/{projectId}`: consulter un projet.
- `PUT /api/projects/{projectId}`: remplacer son nom et sa description.
- `DELETE /api/projects/{projectId}`: supprimer un projet.

La gestion des membres et des tâches sera ajoutée dans des fonctionnalités distinctes.