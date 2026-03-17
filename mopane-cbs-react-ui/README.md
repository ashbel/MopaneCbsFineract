# Mopane CBS React UI

Modern React frontend for Mopane CBS (Apache Fineract). Connects to the Fineract REST API for clients, loans, savings, and other microfinance operations.

## Prerequisites

- Node.js 18+
- npm or yarn
- A running Fineract backend (e.g. `https://localhost:8443`)

## Setup

1. Clone this repository (or copy this folder as its own Git repo for GitHub).
2. Install dependencies:

   ```bash
   npm install
   ```

3. Copy environment variables:

   ```bash
   cp .env.example .env
   ```

4. Edit `.env` and set:
   - `VITE_BASE_API_URL` – Fineract API base URL (e.g. `https://localhost:8443/fineract-provider/api/v1`).
   - `VITE_TENANT_IDENTIFIER` – Tenant identifier (e.g. `default`).

   For local dev with the Vite proxy, you can use:
   - `VITE_BASE_API_URL=/fineract-provider/api/v1` (requests are proxied to `https://localhost:8443`).

## Development

1. Start the Fineract backend (e.g. run the server from the [MopaneCbsFineract](https://github.com/ashbel/MopaneCbsFineract) repo).
2. Start the React dev server:

   ```bash
   npm run dev
   ```

3. Open the URL shown (e.g. `http://localhost:5173`). Log in with your Fineract username and password.

The dev server proxies `/fineract-provider` to `https://localhost:8443` by default (see `vite.config.ts`), so API calls avoid CORS when the backend runs locally.

## Build

```bash
npm run build
```

Output is in the `dist/` directory.

## Deploy with Fineract (optional)

To serve this UI from the same host as the Fineract backend:

1. Build the app: `npm run build`
2. Copy the contents of `dist/` into the Fineract backend repo’s `apps/` directory, e.g.:
   - Backend repo: `MopaneCbsFineract`
   - Create folder: `MopaneCbsFineract/apps/mopane-cbs-react-ui/`
   - Copy all files from this repo’s `dist/` into `apps/mopane-cbs-react-ui/` (so that `index.html` is at `apps/mopane-cbs-react-ui/index.html`).
3. Start the Fineract server. The UI will be available at:
   - `https://<host>:8443/fineract-provider/apps/mopane-cbs-react-ui/index.html?baseApiUrl=https://<host>:8443&tenantIdentifier=default`

You can add `baseApiUrl` and `tenantIdentifier` as query parameters when opening the app; the app uses `VITE_*` env at build time, but query params can override for deployment flexibility.

## Pushing to GitHub as its own repo

This project is intended to live in its own Git repository:

- If you created it inside another repo, copy the `mopane-cbs-react-ui` folder to a new directory, run `git init`, add a remote, and push.
- Or clone the parent repo and use `git subtree split` / filter-branch to publish only this subfolder as a separate GitHub repo.

## Tech stack

- React 19, TypeScript, Vite
- React Router, TanStack Query (React Query), Axios
- CSS (no UI library; minimal custom styles)

## License

Same as the Mopane CBS / Fineract project you use this with.
