package com.gestioncolis.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * PhotoManager — gestion des photos de profil.
 *
 * Les images sont copiées dans  src/main/resources/images/profiles/
 * (résolu depuis le dossier de travail courant du projet IntelliJ).
 * En base de données, on enregistre uniquement le chemin RELATIF :
 *   images/profiles/<uuid>.<ext>
 *
 * Pour afficher depuis la DB :
 *   PhotoManager.toUrl("images/profiles/abc.jpg")
 *   → url utilisable dans new Image(url)
 */
public class PhotoManager {

    /** Sous-dossier dans resources/ où les photos sont stockées. */
    private static final String PROFILES_DIR = "images/profiles";

    /**
     * Copie le fichier source dans le dossier de profils du projet,
     * avec un nom unique (UUID + extension d'origine).
     *
     * @param sourceFile  fichier choisi par l'utilisateur
     * @return            chemin relatif à enregistrer en base (ex: images/profiles/uuid.jpg)
     * @throws IOException si la copie échoue
     */
    public static String copyToProject(File sourceFile) throws IOException {
        // ── Résoudre le dossier destination ──────────────────────────────────
        // En développement IntelliJ, le working directory = racine du module Maven.
        // Le dossier resources est donc : src/main/resources/
        Path resourcesDir = resolveResourcesDir();
        Path destDir = resourcesDir.resolve(PROFILES_DIR);
        Files.createDirectories(destDir);          // crée le dossier s'il n'existe pas

        // ── Générer un nom unique ────────────────────────────────────────────
        String ext = getExtension(sourceFile.getName());
        String uniqueName = UUID.randomUUID().toString() + ext;
        Path dest = destDir.resolve(uniqueName);

        // ── Copier ──────────────────────────────────────────────────────────
        Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

        // ── Retourner le chemin RELATIF (séparateur /) ──────────────────────
        return PROFILES_DIR + "/" + uniqueName;
    }

    /**
     * Convertit un chemin relatif stocké en base en URL utilisable par JavaFX Image.
     * Exemple d'appel :
     *   new Image(PhotoManager.toUrl(user.getPhoto()))
     *
     * @param relativePath  chemin relatif tel que stocké en DB
     *                      (ex: "images/profiles/uuid.jpg")
     * @return  URL String, ou null si le chemin est vide/null/fichier introuvable
     */
    public static String toUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;

        // 1) Chercher dans les ressources du classpath (fonctionne en runtime packagé)
        var resource = PhotoManager.class.getResource("/" + relativePath);
        if (resource != null) return resource.toExternalForm();

        // 2) Fallback : chercher dans src/main/resources/ (mode développement)
        try {
            Path resourcesDir = resolveResourcesDir();
            Path file = resourcesDir.resolve(relativePath);
            if (Files.exists(file)) return file.toUri().toString();
        } catch (Exception ignored) {}

        return null;   // fichier introuvable
    }

    /**
     * Même chose que toUrl() mais retourne un javafx.scene.image.Image prêt à l'emploi.
     * Retourne null si aucune image n'est trouvable.
     */
    public static javafx.scene.image.Image loadImage(String relativePath) {
        String url = toUrl(relativePath);
        if (url == null) return null;
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(url, true);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Helpers privés ──────────────────────────────────────────────────────

    /**
     * Résout le dossier src/main/resources/ du projet Maven courant.
     * En IntelliJ, le working directory est la racine du module.
     */
    private static Path resolveResourcesDir() {
        // Essai 1 : src/main/resources (Maven standard)
        Path maven = Paths.get("src", "main", "resources");
        if (Files.isDirectory(maven)) return maven;

        // Essai 2 : resources/ directement (projet simple)
        Path simple = Paths.get("resources");
        if (Files.isDirectory(simple)) return simple;

        // Fallback : créer src/main/resources
        return maven;
    }

    /** Extrait l'extension d'un nom de fichier (avec le point), ex: ".jpg" */
    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot).toLowerCase() : ".jpg";
    }
}
