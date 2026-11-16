# NESTA landing page

Static information and Android download page. No Node dependencies or backend are required.

## Deploy on Render

Create a Static Site connected to AkhwaleLucy/NESTA-APP, branch main.

- Root Directory: leave blank
- Build Command: `echo "Static site ready"`
- Publish Directory: `website`

The page deliberately keeps APK download unavailable until a signed release exists.

## Add the signed APK

1. Test the signed release APK on a physical Android device.
2. Upload only the APK (never its keystore or passwords) as a GitHub Release asset.
3. Replace the disabled download button in index.html with a link to the exact uploaded asset URL.
4. Update the release availability text, FAQ, version and Android requirements to match the tested APK.
5. Redeploy and check the actual download on Android.

The architectural illustration is illustrative, not an app screenshot or an actual listing. No testimonials, user counts, or property inventory are fabricated.
