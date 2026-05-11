# Forum JavaFX MVC

Module desktop JavaFX pour forum avec architecture MVC, persistance MySQL via XAMPP, gestion des roles USER/ADMIN, CRUD complet et synchronisation temps reel par WebSocket.

## Architecture

### Packages

- `com.example.forumapp.config` : chargement des properties, connexion JDBC, bootstrap des services et creation des controllers.
- `com.example.forumapp.model` : entites `User`, `Forum`, `Post`, `Comment`, `LikeRecord`, enums de roles et events temps reel.
- `com.example.forumapp.repository` : DAO JDBC responsables des requetes SQL et mappings ResultSet.
- `com.example.forumapp.service` : logique metier, validations, droits d acces, orchestration CRUD et publication des events WebSocket.
- `com.example.forumapp.session` : session utilisateur courante.
- `com.example.forumapp.realtime` : client WebSocket, coordinateur de notifications et serveur de diffusion minimal.
- `com.example.forumapp.controller` : controllers JavaFX des interfaces Login, User et Admin.
- `com.example.forumapp.navigation` : chargement des scenes FXML et cycle de vie des ecrans.
- `com.example.forumapp.util` : hash de mot de passe, alertes UI, formatage date/heure.

### Repartition MVC

- Model : entites Java simples dans `model`.
- View : fichiers FXML dans `src/main/resources/com/example/forumapp/view` et theme CSS dans `css/forum-theme.css`.
- Controller : logique d interaction JavaFX dans `controller`.
- Service : validations, autorisations et cas d usage.
- Repository : acces base de donnees JDBC.

## Gestion des roles

- `USER` : consulter, rechercher, publier, commenter, liker, modifier ou supprimer uniquement ses propres posts/commentaires.
- `ADMIN` : gerer forums, posts, commentaires, utilisateurs et consulter le tableau de bord des statistiques.
- Les controles sont appliques dans trois couches : UI, services, et verification supplementaire dans la logique applicative.

## Base de donnees MySQL

- Script complet : `src/main/resources/db/forum_schema.sql`
- Tables : `users`, `forums`, `posts`, `comments`, `likes`
- Comptes inclus :
  - `admin@test.com / admin123`
  - `user@test.com / user123`
- Les mots de passe de demo sont stockes en SHA-256 pour la demonstration locale.

## Temps reel WebSocket

Le desktop utilise `ForumRealtimeCoordinator` pour deux usages :

1. diffuser les mises a jour localement dans l instance courante sans recharger la scene ;
2. relayer les memes events vers un serveur WebSocket afin de synchroniser plusieurs clients ouverts en meme temps.

### Classes clefs

- `ForumRealtimeCoordinator` : abonnement des controllers et diffusion d events.
- `ForumWebSocketClient` : client WebSocket Java connecte a `ws://localhost:8090/forum`.
- `ForumRealtimeServer` : petit serveur broadcast a lancer si vous voulez une synchro multi-clients.

### Flux

1. un CRUD ou un like est execute dans un service ;
2. le service publie un `RealtimeEvent` ;
3. le controller recharge uniquement la partie utile de l ecran ;
4. si le serveur WebSocket tourne, les autres clients recoivent le meme event sans refresh manuel.

## Lancement

### Pre-requis

- XAMPP avec MySQL demarre.
- `JAVA_HOME` configure vers votre JDK.
- Maven Wrapper disponible via `mvnw.cmd`.

### Etapes

1. Importer `src/main/resources/db/forum_schema.sql` dans MySQL/XAMPP.
2. Verifier `src/main/resources/forum.properties` si vous utilisez un autre host, user ou mot de passe.
3. Optionnel : lancer `com.example.forumapp.realtime.ForumRealtimeServer` pour la synchro temps reel entre plusieurs clients.
4. Lancer l application : `mvn javafx:run`

## Interfaces livrees

- Login : authentification et acces direct a l interface correspondant au role.
- User : forums, recherche, posts, commentaires, likes et actions conditionnees par le proprietaire.
- Admin : dashboard statistique, CRUD forums/posts/commentaires/utilisateurs.

## Notes techniques

- La suppression de posts, commentaires et forums nettoie aussi les likes dependants pour eviter les donnees orphelines.
- La suppression d un utilisateur est volontairement refusee s il possede encore des posts/commentaires, afin d eviter un effacement transversal non maitrise.
- Les scenes sont chargees via `SceneManager`, ce qui permet d injecter les services directement dans les controllers.