# Arithmetics Service — Specification

## Endpoint

- **SOAP**: `https://ai-maxxing.cc/arithmetics`
- **WSDL**: `https://ai-maxxing.cc/arithmetics?wsdl`
- **Health**: `https://ai-maxxing.cc/health`

> Port 8081 is internal only. Nginx terminates TLS on 443 and proxies to `127.0.0.1:8081`.

## Operations

### add(int a, int b) → int
Returns `a + b`.
Must throw a SOAP Fault if the result overflows a 32-bit signed integer.

### subtract(int a, int b) → int
Returns `a - b`.
Must throw a SOAP Fault if the result overflows a 32-bit signed integer.

### multiply(int a, int b) → int
Returns `a * b`.
Must throw a SOAP Fault if the result overflows a 32-bit signed integer.

### divide(int a, int b) → int
Returns integer division `a / b` (truncated toward zero, e.g. `7 / 2 = 3`).
Must throw a SOAP Fault if `b == 0`.

## Error Handling

All error conditions return a SOAP Fault with:
- `faultcode`: `env:Server`
- `faultstring`: human-readable description of the error

## Integer Bounds

| Bound | Value |
|---|---|
| Minimum | -2 147 483 648 (`Integer.MIN_VALUE`) |
| Maximum |  2 147 483 647 (`Integer.MAX_VALUE`) |

## Implementation Notes

Overflow detection uses Java 21 exact-arithmetic methods:
- `Math.addExact(a, b)` — throws `ArithmeticException` on overflow
- `Math.subtractExact(a, b)` — throws `ArithmeticException` on overflow
- `Math.multiplyExact(a, b)` — throws `ArithmeticException` on overflow

---

## Development

**Prerequisites:** Java 21, Maven 3.x

```bash
# Build fat JAR
mvn -B package --no-transfer-progress

# Run locally
java -jar target/soap-arithmetics-1.0.0.jar
```

The service starts on port `8081`:
- SOAP: `http://localhost:8081/arithmetics`
- WSDL: `http://localhost:8081/arithmetics?wsdl`
- Health: `http://localhost:8081/health`

---

## Server Provisioning (Ansible)

`ansible/bootstrap.yml` fully provisions a fresh Ubuntu droplet in one run:

| Phase | What happens |
|---|---|
| **Java service** | Installs OpenJDK 21, creates `arithmetic-user` system account and `/opt/arithmetics/`, deploys and enables the `arithmetics` systemd unit |
| **Nginx (HTTP)** | Installs Nginx + Certbot, deploys an HTTP-only config that serves the Let's Encrypt ACME challenge at `/.well-known/acme-challenge/` and redirects everything else to HTTPS |
| **Certificate** | Runs `certbot certonly --webroot` to obtain a Let's Encrypt cert for `ai-maxxing.cc` (idempotent — skipped if cert already exists) |
| **Nginx (HTTPS)** | Swaps in the full HTTPS config: TLS on 443 using the obtained cert, proxying all traffic to `127.0.0.1:8081` |
| **Auto-renewal** | Installs a systemd timer that runs `certbot renew` twice daily and reloads Nginx after a successful renewal |

The two-phase Nginx config (HTTP-only first, HTTPS after cert exists) avoids two pitfalls:

- **Missing SSL option files** — `options-ssl-nginx.conf` and `ssl-dhparams.pem` are only created by the `certbot --nginx` plugin, not by `certbot certonly --webroot`. The HTTPS template therefore inlines equivalent SSL settings (`TLSv1.2+`, session cache, HSTS) instead of referencing those files.
- **Handler batching** — Ansible handlers fire at the end of the play, so the final Nginx reload is an explicit task immediately after the HTTPS config is deployed rather than a notified handler, ensuring the reload happens while the cert files are already in place.

**Prerequisite:** the `ai-maxxing.cc` DNS A record must point to the droplet IP before running the playbook — Let's Encrypt validates domain ownership over HTTP.

**Run from the repo root:**

```bash
ansible-playbook ansible/bootstrap.yml -i ansible/inventory.ini --private-key ~/.ssh/your_key
```

The playbook only needs to be run once per server. Subsequent deploys are handled by CI/CD.

**Adding future services:** edit `ansible/templates/nginx.conf.j2` and add a `location /path/ { proxy_pass http://127.0.0.1:<port>; }` block above the catch-all `location /` block, then re-run the playbook.

---

## CI/CD

Pushes to `main` trigger the GitHub Actions workflow (`.github/workflows/ci-cd.yml`):

1. **Build** — compiles the project and produces `target/soap-arithmetics-1.0.0.jar`
2. **Deploy** — copies the JAR to `/opt/arithmetics/soap-arithmetics.jar` on the droplet via `scp`, then restarts the `arithmetics` systemd service and waits for the health endpoint to respond

### Required GitHub Secret

| Secret | Description |
|---|---|
| `DROPLET_SSH_KEY` | Private SSH key authorised to connect as `root` on the droplet |

**One-time setup** — run on the droplet:

```bash
# Generate key pair
ssh-keygen -t ed25519 -f ~/.ssh/deploy_key -N "" -C "github-actions-deploy"

# Authorise the public key
cat ~/.ssh/deploy_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Print the private key — copy this into the GitHub secret
cat ~/.ssh/deploy_key

# Optional: remove the private key from the server
rm ~/.ssh/deploy_key
```

Then in the repository: **Settings → Secrets and variables → Actions → New repository secret**, name `DROPLET_SSH_KEY`, paste the full private key output.

---

## Intentional Defects (Agentic Demo)

The deployed service ships with three logic bugs for the Auditor agent to detect and the Architect agent to fix.

| Operation | Bug | Symptom |
|---|---|---|
| `subtract(a, b)` | Implementation computes `a - 2b` instead of `a - b` | `subtract(10, 3)` returns `4` instead of `7` |
| `multiply(a, b)` | Calls `Math.addExact(a, b)` instead of `Math.multiplyExact(a, b)` — adds instead of multiplying | `multiply(2, 3)` returns `5` instead of `6` |
| `divide(a, b)` | Zero-check is missing — raw `ArithmeticException` propagates instead of a SOAP Fault | `divide(1, 0)` returns an unstructured server error instead of `faultcode: S:Server` |

### Expected Auditor test cases

```
subtract(10, 3)  → expected 7,  actual 4           → FAIL
multiply(2, 3)   → expected 6,  actual 5           → FAIL
divide(1, 0)     → expected SOAP Fault S:Server,
                   actual unhandled exception fault → FAIL
```

### Correct implementations (target state after Architect fix)

```java
// subtract
return Math.subtractExact(a, b);

// multiply
return Math.multiplyExact(a, b);

// divide
if (b == 0) throw soapFault("Division by zero in divide(" + a + ", 0)");
return a / b;
```
