# Forum App - Final README

## 1. Project overview

This project is a desktop forum application built with:

- Java 17
- JavaFX 21 for the user interface
- MySQL for the database
- JDBC for database access
- Maven for dependency management and running the app

The application lets users:

- log in with a role (`ADMIN` or `USER`)
- browse forums
- create, read, update, and delete posts
- create, read, update, and delete comments
- like and unlike posts and comments
- manage forum content from the admin dashboard

## 2. Real architecture used in this project

The current code uses a practical MVC-style structure with a service layer and direct JDBC access.

It is best described like this:

- View = FXML files
- Controller = JavaFX controllers
- Model = entity classes
- Service = business logic + SQL queries
- Database utility = shared JDBC connection helper

Important note:

This project does **not** currently use a separate DAO or Repository package.  
Instead, the SQL queries are written directly inside the service classes.

So the architecture is:

`FXML View -> Controller -> Service -> DataSource -> MySQL`

## 3. Main files and what each one does

### Entry point and app startup

- `src/main/java/com/example/forumapp/Launcher.java`
  - Simple entry point with `main()`.
  - Launches the JavaFX application.

- `src/main/java/com/example/forumapp/ForumApplication.java`
  - Main JavaFX application class.
  - Creates all service objects.
  - Creates the `SceneManager`.
  - Injects services into controllers using a controller factory.
  - Opens the login screen first.

- `src/main/java/module-info.java`
  - Declares required modules:
    - `javafx.controls`
    - `javafx.fxml`
    - `java.sql`
    - `java.net.http` (for OpenAI moderation API calls)

### Views

- `src/main/resources/com/example/forumapp/view/login-view.fxml`
  - Login screen.

- `src/main/resources/com/example/forumapp/view/user-dashboard-view.fxml`
  - Normal user dashboard.

- `src/main/resources/com/example/forumapp/view/admin-dashboard-view.fxml`
  - Admin dashboard.

- `src/main/resources/com/example/forumapp/css/forum-theme.css`
  - UI styling.

### Controllers

- `src/main/java/com/example/forumapp/controllers/LoginController.java`
  - Handles login.
  - Reads email and password from the form.
  - Calls `AuthService.login()`.
  - Redirects to admin or user dashboard depending on role.

- `src/main/java/com/example/forumapp/controllers/UserDashboardController.java`
  - Handles the user screen.
  - Loads forums, posts, and comments.
  - Handles creating, editing, deleting, and liking posts/comments.
  - Uses `ObservableList`, `ListView`, and UI event handlers.

- `src/main/java/com/example/forumapp/controllers/AdminDashboardController.java`
  - Handles the admin screen.
  - Shows statistics.
  - Lets the admin create, update, and delete forums, posts, and comments.

### Services

- `src/main/java/com/example/forumapp/services/AuthService.java`
  - Authentication logic.
  - Finds user by email.
  - Verifies hashed password.
  - Stores the current logged-in user in memory.
  - Checks if the current user is admin or normal user.

- `src/main/java/com/example/forumapp/services/ForumService.java`
  - Forum CRUD logic.
  - Reads forums from database.
  - Creates and updates forums.
  - Deletes forums.
  - Enforces admin-only access for forum management.

- `src/main/java/com/example/forumapp/services/PostService.java`
  - Post CRUD logic.
  - Reads posts by forum.
  - Creates, updates, deletes posts.
  - Handles likes for posts.
  - Checks if the current user is the author or an admin.

- `src/main/java/com/example/forumapp/services/CommentService.java`
  - Comment CRUD logic.
  - Reads comments by post.
  - Creates, updates, deletes comments.
  - Handles likes for comments.
  - Checks if the current user is the author or an admin.

- `src/main/java/com/example/forumapp/services/UserService.java`
  - User management logic.
  - Contains methods for reading, creating, updating, and deleting users.
  - Counts users and likes for the admin stats.

- `src/main/java/com/example/forumapp/services/ModerationService.java`
  - Automatic content moderation via OpenAI API.
  - Checks if text contains inappropriate content.
  - Returns human-readable violation summary in French.
  - Loads API key from `forum.properties`.
  - Uses native Java 11+ `HttpClient` (no external dependencies).

Important note about `UserService`:

The service contains user CRUD methods, but the current admin controller and FXML do **not** expose a user-management screen yet.  
Right now, `AdminDashboardController` uses `UserService` only for statistics like total users and total likes.

### Entities

- `src/main/java/com/example/forumapp/entities/User.java`
- `src/main/java/com/example/forumapp/entities/UserRole.java`
- `src/main/java/com/example/forumapp/entities/Forum.java`
- `src/main/java/com/example/forumapp/entities/Post.java`
- `src/main/java/com/example/forumapp/entities/Comment.java`

These classes are plain Java objects used to represent the application data.

They store values like:

- ids
- titles
- descriptions
- content
- role
- timestamps
- like counts
- linked objects such as author, forum, and post

### Utility classes

- `src/main/java/com/example/forumapp/utils/DataSource.java`
  - Loads database config from `forum.properties`.
  - Opens the JDBC connection.
  - Stores a singleton shared connection.
  - Provides helper methods like generated key extraction and SQL counting.

- `src/main/java/com/example/forumapp/utils/SceneManager.java`
  - Loads FXML screens.
  - Switches between login, user dashboard, and admin dashboard.
  - Applies the CSS file.

- `src/main/java/com/example/forumapp/utils/PasswordUtils.java`
  - Hashes passwords using SHA-256.
  - Compares a raw password with the stored hash.

- `src/main/java/com/example/forumapp/utils/AlertUtils.java`
  - Shows error, information, and confirmation dialogs.

- `src/main/java/com/example/forumapp/utils/DateTimeUtils.java`
  - Formats `LocalDateTime` for display in the UI.

### Database files

- `src/main/resources/forum.properties`
  - Stores database connection settings.

- `src/main/resources/db/forum_schema.sql`
  - Creates the database and tables.
  - Adds sample data for testing.

### Build file

- `pom.xml`
  - Declares JavaFX dependencies
  - Declares MySQL Connector/J dependency
  - Configures Maven compiler and JavaFX Maven plugin

## 4. How database connection works

The database connection flow is simple and centralized.

### Files involved

- `pom.xml`
- `src/main/resources/forum.properties`
- `src/main/java/com/example/forumapp/utils/DataSource.java`
- `src/main/resources/db/forum_schema.sql`

### Step-by-step connection process

#### Step 1: MySQL driver dependency

In `pom.xml`, the project uses:

- `com.mysql:mysql-connector-j`

This is the JDBC driver that allows Java to talk to MySQL.

#### Step 2: Database credentials

In `src/main/resources/forum.properties`, the project stores:

- `db.url`
- `db.user`
- `db.password`
- `openai.api.key` (for moderation service)

Current values point to:

- database name: `forum_fx`
- server: `localhost`
- port: `3306`
- OpenAI API key: must be set to enable auto-moderation

#### Step 3: Loading the properties file

In `DataSource.java`, the constructor uses:

- `Properties`
- `getClass().getClassLoader().getResourceAsStream("forum.properties")`

This reads the values from the resource file at runtime.

#### Step 4: Opening the JDBC connection

Still in `DataSource.java`, the code uses:

- `DriverManager.getConnection(URL, USER, PWD)`

This creates the connection to MySQL.

#### Step 5: Shared singleton access

`DataSource.java` uses a singleton pattern:

- `private static DataSource instance;`
- `public static DataSource getInstance()`

This means the whole app reuses one shared `DataSource` object and one connection.

#### Step 6: Services use the connection

Every service gets the connection with:

- `DataSource.getInstance().getCnx()`

Then the service creates SQL statements with:

- `PreparedStatement`
- `ResultSet`

This is how the app reads and writes data.

## 5. Database schema and table relationships

The schema is defined in:

- `src/main/resources/db/forum_schema.sql`

### Tables

- `users`
- `forums`
- `posts`
- `comments`
- `likes`

### Relationships

- One `forum` has many `posts`
- One `post` has many `comments`
- One `user` can create many `posts`
- One `user` can create many `comments`
- One `user` can like many posts or comments

### Foreign keys

- `posts.forum_id -> forums.id`
- `posts.author_id -> users.id`
- `comments.post_id -> posts.id`
- `comments.author_id -> users.id`
- `likes.user_id -> users.id`

Important detail:

The `likes` table uses:

- `target_type`
- `target_id`

This allows the same table to store likes for both posts and comments.

Because of this design, likes for posts/comments are cleaned manually in the service layer when deleting some data.

## 6. How login and role management work

### Files involved

- `LoginController.java`
- `AuthService.java`
- `User.java`
- `UserRole.java`
- `PasswordUtils.java`

### Login flow

1. The user enters email and password in `login-view.fxml`.
2. `LoginController.handleLogin()` calls `authService.login(email, password)`.
3. `AuthService` searches the user in the `users` table.
4. `AuthService` compares the typed password with the stored hash using `PasswordUtils.matches()`.
5. If login succeeds, the current user is stored in memory.
6. If the role is `ADMIN`, the app opens the admin dashboard.
7. If the role is `USER`, the app opens the user dashboard.

### Role checks

Role checks are done in service methods using:

- `requireUser()`
- `requireAdmin()`

This is important because the controller does not directly trust the UI.  
The service layer also verifies permissions before changing data.

## 7. How CRUD is implemented

CRUD means:

- Create
- Read
- Update
- Delete

In this project, CRUD is mainly implemented inside the service classes with JDBC.

### JDBC tools used for CRUD

The code uses:

- `PreparedStatement` for SQL queries
- `ResultSet` for reading query results
- `Statement.RETURN_GENERATED_KEYS` to get the id after an insert
- `Optional<T>` for safe single-record search
- entity mapping methods like `mapForum()`, `mapPost()`, `mapComment()`, `mapUser()`

### Generic CRUD pattern used in the code

#### Create

The code uses `INSERT INTO ...` SQL.

Example pattern:

1. Build a draft object in the controller.
2. Send it to a service method like `savePost()` or `saveComment()`.
3. If the id is `0`, the service treats it as a new record.
4. The service runs an `INSERT`.
5. The service gets the generated id with `extractGeneratedId()`.
6. The service reloads the saved object with `findById()` and returns it.

#### Read

The code uses `SELECT ...` queries.

Examples:

- `ForumService.getForums()`
- `PostService.getPostsByForum()`
- `CommentService.getCommentsByPost()`
- `AuthService.findByEmail()`

The result is read with `ResultSet`, then converted into entity objects.

#### Update

The code uses `UPDATE ... WHERE id = ?`.

Pattern:

1. The controller sends an object with an existing id.
2. The service checks permissions and validation.
3. The service runs the `UPDATE`.
4. The service reloads the entity from database and returns the updated version.

#### Delete

The code uses `DELETE FROM ... WHERE id = ?`.

Before deleting, the services often:

- check if the record exists
- check permissions
- clean dependent likes if needed

## 8. CRUD by class

### 8.1 Forum CRUD

File:

- `src/main/java/com/example/forumapp/services/ForumService.java`

Methods:

- `getForums(String searchText)` = Read list of forums
- `findById(long forumId)` = Read one forum
- `saveForum(Forum draft)` = Create or Update
- `deleteForum(long forumId)` = Delete

How it works:

- `saveForum()` checks if the current user is admin.
- If `draft.getId() == 0`, it calls `createForum()`.
- Otherwise it calls `updateForum()`.
- Deleting a forum also cleans likes linked to posts and comments inside that forum before deleting the forum row.

### 8.2 Post CRUD

File:

- `src/main/java/com/example/forumapp/services/PostService.java`

Methods:

- `getPostsByForum(long forumId, String searchText, long viewerId)` = Read forum posts
- `getAllPostsForAdmin(long viewerId)` = Read all posts for admin view
- `findById(long postId, long viewerId)` = Read one post
- `savePost(Post draft)` = Create or Update
- `deletePost(long postId)` = Delete
- `toggleLike(long postId)` = Like or unlike a post

How it works:

- Normal users can create posts.
- Only the author or an admin can update or delete a post.
- The method `requireCanManage()` checks that rule.
- The method `canManage()` is used by the UI to decide whether to show Edit/Delete buttons.
- The SQL uses joins with `forums`, `users`, and `likes`.
- Like count is computed with `COUNT(DISTINCT l.id)`.
- Whether the current viewer already liked the post is computed with:
  - `MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END)`

### 8.3 Comment CRUD

File:

- `src/main/java/com/example/forumapp/services/CommentService.java`

Methods:

- `getCommentsByPost(long postId, long viewerId)` = Read comments for a post
- `getAllCommentsForAdmin(long viewerId)` = Read all comments for admin view
- `findById(long commentId, long viewerId)` = Read one comment
- `saveComment(Comment draft)` = Create or Update
- `deleteComment(long commentId)` = Delete
- `toggleLike(long commentId)` = Like or unlike a comment

How it works:

- The logic is almost the same as `PostService`.
- The service checks that the current user is the author or an admin before editing/deleting.
- Likes for a comment are stored in the same `likes` table.

### 8.4 User CRUD

File:

- `src/main/java/com/example/forumapp/services/UserService.java`

Methods:

- `getAllUsers()` = Read all users
- `findById(long userId)` = Read one user
- `saveUser(User draft, String rawPassword)` = Create or Update
- `deleteUser(long userId)` = Delete

How it works:

- Only admins can manage users.
- Email uniqueness is checked in `checkEmailUniqueness()`.
- Passwords are hashed with `PasswordUtils.hash()`.
- The code prevents removing the last admin.
- The code also prevents deleting a user who still owns posts or comments.

Important current state:

These methods exist and are ready at service level, but there is no current user-management tab in `AdminDashboardController` and `admin-dashboard-view.fxml`.

### 8.5 ModerationService (Auto-moderation with OpenAI)

File:

- `src/main/java/com/example/forumapp/services/ModerationService.java`

Methods:

- `isInappropriate(String text)` = Check if content violates OpenAI moderation policy
- `getViolationSummary(String text)` = Get human-readable list of detected violations in French

How it works:

- This service calls the OpenAI moderation API endpoint at `https://api.openai.com/v1/moderations`.
- The API key is loaded from `src/main/resources/forum.properties` with the key `openai.api.key`.
- When a user tries to create or update a post or comment, the service checks the content automatically.
- If the content is flagged, an `IllegalArgumentException` is thrown with the list of violations.
- Network errors are handled silently (fail-open): the post/comment is allowed to be saved if the API is unreachable.

Integration with other services:

- `PostService.savePost()` now calls `moderationService.isInappropriate()` to validate the title and content before saving.
- `CommentService.saveComment()` now calls `moderationService.isInappropriate()` to validate the comment content before saving.
- If content is flagged, the exception is caught by the controller's `safeRun()` method and displayed to the user.

Error handling in controllers:

- `UserDashboardController.safeRun()` catches `RuntimeException` (of which `IllegalArgumentException` is a subclass).
- The error message is displayed via `AlertUtils.showError()`.
- Same pattern is used in `AdminDashboardController`.

Configuration:

- Add your OpenAI API key to `src/main/resources/forum.properties`:
  ```properties
  openai.api.key=sk-proj-YOUR_OPENAI_API_KEY_HERE
  ```

Detected violation categories (translated to French):

- Contenu sexuel
- Contenu haineux
- Harcèlement
- Menaces
- Automutilation
- Intention d'automutilation
- Instructions d'automutilation
- Contenu violent
- Violence graphique

No external dependencies:

- This feature uses `java.net.http.HttpClient` which is part of Java 11+.
- No additional Maven dependencies were added.
- The JSON parsing is done manually using `java.util.regex.Pattern` and `Matcher`.

## 9. How controllers connect the UI to CRUD

### User flow

Files:

- `UserDashboardController.java`
- `user-dashboard-view.fxml`

What the controller does:

- loads forums when the screen opens
- reloads posts when a forum is selected
- reloads comments when a post is selected
- builds a draft `Post` or `Comment` object from form fields
- calls service methods such as:
  - `savePost()`
  - `deletePost()`
  - `saveComment()`
  - `deleteComment()`
  - `toggleLike()`
- refreshes the screen after each action

Important detail:

The controller does not write SQL.  
It only manages the UI and calls the service layer.

### Admin flow

Files:

- `AdminDashboardController.java`
- `admin-dashboard-view.fxml`

What the controller does:

- loads admin statistics
- fills `TableView` components
- loads selected row data into the edit form
- creates draft objects from form fields
- calls the correct service method
- refreshes all tables after create, update, or delete

## 10. What was used in the code to make everything work

### JavaFX

Used for:

- screens
- forms
- buttons
- lists
- tables
- labels
- dialogs
- scene switching

Classes and files used:

- `FXMLLoader`
- `Scene`
- `Stage`
- `ListView`
- `TableView`
- `ObservableList`
- FXML files
- CSS file

### JDBC

Used for:

- connecting Java to MySQL
- sending SQL queries
- reading SQL results

Classes used:

- `Connection`
- `DriverManager`
- `PreparedStatement`
- `ResultSet`
- `Statement`
- `Timestamp`

### MySQL

Used for:

- storing users
- storing forums
- storing posts
- storing comments
- storing likes

### Properties file

Used for:

- keeping database connection settings outside the Java code

Class used:

- `java.util.Properties`

### Password hashing

Used for:

- safer password storage than plain text

Class used:

- `MessageDigest` with `SHA-256`

File used:

- `PasswordUtils.java`

### HTTP Client and API integration

Used for:

- automatic content moderation via OpenAI API
- sending JSON requests to external services
- parsing JSON responses

Classes used:

- `java.net.http.HttpClient`
- `java.net.http.HttpRequest`
- `java.net.http.HttpResponse`
- `java.util.regex.Pattern` and `Matcher` for JSON parsing

File used:

- `ModerationService.java`

## 11. Search, likes, and extra logic

### Search

Search is implemented in SQL using:

- `LOWER(...) LIKE ?`

The text is normalized in service methods with helper code like:

- `trim()`
- `toLowerCase()`

This is used in:

- `ForumService`
- `PostService`

### Likes

Likes are implemented with a toggle system:

1. Check if a like already exists using `countBySql()`.
2. If it exists, delete it.
3. If it does not exist, insert it.

This is done in:

- `PostService.toggleLike()`
- `CommentService.toggleLike()`

### Timestamp mapping

The app reads SQL timestamps and converts them with:

- `DataSource.toLocalDateTime(ResultSet rs, String column)`

The UI then formats them using:

- `DateTimeUtils.format(LocalDateTime)`

## 12. Current strengths of this design

- Simple to understand for a school or learning project
- Clear separation between UI and business logic
- Centralized database connection
- Service methods contain validation and permission checks
- Reusable entity classes
- Good use of `PreparedStatement` to avoid SQL injection problems

## 13. Current limitations of this design

- SQL is inside service classes instead of a dedicated DAO/repository layer
- `DataSource` keeps one shared connection instead of using a connection pool
- Password hashing uses SHA-256, which is better than plain text but weaker than a dedicated password hashing algorithm like BCrypt
- User CRUD exists in service code but is not yet connected to the admin UI

## 14. Full request flow example

Example: creating a new post

1. The user opens `user-dashboard-view.fxml`.
2. `UserDashboardController` reads the title and content fields.
3. The controller creates a `Post` object.
4. The controller calls `postService.savePost(draft)`.
5. `PostService` checks login state and validates the data.
6. **NEW:** `PostService` calls `moderationService.isInappropriate(title + content)`.
7. If content is flagged:
   - `moderationService` calls the OpenAI moderation API
   - An `IllegalArgumentException` is thrown with the list of violations
   - The exception is caught by `UserDashboardController.safeRun()`
   - An error dialog is shown to the user
   - The post is NOT saved
8. If content passes moderation:
   - `PostService` runs an `INSERT INTO posts ...` query with `PreparedStatement`.
   - The generated id is recovered using `Statement.RETURN_GENERATED_KEYS`.
   - The service reloads the post from MySQL.
   - The controller refreshes the `ListView`.
   - The new post appears on the screen.

Example: moderation failure

If a user tries to create a post with inappropriate content:

1. User enters inappropriate content in the title or body.
2. User clicks "Save Post".
3. `UserDashboardController.handleSavePost()` calls `postService.savePost()`.
4. `PostService.savePost()` calls `moderationService.isInappropriate()`.
5. `ModerationService` sends the text to OpenAI API.
6. OpenAI returns: `"flagged": true` with categories like `"violence": true`.
7. `PostService` throws `IllegalArgumentException("Contenu détecté comme inapproprié: contenu violent")`.
8. `UserDashboardController.safeRun()` catches it.
9. `AlertUtils.showError("Forum", "Contenu détecté comme inapproprié: contenu violent")` is called.
10. A dialog appears with the error message.
11. The post is NOT saved to the database.

## 15. How to run the project

1. Start MySQL using XAMPP or another local MySQL server.
2. Execute `src/main/resources/db/forum_schema.sql`.
3. Check `src/main/resources/forum.properties`.
4. Make sure the database name matches `forum_fx`.
5. (Optional) Add your OpenAI API key to `forum.properties` to enable auto-moderation:
   ```properties
   openai.api.key=sk-proj-YOUR_KEY_HERE
   ```
   - Without this key, content moderation will be skipped (fail-open).
6. Run the project with Maven:

```bash
mvn javafx:run
```

## 16. Demo accounts from the SQL script

- Admin:
  - `admin@test.com`
  - `admin123`

- User:
  - `user@test.com`
  - `user123`

## 17. Final summary

This project uses a clean desktop architecture based on JavaFX MVC ideas, a service layer, and JDBC with MySQL.

The most important technical idea is that:

- controllers manage the screens
- services contain the real business logic and SQL
- `DataSource.java` manages the database connection
- entities represent the data
- FXML files represent the views

CRUD is implemented directly inside the service classes with `PreparedStatement`, `ResultSet`, `INSERT`, `SELECT`, `UPDATE`, and `DELETE` queries.

The database connection is handled through `forum.properties` plus the singleton `DataSource`, and the whole app is connected together in `ForumApplication.java`.
