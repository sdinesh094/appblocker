# App Blocker

A small, self-contained Android app that blocks the apps you choose. Pick the apps,
flip one switch, done. No account, no network access, no ads, no analytics — the app
does not request the INTERNET permission at all, so it cannot phone home even in principle.

Built for a Samsung Galaxy A15 / A16 (Android 14–15), works on anything Android 7.0+.

---

## Part 1 — Get the APK built (about 5 minutes, no software to install)

GitHub will compile the app for you on their servers and hand you the finished `.apk`.

1. **Make a free GitHub account** at [github.com](https://github.com) if you don't have one.

2. **Create a new repository.** Click **+** (top right) → **New repository**.
   - Name it anything, e.g. `app-blocker`
   - Choose **Private** if you prefer
   - Do **not** tick "Add a README file"
   - Click **Create repository**

3. **Upload the project.** On the empty repository page, click the
   **uploading an existing file** link.
   - Unzip `AppBlocker.zip` on your computer first
   - Open the unzipped `AppBlocker` folder, select **everything inside it**
     (including the hidden `.github` folder — see the note below), and drag it
     onto the GitHub upload page
   - Click **Commit changes**

   > **Important:** the `.github` folder is what tells GitHub to build the app.
   > macOS Finder hides folders starting with a dot — press `Cmd + Shift + .`
   > to show them before selecting. On Windows, tick **Hidden items** in the
   > View tab of File Explorer.

4. **Watch it build.** Go to the **Actions** tab. A run called *Build APK* starts
   automatically and takes 3–5 minutes. Green tick = success.

5. **Download the APK.** Click the finished run → scroll to **Artifacts** at the
   bottom → download **AppBlocker-apk**. It arrives as a zip; inside is
   `app-debug.apk`. Copy that file to your phone.

If the build fails, open the run, click the red step, and the error message will
say what went wrong.

---

## Part 2 — Install it on the phone

Sideloading on One UI has two gates that will otherwise look like the app is broken.

1. **Turn off Auto Blocker** (Samsung, One UI 6.1+):
   Settings → **Security and privacy** → **Auto Blocker** → turn it off.
   Leaving it on silently prevents installing apps from outside the Play Store.

2. **Open the APK** from your Files app and allow installation from that source
   when prompted (Settings → Apps → Special access → Install unknown apps).

3. **Allow restricted settings.** Android 13+ greys out the accessibility toggle
   for sideloaded apps. After installing:
   Settings → Apps → **App Blocker** → **⋮** (top right) → **Allow restricted settings**.

   Skip this and step 4 below will simply refuse to turn on, with no explanation.

---

## Part 3 — Set it up

Open **App Blocker**. There are two setup buttons at the top:

1. **Turn on the accessibility service** — takes you to
   Settings → Accessibility → Installed apps → App Blocker → On.
   Samsung shows a warning dialog about full control; that permission is what
   lets the app see which app is in the foreground. Accept it.

2. **Allow display over other apps** — optional but recommended. Without it the
   app can still block, but you'll just be dropped on the home screen with no
   explanation. With it you get the proper "Blocked" screen.

Then tick the apps you want blocked (search for *YouTube*, *Chrome*, etc.) and
turn on **Blocking active** at the top.

The status lines under the switch tell you at a glance whether both permissions
are actually granted.

---

## How it works

`BlockerService` is an `AccessibilityService`. Android tells it the package name
of every window that comes to the foreground. If that package is on your list and
the master switch is on, it calls `performGlobalAction(GLOBAL_ACTION_HOME)` to
push the app off screen and then shows `BlockActivity`.

This is the only approach that works without rooting the phone — there is no
Android API that lets an ordinary app prevent another app from launching.

The launcher, Settings and the system UI are permanently excluded from the
blockable list (`SafetyList.kt`), so you can never lock yourself out of the phone.

## Limits worth knowing

- **The block is not tamper-proof.** You chose a simple toggle, so turning it off
  takes one tap. It defeats habit, not determination.
- **Blocking Chrome does not block websites.** Blocking the Chrome *app* stops
  Chrome opening; it doesn't stop another browser. For site-level blocking, set a
  Private DNS provider (Settings → Connections → More connection settings →
  Private DNS) that filters the domains you want gone.
- **The service stops if you force-stop the app** or if One UI's battery
  optimisation kills it. Settings → Apps → App Blocker → Battery →
  **Unrestricted** avoids that.
- **Debug-signed.** The APK is signed with the standard Android debug key, which
  is fine for personal sideloading. Play Protect may show a "unrecognised app"
  notice on install; choose *Install anyway*.

## Project layout

```
app/src/main/java/com/dinesh/appblocker/
  MainActivity.kt      the UI: master switch, permission status, app picker
  BlockerService.kt    the accessibility service that does the blocking
  BlockActivity.kt     the full-screen "Blocked" panel
  AppListAdapter.kt    RecyclerView adapter for the app list
  Prefs.kt             persisted state (SharedPreferences)
  SafetyList.kt        packages that must never be blockable
.github/workflows/build.yml   the GitHub Actions build
```

Toolchain: Android Gradle Plugin 8.6.1, Gradle 8.9, Kotlin 2.0.21, JDK 17,
compileSdk 34, minSdk 24.
