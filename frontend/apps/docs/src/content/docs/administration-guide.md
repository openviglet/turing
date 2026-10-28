---
title: "Administration Guide"
description: "Manage users, groups, roles, API tokens, global settings, and system diagnostics in Viglet Turing."
---
The **Administration** section (`/admin-settings`) manages users, access control, API credentials, global configuration, and system diagnostics. It is accessible from the main sidebar.

:::note[Keycloak mode]
When Keycloak is enabled (`turing.keycloak=true`), the **Users**, **Groups**, and **Roles** subsections are hidden: identity and access management is fully delegated to Keycloak. See [Security & Keycloak](/docs/security-keycloak/).
:::

---

## Login

When accessing Viglet Turing, a login page is displayed. The default username is `admin`. Turing never ships a default password: you choose it in one of two ways on the first startup.

### Option 1: the `TURING_ADMIN_PASSWORD` environment variable

Best for containers, unattended installs, and CI: the password is applied while the server boots, so the console goes straight to the login page. Set the variable before starting Turing for the first time:

**Windows**
```bat
set TURING_ADMIN_PASSWORD=your_password
```

**Linux / macOS**
```bash
export TURING_ADMIN_PASSWORD=your_password
```

The password must be at least **6 characters**: a shorter value is ignored (a warning is logged) and Turing falls back to Option 2.

:::note[Applied once, never overwritten]
The variable is only applied while the `admin` password is still unset. Once a password exists (whether it came from this variable or from the setup screen), restarting with the variable still set changes nothing. To change the password afterwards, use the **Users** page in the console.
:::

### Option 2: the first-access setup screen

If the variable is not set, the console asks you to choose the `admin` password the first time it is opened (minimum 6 characters). After it is set, the setup screen is no longer reachable.

### Calling the API from scripts and CI

Scripts don't need the console's cookie-and-XSRF login. With local authentication (Keycloak off), send the user's credentials with HTTP Basic:

```bash
curl -u admin:your_password http://localhost:2700/api/sn
```

For connectors and long-lived automation, prefer an API token sent in the `Key` header, which works without a password.

If Basic answers `401`, the password is wrong. The usual cause is a reused database: `TURING_ADMIN_PASSWORD` was ignored at startup because a password already existed (the log says so), so the admin password is still the one stored earlier.

---

## Users

The Users page (`/admin/users`) lists all local user accounts. Click a user to edit, or use the creation button to add a new account.

**Account Information:**

| Field | Description |
|---|---|
| Avatar | Profile picture using DiceBear: click to pick a style, or remove to use initials |
| Username | Unique login identifier (read-only for existing users) |
| First Name | User's first name (required) |
| Last Name | User's last name (required) |

**Contact:**

| Field | Description |
|---|---|
| Email | User's email address (required) |

**Groups & Roles** (tabbed view):

| Tab | Description |
|---|---|
| **Groups** | Search and assign available groups. Remove the user from groups using the remove button. |
| **Roles** | Read-only view of roles inherited from the user's assigned groups |

**Security:**

| Field | Description |
|---|---|
| Password | Required for new users. Leave blank when editing to keep the current password. |

---

## Groups

The Groups page (`/admin/groups`) organises users into groups for role-based access control.

**Group Details:**

| Field | Description |
|---|---|
| Name | Unique group identifier (required) |
| Description | Purpose or scope of this group |

**Members & Roles** (tabbed view):

| Tab | Description |
|---|---|
| **Users** | Search and add members. Remove users from the group using the remove button. |
| **Roles** | Search and assign roles. Remove roles using the remove button. |

---

## Roles

The Roles page (`/admin/roles`) defines permissions that are assigned to groups.

**Role Details:**

| Field | Description |
|---|---|
| Name | Unique role identifier (required) |
| Description | What this role permits |

### Privilege Matrix

Each role has a **privilege matrix**, an interactive table where you toggle individual permissions per resource category. The matrix is organized into two sections:

**Generative AI:**

| Category | Resources Covered |
|---|---|
| LLM | Language model instances |
| EMBEDDING | Embedding model instances |
| STORE | Embedding store instances |
| AI_AGENT | AI Agent configurations |
| INTENT | Intent categories and actions |

**Enterprise Search:**

| Category | Resources Covered |
|---|---|
| SE | Search engine instances and cores |
| SN | Semantic Navigation sites and fields |

**Actions per category:**

| Action | Description |
|---|---|
| VIEW | Read access to the resource |
| CREATE | Create new resources |
| EDIT | Modify existing resources |
| DELETE | Remove resources |
| ALL | Bulk toggle: enables or disables all four actions at once |

:::tip
The `ROLE_ADMIN` role has full access to all resources regardless of the privilege matrix. The matrix applies to non-admin roles only.
:::

---

## API Tokens

The API Tokens page (`/admin/tokens`) manages tokens used to authenticate REST API requests. Every token is passed in the `Key` request header.

**Form fields:**

| Field | Description |
|---|---|
| Title | A human-readable name for the token (required) |
| Description | Purpose or owner of this token |

**Token display** (existing tokens only):

| Field | Description |
|---|---|
| API Token | Read-only, monospace display with copy-to-clipboard button |

:::note
The token value is generated automatically on creation and displayed once. It cannot be retrieved again: store it securely.
:::

**Using the token in API requests:**

```bash
curl "http://localhost:2700/api/sn/Sample/search?q=cloud&_setlocale=en_US" \
  -H "Key: <YOUR_API_TOKEN>"
```

---

## Global Settings

The Global Settings page (`/admin/settings`) is the central configuration panel for defaults and external service integrations. Divided into four sections.

### General

| Field | Description |
|---|---|
| Decimal Separator | Choose between period (`.`) and comma (`,`) for numeric display. Preview shows formatted examples (e.g., `1,500.75` vs `1.500,75`). |
| Python Executable | Absolute path to the Python 3 binary used by the Code Interpreter tool. When left blank, Turing searches standard OS locations automatically. |

### LLM Settings

| Field | Description |
|---|---|
| Default LLM Instance | Dropdown of enabled LLM instances: the default used when no site-level instance is configured. Select "None" to require explicit per-site assignment. |
| LLM Cache | Toggle to enable caching of LLM responses |
| Cache Duration | How long cached responses are retained (duration input with configurable time unit), visible when cache is enabled |
| Regenerate Cache | Toggle to force fresh LLM calls instead of serving cached responses, visible when cache is enabled |

:::caution
Caching LLM responses improves performance but may return stale answers if the underlying content changes frequently. Tune the duration to match your content update cadence.
:::

### RAG Settings

| Field | Description |
|---|---|
| Enable RAG Globally | Master switch for Retrieval-Augmented Generation across all sites |
| Default Embedding Model | Dropdown of enabled embedding models: required when RAG is enabled |
| Default Embedding Store | Dropdown of enabled embedding stores (ChromaDB, PgVector, or Milvus), required when RAG is enabled |

:::caution
Changing the Default Embedding Model invalidates all existing embeddings. All indexed content must be re-indexed after changing this setting.
:::

:::note
The RAG Settings section is only visible if an embedding store is configured and available. If MinIO is not configured, the Knowledge Base and related RAG options will not appear.
:::

### Email Settings

Used by Turing to send notifications and test email connectivity.

| Field | Description |
|---|---|
| Provider | Email service provider (currently Brevo) |
| API Key | API key for the email provider |
| Sender Email | The `From` email address |
| Sender Name | The display name shown to recipients |
| Recipient Email | Default destination for test and automated emails |
| Send Test Email | Button to send a test message and verify configuration |

---

## System Information

The System Information page (`/admin/system-info`) is a diagnostic panel to monitor the health of the Turing instance. It contains two tabs.

### Overview

**Application:**

| Item | Description |
|---|---|
| Version | Current Turing build version |
| Java Version | JVM version in use |
| Java Vendor | JVM vendor (e.g., Eclipse Adoptium) |
| OS | Operating system name, version, and architecture |

**Database:**

| Item | Description |
|---|---|
| Status | UP (green) or DOWN (red): connection health |
| Product | Database product name (e.g., H2, MariaDB) |
| Version | Database server version |
| Driver | JDBC driver name and version |
| URL | JDBC connection URL |

**Physical Memory (RAM):**

| Item | Description |
|---|---|
| RAM Usage | Percentage used with progress bar |
| Total RAM | Total physical memory installed |
| Free RAM | Available physical memory |
| Used RAM | Memory currently in use |

**Swap / Pagefile** (if available):

| Item | Description |
|---|---|
| Swap Usage | Percentage used with progress bar |
| Total Swap | Total swap space configured |
| Free Swap | Available swap space |

**Memory (JVM Heap):**

| Item | Description |
|---|---|
| Heap Usage | Percentage used with progress bar |
| Total Allocated | JVM heap currently allocated |
| Free (Allocated) | Free space within the allocated heap |
| Used Memory | Heap memory currently in use |
| Max Heap | Maximum heap size configured |

**Disk Space:**

| Item | Description |
|---|---|
| Disk Usage | Percentage used with progress bar |
| Total Space | Total disk capacity |
| Available Space | Free disk space |
| Used Space | Disk space currently in use |

**MongoDB** (shown only when `turing.mongodb.enabled: true`):

| Item | Description |
|---|---|
| Status | UP (green) or DOWN (red) |
| Version | MongoDB server version |
| Endpoint | Connection URI |

**MinIO** (shown only when `turing.minio.enabled: true`):

| Item | Description |
|---|---|
| Status | UP (green) or DOWN (red) |
| Version | MinIO server version |
| Endpoint | MinIO server URL |

### System Variables

A searchable table of all JVM system properties active at runtime. The search input filters properties by name or value in real time. A counter shows the number of visible entries out of the total.

Useful for verifying configuration overrides, checking classpath entries, or confirming environment variable values at deployment.

---

## User Account

The User Account page (`/account`) allows the currently logged-in user to manage their own profile without requiring admin privileges.

**Profile fields:**

| Field | Description |
|---|---|
| Avatar | DiceBear avatar picker: choose a style or remove to use initials |
| First Name | User's first name |
| Last Name | User's last name |
| Email | User's email address |
| Username | Read-only: cannot be changed |

**Change Password:**

| Field | Description |
|---|---|
| New Password | Leave blank to keep the current password |
| Confirm Password | Must match the new password |

---

## REST API

All administration endpoints require authentication via the `Key` header. For the full endpoint reference, see [REST API Reference](/docs/rest-api/).

### Users

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v2/user` | List all users |
| `GET` | `/api/v2/user/{username}` | Get user by username |
| `GET` | `/api/v2/user/current` | Get the currently authenticated user |
| `POST` | `/api/v2/user` | Create a new user |
| `PUT` | `/api/v2/user/{username}` | Update a user |
| `DELETE` | `/api/v2/user/{username}` | Delete a user |
| `PUT` | `/api/v2/user/{username}/avatar` | Update user avatar |
| `PUT` | `/api/v2/user/{username}/password` | Change user password |

### Groups

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v2/group` | List all groups |
| `GET` | `/api/v2/group/{id}` | Get a group |
| `POST` | `/api/v2/group` | Create a group |
| `PUT` | `/api/v2/group/{id}` | Update a group |
| `DELETE` | `/api/v2/group/{id}` | Delete a group |

### Roles

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v2/role` | List all roles |
| `GET` | `/api/v2/role/{id}` | Get a role |
| `POST` | `/api/v2/role` | Create a role |
| `PUT` | `/api/v2/role/{id}` | Update a role |
| `DELETE` | `/api/v2/role/{id}` | Delete a role |
| `GET` | `/api/v2/privilege` | List all available privileges |

### API Tokens

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/dev/token` | List all tokens |
| `GET` | `/api/dev/token/{id}` | Get a token |
| `POST` | `/api/dev/token` | Create a token (auto-generates value) |
| `PUT` | `/api/dev/token/{id}` | Update token metadata |
| `DELETE` | `/api/dev/token/{id}` | Delete a token |

### Global Settings

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/global-settings` | Get all settings |
| `PUT` | `/api/system/global-settings` | Update settings |
| `POST` | `/api/system/global-settings/email/test` | Send test email |

### System Information

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/info` | System overview (version, database, memory, disk, external services) |
| `GET` | `/api/system/info/variables` | JVM system properties |

---

## Related Pages

| Page | Description |
|---|---|
| [Security & Authentication](/docs/security-authentication/) | Authentication mechanisms and API token usage |
| [Security & Keycloak](/docs/security-keycloak/) | Keycloak OAuth2/OIDC integration |
| [Configuration Reference](/docs/configuration-reference/) | Full `application.yaml` property reference |
| [Logging](/docs/logging/) | Server, indexing, and AEM log viewer |
| [REST API Reference](/docs/rest-api/) | Complete API endpoint reference |

---

