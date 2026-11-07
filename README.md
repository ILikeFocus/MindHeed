# MindHeed — application Android

La version téléphone de l'extension Chrome Scrollful/SocialBit. MindHeed s'installe **par-dessus la vraie appli Instagram** :

1. Les premières publications restent nettes (3 par défaut, comme l'extension).
2. Ensuite, publication après publication, l'écran se voile : vrai flou sur Android 12+ quand le téléphone le permet, assombrissement sinon. Il atteint son maximum après 9 publications de plus par défaut, avec la même courbe que l'extension.
3. À 55 %, la bulle étoile de l'extension apparaît. À 80 %, elle se met à bouger.
4. Toucher la bulle ouvre tes publications enregistrées. Le voile disparaît alors pendant 5 minutes et le compteur repart de zéro.
5. Quitter Instagram plus de 10 minutes compte comme une vraie pause et remet le compteur à zéro.

**Comment les publications sont comptées :** MindHeed ne lit jamais l'écran, il ne « voit » donc pas les posts. Il compte les **gestes de scroll**. Android envoie une rafale de signaux à chaque swipe, et une pause de plus de 0,4 s marque un nouveau geste. Sur le feed, un swipe fait avancer d'environ une publication ; dans les Reels, un swipe correspond à exactement un reel.
- Les swipes vers le haut et le défilement des photos d'un carrousel ne comptent pas.
- Un second compteur, les éléments de liste parcourus, est affiché dans l'appli pour la calibration (voir la section 3).

Le flou couvre tout l'écran, pas publication par publication.

---

## 1. Ouvrir le projet

1. Installe **Android Studio** depuis https://developer.android.com/studio (gratuit).
2. Au premier lancement, laisse-le installer le SDK Android (installation « Standard »).
3. `File › Open`, puis choisis ce dossier : `C:\Users\User\Documents\MindHeed-Android`.
4. Android Studio télécharge Gradle et les dépendances. La première fois, ça prend plusieurs minutes.
   - S'il propose une **mise à jour de l'Android Gradle Plugin** (« AGP Upgrade Assistant »), accepte.
   - S'il signale une erreur de version, copie-la-moi.

> Le projet est volontairement **hors de OneDrive** : OneDrive verrouille les fichiers pendant les compilations et casse Gradle.

## 2. Tester sur ton téléphone

1. Sur le téléphone, active les **Options pour les développeurs** : Paramètres › À propos du téléphone › touche 7 fois « Numéro de build » (sur Xiaomi : « Version de MIUI/HyperOS »).
2. Dans les Options pour les développeurs, active le **Débogage USB**.
3. Branche le téléphone en USB, accepte l'autorisation, puis clique sur ▶ **Run** dans Android Studio.
4. Dans MindHeed : **Activer MindHeed** › J'accepte › Accessibilité › MindHeed › Activer.
   - Sur Android 13+, si l'activation est grisée (« paramètre restreint ») : Paramètres › Applications › MindHeed › menu ⋮ › **Autoriser les paramètres restreints**. Ça n'arrive que pour les applis installées hors store.
5. Entre ton nom d'utilisateur Instagram.
6. **Pour tester vite :** mets « Publications nettes » à 0 et la montée à 3, puis scrolle sur Instagram.

Pour lancer les tests unitaires : clic droit sur `app/src/test` › **Run tests**.

## 3. À vérifier sur un vrai téléphone (pas encore testé)

Le code n'a pas encore été compilé ni testé : il a été écrit sans Android Studio sur la machine.

- [ ] Le projet compile, et les tests unitaires passent.
- [ ] Les scrolls dans Instagram sont bien détectés : feed, Reels, Explore.
- [ ] **Calibration du comptage :**
  1. Va sur ton feed, et fais défiler exactement 10 publications.
  2. Reviens dans MindHeed et regarde la ligne « Session en cours ».
     - Si le nombre de **gestes** est proche de 10, le comptage par swipe est bon.
     - Si ce sont les **éléments parcourus** qui sont proches de 10 (ou un multiple stable, par exemple 20 ou 30), on basculera sur ce compteur, plus précis.
  3. Refais le test dans les Reels.
  4. Envoie-moi les chiffres.
- [ ] Le **flou réel** apparaît sur Android 12+. Sur certains téléphones, le flou entre fenêtres est désactivé (économie de batterie, Samsung en mode économie) : on aura alors seulement l'assombrissement.
- [ ] On peut toujours scroller et toucher Instagram sous le voile.
- [ ] La bulle ouvre bien la page « Enregistrés » dans l'appli Instagram. Si Instagram ouvre seulement le profil, il faudra trouver un autre lien.
- [ ] Xiaomi : le service survit en arrière-plan après avoir activé Démarrage automatique et « Aucune restriction » de batterie.
- [ ] Les réglages par défaut (3 publications nettes, 9 de montée, intensité 70 %) sont agréables à l'usage. Sur téléphone, on scrolle vite : 12 publications avant le voile maximal, c'est peut-être trop agressif pour le grand public.

## 4. Créer la version pour les stores

1. `Build › Generate Signed App Bundle or APK`.
2. Crée une **clé de signature** (keystore). **Garde le fichier et les mots de passe en lieu sûr, hors de OneDrive public, et fais une sauvegarde.** Sans cette clé, tu ne pourras plus jamais mettre l'appli à jour sur Samsung, Xiaomi ou Huawei.
3. Génère :
   - un **APK** signé (release) pour Samsung Galaxy Store, Xiaomi GetApps et Huawei AppGallery ;
   - un **App Bundle (AAB)** signé pour Google Play.
4. Avant chaque nouvelle version, augmente `versionCode` (et `versionName`) dans `app/build.gradle.kts`.

La publication store par store est détaillée dans `store/STORES.md`.

## Structure

```
app/src/main/java/com/mindheed/app/
├── MindHeedService.kt    service d'accessibilité : appli au premier plan + scrolls Instagram
├── ScrollTracker.kt      gestes de scroll (≈ publications) → progression 0..1 (même formule que l'extension)
├── OverlayController.kt  le voile (flou + assombrissement) et la bulle étoile
├── MainActivity.kt       écran d'accueil, déclaration d'accessibilité, réglages
├── Instagram.kt          ouverture des publications enregistrées
└── Prefs.kt              réglages stockés sur le téléphone
app/src/main/res/xml/accessibility_service_config.xml   ce que le service a le droit de voir
store/                    fiches des stores, politique de confidentialité, checklist
```
