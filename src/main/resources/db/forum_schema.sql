-- Nettoyage des tables (ordre respecté pour les clés étrangères)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS forums;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. CRÉATION DES TABLES (Structure exacte demandée)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE forums (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    forum_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    image_path VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_forum FOREIGN KEY (forum_id) REFERENCES forums(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_posts_forum (forum_id),
    INDEX idx_posts_author (author_id)
);

CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_comments_post (post_id),
    INDEX idx_comments_author (author_id),
    INDEX idx_comments_parent (parent_comment_id)
);

CREATE TABLE likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST', 'COMMENT') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_likes UNIQUE (user_id, target_type, target_id),
    INDEX idx_likes_target (target_type, target_id)
);

-- 2. INSERTION DES 7 UTILISATEURS
-- Note: Les mots de passe sont en clair ici pour tes tests, 
-- mais utilise des hashs si ton app utilise Spring Security.
INSERT INTO users (id, nom, email, password, role) VALUES
    (1, 'Admin PackTrack', 'admin@test.com', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),
    (2, 'Ahmed Ben Salem', 'user@test.com', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER'),
    (3, 'Sonia Mansour', 'sonia@test.tn', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER'),
    (4, 'Yassine Khelifi', 'yassine@test.tn', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER'),
    (5, 'Meriem Trabelsi', 'meriem@test.tn', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER'),
    (6, 'Omar Dridi', 'omar@test.tn', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER'),
    (7, 'Leila Ayari', 'leila@test.tn', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'USER');

-- 3. INSERTION DES 3 FORUMS
INSERT INTO forums (id, title, description) VALUES
    (1, 'Suivi & Livraison', 'Tout sur les délais entre gouvernorats et le tracking GPS.'),
    (2, 'Problèmes & Réclamations', 'Retards, colis endommagés ou erreurs de livraison.'),
    (3, 'Conseils & Astuces', 'Bien emballer ses produits et optimiser les frais d''envoi.');

-- 4. INSERTION DES 30 POSTS (10 par forum)
INSERT INTO posts (id, forum_id, author_id, title, content) VALUES
-- Forum 1
(1, 1, 2, 'Délai Tunis vers Gabès', 'Est-ce normal que ça prenne plus de 48h ?'),
(2, 1, 3, 'Tracking bloqué à Sfax', 'Mon colis PT-550 ne bouge plus depuis hier.'),
(3, 1, 1, 'Update : Grand Tunis', 'Nouveau centre de tri ouvert à Ben Arous.'),
(4, 1, 4, 'Livraison Djerba', 'Est-ce que vous livrez à Midoun le samedi ?'),
(5, 1, 5, 'SMS non reçu', 'J''ai validé ma commande mais pas de SMS de confirmation.'),
(6, 1, 6, 'Livreur injoignable', 'Le numéro du livreur sur l''app ne répond pas.'),
(7, 1, 7, 'Tarif Express', 'La différence de prix entre Standard et Express ?'),
(8, 1, 2, 'Colis arrivé à Bizerte', 'Très satisfait de la rapidité, merci !'),
(9, 1, 3, 'Poids maximum autorisé', 'Peut-on envoyer un colis de 30kg via PackTrack ?'),
(10, 1, 4, 'Changement d''adresse', 'Comment modifier l''adresse de destination en cours de route ?'),
-- Forum 2
(11, 2, 5, 'Colis endommagé', 'Le carton est arrivé mouillé à cause de la pluie.'),
(12, 2, 6, 'Livreur impoli', 'Mauvaise expérience avec le livreur ce matin.'),
(13, 2, 7, 'Erreur de colis', 'J''ai reçu une commande qui n''est pas la mienne.'),
(14, 2, 1, 'Guide : Réclamations', 'Comment ouvrir un ticket de support efficacement.'),
(15, 2, 2, 'Remboursement en attente', 'J''attends mon remboursement depuis une semaine.'),
(16, 2, 3, 'Point relais fermé', 'Le point relais de Nabeul est fermé pour travaux.'),
(17, 2, 4, 'Problème de paiement', 'Le paiement par carte a échoué sur l''interface.'),
(18, 2, 5, 'Article manquant', 'Il manque un article dans mon colis scellé.'),
(19, 2, 6, 'Retard de 5 jours', 'Mon colis est bloqué au centre de tri de Sousse.'),
(20, 2, 7, 'Vendeur Marketplace', 'Le vendeur n''a toujours pas déposé le colis.'),
-- Forum 3
(21, 3, 2, 'Emballage Dattes', 'Conseils pour envoyer du Deglet Nour vers l''étranger.'),
(22, 3, 3, 'Protection High-Tech', 'Comment protéger un PC portable pour le transport ?'),
(23, 3, 4, 'Tarifs étudiants', 'Existe-t-il des remises pour les étudiants ?'),
(24, 3, 5, 'Envoi Huile d''olive', 'Est-il possible d''envoyer des bidons de 5L ?'),
(25, 3, 6, 'Astuce : Étiquetage', 'Mettez toujours le numéro de téléphone en gros !'),
(26, 3, 7, 'Assurance Colis', 'Est-ce que l''assurance couvre les objets fragiles ?'),
(27, 3, 1, 'Bonnes pratiques', 'Comment scotcher votre carton efficacement.'),
(28, 3, 2, 'Envoi à Kairouan', 'Meilleur créneau horaire pour les livraisons en centre-ville.'),
(29, 3, 3, 'Taille des cartons', 'Où trouver des cartons standard pas chers ?'),
(30, 3, 4, 'Preuve de livraison', 'L''importance de garder le reçu signé.');

-- 5. INSERTION DES 64 COMMENTAIRES (Sélection stratégique)
INSERT INTO comments (id, post_id, author_id, content) VALUES
(1, 1, 1, 'Bonjour Ahmed, vérifiez vos appels, le livreur a essayé de vous joindre.'),
(2, 1, 2, 'Merci, je vais vérifier mon journal d''appels.'),
(3, 2, 1, 'Sonia, il y a un léger retard à Sfax cause météo.'),
(4, 11, 1, 'Désolé, envoyez-nous une photo du carton en MP.'),
(5, 21, 7, 'Il faut des boites en plastique hermétiques pour les dattes !');

-- Remplissage automatique pour atteindre 64 commentaires
INSERT INTO comments (post_id, author_id, content) 
SELECT id, 1, 'L''équipe PackTrack s''occupe de votre demande.' FROM posts WHERE id BETWEEN 1 AND 30;
INSERT INTO comments (post_id, author_id, content) 
SELECT id, 2, 'Merci pour cette information utile !' FROM posts WHERE id BETWEEN 1 AND 29;

-- 6. INSERTION DES LIKES
INSERT INTO likes (user_id, target_type, target_id) VALUES
(1, 'POST', 1), (2, 'POST', 3), (3, 'POST', 3), (4, 'COMMENT', 1), (5, 'COMMENT', 5);

-- 7. RÉGLAGE DES COMPTEURS (Crucial pour ton application Java)
ALTER TABLE users AUTO_INCREMENT = 100;
ALTER TABLE forums AUTO_INCREMENT = 100;
ALTER TABLE posts AUTO_INCREMENT = 1000;
ALTER TABLE comments AUTO_INCREMENT = 5000;
ALTER TABLE likes AUTO_INCREMENT = 5000;