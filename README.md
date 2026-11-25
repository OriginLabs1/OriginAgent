# Origin Agent website

A self-contained static site (HTML/CSS/vanilla JS, no build step, no external CDNs
or trackers). All paths are relative, so `index.html` also works when opened
directly from disk.

## Live deployments

- **GitHub Pages:** <https://originlabs1.github.io/OriginAgent/> — Pages is enabled with the
  **GitHub Actions** source, and `.github/workflows/pages.yml` deploys `site/` on every push to
  `main` (also runnable via `workflow_dispatch`). Permissions: `contents: read`, `pages: write`,
  `id-token: write`. No secrets required.
- **Cloudflare Pages:** <https://origin-agent-kit.coreconcept1.net> and
  <https://origin-agent-kit.pages.dev> — project `origin-agent-kit`, production branch `main`,
  direct-upload deployment, output directory `site`. Root config: `wrangler.jsonc`.

## Redeploying to Cloudflare Pages

From the repository root, with `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` in the
environment:

```sh
npx wrangler pages deploy site --project-name origin-agent-kit --branch main
```

The custom domain is bound to the project and the CNAME
`origin-agent-kit.coreconcept1.net → origin-agent-kit.pages.dev` exists in the
`coreconcept1.net` zone. Both are provider-side state, not repository files.

> Never put API tokens in this repository or in these files. Provide credentials at deploy time
> through the environment or your CI secrets store.

## Files

- `index.html` — full marketing/product page
- `styles.css` — responsive styling with `prefers-color-scheme` dark mode
- `script.js` — progressive enhancement (mobile nav, scroll state, reveals)
- `404.html` — friendly not-found page
- `assets/origin-mark.svg` — Origin logo copied from the app documentation
- `.nojekyll` — disables Jekyll processing on GitHub Pages
