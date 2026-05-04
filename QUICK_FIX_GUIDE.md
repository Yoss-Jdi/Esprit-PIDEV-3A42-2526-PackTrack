# 🚀 PLAN DE RÉSOLUTION RAPIDE

## ⚡ En 3 Étapes

### ✅ Étape 1: Exécuter le Script de Correction
Ouvrez PowerShell dans le répertoire projet et exécutez:

```powershell
# Donnez les permissions d'exécution (une seule fois)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser -Force

# Exécutez le script
.\fix_startup.ps1
```

**Ou le script batch (alternative):**
```cmd
extract_resources.bat
```

### ✅ Étape 2: Invalider les Caches de l'IDE (IntelliJ IDEA)
1. Allez à **File → Invalidate Caches / Restart**
2. Cochez:
   - ☑ Clear file system cache and Local History
   - ☑ Clear VCS Log caches and indexes
3. Cliquez **Invalidate and Restart**

### ✅ Étape 3: Lancer l'Application
- Appuyez sur **Shift+F10** (ou Build → Run)
- L'application devrait démarrer sans l'erreur "Location is not set"

---

## 🎯 Ce Qui a Été Corrigé

| Problème | Cause | Solution |
|----------|-------|----------|
| `Location is not set` | FXML charges via chemin invalide | ✓ Chemin complet utilisé `/com/example/rayen/display.fxml` |
| Ressources manquantes | Fichiers `.fxml/.css` pas dans `src/main/resources/` | ✓ Extraire depuis `target/classes/` |
| Classe introuvable | `HelloApplication` chemin incorrect | ✓ Utiliser `getClassLoader().getResource()` |

---

## 📊 État de Déploiement

```
✓ RayenFxApplication.java       - CORRIGÉ
✓ Chemin FXML                    - VALIDE
✓ SceneNavigator.java            - Non touché (sera utilisé après démarrage)
? Ressources compilées          - À extraire via script
? Ancienne application          - Charge display.fxml (véhicules/techniciens)
? Nouvelle application          - Chargera AuthView.fxml si fichiers présents
```

---

## 🔍 Vérification Rapide

Après avoir exécuté le script, vérifiez que les fichiers existent :

```powershell
# Vérifier les ressources extraites
ls src/main/resources/views/*.fxml -ErrorAction SilentlyContinue | Measure-Object | Select-Object Count
ls src/main/resources/fxml/*.fxml -ErrorAction SilentlyContinue | Measure-Object | Select-Object Count
ls src/main/resources/css/*.css -ErrorAction SilentlyContinue | Measure-Object | Select-Object Count
```

**Résultat attendu:** > 0 fichiers dans chaque répertoire

---

## ❓ Troubleshooting

### L'erreur persiste après avoir exécuté le script?

1. **Nettoyez complètement:**
   ```powershell
   .\mvnw.cmd clean
   Remove-Item -Recurse target/
   ```

2. **Invalider caches IDE:** File → Invalidate Caches → Restart

3. **Compilez:**
   ```powershell
   .\mvnw.cmd clean compile
   ```

4. **Exécutez le script de nouveau:**
   ```powershell
   .\fix_startup.ps1
   ```

### Le script dit "Aucun fichier dans views/"?

C'est normal si c'est la première compilation. Les fichiers du `/target/classes/` ne sont créés qu'après une compilation.

**Solution:** Exécutez le script deux fois:
```powershell
# Première fois: compile et genère target/classes
.\fix_startup.ps1
```

### L'application démarre mais affiche une vue vide?

Cela peut être dû à:
- Les fichiers FXML n'ont pas les bons contrôleurs
- Les CSS manquent
- Les chemins d'images sont incorrects

**Solution:** Vérifiez que les contrôleurs correspondent aux fichiers FXML chargés.

---

## 📚 Fichiers de Documentation

- `STARTUP_ERROR_FIX.md` - Analyse détaillée du problème
- `fix_startup.ps1` - Script PowerShell (recommandé)
- `extract_resources.bat` - Script batch (alternative)

---

## 💡 Prochain Travail

Une fois que l'application démarre:

1. ✓ Corrigez les chemins pour charger la bonne première vue
2. ✓ Crée les fichiers FXML manquants (`AuthView.fxml`, etc.) ou restaurez-les depuis Git
3. ✓ Testez la navigation entre les vues
4. ✓ Intégrez la logique métier (AuthController, usagers, etc.)

---

**Besoin d'aide?** Consultez `STARTUP_ERROR_FIX.md` pour une explication détaillée.

