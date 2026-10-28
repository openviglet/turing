---
title: "Assets"
description: "Manage files and train the RAG knowledge base with Viglet Turing Assets."
---
Assets is the file manager that feeds the RAG Knowledge Base in Viglet Turing. Content managers and administrators use it to upload documents (PDFs, Word files, spreadsheets, and more) that are automatically indexed as vector embeddings and made available to AI Agents for semantic search. It is the bridge between your files and the Knowledge Base, everything uploaded here becomes part of what the AI can reference when answering questions.

The **Assets** section (`/console/asset`) is a file manager with built-in RAG training capabilities. It is available in the **Management** section of the sidebar and is only visible when a **storage backend is configured** (`turing.storage.type` is `minio` or `filesystem`).

Assets serves as the Knowledge Base for AI Agents: every file uploaded here can be indexed as vector embeddings and queried by the LLM via tool calling. For the conceptual overview of how this fits into the GenAI architecture, see [Generative AI & LLM Configuration](/docs/genai-llm/).

:::note[Storage required]
Assets and all RAG Knowledge Base features require a storage backend to be configured. See [Storage Configuration](#storage-configuration) at the bottom of this page.
:::

---

## Layout

The interface uses a **resizable dual-panel layout**:

- **Left panel**: file and folder listing with the action toolbar
- **Right panel**: inline preview of the selected file

A **breadcrumb** at the top of the left panel shows the current folder path and allows navigation to any parent level. A **Root** button returns to the top-level folder instantly.

---

## File Table

The file listing displays the following columns:

| Column | Description |
|---|---|
| **Name** | File or folder name |
| **Size** | File size in human-readable format |
| **Type** | MIME type or folder indicator |
| **Last Modified** | Date and time of the last modification |
| **AI** | Training status: a checkmark indicates the file has been indexed as embeddings, with a tooltip showing the training timestamp |
| **Actions** | Per-row download and delete buttons |

---

## File Management

### Upload Files

Files are uploaded to the **current folder** via a file picker dialog (click **Upload Files**). Multiple files can be selected in one operation. Uploads are sent to:

```
POST /api/asset
```

After upload, an **asynchronous event automatically triggers individual AI indexing** for each uploaded file: no manual training step is needed for new uploads.

### Create Folder

A dialog prompts for a folder name. Folders can be nested to any depth and are navigated via the breadcrumb.

### Download

Each file has a dedicated download button that preserves the original filename.

### Delete

Files and folders can be deleted via an inline button. A **toast notification** confirms completion. When a file is deleted, its **embeddings are automatically removed from the vector store**.

:::note[No rename]
Renaming files or folders is not currently supported. To rename a file, download it, delete the original, and re-upload with the new name.
:::

---

## Supported File Formats for Training

Apache Tika is the text-extraction engine used during AI Training. It supports a broad range of document types:

| Category | Formats |
|---|---|
| **Documents** | PDF, DOCX, DOC, ODT, RTF, EPUB |
| **Spreadsheets** | XLSX, XLS, ODS, CSV |
| **Presentations** | PPTX, PPT, ODP |
| **Web / Markup** | HTML, XHTML, XML |
| **Plain text** | TXT, LOG, Markdown |
| **Email** | EML, MSG, MBOX |
| **Images (with OCR)** | PNG, JPEG, TIFF, BMP, GIF (requires Tesseract) |

Files that Tika cannot extract text from (e.g. binary executables, ZIP archives without textual content) are **silently skipped** during training with no error counted.

---

## Preview Panel

Selecting a file opens an inline preview in the right panel without leaving the page. Supported formats:

| Category | Formats |
|---|---|
| **Images** | PNG, JPEG, GIF, WebP, SVG, BMP |
| **PDFs** | Rendered via iframe |
| **Video** | MP4, WebM, OGG (with player controls) |
| **Audio** | MP3, OGG, WAV, WebM (with player controls) |
| **Text** | TXT, CSV, HTML, CSS, JS, JSON, XML |

**Panel actions:**

- **Maximise**: opens fullscreen view (press `Esc` to close)
- **Download**: downloads the file directly from the preview panel
- **Close**: collapses the preview panel

The panel footer displays the file size, content type, modification date, and file extension.

---

## AI Training (RAG)

The AI training features are only available when `ragEnabled=true` **and** an embedding model and embedding store are configured in **Administration → Global Settings → RAG Settings**.

### Training Status per File

The **AI column** in the file table shows the indexing state of each file:

- ✅ **Checkmark**: file has been indexed; hover to see the training timestamp
- *(empty)*: file has not yet been indexed

### Automatic Training on Upload

When a file is uploaded, Turing dispatches an **asynchronous event** that indexes the file individually without any user action required. Similarly, when a file is deleted, its embeddings are automatically purged from the vector store.

### Batch Training

To index all existing files at once (useful after enabling RAG on an existing installation, or after changing the embedding model) use the **"Train AI with Assets"** button.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '13px', 'actorBkg': '#dbeafe', 'actorBorder': '#4A90D9', 'actorTextColor': '#1a1a1a', 'activationBkgColor': '#ede9fe', 'activationBorderColor': '#9B6EC5', 'labelBoxBkgColor': '#fef3c7', 'labelBoxBorderColor': '#E8A838', 'labelTextColor': '#1a1a1a', 'loopTextColor': '#b07a1a', 'noteBkgColor': '#dcfce7', 'noteBorderColor': '#50B86C', 'noteTextColor': '#1a1a1a', 'signalColor': '#333', 'signalTextColor': '#333'}}}%%
sequenceDiagram
    participant Admin
    participant UI as Assets UI
    participant API as Turing
    participant MinIO
    participant Tika as Apache Tika
    participant VS as Vector Store

    Admin->>UI: Click "Train AI with Assets"
    UI->>API: Start batch training
    loop For each file in MinIO (recursive)
        API->>MinIO: Download file
        MinIO-->>API: File bytes
        API->>Tika: Extract text
        Tika-->>API: Plain text (truncated at 100,000 chars)
        API->>API: Split into 1,024-char chunks
        API->>VS: Create embeddings and store chunks
        API->>API: Write record to asset_training_record
    end
    API-->>UI: Training complete
```

**Batch training steps for each file:**

1. Download file bytes from MinIO
2. Extract plain text via **Apache Tika**: supports PDF, DOCX, XLSX, PPTX, HTML, TXT, and images (with OCR)
3. Truncate text to **100,000 characters**
4. Split into **chunks of 1,024 characters**
5. Generate embeddings and store in the configured vector store
6. Write a record to `asset_training_record` with timestamp

**Progress monitoring**, while the batch is running, the UI polls every **3 seconds** and displays:

```
X / Y files processed, Z errors
```

**Training states:** `IDLE` → `RUNNING` → `COMPLETED` / `FAILED`

:::caution[Re-training after embedding model change]
If you change the Default Embedding Model in **Administration → Global Settings → RAG Settings**, all existing embeddings become invalid. Run "Train AI with Assets" again to re-index all files with the new model.
:::

### Embedding Metadata

Each chunk stored in the vector store carries the following metadata:

| Field | Value |
|---|---|
| `source` | `"minio-asset"` |
| `objectName` | Full object path in MinIO |
| `objectPath` | Folder path within the bucket |
| `fileName` | Original filename |
| `contentType` | MIME type of the source file |
| `size` | File size in bytes |

This metadata is used by AI Agents when returning search results, so the LLM can cite the source file and provide context about where the information came from.

### Text Extraction Limits

There is **no hard file size limit** for uploads: MinIO accepts files of any size. However, during AI Training the extracted text is **truncated to 100,000 characters** before chunking. For very large documents this means only the first portion of the content is indexed. The truncation limit is a fixed internal constant.

---

## How AI Agents Use the Knowledge Base

Once files are indexed, AI Agents can query the knowledge base via four built-in tools:

| Tool | Description |
|---|---|
| `search_knowledge_base` | Semantic similarity search across all indexed chunks |
| `knowledge_base_stats` | Returns total files, chunks, and storage size |
| `list_knowledge_base_files` | Lists all indexed files, with optional keyword filter |
| `get_file_from_knowledge_base` | Retrieves the full indexed content of a specific file |

For details on configuring AI Agents and tools, see [AI Agents](/docs/ai-agents/) and [Tool Calling](/docs/tool-calling/).

---

## Storage Configuration

A storage backend must be configured before Assets becomes available. Turing supports two storage backends: **MinIO** (recommended for production) and **Filesystem** (for simple deployments). Set `turing.storage.type` to enable one.

### Option 1: MinIO (Recommended)

```yaml
turing:
  storage:
    type: minio
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: turing-assets
```

The bucket (`turing-assets` by default) is **created automatically on startup** if it does not exist.

With Docker Compose, add the MinIO service alongside Turing:

```yaml
minio:
  image: minio/minio
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  command: server /data --console-address ":9001"
  volumes:
    - minio_data:/data
```

:::tip
The MinIO web console is available at `http://localhost:9001` when running locally via Docker Compose. Use it to inspect buckets and verify that files are being stored correctly.
:::

### Option 2: Filesystem

```yaml
turing:
  storage:
    type: filesystem
    filesystem:
      path: ./store/assets
```

Files are stored in the configured directory, which is created automatically if it does not exist. The filesystem backend includes **path traversal protection**: all paths are resolved against the base directory.

:::note
The filesystem backend is suitable for single-server deployments and development. For production environments with multiple instances or high availability requirements, use MinIO.
:::

For the full property reference, see [Configuration Reference: Storage](/docs/configuration-reference/#storage).

---

## REST API Reference

All endpoints are under `/api/asset` and require authentication.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/asset?prefix=` | List files and folders in a prefix (folder) |
| `POST` | `/api/asset` | Upload one or more files (multipart/form-data) |
| `GET` | `/api/asset/download?objectName=` | Download a file (Content-Disposition: attachment) |
| `GET` | `/api/asset/preview?objectName=` | Preview a file inline (Content-Disposition: inline) |
| `GET` | `/api/asset/metadata?objectName=` | Retrieve file metadata (size, type, date) |
| `POST` | `/api/asset/folder?path=` | Create a new folder |
| `DELETE` | `/api/asset?objectName=` | Delete a file or folder |
| `POST` | `/api/asset/train` | Start batch AI Training |
| `GET` | `/api/asset/train/status` | Poll current training status |
| `GET` | `/api/asset/train/records?objectNames=` | Get training timestamps for specific files |

---

## Related Pages

| Page | Description |
|---|---|
| [Embedding Stores & Models](/docs/embedding-stores/) | Configure vector database backends and embedding models used by AI Training |
| [Tool Calling](/docs/tool-calling/) | All 27 native tools, including the four Knowledge Base tools |
| [AI Agents](/docs/ai-agents/) | Compose agents that query the Knowledge Base via tool calling |
| [Generative AI & LLM Configuration](/docs/genai-llm/) | Platform-wide GenAI settings and RAG architecture overview |

---


