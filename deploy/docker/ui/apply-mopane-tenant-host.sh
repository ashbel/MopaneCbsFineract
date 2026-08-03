#!/usr/bin/env bash
# Apply Mopane multi-tenant host resolution to community-app dist and cache-bust loaders.
# Run after copying UI dist into deploy/docker/ui/dist (or on the VPS under /opt/fineract/ui/dist).
#
# Behaviour:
#   million.mopane.co.zw -> Fineract-Platform-TenantId: million
#   pivot.mopane.co.zw   -> pivot
#   fineract/demo/localhost/www/app/platform -> default (override with ?tenantIdentifier=)
set -euo pipefail

DIST_DIR="${1:-}"
if [[ -z "${DIST_DIR}" ]]; then
  DIST_DIR="$(cd "$(dirname "$0")/dist" && pwd)"
fi

SCRIPTS="${DIST_DIR}/scripts"
INDEX="${DIST_DIR}/index.html"
ROUTES_SRC="${SCRIPTS}/routes-initialTasks-webstorage-configuration.2c0e0a4e.js"
COMP_SRC="${SCRIPTS}/mifosXComponents.84557cbb.js"
LOADER_SRC="${SCRIPTS}/loader.aa098775.js"

ROUTES_NEW="${SCRIPTS}/routes-initialTasks-webstorage-configuration.mtenant1.js"
COMP_NEW="${SCRIPTS}/mifosXComponents.mtenant1.js"
LOADER_NEW="${SCRIPTS}/loader.mtenant1.js"

for f in "${ROUTES_SRC}" "${COMP_SRC}" "${LOADER_SRC}" "${INDEX}"; do
  if [[ ! -f "${f}" ]]; then
    echo "Missing required file: ${f}" >&2
    exit 1
  fi
done

export ROUTES_SRC COMP_SRC LOADER_SRC INDEX ROUTES_NEW COMP_NEW LOADER_NEW

python3 <<'PY'
import os
from pathlib import Path

routes = Path(os.environ["ROUTES_SRC"])
text = routes.read_text()

old_blocks = [
"""            // Single-tenant Mopane / local hosts use default; subdomain multi-tenant otherwise.
            if (domains[0]==\"demo\" || domains[0]==\"localhost\" || domains[0]==\"fineract\" ||
                mainLink.hostname.indexOf('mopane.co.zw') >= 0)
            {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = 'default';
              ResourceFactoryProvider.setTenantIdenetifier('default');
            }
            else {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = domains[0];
              ResourceFactoryProvider.setTenantIdenetifier(domains[0]);
            }""",
"""            // Non-tenant hosts use default/query-param; tenant subdomains (million.*, pivot.*) use subdomain.
            var nonTenantHosts = [\"demo\", \"localhost\", \"fineract\", \"www\", \"app\", \"platform\"];
            if (nonTenantHosts.indexOf(domains[0]) >= 0)
            {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = 'default';
              ResourceFactoryProvider.setTenantIdenetifier('default');
            }
            else {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = domains[0];
              ResourceFactoryProvider.setTenantIdenetifier(domains[0]);
            }""",
]

new_block = """            // Tenant subdomains on mopane.co.zw use the subdomain as tenant id.
            // Only true single-host entrypoints fall back to default.
            var nonTenantHosts = [\"demo\", \"localhost\", \"fineract\", \"www\", \"app\", \"platform\"];
            var tenantFromHost = domains[0];
            if (nonTenantHosts.indexOf(tenantFromHost) >= 0) {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = 'default';
              ResourceFactoryProvider.setTenantIdenetifier('default');
            } else {
              $httpProvider.defaults.headers.common['Fineract-Platform-TenantId'] = tenantFromHost;
              ResourceFactoryProvider.setTenantIdenetifier(tenantFromHost);
              console.log(\"Mopane tenant from host\", tenantFromHost);
            }"""

if "Mopane tenant from host" in text and "nonTenantHosts" in text:
    print("routes already patched")
else:
    replaced = False
    for old in old_blocks:
        if old in text:
            text = text.replace(old, new_block, 1)
            replaced = True
            break
    if not replaced:
        raise SystemExit("Could not find tenant-resolution block to patch in routes file")
    routes.write_text(text)
    print("patched", routes)

Path(os.environ["ROUTES_NEW"]).write_text(Path(os.environ["ROUTES_SRC"]).read_text())

comp_text = Path(os.environ["COMP_SRC"]).read_text().replace(
    "routes-initialTasks-webstorage-configuration.2c0e0a4e",
    "routes-initialTasks-webstorage-configuration.mtenant1",
)
Path(os.environ["COMP_SRC"]).write_text(comp_text)
Path(os.environ["COMP_NEW"]).write_text(comp_text)
print("updated components")

loader_text = Path(os.environ["LOADER_SRC"]).read_text().replace(
    "mifosXComponents.84557cbb",
    "mifosXComponents.mtenant1",
)
Path(os.environ["LOADER_SRC"]).write_text(loader_text)
Path(os.environ["LOADER_NEW"]).write_text(loader_text)
print("updated loader")

index = Path(os.environ["INDEX"])
index.write_text(index.read_text().replace("scripts/loader.aa098775.js", "scripts/loader.mtenant1.js"))
print("updated index")
PY

echo "Mopane tenant-host patch applied under ${DIST_DIR}"
