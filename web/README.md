# PeytzPvP web

Marketing site for the PeytzPvP Hardcore Games plugin. Built with Astro (static).

## Local dev

```bash
cd web
npm install
npm run dev
```

Then open http://localhost:4321.

## Build

```bash
npm run build      # output: web/dist
npm run preview    # serve the built site locally
```

## Deploy to Vercel

The site is a plain Astro static build — no SSR adapter required.

1. Push this repo to GitHub.
2. Go to https://vercel.com/new and import the repo.
3. **Important:** in the Vercel project settings, set:
   - **Root Directory:** `web`
   - **Framework Preset:** Astro (auto-detected once the root is set)
   - Build command: `npm run build`
   - Output directory: `dist`
4. Deploy. Vercel will give you a `*.vercel.app` URL.

## Point peytzpvp.com (Simply DNS) at Vercel

In the Vercel project, open **Settings → Domains** and add both:

- `peytzpvp.com`
- `www.peytzpvp.com`

Vercel will show you the exact records it wants. With Simply as DNS provider,
log in to https://www.simply.com → your domain → DNS records, and add:

| Type  | Host  | Value                  | TTL  |
| ----- | ----- | ---------------------- | ---- |
| A     | `@`   | `76.76.21.21`          | 3600 |
| CNAME | `www` | `cname.vercel-dns.com` | 3600 |

Notes:

- Delete any existing `A` record on `@` and any `CNAME` on `www` first.
- Simply sometimes auto-adds a default `A` record — remove it.
- DNS propagation usually takes a few minutes; can be up to an hour.
- Once propagated, Vercel auto-issues a Let's Encrypt cert for both hostnames.
- In Vercel **Domains**, mark `peytzpvp.com` as the primary and 308-redirect
  `www.peytzpvp.com` to it (or the other way around — your call).

## Project layout

```
web/
├── public/          # static assets served as-is
│   └── favicon.svg
├── src/
│   ├── layouts/Layout.astro
│   ├── pages/index.astro
│   └── styles/global.css
├── astro.config.mjs
├── package.json
└── tsconfig.json
```
