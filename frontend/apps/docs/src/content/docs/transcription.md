---
title: "Transcribe Audio to Text with OpenAI or a Self-Hosted Whisper Server"
description: "Set up speech-to-text in Viglet Turing with OpenAI or a self-hosted OpenAI-compatible server, split recordings past the upload limit, and transcribe multi-hour audio as async jobs."
sidebar:
  label: "Transcription"
---
**Transcription** turns an audio recording into text inside Viglet Turing. It is the engine behind three product surfaces: [drafting a persona from a voice recording](/docs/personas/#persona-from-audio-authoring), filling an **AUDIO chat slot** from an uploaded clip, and the standalone **async transcription jobs** API for long recordings.

You'd reach for this page when you want to point transcription at a specific backend (a cloud provider or a self-hosted, air-gapped server), when a recording is **larger than the provider's per-request upload limit**, or when you need to transcribe multi-hour audio without tying up a request thread.

The design goal is that everything here is **opt-in**: with no `turing.transcription.*` configuration set, transcription behaves exactly as it did before: a single cloud OpenAI-compatible call using your default [LLM instance](/docs/llm-instances/)'s connection.

---

## The transcription seam

Transcription rides a single pluggable seam (`TurTranscriptionProvider`) so every caller (persona-from-audio, AUDIO slots, async jobs) inherits the same backend choice, chunking, and metrics for free. You choose the backend once; the rest of the platform follows.

```
Audio ──► Chunker (splits past the per-request limit) ──► Provider ──► Transcript
                                                            │
                                        OPENAI · OPENAI_COMPATIBLE · NONE
```

---

## From zero: turn on transcription

1. Open **Administration → Settings → Global Settings**, **Transcription (Speech-to-Text)** section (Generative AI tab).
2. Pick a **Strategy** (start with `OPENAI`, it reuses your default LLM instance's OpenAI-compatible connection, no extra setup).
3. Optionally set a dedicated **Endpoint**, **Model**, **API key** (write-only, encrypted) and **Max upload bytes** for the backend.
4. Save, then use any transcription surface (upload to an AUDIO slot, derive a persona from audio, or submit an async job).

Headless / Viglet Cloud deployments can pin the backend per container with the `turing.transcription.*` properties instead of the UI: a non-blank property **wins** over the DB Global Settings value, so the container is authoritative.

---

## Backends

Transcription is a **selectable strategy** so you can match cost, privacy, and infrastructure to your deployment.

| Strategy | What it is | Setup | Best for |
|---|---|---|---|
| **`OPENAI`** *(default)* | Cloud OpenAI-compatible `/audio/transcriptions` (`gpt-transcribe`). With no dedicated config it reuses your default LLM instance's base URL + key. | Zero-config if you already run OpenAI | Managed cloud stacks |
| **`OPENAI_COMPATIBLE`** | The **fully-local path**: a self-hosted server speaking the OpenAI transcription contract (e.g. [`faster-whisper-server`](https://github.com/fedirz/faster-whisper-server)), defaulting to `whisper-1`. | Run the server, set a dedicated **Endpoint** | Cost-sensitive, on-prem, air-gapped, GPU-capable |
| **`NONE`** | Transcription disabled. Callers fail soft with a clear message. | n/a | Turning the feature off |

### Choosing a model

Leave **Model** blank and each backend uses the right default: `gpt-transcribe` for the cloud path, `whisper-1` for a self-hosted OpenAI-compatible server (which is what such servers actually serve). Turing adapts the request shape to whichever model you name, so you never have to think about the wire format.

| Model | Use it for | Notes |
|---|---|---|
| **`gpt-transcribe`** | Completed files and batches. The cloud default. | Cheaper than `whisper-1` and markedly more accurate on accents, numbers, jargon and noisy audio. Accepts the **Context prompt** and **Domain keywords** hints. Returns no per-segment scores, so the confidence-based fallback reports "no signal" rather than a made-up number. |
| **`gpt-live-transcribe`** | Low-latency live transcription in realtime voice. | Used automatically by voice sessions: the server picks it, the browser just follows. Not available on `/audio/transcriptions`. |
| **`whisper-1`** | Timestamps, translation, subtitles. | Still fully supported and still the right pick when you need segment timestamps or a confidence score. The default for self-hosted servers. |

:::note[Nothing breaks]
If you already configured `whisper-1`, or left the model blank on a self-hosted server, your setup keeps behaving exactly as before. The new models are additive.
:::

:::note[Running fully locally]
There is **no in-process transcription engine**: a fully-local / air-gapped deployment runs `OPENAI_COMPATIBLE` against a self-hosted server (e.g. faster-whisper-server) on your own network. This keeps audio on your infrastructure while reusing the exact same, battle-tested REST path as the cloud backend.
:::

### Self-hosted OpenAI-compatible server

`OPENAI_COMPATIBLE` points the same REST path at your own server. Any server implementing OpenAI's `POST {baseUrl}/audio/transcriptions` works, a common choice is `faster-whisper-server`:

```yaml
# docker-compose snippet
services:
  faster-whisper:
    image: fedirz/faster-whisper-server:latest-cpu   # or -cuda for GPU
    ports:
      - "8000:8000"
```

Then set the transcription **Endpoint** to `http://faster-whisper:8000/v1` (leave the API key blank, self-hosted servers usually need none; Turing omits the `Authorization` header when the key is blank). Only the URL differs from the cloud path, so it scales horizontally and can be GPU-backed.

---

## Context hints: stop transcribing your product names phonetically

Speech models guess. Give one your vocabulary and it stops guessing. Global Settings → **Transcription** has two hints, both optional and both ignored by backends that can't use them:

- **Context prompt**: free-form text describing the recording: the domain, who is speaking, the expected style. One or two sentences is plenty.
- **Domain keywords**: a comma-separated list of terms that must come out spelled right: product names, model ids, SKUs, internal jargon. Capped at 100 terms; it is a hint, not a dictionary.

Turing already curates exactly this vocabulary elsewhere: your **Thesaurus**. Turn on **Add Thesaurus terms** and the same terms the search engine expands at query time are handed to the transcriber, broadest descriptors first. Keywords you typed yourself always come first.

:::tip
The keyword hint is read by the `gpt-transcribe` family. The context prompt works more widely, including `whisper-1`.
:::

---

## Chunking: transcribing past the upload limit

Every transcription backend has a per-request upload ceiling (cloud OpenAI caps at **25 MiB**). A recording over that limit used to fail outright. Turing now **chunks transparently**:

- Audio **at or under** the limit is sent as-is (no processing overhead).
- Audio **over** the limit is split with `ffmpeg` into segments that each stay under the limit, re-encoded to compact 16 kHz mono, with cut points **snapped to silence** and a small **overlap** carried between chunks. Chunks transcribe in parallel (bounded) and the transcripts are stitched back together, de-duplicating the overlap window.

Chunking requires **`ffmpeg`** (and `ffprobe`) on the host. If they're absent, small clips still work; a large recording fails with a clear "install ffmpeg" message. Use the **Check ffmpeg** button in the Transcription settings section to verify the binaries are reachable.

---

## Async transcription jobs

Long recordings shouldn't block an HTTP request. Submit them as **jobs** and poll or stream for the result.

| Endpoint | Purpose |
|---|---|
| `POST /api/genai/transcription/jobs` | Multipart upload (`file` + optional `language`) → returns a `jobId` with state `QUEUED`. |
| `GET /api/genai/transcription/jobs/{jobId}` | Poll the current status/result. |
| `GET /api/genai/transcription/jobs/{jobId}/stream` | Server-Sent Events of state changes; the stream completes on the terminal event. |

```bash
# Submit
curl -X POST https://your-turing/api/genai/transcription/jobs \
  -F "file=@meeting.mp3" -F "language=en"
# → {"jobId":"…","state":"QUEUED", …}

# Poll
curl https://your-turing/api/genai/transcription/jobs/{jobId}
# → {"state":"SUCCEEDED","transcript":"…","language":"en", …}
```

A job moves `QUEUED → RUNNING → SUCCEEDED | FAILED`. Work runs on a **bounded worker pool** with a bounded queue: when both are saturated, a submission is rejected with **HTTP 429** (back-pressure) rather than growing memory without limit: retry shortly. Terminal results are retained for a configurable window, then evicted. Each job reuses the same chunk-aware pipeline, so multi-hour audio just works.

### Watching the transcript arrive

On a `gpt-transcribe` backend, a running job also streams the words it has heard so far as `partialTranscript` on each SSE snapshot, so a long recording shows text building instead of a spinner. The **persona from audio** flow renders this live.

Two things to know:

- It appears only for a recording that fits a **single chunk**. Chunks are transcribed in parallel, so relaying their text would show it out of order; a chunked recording keeps its `completedChunks / totalChunks` counter instead.
- It is a **preview**, not the result. Always read the final `transcript` from the terminal snapshot (or a `GET` on the job): `partialTranscript` is there to be shown, not stored.

`whisper-1` does not stream; those jobs simply never carry the field. Set `turing.transcription.stream-partial-text: false` to turn the whole behaviour off.

---

## Identifying speakers

Turn **Identify speakers** on (Global Settings → Transcription, or `turing.transcription.diarization-enabled`) and a transcription comes back as attributable turns (who said what, and when) instead of one flat text. Useful for interviews, calls and meeting recordings.

Each turn carries a `speaker` label, the `text`, and `start`/`end` offsets in seconds:

```json
{
  "transcript": "Tell me about your work. I have sold cars for twenty years.",
  "segments": [
    { "speaker": "A", "text": "Tell me about your work.", "startSeconds": 0.0, "endSeconds": 2.5 },
    { "speaker": "B", "text": "I have sold cars for twenty years.", "startSeconds": 2.5, "endSeconds": 6.0 }
  ]
}
```

Three things to know:

- **`transcript` is always authoritative.** `segments` annotates it; it never replaces it. Anything already reading `transcript` is unaffected whether diarization is on or off.
- **A speaker label means "a distinct voice in this recording"**, not a person. Labels are assigned per request, so the same human gets no stable identity across two uploads.
- **It applies to a recording that fits a single request.** A longer recording is split into chunks, and because labels are assigned per request there is no way to tell chunk 2's "A" from chunk 1's "B". Rather than guess, Turing returns the flat transcript with no segments. In practice a ~50-minute recording is still one chunk, so most interviews are covered.

Needs a `gpt-transcribe` model: `whisper-1` ignores the setting (its segments are timing boundaries with no speaker). Diarization also takes precedence over the live partial transcript above, since the two response formats are mutually exclusive.

---

## AUDIO chat slots

When an [AI Agent](/docs/ai-agents/) has an **AUDIO** multi-modal slot, uploading a clip to it now **transcribes** the audio (through the configured backend, chunked if large) and writes the transcript into the agent's first target **text** slot, so a spoken answer becomes a filled slot value. If no transcription backend is configured, the upload falls back to Gemini native understanding (when the agent's LLM is a Gemini instance). A **VIDEO** slot continues to use Gemini understanding, which also describes the visual track.

## Persona from audio

Drafting a persona from a voice recording (`POST /api/persona/derive-from-audio`) transcribes the clip through this same seam before the LLM analyses it, so it, too, benefits from chunking and your chosen backend. See [Personas → Persona-from-audio authoring](/docs/personas/#persona-from-audio-authoring).

---

## Per-backend metrics

`GET /api/genai/transcription/metrics` returns one row per backend that has served a transcription, **count, error rate, latency percentiles (avg / p50 / p95 / max), and total bytes**, so you can see whether the local path is fast enough or the cloud fallback is carrying the load. It's the same "which backend is the p95 outlier?" view as [tool-latency analytics](/docs/chat-analytics/), applied to transcription.

## Confidence fallback (local → cloud)

An **optional** escalation: run a local backend first and fall back to a cloud backend only when the local result **fails** or comes back **below a confidence threshold**. Enable it with `turing.transcription.confidence-fallback-enabled=true` and set `confidence-fallback-type` (default cloud `OPENAI`). Confidence is derived from an OpenAI-compatible `verbose_json` response's segment log-probabilities; a backend that reports none never triggers the threshold (only outright failure does). Default off = a single backend, no retry.

---

## Configuration reference

All keys live under `turing.transcription.*`. A non-blank value **wins** over the DB Global Settings row (so a container can pin the backend); anything unset falls back to the UI value, then the default.

| Key | Default | Purpose |
|---|---|---|
| `type` | `OPENAI` | Backend: `OPENAI` · `OPENAI_COMPATIBLE` · `NONE`. |
| `endpoint` | n/a | Dedicated OpenAI-compatible base URL (e.g. `http://faster-whisper:8000/v1`). |
| `model` | per backend | Transcription model name. Blank = `gpt-transcribe` on `OPENAI`, `whisper-1` on `OPENAI_COMPATIBLE`. |
| `prompt` | n/a | Free-form context handed to the model (domain, speakers, expected style). Ignored by backends that don't accept it. |
| `keywords` | n/a | Domain terms the model should spell correctly: product names, model ids, jargon. Used by the `gpt-transcribe` family; capped at 100. |
| `keywords-from-thesaurus` | `false` | Top the keyword list up from your Thesaurus, so the vocabulary the search engine already expands also reaches the transcriber. Your own keywords come first. |
| `stream-partial-text` | `true` | Stream a completed file's transcript as it is produced, so a running job carries `partialTranscript`. Only engages on a streaming-capable model and a single-chunk recording; any failure falls back to the ordinary call. |
| `diarization-enabled` | `false` | Return attributable per-speaker turns (`segments`) alongside the transcript. Needs a `gpt-transcribe` model and a single-chunk recording. |
| `api-key` | n/a | API key for the endpoint (blank = no `Authorization` header). |
| `max-upload-bytes` | `26214400` | Per-request upload limit (25 MiB); the chunker splits above this. |
| `ffmpeg-path` | `ffmpeg` | `ffmpeg` executable used to split/re-encode large audio. |
| `ffprobe-path` | `ffprobe` | `ffprobe` executable used to probe duration. |
| `chunk-overlap-seconds` | `2.0` | Overlap carried between chunks so the stitch drops no words. |
| `ffmpeg-timeout-seconds` | `120` | Timeout for a single ffmpeg/ffprobe call. |
| `chunk-concurrency` | `3` | Max chunks transcribed concurrently per recording. |
| `async-workers` | `2` | Worker threads in the async job pool. |
| `async-queue-capacity` | `8` | Bounded queue depth before submissions are rejected (429). |
| `async-job-retention-seconds` | `3600` | How long a terminal job's result is retained for polling. |
| `confidence-fallback-enabled` | `false` | Enable the local→cloud confidence fallback. |
| `confidence-fallback-type` | `OPENAI` | Backend the fallback escalates to. |
| `confidence-threshold` | `0.5` | Minimum 0–1 confidence before the fallback escalates. |

See the [Configuration Reference](/docs/configuration-reference/) for how these sit alongside the rest of `turing.*`.

