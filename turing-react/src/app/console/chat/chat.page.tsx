"use client"
import { IconArrowUp, IconBolt, IconCheck, IconCompass, IconCopy, IconCpu2, IconFile, IconLoader2, IconMessageCircle, IconPaperclip, IconUser, IconX } from "@tabler/icons-react"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import rehypeHighlight from "rehype-highlight"
import "./chat-highlight.css"
import { toast } from "sonner"

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { GradientAvatar, GradientAvatarFallback } from "@/components/ui/gradient-avatar"
import { GradientButton } from "@/components/ui/gradient-button"
import { ModeToggle } from "@/components/mode-toggle"
import type { TurLLMInstance } from "@/models/llm/llm-instance.model"
import { type ChatMessageItem, TurChatService } from "@/services/chat/chat.service"

const turChatService = new TurChatService()

const DEFAULT_CONTEXT_WINDOW = 128000
const contextWindowCache = new Map<string, number>()
const LLM_STORAGE_KEY = "turing-chat-selected-llm"

interface ChatAttachment {
  name: string
  type: string
  size: number
}

interface ChatMessage extends ChatMessageItem {
  id: string
  attachments?: ChatAttachment[]
}

const generateId = () => crypto.randomUUID()

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4)
}

function formatTokenCount(tokens: number): string {
  if (tokens >= 1000) return `${(tokens / 1000).toFixed(1)}k`
  return `${tokens}`
}

export default function ChatPage() {
  const [activeTab, setActiveTab] = useState<"chat" | "semantic">("chat")
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([])
  const [selectedLlmId, setSelectedLlmId] = useState<string>(() => localStorage.getItem(LLM_STORAGE_KEY) ?? "")
  const [fetchedContextWindow, setFetchedContextWindow] = useState<number | null>(null)

  // Chat tab state
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatInput, setChatInput] = useState("")
  const [chatLoading, setChatLoading] = useState(false)
  const [chatCompacting, setChatCompacting] = useState(false)
  const [attachedFiles, setAttachedFiles] = useState<File[]>([])
  const [isDragOver, setIsDragOver] = useState(false)
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)
  const chatTextareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Semantic tab state
  const [semMessages, setSemMessages] = useState<ChatMessage[]>([])
  const [semInput, setSemInput] = useState("")
  const [semLoading, setSemLoading] = useState(false)
  const [semCompacting, setSemCompacting] = useState(false)
  const semEndRef = useRef<HTMLDivElement>(null)
  const semTextareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    turChatService.queryLLMInstances().then((instances) => {
      const enabled = instances.filter((i) => i.enabled === 1)
      setLlmInstances(enabled)
      if (enabled.length > 0) {
        const stored = localStorage.getItem(LLM_STORAGE_KEY)
        const valid = stored && enabled.some((i) => i.id === stored)
        if (!valid) {
          setSelectedLlmId(enabled[0].id)
          localStorage.setItem(LLM_STORAGE_KEY, enabled[0].id)
        }
      }
    })
  }, [])

  useEffect(() => {
    if (!selectedLlmId) {
      setFetchedContextWindow(null)
      return
    }
    const cached = contextWindowCache.get(selectedLlmId)
    if (cached) {
      setFetchedContextWindow(cached)
      return
    }
    turChatService.fetchContextInfo(selectedLlmId).then((info) => {
      if (info.contextWindow > 0) {
        contextWindowCache.set(selectedLlmId, info.contextWindow)
        setFetchedContextWindow(info.contextWindow)
      }
    }).catch((err) => {
      console.warn("Failed to fetch context info:", err)
      setFetchedContextWindow(null)
    })
  }, [selectedLlmId])

  // Auto-scroll for active tab
  useEffect(() => {
    if (activeTab === "chat") chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [chatMessages, activeTab])
  useEffect(() => {
    if (activeTab === "semantic") semEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [semMessages, activeTab])

  // Textarea auto-resize
  const adjustHeight = useCallback((ref: React.RefObject<HTMLTextAreaElement | null>) => {
    const textarea = ref.current
    if (textarea) {
      textarea.style.height = "auto"
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`
    }
  }, [])

  useEffect(() => { adjustHeight(chatTextareaRef) }, [chatInput, adjustHeight])
  useEffect(() => { adjustHeight(semTextareaRef) }, [semInput, adjustHeight])

  const selectedInstance = llmInstances.find((i) => i.id === selectedLlmId)
  const contextWindow = fetchedContextWindow || selectedInstance?.contextWindow || DEFAULT_CONTEXT_WINDOW

  // ── Chat tab handlers ──

  const chatContextUsage = useMemo(() => {
    const tokens = estimateTokens(chatMessages.map((m) => m.content).join(""))
    return { tokens, percentage: Math.min(Math.round((tokens / contextWindow) * 100), 100) }
  }, [chatMessages, contextWindow])

  const addFiles = (newFiles: FileList | File[]) => setAttachedFiles((prev) => [...prev, ...Array.from(newFiles)])
  const removeFile = (index: number) => setAttachedFiles((prev) => prev.filter((_, i) => i !== index))

  const handleChatSend = async () => {
    const trimmed = chatInput.trim()
    if ((!trimmed && attachedFiles.length === 0) || chatLoading || !selectedLlmId) return

    const filesToSend = [...attachedFiles]
    const userMessage: ChatMessage = {
      id: generateId(),
      role: "user",
      content: trimmed || (filesToSend.length > 0 ? `[${filesToSend.map((f) => f.name).join(", ")}]` : ""),
      attachments: filesToSend.map((f) => ({ name: f.name, type: f.type, size: f.size })),
    }

    const assistantId = generateId()
    const updated = [...chatMessages, userMessage]
    setChatMessages([...updated, { id: assistantId, role: "assistant", content: "" }])
    setChatInput("")
    setAttachedFiles([])
    setChatLoading(true)

    const apiMessages: ChatMessageItem[] = updated.map(({ role, content }) => ({ role, content }))

    await turChatService.sendStream(
      selectedLlmId,
      apiMessages,
      (token) => setChatMessages((prev) => prev.map((msg) => msg.id === assistantId ? { ...msg, content: msg.content + token } : msg)),
      () => setChatLoading(false),
      (error) => {
        console.error(error)
        toast.error("Failed to get a response. Please check your LLM configuration.")
        setChatMessages((prev) => prev.filter((msg) => msg.id !== assistantId || msg.content !== ""))
        setChatLoading(false)
      },
      filesToSend.length > 0 ? filesToSend : undefined,
    )
  }

  const handleChatCompact = async () => {
    if (chatLoading || chatCompacting || chatMessages.length < 4 || !selectedLlmId) return
    setChatCompacting(true)
    const text = chatMessages.map((m) => `${m.role}: ${m.content}`).join("\n\n")
    let summary = ""
    await turChatService.sendStream(
      selectedLlmId,
      [{ role: "user", content: `Summarize the following conversation concisely, preserving key facts, decisions, and context needed to continue the conversation. Write the summary in the same language as the conversation.\n\n${text}` }],
      (token) => { summary += token },
      () => {
        setChatMessages([{ id: generateId(), role: "assistant", content: `**[Context compacted]**\n\n${summary}` }])
        setChatCompacting(false)
        toast.success("Context compacted successfully.")
      },
      (error) => { console.error(error); toast.error("Failed to compact context."); setChatCompacting(false) },
    )
  }

  // ── Semantic tab handlers ──

  const semContextUsage = useMemo(() => {
    const tokens = estimateTokens(semMessages.map((m) => m.content).join(""))
    return { tokens, percentage: Math.min(Math.round((tokens / contextWindow) * 100), 100) }
  }, [semMessages, contextWindow])

  const handleSemSend = async () => {
    const trimmed = semInput.trim()
    if (!trimmed || semLoading || !selectedLlmId) return

    const userMessage: ChatMessage = { id: generateId(), role: "user", content: trimmed }
    const assistantId = generateId()
    const updated = [...semMessages, userMessage]
    setSemMessages([...updated, { id: assistantId, role: "assistant", content: "" }])
    setSemInput("")
    setSemLoading(true)

    const apiMessages: ChatMessageItem[] = updated.map(({ role, content }) => ({ role, content }))

    await turChatService.sendSemanticStream(
      selectedLlmId,
      apiMessages,
      (token) => setSemMessages((prev) => prev.map((msg) => msg.id === assistantId ? { ...msg, content: msg.content + token } : msg)),
      () => setSemLoading(false),
      (error) => {
        console.error(error)
        toast.error("Failed to get a response. Please check your LLM configuration.")
        setSemMessages((prev) => prev.filter((msg) => msg.id !== assistantId || msg.content !== ""))
        setSemLoading(false)
      },
    )
  }

  const handleSemCompact = async () => {
    if (semLoading || semCompacting || semMessages.length < 4 || !selectedLlmId) return
    setSemCompacting(true)
    const text = semMessages.map((m) => `${m.role}: ${m.content}`).join("\n\n")
    let summary = ""
    await turChatService.sendSemanticStream(
      selectedLlmId,
      [{ role: "user", content: `Summarize the following conversation concisely, preserving key facts, decisions, and context needed to continue the conversation. Write the summary in the same language as the conversation.\n\n${text}` }],
      (token) => { summary += token },
      () => {
        setSemMessages([{ id: generateId(), role: "assistant", content: `**[Context compacted]**\n\n${summary}` }])
        setSemCompacting(false)
        toast.success("Context compacted successfully.")
      },
      (error) => { console.error(error); toast.error("Failed to compact context."); setSemCompacting(false) },
    )
  }

  // ── Shared handlers ──

  const handleKeyDown = (sendFn: () => void) => (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendFn() }
  }

  const handleNewChat = () => {
    if (activeTab === "chat") { setChatMessages([]); setChatInput(""); setAttachedFiles([]) }
    else { setSemMessages([]); setSemInput("") }
  }

  const handleDragOver = (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(true) }
  const handleDragLeave = (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(false) }
  const handleDrop = (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(false); if (e.dataTransfer.files.length > 0) addFiles(e.dataTransfer.files) }
  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => { if (e.target.files && e.target.files.length > 0) { addFiles(e.target.files); e.target.value = "" } }

  // Active tab derived values
  const messages = activeTab === "chat" ? chatMessages : semMessages
  const isEmpty = messages.length === 0
  const activeContextUsage = activeTab === "chat" ? chatContextUsage : semContextUsage

  const contextBarColor =
    activeContextUsage.percentage >= 80 ? "bg-red-500"
      : activeContextUsage.percentage >= 60 ? "bg-yellow-500"
        : "bg-blue-500"

  // ── Message list renderer ──

  const renderMessages = (
    msgs: ChatMessage[],
    loading: boolean,
    endRef: React.RefObject<HTMLDivElement | null>,
  ) => (
    <div className="max-w-3xl mx-auto py-6 px-4 space-y-6">
      {msgs.map((message) => (
        <div key={message.id} className="flex gap-3">
          <div className="shrink-0 pt-0.5">
            <GradientAvatar className="size-7">
              <GradientAvatarFallback variant={message.role === "assistant" ? "info" : "secondary"}>
                {message.role === "assistant" ? <IconCpu2 className="size-4" /> : <IconUser className="size-4" />}
              </GradientAvatarFallback>
            </GradientAvatar>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground mb-1">
              <span>{message.role === "assistant" ? selectedInstance?.title ?? "Assistant" : "You"}</span>
              {message.role === "assistant" && loading && message === msgs.at(-1) && (
                <IconLoader2 className="size-3 animate-spin" />
              )}
            </div>
            {message.attachments && message.attachments.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-2">
                {message.attachments.map((att, idx) => (
                  <div key={idx} className="flex items-center gap-1.5 rounded-lg border bg-muted/50 px-2.5 py-1.5 text-xs text-muted-foreground">
                    <IconFile className="size-3.5 shrink-0" />
                    <span className="truncate max-w-[150px]">{att.name}</span>
                    <span className="text-muted-foreground/60">{formatFileSize(att.size)}</span>
                  </div>
                ))}
              </div>
            )}
            {message.role === "assistant" ? (
              <div className="relative group/msg text-sm leading-relaxed prose prose-sm dark:prose-invert prose-neutral max-w-none break-words prose-p:my-2 prose-pre:my-2 prose-ul:my-2 prose-ol:my-2 prose-headings:my-3 prose-code:before:content-none prose-code:after:content-none prose-code:bg-muted prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-pre:bg-muted prose-pre:border prose-pre:rounded-lg">
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(message.content)
                    setCopiedId(message.id)
                    setTimeout(() => setCopiedId((prev) => prev === message.id ? null : prev), 2000)
                  }}
                  title="Copy to clipboard"
                  className={`not-prose absolute top-1 right-1 z-10 rounded-md p-1.5 hover:bg-muted/80 transition-all ${copiedId === message.id ? "opacity-100 text-emerald-500" : "opacity-0 group-hover/msg:opacity-100 text-muted-foreground/60 hover:text-foreground"}`}
                >
                  {copiedId === message.id ? <IconCheck className="size-4" /> : <IconCopy className="size-4" />}
                </button>
                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                  {message.content}
                </ReactMarkdown>
              </div>
            ) : (
              <div className="text-sm leading-relaxed whitespace-pre-wrap break-words">{message.content}</div>
            )}
          </div>
        </div>
      ))}
      <div ref={endRef} />
    </div>
  )

  return (
    <div
      className="flex flex-col h-[calc(100vh-2rem)] max-h-screen"
      onDragOver={activeTab === "chat" ? handleDragOver : undefined}
      onDragLeave={activeTab === "chat" ? handleDragLeave : undefined}
      onDrop={activeTab === "chat" ? handleDrop : undefined}
    >
      {/* Drag overlay (chat tab only) */}
      {isDragOver && activeTab === "chat" && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm border-2 border-dashed border-blue-500 rounded-lg m-2">
          <div className="flex flex-col items-center gap-2 text-blue-500">
            <IconFile className="size-10" />
            <span className="text-lg font-medium">Drop files here</span>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-3 shrink-0">
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <Select value={selectedLlmId} onValueChange={(v) => { setSelectedLlmId(v); localStorage.setItem(LLM_STORAGE_KEY, v); setChatMessages([]); setChatInput(""); setAttachedFiles([]); setSemMessages([]); setSemInput("") }}>
            <SelectTrigger className="w-56">
              <SelectValue placeholder="Select a model..." />
            </SelectTrigger>
            <SelectContent>
              {llmInstances.map((instance) => (
                <SelectItem key={instance.id} value={instance.id}>{instance.title}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Center: pill tab switcher */}
        <div className="flex items-center rounded-full bg-muted p-1 gap-0.5">
          <button
            type="button"
            onClick={() => setActiveTab("chat")}
            className={`flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium transition-all ${activeTab === "chat" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
          >
            <IconMessageCircle className="size-4" />
            Chat
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("semantic")}
            className={`flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium transition-all ${activeTab === "semantic" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
          >
            <IconCompass className="size-4" />
            Semantic Navigation
          </button>
        </div>

        {/* Right: actions */}
        <div className="flex items-center gap-3 flex-1 min-w-0 justify-end">
          {messages.length > 0 && (
            <GradientButton variant="outline" size="sm" onClick={handleNewChat}>
              New Chat
            </GradientButton>
          )}
          <ModeToggle />
        </div>
      </div>

      {/* ── Chat Tab ── */}
      {activeTab === "chat" && (
        <>
          <div className="flex-1 overflow-y-auto">
            {isEmpty ? (
              <div className="flex flex-col items-center justify-center h-full gap-4 text-center px-4">
                <div className="rounded-full bg-gradient-to-br from-blue-600 to-indigo-600 p-4 dark:from-blue-500 dark:to-indigo-500">
                  <IconCpu2 className="size-8 text-white" />
                </div>
                <div>
                  <h2 className="text-2xl font-semibold mb-2">How can I help you today?</h2>
                  <p className="text-muted-foreground text-sm max-w-md">
                    {selectedInstance ? `Using ${selectedInstance.title} (${selectedInstance.turLLMVendor?.id ?? ""})` : "Select a language model to start chatting."}
                  </p>
                </div>
              </div>
            ) : renderMessages(chatMessages, chatLoading, chatEndRef)}
          </div>

          {/* Chat Input */}
          <div className="shrink-0 border-t bg-background">
            <div className="max-w-3xl mx-auto px-4 py-4">
              {attachedFiles.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-2">
                  {attachedFiles.map((file, idx) => (
                    <div key={idx} className="flex items-center gap-1.5 rounded-lg border bg-muted/50 px-2.5 py-1.5 text-xs">
                      <IconFile className="size-3.5 shrink-0 text-muted-foreground" />
                      <span className="truncate max-w-[150px]">{file.name}</span>
                      <span className="text-muted-foreground/60">{formatFileSize(file.size)}</span>
                      <button type="button" onClick={() => removeFile(idx)} title={`Remove ${file.name}`} className="ml-0.5 rounded-full p-0.5 hover:bg-muted-foreground/20 transition-colors">
                        <IconX className="size-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <div className="relative flex items-end rounded-xl border bg-muted/50 focus-within:ring-2 focus-within:ring-blue-500/50 focus-within:border-blue-500/50 transition-all">
                <input ref={fileInputRef} type="file" multiple className="hidden" title="Attach files" onChange={handleFileInputChange} />
                <button type="button" onClick={() => fileInputRef.current?.click()} disabled={!selectedLlmId || chatLoading} className="p-3 text-muted-foreground hover:text-foreground transition-colors disabled:opacity-50 disabled:pointer-events-none" title="Attach files">
                  <IconPaperclip className="size-5" />
                </button>
                <Textarea
                  ref={chatTextareaRef}
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={handleKeyDown(handleChatSend)}
                  placeholder={selectedLlmId ? "Send a message..." : "Select a model to start..."}
                  disabled={!selectedLlmId || chatLoading}
                  className="flex-1 resize-none border-0 bg-transparent px-2 py-3 text-sm focus-visible:ring-0 focus-visible:ring-offset-0 min-h-[44px] max-h-[200px]"
                  rows={1}
                />
                <div className="p-2">
                  <GradientButton size="icon-sm" disabled={(!chatInput.trim() && attachedFiles.length === 0) || chatLoading || !selectedLlmId} onClick={handleChatSend} className="rounded-lg">
                    {chatLoading ? <IconLoader2 className="size-4 animate-spin" /> : <IconArrowUp className="size-4" />}
                  </GradientButton>
                </div>
              </div>
              <div className="flex items-center justify-between mt-2 gap-4">
                <span className="text-xs text-muted-foreground shrink-0">
                  {selectedInstance ? `${selectedInstance.turLLMVendor?.id ?? ""} · ${selectedInstance.modelName ?? ""}` : "No model selected"}
                </span>
                {!isEmpty && (
                  <div className="flex items-center gap-2 flex-1 min-w-0 justify-end">
                    <div className="flex items-center gap-2 max-w-xs w-full">
                      <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
                        <div className={`h-full rounded-full transition-all duration-300 ${contextBarColor}`} style={{ width: `${chatContextUsage.percentage}%` }} />
                      </div>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">{formatTokenCount(chatContextUsage.tokens)}/{formatTokenCount(contextWindow)}</span>
                    </div>
                    <button type="button" onClick={handleChatCompact} disabled={chatLoading || chatCompacting || chatMessages.length < 4} title={`${100 - chatContextUsage.percentage}% of context remaining. Click to compact.`} className="flex items-center gap-1 rounded-md border px-2 py-1 text-xs text-muted-foreground hover:text-foreground hover:bg-muted transition-colors disabled:opacity-50 disabled:pointer-events-none shrink-0">
                      {chatCompacting ? <IconLoader2 className="size-3 animate-spin" /> : <IconBolt className="size-3" />}
                      Compact
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {/* ── Semantic Navigation Tab ── */}
      {activeTab === "semantic" && (
        <>
          <div className="flex-1 overflow-y-auto">
            {semMessages.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full gap-4 text-center px-4">
                <div className="rounded-full bg-gradient-to-br from-emerald-600 to-teal-600 p-4 dark:from-emerald-500 dark:to-teal-500">
                  <IconCompass className="size-8 text-white" />
                </div>
                <div>
                  <h2 className="text-2xl font-semibold mb-2">Semantic Navigation</h2>
                  <p className="text-muted-foreground text-sm max-w-md">
                    {selectedInstance
                      ? `Ask questions about your indexed content. Using ${selectedInstance.title}.`
                      : "Select a language model to start exploring."}
                  </p>
                </div>
              </div>
            ) : renderMessages(semMessages, semLoading, semEndRef)}
          </div>

          {/* Semantic Input */}
          <div className="shrink-0 border-t bg-background">
            <div className="max-w-3xl mx-auto px-4 py-4">
              <div className="relative flex items-end rounded-xl border bg-muted/50 focus-within:ring-2 focus-within:ring-emerald-500/50 focus-within:border-emerald-500/50 transition-all">
                <Textarea
                  ref={semTextareaRef}
                  value={semInput}
                  onChange={(e) => setSemInput(e.target.value)}
                  onKeyDown={handleKeyDown(handleSemSend)}
                  placeholder={selectedLlmId ? "Ask about your semantic navigation content..." : "Select a model to start..."}
                  disabled={!selectedLlmId || semLoading}
                  className="flex-1 resize-none border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0 focus-visible:ring-offset-0 min-h-[44px] max-h-[200px]"
                  rows={1}
                />
                <div className="p-2">
                  <GradientButton size="icon-sm" disabled={!semInput.trim() || semLoading || !selectedLlmId} onClick={handleSemSend} className="rounded-lg">
                    {semLoading ? <IconLoader2 className="size-4 animate-spin" /> : <IconArrowUp className="size-4" />}
                  </GradientButton>
                </div>
              </div>
              <div className="flex items-center justify-between mt-2 gap-4">
                <span className="text-xs text-muted-foreground shrink-0">
                  {selectedInstance ? `${selectedInstance.turLLMVendor?.id ?? ""} · ${selectedInstance.modelName ?? ""}` : "No model selected"}
                </span>
                {semMessages.length > 0 && (
                  <div className="flex items-center gap-2 flex-1 min-w-0 justify-end">
                    <div className="flex items-center gap-2 max-w-xs w-full">
                      <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
                        <div className={`h-full rounded-full transition-all duration-300 ${contextBarColor}`} style={{ width: `${semContextUsage.percentage}%` }} />
                      </div>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">{formatTokenCount(semContextUsage.tokens)}/{formatTokenCount(contextWindow)}</span>
                    </div>
                    <button type="button" onClick={handleSemCompact} disabled={semLoading || semCompacting || semMessages.length < 4} title={`${100 - semContextUsage.percentage}% of context remaining. Click to compact.`} className="flex items-center gap-1 rounded-md border px-2 py-1 text-xs text-muted-foreground hover:text-foreground hover:bg-muted transition-colors disabled:opacity-50 disabled:pointer-events-none shrink-0">
                      {semCompacting ? <IconLoader2 className="size-3 animate-spin" /> : <IconBolt className="size-3" />}
                      Compact
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
