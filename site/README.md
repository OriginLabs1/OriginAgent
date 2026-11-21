# Origin Agent website

A self-contained static site (HTML/CSS/vanilla JS, no build step, no external CDNs
or trackers). All paths are relative, so `index.html` also works when opened
directly from disk.

## GitHub Pages

1. In the repository settings, set **Settings → Pages → Source** to **GitHub Actions**.
2. The included `.github/workflows/pages.yml` deploys `site/` on every push to
   `main` and can also be run manually with `workflow_dispatch`.
3. Permissions used: `contents: read`, `pages: write`, `id-token: write`.
   No secrets are required.

## Cloudflare Pages

Project name: `origin-agent-kit`. Root config: `wrangler.jsonc` (build output
`./site`).

From the repository root:

```sh
npx wrangler pages deploy site --project-name origin-agent-kit
```

Then attach the custom domain in the Cloudflare dashboard:
**Workers & Pages → origin-agent-kit → Custom domains → Add custom domain →
`origin-agent-kit.coreconcept1.net`**. Cloudflare will prompt for the DNS
records needed; the domain must be in the same Cloudflare account. The domain
binding is a dashboard step — it is not stored in this repository.

For a connected Git integration instead of a direct upload, set the build
command to empty/none and the build output directory to `site`.

> No credentials or API tokens exist in this environment. Authentication for
> either provider must be supplied by the maintainer at deploy time.

## Files

- `index.html` — full marketing/product page
- `styles.css` — responsive styling with `prefers-color-scheme` dark mode
- `script.js` — progressive enhancement (mobile nav, scroll state, reveals)
- `404.html` — friendly not-found page
- `assets/origin-mark.svg` — Origin logo copied from the app documentation
- `.nojekyll` — disables Jekyll processing on GitHub Pages
