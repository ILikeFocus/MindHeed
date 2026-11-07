# Publier MindHeed sur 4 stores

*Rédigé le 30 octobre 2026. Les règles des stores changent souvent : vérifie chaque point sur le site du store au moment de publier.*

## Avant tout (commun aux 4 stores)

- [ ] L'appli a été testée sur au moins un vrai téléphone (voir le README, section 3).
- [ ] La **politique de confidentialité** est en ligne, à une adresse publique. Tous les stores l'exigent.
  - Le texte est prêt : `store/privacy-policy.html`.
  - Hébergement gratuit : GitHub Pages ou Google Sites.
- [ ] Une **adresse e-mail de contact** pour le support. Il vaut mieux en créer une dédiée à MindHeed.
- [ ] Une **icône 512 × 512 px** (PNG), à exporter depuis `ic_launcher`, ou à faire sur Canva.
- [ ] **3 à 8 captures d'écran** du téléphone (1080 × 1920 conseillé) : l'écran d'accueil, les réglages, le voile et la bulle en action.
  - ⚠️ **Ne montre pas le logo Instagram** et masque les contenus et visages d'autres personnes dans les captures. Les stores refusent souvent pour atteinte à une marque.
- [ ] Une **courte vidéo de démonstration** de l'activation de l'accessibilité et du voile. Google Play l'exige, et elle aide à la validation partout ailleurs.
- [ ] Un **APK signé** pour Samsung, Xiaomi et Huawei, et un **AAB signé** pour Google Play.
- [ ] Les textes de la fiche : `store/listing-fr.md` et `store/listing-en.md`.

### Règles de nom et de marque (tous les stores)

- Ne mets pas « Instagram » dans le **nom** de l'appli. Utilise par exemple « MindHeed – Scrolle moins ».
- Dans la description, tu peux écrire « fonctionne avec Instagram », avec la mention « MindHeed n'est pas affilié à Instagram ni à Meta ».
- N'utilise ni logo ni couleurs Instagram dans l'icône.

### Le point délicat : le service d'accessibilité

MindHeed utilise l'API d'accessibilité **sans être un outil pour les personnes handicapées**. C'est autorisé, mais très surveillé. Ce qui est déjà prêt dans l'appli :

- un écran de **déclaration claire** avant l'activation : ce qui est utilisé, pourquoi, et ce qui ne l'est jamais ;
- un service qui **ne peut pas lire le contenu de l'écran** (`canRetrieveWindowContent="false"`) ;
- `isAccessibilityTool="false"`, ce qui est honnête et attendu par Google Play ;
- aucune donnée collectée, aucun serveur.

Texte à réutiliser dans les formulaires de déclaration : `store/accessibility-declaration.md`.

---

## 1. Samsung Galaxy Store

- **Compte :** Samsung Seller Portal (seller.samsungapps.com). Gratuit.
- **Format :** APK signé.
- **Étapes :**
  1. Crée le compte vendeur (compte Samsung, puis demande de compte vendeur).
  2. « Add New Application » › Android.
  3. Remplis les infos : textes, captures, icône, catégorie **Santé et bien-être** ou **Lifestyle**, URL de la politique de confidentialité, âge.
  4. Envoie l'APK, puis soumets pour validation. Compte quelques jours.
- **Point de vigilance :** les testeurs Samsung vérifient que l'appli fonctionne sur des Galaxy. Mentionne dans les notes de test qu'il faut avoir Instagram installé et activer l'accessibilité.

## 2. Xiaomi GetApps

- **Compte :** Xiaomi Developer Platform. ⚠️ À vérifier au moment de publier : l'ouverture des comptes internationaux et les justificatifs demandés (pièce d'identité, parfois une entreprise) ont varié selon les périodes et les pays.
- **Format :** APK signé.
- **Point de vigilance :** sur Xiaomi, le système tue souvent les services en arrière-plan. L'appli affiche déjà un conseil aux utilisateurs Xiaomi (Démarrage automatique et batterie « Aucune restriction »). Teste sur un Xiaomi avant de soumettre.

## 3. Huawei AppGallery

- **Compte :** AppGallery Connect (developer.huawei.com). Gratuit, avec une **vérification d'identité** (pièce d'identité pour un compte individuel).
- **Format :** APK ou AAB signé.
- **Étapes :** créer l'appli dans AppGallery Connect, remplir la fiche et la politique de confidentialité, choisir les pays de diffusion, envoyer le paquet, soumettre.
- **Point de vigilance :** les téléphones Huawei récents n'ont pas les services Google. MindHeed n'en utilise aucun, donc c'est compatible. Instagram doit cependant être installé sur le téléphone (via AppGallery ou un APK).

## 4. Google Play

- **Compte :** Play Console (play.google.com/console). **25 USD, une seule fois.**
- **Format :** **AAB** signé. Active « Play App Signing » : Google garde une copie de la clé, ce qui te sauve si tu perds la tienne.
- **⚠️ Compte personnel :** les nouveaux comptes personnels doivent d'abord faire un **test fermé avec au moins 12 testeurs pendant 14 jours d'affilée** avant de pouvoir publier en production. Tes répondants de l'étude de marché sont des candidats parfaits.
- **Formulaires à remplir :**
  - **Sécurité des données :** « aucune donnée collectée, aucune donnée partagée ».
  - **Déclaration d'API d'accessibilité :** objectif « bien-être numérique », avec la vidéo de démonstration et le texte de `accessibility-declaration.md`.
  - **Questionnaire de classification du contenu.**
  - **Public cible :** ne coche pas les moins de 13 ans. Une appli « pour enfants » déclenche des règles bien plus strictes.
- **Niveau d'API cible :** l'appli vise Android 16 (API 36). Google relève l'exigence chaque année vers la fin août : vérifie qu'elle est toujours suffisante.

---

## Ordre conseillé

1. **Samsung Galaxy Store** : gratuit, validation plutôt rapide, et c'est le plus gros parc Android hors Google.
2. **Huawei AppGallery** et **Xiaomi GetApps** en parallèle.
3. **Google Play** : lance le **test fermé à 12 testeurs dès maintenant**, puisqu'il faut 14 jours de toute façon.
