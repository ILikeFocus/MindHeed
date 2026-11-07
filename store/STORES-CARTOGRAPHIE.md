# Où déployer MindHeed ? Stores et téléphones (recherche du 2026-11-07)

**Parts de marché mondiales, T3 2026** (Omdia, Counterpoint, IDC) :

| Marque | Part |
|---|---|
| Samsung | ~23 % |
| Apple | ~22 % |
| Xiaomi | ~10 % |
| OPPO (y compris realme et OnePlus) | ~9 % |
| vivo | ~8 % |
| Transsion | sorti du top 5 au T3 2026 (4e au T3 2025) |

**Rappel :** l'appli Android (couche par-dessus Instagram) ne fonctionne **que sur Android**. Sur iPhone, voir `../ios/`.

## Stores Android

| Store | Téléphones couverts | Coût développeur | Priorité MindHeed |
|---|---|---|---|
| **Google Play** | Presque tous les Android hors Chine et hors Huawei récents (Samsung, Xiaomi, OPPO, vivo, Motorola, Pixel…) | 25 USD une fois. Compte perso récent : test fermé de 12 testeurs pendant 14 jours. Déclaration Accessibilité obligatoire. | ★★★ Le plus gros public. Lancer le test fermé tôt. |
| **Samsung Galaxy Store** | Samsung (≈ 1 téléphone sur 4 dans le monde) | Gratuit | ★★★ Premier store visé : public énorme, validation plus simple. |
| **Xiaomi GetApps** | Xiaomi, Redmi, POCO | Gratuit (compte Mi développeur) | ★★ |
| **Huawei AppGallery** | Huawei (sans Google depuis 2019), anciens Honor | Gratuit (identité vérifiée) | ★★ Seul moyen d'atteindre les Huawei récents. |
| **OPPO App Market** (plateforme HeyTap / OPPO Open Platform) | OPPO, realme, OnePlus (≈ 9 %) | Gratuit, à vérifier | ★★ À ajouter : même part que Xiaomi. |
| **vivo App Store** (vivo Developer) | vivo, iQOO (≈ 8 %) | Gratuit, à vérifier | ★ Surtout Asie et Inde. |
| **Honor App Market** | Honor depuis 2021 | Gratuit, à vérifier | ★ |
| **Transsion Palm Store** | Tecno, Infinix, itel (Afrique, Asie du Sud) | Partenariat, à vérifier | ☆ Plus tard. |
| **Amazon Appstore** | Tablettes Fire seulement : Amazon a arrêté l'Appstore sur les téléphones Android en 2025 | Gratuit | ✗ Inutile (pas d'Instagram en usage mobile). |
| **APKPure, Uptodown, Aptoide** (stores tiers) | Tout Android (téléchargement manuel) | Gratuit | ☆ Visibilité. Image de marque moins sérieuse. |
| **F-Droid** | Tout Android | Gratuit, open source obligatoire | ☆ Seulement si le code devient open source. |
| **GitHub Releases** (notre canal de test) | Tout Android | Gratuit | ★★★ Tests dès maintenant (APK auto-construit). |

## iPhone

| Canal | Ce que MindHeed peut y faire |
|---|---|
| **App Store** (99 USD par an, Mac nécessaire) | Pas de couche sur l'appli Instagram. Possible : extension Safari (voile sur instagram.com), Screen Time API (blocage ou écran d'interception façon Opal), Vault des enregistrements. |
| **Stores alternatifs UE** (AltStore PAL, etc., grâce au DMA) | Mêmes limites techniques qu'iOS. Ça ne débloque pas la couche par-dessus Instagram. |
| **Safari + Userscripts** (gratuit, aujourd'hui) | Test du voile et de la bulle sur instagram.com : `../ios/mindheed.user.js` |

## À surveiller : vérification des développeurs Android (Google)

- **Calendrier :** depuis le 30 septembre 2026, au Brésil, en Indonésie, à Singapour et en Thaïlande, une appli installée sur un appareil Android certifié doit venir d'un développeur **vérifié** par Google (inscription à 25 USD). Google prévoit d'étendre la règle à tous les appareils certifiés en **2027**.
- **Conséquence :** même pour GetApps, Galaxy Store ou un APK direct, il faudra vérifier son identité auprès de Google.
- **À faire :** créer le compte de vérification en même temps que le compte Play.
- **Pour nos tests :** l'installation par ADB reste possible pour les développeurs.

## Ordre conseillé

1. **Maintenant :** GitHub Releases pour les tests (Android), et Userscripts pour iPhone.
2. **Ensuite :** Galaxy Store, plus le test fermé Google Play lancé en parallèle (14 jours incompressibles).
3. **Puis :** GetApps, OPPO, AppGallery.
4. **Enfin :** vivo, Honor, et App Store iOS (Vault + extension Safari) quand un Mac est disponible.

**Sources :**
- [Omdia T3 2026](https://omdia.tech.informa.com/pr/2026/oct/global-smartphone-shipments-fell-20percent-in-3q26-the-lowest-third-quarter-total-since-2013)
- [Android Headlines T3 2026](https://androidheadlines.com/2026/10/global-smartphone-market-shrank-20-in-q3-2026-samsung-apple-dominate.html)
- [Google, vérification des développeurs](https://developer.android.com/developer-verification)
- [Android Authority, déploiement](https://androidauthority.com/android-sideloading-developer-verification-first-wave-rollout-3717921)
- [Forasoft, distribution hors Play](https://www.forasoft.com/blog/article/distribute-android-app-without-google-play)
- [Wikipédia, stores alternatifs](https://en.wikipedia.org/wiki/Alternative_app_stores)

Les frais des stores OPPO, vivo, Honor et Transsion ne sont pas confirmés par les sources trouvées. Ils sont à vérifier sur leurs portails développeurs.
