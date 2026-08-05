# Legacy Fineract Docker deploy (Contabo)

Production host: `mysql.mopane.co.zw` (SSH key-based as `root`).  
Public URL: `https://fineract.mopane.co.zw`  
Deploy path on server: `/opt/fineract`  
Compose project: `fineract-legacy`

Do **not** modify `mopanecbs-demo` (`/opt/mopane-cbs`) or `mopane-apps` (host MySQL on `:3306`).

## Stack

| Service | Container | Host bind | Notes |
|---------|-----------|-----------|--------|
| MySQL 5.5 | `fineract-legacy-mysql` | `127.0.0.1:3307` → 3306 | Classic Fineract / Flyway-friendly |
| Tomcat 8.5 + Java 8 | `fineract-legacy-app` | `127.0.0.1:8085` → 8080 | Serves `fineract-provider.war` |
| UI nginx | `fineract-legacy-ui` | `127.0.0.1:3000` → 80 | Static community-app from `ui/dist` |

Host nginx (`fineract.mopane.co.zw`):

- `/fineract-provider/` → `http://127.0.0.1:8085/fineract-provider/`
- `/` → `http://127.0.0.1:3000/`

Template: `nginx-fineract.mopane.co.zw.conf` → `/etc/nginx/sites-available/fineract`.

### Important runtime details

- **MySQL 5.5** (not 5.7+/8): looser `DATE` / `sql_mode` behaviour required by older Flyway scripts.
- Classic seed uses **root / mysql** (or values from `.env`) in `tenant_server_connections` with host `localhost`.
- Tomcat entrypoint runs **socat** `127.0.0.1:3306` → `mysql:3306` so those seeded rows work without rewrite.
- `RemoteIpValve` trusts `X-Forwarded-Proto` from host nginx so Spring Security HTTPS checks pass.
- UI nginx returns **404** for missing `.js`/`.css`/etc. (never SPA-fallback HTML for assets).

UI build/deploy steps: sibling repo `mopane-cbs-ui` → `DEPLOY.md`.

## Prerequisites (local)

- JDK 8 + this repo’s Gradle wrapper
- Docker + Docker Compose plugin
- SSH key for `root@mysql.mopane.co.zw`

## 1. Build the WAR

From the repo root:

```bash
./gradlew :fineract-provider:war
cp fineract-provider/build/libs/fineract-provider.war deploy/docker/tomcat/fineract-provider.war
```

The WAR is gitignored; copy it into `deploy/docker/tomcat/` before image build.

## 2. Configure environment

```bash
cd deploy/docker
cp .env.example .env
# Edit .env if needed. Classic installs expect MYSQL_ROOT_PASSWORD=mysql
# and app/tenant credentials aligned with seed data.
```

Never commit `.env`.

## 3. Sync to the VPS

```bash
rsync -az --delete \
  --exclude '.env' \
  --exclude 'ui/dist' \
  -e 'ssh -i ~/.ssh/mopane_vps -o IdentitiesOnly=yes' \
  ./ root@mysql.mopane.co.zw:/opt/fineract/
```

On first deploy, also copy a real `.env` to the server (or create it there from `.env.example`). Ensure the WAR is present under `/opt/fineract/tomcat/` before `docker compose build`.

Scripts must be LF (Unix) line endings:

```bash
ssh -i ~/.ssh/mopane_vps -o IdentitiesOnly=yes root@mysql.mopane.co.zw \
  'chmod +x /opt/fineract/tomcat/entrypoint.sh /opt/fineract/mysql/fixup-tenants.sh'
```

## 4. Build and start

```bash
ssh -i ~/.ssh/mopane_vps -o IdentitiesOnly=yes root@mysql.mopane.co.zw '
  cd /opt/fineract
  docker compose build fineract
  docker compose up -d
  docker compose ps
'
```

Install/reload host nginx from the template if the site is not already wired:

```bash
cp /opt/fineract/nginx-fineract.mopane.co.zw.conf /etc/nginx/sites-available/fineract
nginx -t && systemctl reload nginx
```

## 5. Deploy the UI

Build community-app in `mopane-cbs-ui` with `grunt prod`, then copy `dist/community-app/` contents to `deploy/docker/ui/dist` (and sync to `/opt/fineract/ui/dist` on the VPS; served by `fineract-legacy-ui`). Tenant subdomain resolution (`million.mopane.co.zw` → tenant `million`) lives in that repo’s `app/scripts/initialTasks.js` — see its `DEPLOY.md`.

## Optional: Windows DB restore

1. Stop the app (keep MySQL up):

   ```bash
   cd /opt/fineract && docker compose stop fineract
   ```

2. Restore dumps into the legacy MySQL on **host** `127.0.0.1:3307` (container port 3306):

   ```bash
   mysql -h127.0.0.1 -P3307 -uroot -p"$MYSQL_ROOT_PASSWORD" < tenants.sql
   mysql -h127.0.0.1 -P3307 -uroot -p"$MYSQL_ROOT_PASSWORD" < default-tenant.sql
   ```

3. Prefer leaving `tenant_server_connections.schema_server = localhost` and relying on socat.  
   If you need the compose hostname instead:

   ```bash
   docker compose --profile fixup run --rm db-fixup
   ```

4. Start Tomcat again: `docker compose up -d fineract`.

### Flyway / V344 note

If startup fails on `V344__capitalized_fee_accounting_config.sql`, the migration must insert into `c_configuration.value` as **INT/NULL**, not a string (mode text belongs in `description`). That fix lives in this repo’s migration file — rebuild/redeploy the WAR after pulling it.

## Useful commands

```bash
cd /opt/fineract
docker compose logs -f fineract
docker compose logs -f mysql
docker exec -it fineract-legacy-mysql mysql -uroot -p
curl -sk https://fineract.mopane.co.zw/fineract-provider/api/v1/offices -H 'Fineract-Platform-TenantId: default' -u mifos:password
```

## Layout in this directory

```
deploy/docker/
  docker-compose.yml
  .env.example
  nginx-fineract.mopane.co.zw.conf
  mysql/init/01-databases.sql
  mysql/fixup-tenants.sh          # optional profile "fixup"
  tomcat/Dockerfile
  tomcat/entrypoint.sh            # envsubst + socat + catalina
  tomcat/server.xml.template      # JNDI + RemoteIpValve
  ui/nginx.conf
  ui/dist/                        # deploy built community-app here (gitignored)
```
