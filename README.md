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

## Membres de projet

Seul le propriétaire du projet peut gérer ses membres. Toutes les routes nécessitent un JWT.

- `GET /api/projects/{projectId}/members`: lister les membres du projet.
- `POST /api/projects/{projectId}/members`: ajouter un membre avec son email (`{"email":"member@example.com"}`).
- `DELETE /api/projects/{projectId}/members/{memberId}`: retirer un membre du projet.

Un compte doit déjà exister pour être ajouté. Un membre déjà présent renvoie `409 Conflict`; un compte, un projet ou une appartenance introuvable renvoie `404 Not Found`.

Un membre qui a encore des tâches assignées ne peut pas être retiré du projet (`409 Conflict`).

## Tâches

Les tâches sont rattachées à un projet. Toutes les routes nécessitent un JWT et sont limitées au propriétaire et aux membres du projet.

- `POST /api/projects/{projectId}/tasks`: créer une tâche (`title`, `description`, `priority`, `dueDate`).
- `GET /api/projects/{projectId}/tasks`: lister les tâches du projet.
- `GET /api/projects/{projectId}/tasks/{taskId}`: consulter une tâche.
- `PUT /api/projects/{projectId}/tasks/{taskId}`: modifier les détails d’une tâche. Le propriétaire peut modifier toutes les tâches ; un membre ne peut modifier que les tâches qui lui sont assignées.
- `PATCH /api/projects/{projectId}/tasks/{taskId}/status`: modifier le statut (`TODO`, `IN_PROGRESS`, `DONE`).
- `PATCH /api/projects/{projectId}/tasks/{taskId}/assignee`: assigner (`{"userId":2}`) ou désassigner (`{"userId":null}`). Le propriétaire peut choisir un membre du projet ; un membre peut uniquement s’assigner lui-même.
- `DELETE /api/projects/{projectId}/tasks/{taskId}`: supprimer une tâche (propriétaire uniquement).

La priorité accepte `LOW`, `MEDIUM` ou `HIGH`; la date d’échéance est facultative.

## Dashboard

- `GET /api/dashboard`: renvoie les projets dont l’utilisateur connecté est propriétaire ou membre, avec les nombres de tâches par statut et leur progression. La réponse inclut les identifiants des projets pour accéder à leurs tâches via les routes projet.