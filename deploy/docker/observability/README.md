# Fineract prod observability

Stack on **mopane-prod** only: Prometheus, Loki, Promtail, Grafana, cAdvisor, node-exporter, blackbox.

- Grafana URL: `https://grafana-prod.mopane.co.zw`
- Prometheus (localhost): `http://127.0.0.1:9090`
- Alerts: Grafana → **Alerting** (add Contact points later for Slack/Discord/email)

## DNS

Create A record:

```text
grafana-prod.mopane.co.zw  →  169.58.144.19
```

## Deploy

```bash
# from laptop (repo deploy/docker/observability)
rsync -az --delete \
  --exclude '.env' \
  -e 'ssh -o IdentitiesOnly=yes' \
  ./ mopane-prod:/opt/fineract/observability/

ssh mopane-prod '
  mkdir -p /var/lib/node_exporter/textfile
  cd /opt/fineract/observability
  test -f .env || cp .env.example .env
  # edit .env and set a strong GF_SECURITY_ADMIN_PASSWORD
  docker compose up -d
  cp nginx-grafana-prod.mopane.co.zw.conf /etc/nginx/sites-available/grafana-prod
  ln -sfn /etc/nginx/sites-available/grafana-prod /etc/nginx/sites-enabled/grafana-prod
  nginx -t && systemctl reload nginx
'
```

Login: admin user/password from `/opt/fineract/observability/.env`.

## Backup metrics

Nightly GDrive backup writes Prometheus textfile metrics to:

`/var/lib/node_exporter/textfile/fineract_backup.prom`

Scraped by node-exporter. Dashboard panel + `BackupFailedOrStale` alert use these.

## Adding notification channels later

In Grafana → **Alerting** → **Contact points** → add Slack / Discord / webhook / email, then attach a **Notification policy** to the `fineract-ops` rules (labels `service=fineract` etc.).

## Probes

- **HTTPS UI:** blackbox → `https://million.mopane.co.zw/` and `https://pivot.mopane.co.zw/`
- **Public API:** blackbox → `/fineract-provider/api/v1/offices` on each tenant (401/400 = up)

Prometheus/Loki/cAdvisor stay on localhost only (not proxied).

## Logs (Loki)

Promtail ships **all** Docker logs for `fineract-legacy-*` and `fineract-obs-*` (stdout + stderr). Log level is extracted when present (`level=INFO|WARN|ERROR|...`).

In Grafana → **Explore** → Loki:

```logql
# All app logs
{container="fineract-legacy-app"}

# Errors only (also on the Fineract Ops dashboard)
{container="fineract-legacy-app", level=~"(?i)error|fatal"}

# Filter by text
{container="fineract-legacy-app"} |= "repayment"

# Other containers
{container=~"fineract-legacy-.*"}
```

Retention: 7 days (`loki-config.yml` → `retention_period: 168h`).

## Useful checks

```bash
cd /opt/fineract/observability
docker compose ps
curl -sG 'http://127.0.0.1:9090/api/v1/query' --data-urlencode 'query=probe_success'
curl -sk -o /dev/null -w "%{http_code}\n" https://127.0.0.1/login -H 'Host: grafana-prod.mopane.co.zw'
curl -sk -o /dev/null -w "%{http_code}\n" https://grafana-prod.mopane.co.zw/login
# recent log volume (should be non-zero for all levels, not only ERROR)
curl -sG 'http://127.0.0.1:3100/loki/api/v1/query' \
  --data-urlencode 'query=sum by (level) (count_over_time({container="fineract-legacy-app"}[15m]))'
```
