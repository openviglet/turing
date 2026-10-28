---
title: "Authentication"
description: "Native authentication for Viglet Turing, session-based admin console login and API Key authentication for the REST API."
---
Viglet Turing supports three authentication modes:

| Mode | Best for | External dependency |
|---|---|---|
| **Native** (default) | Development, simple setups | None |
| **Social Login (OAuth2)** | Small teams, GitHub/Google/Microsoft accounts | Provider OAuth App |
| **Keycloak SSO** | Enterprise production with LDAP/AD, MFA, RBAC | Keycloak server |

This page covers **native authentication**. For social login with Google, GitHub, or Microsoft, see [Social Login (OAuth2)](/docs/security-social-login/). For the full Keycloak production setup, see [Security & Keycloak](/docs/security-keycloak/).

---

## Native Authentication (default)

When Keycloak is not enabled, Turing uses its own user store:

### Admin Console

Users log in via a form at `http://localhost:2700/login` and receive a **Java HTTP session**. Sessions are maintained server-side and expire after inactivity. No special configuration is required: this mode works out of the box.

Users, groups, and roles are managed in **Administration → Users**, **Groups**, and **Roles**.

### REST API: API Key

All REST API requests authenticate via an **API Key** passed in the **`Key` request header** (never a query parameter, see the note below). API Tokens are created in **Administration → API Tokens**.

```bash
curl -H "Key: <your-token>" https://your-host/api/sn/mysite/search?q=hello
```

**Creating an API Token:**

1. Sign in to the Administration Console.
2. Navigate to **Administration → API Tokens**.
3. Click **New**, fill in a name and description (optionally set an expiry).
4. Copy the generated token immediately: it will not be shown again.

:::caution[`?apiKey=` query parameter removed]
Passing the token as a `?apiKey=` URL query parameter is **no longer supported**, a secret in the URL leaks into access logs, `Referer` headers and browser history. Always send it in the `Key` header. This affects the public Semantic Navigation endpoints on API-key-mode sites and the MCP server. Tokens also now support **expiry and revocation** (`enabled` flag / `expiresAt`); a disabled or expired token is rejected. See [Security Hardening](/docs/security-hardening/#4-developer-tokens-header-only-with-expiry--revocation).
:::

For the full endpoint reference, authentication examples, and the list of public endpoints that require no authentication, see [REST API Reference → Authentication](/docs/rest-api/#authentication).

:::tip[Tokens are encrypted at rest]
API Token values are encrypted at rest in the database. The encryption key is set via `turing.ai.crypto.key` (env `TURING_AI_CRYPTO_KEY`), **required in production**. See [Configuration Reference](/docs/configuration-reference/) and [Security Hardening](/docs/security-hardening/).
:::

### Authorization: `turing.permissions`

By default (`turing.permissions: true`) authenticated users receive only the authorities from their assigned groups/roles. Setting it to `false` grants **every** authenticated principal full admin rights (a trusted single-user convenience) and logs a `SECURITY` warning at startup. See [Security Hardening § permissions](/docs/security-hardening/#2-turingpermissions-defaults-to-true-authn--admin).

---

## Switching to Social Login (OAuth2)

For teams using Google, GitHub, or Microsoft, you can enable social login buttons by adding the provider configuration in `application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
```

The login page automatically shows buttons only for configured providers. See [Social Login (OAuth2)](/docs/security-social-login/) for complete setup instructions for each provider.

---

## Switching to Keycloak SSO

For production environments with SSO requirements, Turing can delegate authentication to Keycloak via OAuth2 / OpenID Connect. Enable it with:

```bash
-Dturing.keycloak=true
```

See [Security & Keycloak](/docs/security-keycloak/) for the full 6-step production setup: database, Keycloak installation, realm configuration, SSL certificates, JVM properties, and Apache reverse proxy.

---

## Related Pages

| Page | Description |
|---|---|
| [Social Login (OAuth2)](/docs/security-social-login/) | Google, GitHub, and Microsoft social login |
| [Security & Keycloak](/docs/security-keycloak/) | Full production setup with Keycloak OAuth2/OIDC |
| [Administration Guide](/docs/administration-guide/) | Managing users, groups, roles, and API tokens |
| [REST API](/docs/rest-api/) | Full REST API reference including authentication examples |
| [Configuration Reference](/docs/configuration-reference/) | `turing.ai.crypto.key` and other security-related settings |

---


