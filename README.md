# TrackPack — Système de Gestion de Livraison

> Application de bureau JavaFX pour la gestion complète des colis, livraisons, utilisateurs, véhicules, factures et forum communautaire.

---

## 👥 Équipe de développement

| Module | Développeur |
|---|---|
| 📦 Gestion des Colis & Livraisons | Yosra Jendoubi |
| 🧾 Gestion des Factures | Maissa Yousfi |
| 👤 Gestion des Utilisateurs | Yassine Bargaoui |
| 🚗 Gestion des Véhicules | Rayen Khalfaoui |
| 💬 Gestion du Forum | Mahdi Selmi |

---

## 🏗️ Architecture du projet

```
trackpack/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/gestioncolis/
│       │       ├── controllers/  
│       │       ├── models/     
│       │       ├── services/    
│       │       │   └── ICrud.java  
│       │       └── utils/
│       │           ├── MyConnection.java   # Singleton connexion MySQL
│       └── resources/
│           └── fxml/   
├── pom.xml
├── .gitignore
└── README.md
```

---

## ⚙️ Stack technique

- **Langage** : Java 17+
- **UI** : JavaFX
- **Base de données** : MySQL
- **Build** : Maven
- **Versioning** : Git / GitHub



## 🌿 Organisation des branches Git

```
main        →  Squelette du projet (interfaces, utils, structure de base)
develop     →  Branche d'intégration commune (toutes les features mergées ici)
feature/module-colis-livraison     →  Yosra Jendoubi
feature/module-factures            →  Maissa Yousfi
feature/module-utilisateurs        →  Yassine Bargaoui
feature/module-vehicules           →  Rayen Khalfaoui
feature/module-forum               →  Mahdi Selmi
```





*Projet réalisé dans le cadre d'un travail académique — ESPRIT 2024/2025*
