# Turn on sign-in (free, about 10 minutes)

G2 Dash uses **Supabase** for accounts. The free plan is enough.

1. Go to **supabase.com** → sign up → **New project** (pick a region near you, e.g. Frankfurt).
2. Open **SQL Editor → New query**, paste everything from `supabase.sql`, press **Run**.
3. Open **Authentication → URL Configuration**:
   - **Site URL:** your app link, e.g. `https://g2dash.netlify.app`
   - **Redirect URLs:** add the same link.
4. Open **Project Settings → API** and copy:
   - **Project URL**
   - **anon public** key
5. Open `www/config.js` and paste them:
   ```js
   window.G2_CONFIG = {
     supabaseUrl: "https://xxxx.supabase.co",
     supabaseAnonKey: "eyJhbGciOi..."
   };
   ```
6. Upload the **www** folder to Netlify again.

Now **Settings → Account** lets people create an account, sign in, sync rides and settings,
reset their password, sign out and **delete their account** (this removes the account and all
synced data from Supabase).

Note: never put the **service_role** key in the app — only the **anon** key.
