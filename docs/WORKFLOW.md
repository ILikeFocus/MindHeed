# Workflow MindHeed

```
Idée / retour test ──► Notion (Roadmap & tâches) ──► code ──► GitHub (push)
                                                          │
                          GitHub Actions : tests + APK ◄──┘
                                                          │
             tag vX.Y.Z ──► Release (mindheed.apk + .aab) ──► config/remote-config.json mis à jour
                                                          │
                    téléphones des testeurs ◄── « Nouvelle version disponible »
```

## Les 3 boucles

| Boucle | Quoi | Délai | Besoin d'une nouvelle version ? |
|---|---|---|---|
| **Réglages à distance** | Modifier `config/remote-config.json` sur GitHub : seuils, calibration du feed (`countOnlyClasses`), durée du mode Enregistrés | Les applis le lisent dès qu'il y a du réseau (au plus toutes les 3 h) | Non |
| **Version de test** | `git tag v0.2.1 && git push --tags` | ~5 min de build. Les testeurs reçoivent la proposition de mise à jour. | Oui (APK) |
| **Stores** | Envoyer `mindheed.aab` (Play) ou `mindheed.apk` (Samsung, Xiaomi, Huawei, OPPO) | Validation du store : de 1 à 7 jours | Oui |

Les stores interdisent qu'une appli télécharge son propre code. L'« auto-amélioration » passe donc par la config distante et la mise à jour classique. C'est conforme.

## Mise en route (une fois)

1. **Créer le dépôt** sur github.com : *New repository › `mindheed-android`*.
   - Je le conseille **public** : le fichier de config et les APK doivent pouvoir se télécharger sans compte.
   - Le code est une couche d'affichage, sans secret.
2. **Envoyer le code** depuis `C:\Users\User\Documents\MindHeed-Android` :
   ```bash
   git remote add origin https://github.com/<ton-compte>/mindheed-android.git
   ```
   ```bash
   git push -u origin main
   ```
   La première connexion ouvre une fenêtre de login GitHub.
3. **Première version de test :**
   ```bash
   git tag v0.2.0
   ```
   ```bash
   git push origin v0.2.0
   ```
   Le fichier `mindheed.apk` apparaît ensuite dans *Releases*.
4. **Clé de signature** (avant la 2e version, sinon il faut désinstaller entre chaque version) :
   - Dans Android Studio, *Build › Generate Signed App Bundle › Create new keystore*. Garde le fichier `.jks` et ses mots de passe **hors du dépôt**, avec une sauvegarde : les perdre, c'est ne plus pouvoir mettre à jour sur les stores.
   - Dans GitHub, ajoute les secrets sous *Settings › Secrets and variables › Actions* :
     - `MINDHEED_KEYSTORE_BASE64` (le contenu du `.jks` en base64 : `certutil -encode mindheed.jks out.txt`, sans les lignes BEGIN/END) ;
     - `MINDHEED_KEYSTORE_PASSWORD` ;
     - `MINDHEED_KEY_ALIAS` ;
     - `MINDHEED_KEY_PASSWORD`.

## Installer sur un téléphone Android

Sur le téléphone, ouvre `https://github.com/<ton-compte>/mindheed-android/releases/latest` puis télécharge **mindheed.apk**. Autorise ensuite l'installation depuis le navigateur.

Sur Android 13 et plus : *Paramètres › Applis › MindHeed › ⋮ › Autoriser les paramètres restreints*, avant d'activer l'accessibilité.

## Calibrer « feed principal seulement »

L'appli ne lit pas l'écran. Elle voit seulement le **type de vue** qui défile, affiché dans *Diagnostic › Dernière vue scrollée*. Pour calibrer :

1. Scrolle le feed d'accueil et note la vue affichée.
2. Fais de même sur les Reels, puis sur tes enregistrements.
3. Si la vue du feed d'accueil est différente des autres, mets son nom dans `countOnlyClasses` de la config. Tous les téléphones l'appliquent sans nouvelle version.
4. Si toutes les vues sont identiques, on en reparle : il faudra autoriser la lecture des **identifiants** de vues (pas du texte), avec une mise à jour de la déclaration de confidentialité.

## VPS

Il n'est pas nécessaire pour l'instant : GitHub héberge le code, les APK et la config gratuitement. Le VPS servira au Vault (sauvegardes, clustering, résumés), qui a besoin d'un serveur et d'une base de données.
