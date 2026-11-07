# Accessibility API declaration (store review forms)

## Short version (≈ 250 characters)

MindHeed is a digital-wellbeing app. Its Accessibility service only detects which app is in front and that a scroll happened in Instagram, to show a gradual veil and a bubble linking to the user's saved posts. It cannot read screen content and collects no data.

## Full version

**Core purpose of the app:** MindHeed helps people scroll Instagram more intentionally. After the user has scrolled past a number of posts they choose, it gradually veils the screen and shows a bubble that opens the user's own saved posts, redirecting them from passive scrolling to content they chose to keep.

**Why the Accessibility API is required:** this is the only way for a third-party app to know, in real time, that the user is scrolling inside another app (Instagram) and to draw an overlay on top of it. No other Android API provides scroll events from another app.

**What the service accesses:**
- the package name of the app in the foreground (`typeWindowStateChanged`);
- the fact that a scroll event happened in Instagram (`typeViewScrolled`).

**What the service does NOT access:** `canRetrieveWindowContent` is set to `false`, so the service cannot read any text, image, message or password on the screen. It performs no actions on the user's behalf: no clicks, no gestures, no typing.

**Data handling:** nothing is collected, stored off the device, or transmitted. There is no account, server, analytics or advertising SDK. Settings stay on the device and are deleted when the app is uninstalled.

**User consent:** before opening the Accessibility settings, the app shows a prominent in-app disclosure explaining exactly what is used, why, and what is never accessed, with "I agree" and "Not now" buttons. The service can be turned off at any time in the system settings.

**isAccessibilityTool:** `false`. MindHeed is not designed for users with disabilities.

---

# Déclaration d'API d'accessibilité (version française)

**Objectif de l'appli :** MindHeed aide à utiliser Instagram de façon plus intentionnelle. Après un nombre de publications défilées choisi par l'utilisateur, l'appli voile progressivement l'écran et affiche une bulle qui ouvre les publications enregistrées de l'utilisateur.

**Pourquoi l'API d'accessibilité :** c'est le seul moyen, pour une appli tierce, de savoir en temps réel que l'utilisateur scrolle dans une autre appli et d'afficher une couche par-dessus.

**Ce que le service utilise :** le nom de l'appli au premier plan, et le fait qu'un scroll a eu lieu dans Instagram.

**Ce qu'il n'utilise pas :** il ne peut lire aucun contenu de l'écran (`canRetrieveWindowContent="false"`) et n'effectue aucune action à la place de l'utilisateur.

**Données :** aucune collecte, aucun envoi, aucun compte, aucun serveur, aucune statistique, aucune publicité.

**Consentement :** une déclaration claire est affichée dans l'appli avant l'activation, avec « J'accepte » et « Plus tard ».
