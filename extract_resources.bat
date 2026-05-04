@echo off
REM Script pour extraire les fichiers FXML compilés et les copier aux sources
REM Cela permet à l'application de fonctionner avec la nouvelle interface (AuthView, etc.)

setlocal enabledelayedexpansion

echo ========================================================
echo Extraction des ressources compilees
echo ========================================================

set SOURCE_DIR=target\classes
set TARGET_DIR=src\main\resources

REM Créer les répertoires s'ils n'existent pas
if not exist "%TARGET_DIR%\views" mkdir "%TARGET_DIR%\views"
if not exist "%TARGET_DIR%\fxml" mkdir "%TARGET_DIR%\fxml"
if not exist "%TARGET_DIR%\css" mkdir "%TARGET_DIR%\css"
if not exist "%TARGET_DIR%\images" mkdir "%TARGET_DIR%\images"

REM Copier les fichiers FXML
echo.
echo [1] Copie des fichiers FXML...
if exist "%SOURCE_DIR%\views" (
    xcopy "%SOURCE_DIR%\views\*.fxml" "%TARGET_DIR%\views\" /Y /Q
    echo  OK - views/*.fxml
) else (
    echo  SKIP - target/classes/views/ n'existe pas
)

if exist "%SOURCE_DIR%\fxml" (
    xcopy "%SOURCE_DIR%\fxml\*.fxml" "%TARGET_DIR%\fxml\" /Y /Q
    echo  OK - fxml/*.fxml
) else (
    echo  SKIP - target/classes/fxml/ n'existe pas
)

REM Copier les fichiers CSS
echo.
echo [2] Copie des fichiers CSS...
if exist "%SOURCE_DIR%\css" (
    xcopy "%SOURCE_DIR%\css\*.css" "%TARGET_DIR%\css\" /Y /Q
    echo  OK - css/*.css
) else (
    echo  SKIP - target/classes/css/ n'existe pas
)

REM Copier les images
echo.
echo [3] Copie des images...
if exist "%SOURCE_DIR%\images" (
    xcopy "%SOURCE_DIR%\images\*.*" "%TARGET_DIR%\images\" /Y /Q /S /E
    echo  OK - images/**/*.*
) else (
    echo  SKIP - target/classes/images/ n'existe pas
)

echo.
echo ========================================================
echo Extraction terminee!
echo Les fichiers resources ont ete copies vers src/main/resources/
echo Vous pouvez maintenant editer RayenFxApplication.java pour pointer
echo vers les fichiers des nouvelles vues (AuthView.fxml, etc.)
echo ========================================================
echo.

pause

