# Arithmetics Service — Specification

## Endpoint

- **SOAP**: `http://arithmetics.ai-maxxing.cc:8081/arithmetics`
- **WSDL**: `http://arithmetics.ai-maxxing.cc:8081/arithmetics?wsdl`
- **Health**: `http://arithmetics.ai-maxxing.cc:8081/health`

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

The `ansible/bootstrap.yml` playbook provisions a fresh Ubuntu droplet: installs OpenJDK 21, creates the `arithmetic-user` system account, creates `/opt/arithmetics/`, and installs + enables the systemd unit.

**Run from the `ansible/` directory:**

```bash
ansible-playbook bootstrap.yml -i inventory.ini --private-key ~/.ssh/your_key
```

The playbook only needs to be run once per server. Subsequent deploys are handled by CI/CD.

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
