"use client"
import { IconArrowUp, IconBolt, IconCpu2, IconFile, IconLoader2, IconPaperclip, IconUser, IconX } from "@tabler/icons-react"
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
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [isCompacting, setIsCompacting] = useState(false)
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([])
  const [selectedLlmId, setSelectedLlmId] = useState<string>("")
  const [attachedFiles, setAttachedFiles] = useState<File[]>([])
  const [isDragOver, setIsDragOver] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    turChatService.queryLLMInstances().then((instances) => {
      const enabled = instances.filter((i) => i.enabled === 1)
      setLlmInstances(enabled)
      if (enabled.length > 0 && !selectedLlmId) {
        setSelectedLlmId(enabled[0].id)
      }
    })
  }, [])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const adjustTextareaHeight = useCallback(() => {
    const textarea = textareaRef.current
    if (textarea) {
      textarea.style.height = "auto"
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`
    }
  }, [])

  useEffect(() => {
    adjustTextareaHeight()
  }, [input, adjustTextareaHeight])

  const selectedInstance = llmInstances.find((i) => i.id === selectedLlmId)
  const contextWindow = selectedInstance?.contextWindow || DEFAULT_CONTEXT_WINDOW

  const contextUsage = useMemo(() => {
    const totalText = messages.map((m) => m.content).join("")
    const tokens = estimateTokens(totalText)
    const percentage = Math.min(Math.round((tokens / contextWindow) * 100), 100)
    return { tokens, percentage }
  }, [messages, contextWindow])

  const addFiles = (newFiles: FileList | File[]) => {
    const fileArray = Array.from(newFiles)
    setAttachedFiles((prev) => [...prev, ...fileArray])
  }

  const removeFile = (index: number) => {
    setAttachedFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleSend = async () => {
    const trimmed = input.trim()
    if ((!trimmed && attachedFiles.length === 0) || isLoading || !selectedLlmId) return

    const filesToSend = [...attachedFiles]
    const userMessage: ChatMessage = {
      id: generateId(),
      role: "user",
      content: trimmed || (filesToSend.length > 0 ? `[${filesToSend.map((f) => f.name).join(", ")}]` : ""),
      attachments: filesToSend.map((f) => ({ name: f.name, type: f.type, size: f.size })),
    }

    const assistantId = generateId()
    const updatedMessages = [...messages, userMessage]
    setMessages([...updatedMessages, { id: assistantId, role: "assistant", content: "" }])
    setInput("")
    setAttachedFiles([])
    setIsLoading(true)

    const apiMessages: ChatMessageItem[] = updatedMessages.map(({ role, content }) => ({
      role,
      content,
    }))

    await turChatService.sendStream(
      selectedLlmId,
      apiMessages,
      (token) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === assistantId ? { ...msg, content: msg.content + token } : msg
          )
        )
      },
      () => setIsLoading(false),
      (error) => {
        console.error(error)
        toast.error("Failed to get a response. Please check your LLM configuration.")
        setMessages((prev) => prev.filter((msg) => msg.id !== assistantId || msg.content !== ""))
        setIsLoading(false)
      },
      filesToSend.length > 0 ? filesToSend : undefined,
    )
  }

  const handleCompact = async () => {
    if (isLoading || isCompacting || messages.length < 4 || !selectedLlmId) return
    setIsCompacting(true)

    const conversationText = messages
      .map((m) => `${m.role}: ${m.content}`)
      .join("\n\n")

    const summaryPrompt: ChatMessageItem[] = [
      {
        role: "user",
        content: `Summarize the following conversation concisely, preserving key facts, decisions, and context needed to continue the conversation. Write the summary in the same language as the conversation.\n\n${conversationText}`,
      },
    ]

    let summary = ""
    await turChatService.sendStream(
      selectedLlmId,
      summaryPrompt,
      (token) => { summary += token },
      () => {
        const compactedMessages: ChatMessage[] = [
          {
            id: generateId(),
            role: "assistant",
            content: `**[Context compacted]**\n\n${summary}`,
          },
        ]
        setMessages(compactedMessages)
        setIsCompacting(false)
        toast.success("Context compacted successfully.")
      },
      (error) => {
        console.error(error)
        toast.error("Failed to compact context.")
        setIsCompacting(false)
      },
    )
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleNewChat = () => {
    setMessages([])
    setInput("")
    setAttachedFiles([])
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(true)
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(false)
    if (e.dataTransfer.files.length > 0) {
      addFiles(e.dataTransfer.files)
    }
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files)
      e.target.value = ""
    }
  }

  const isEmpty = messages.length === 0

  const contextBarColor =
    contextUsage.percentage >= 80
      ? "bg-red-500"
      : contextUsage.percentage >= 60
        ? "bg-yellow-500"
        : "bg-blue-500"

  return (
    <div
      className="flex flex-col h-[calc(100vh-2rem)] max-h-screen"
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Drag overlay */}
      {isDragOver && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm border-2 border-dashed border-blue-500 rounded-lg m-2">
          <div className="flex flex-col items-center gap-2 text-blue-500">
            <IconFile className="size-10" />
            <span className="text-lg font-medium">Drop files here</span>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-3 shrink-0">
        <div className="flex items-center gap-3">
          <IconCpu2 className="size-5 text-muted-foreground" />
          <span className="text-lg font-semibold">Chat</span>
        </div>
        <div className="flex items-center gap-3">
          <Select value={selectedLlmId} onValueChange={setSelectedLlmId}>
            <SelectTrigger className="w-56">
              <SelectValue placeholder="Select a model..." />
            </SelectTrigger>
            <SelectContent>
              {llmInstances.map((instance) => (
                <SelectItem key={instance.id} value={instance.id}>
                  {instance.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {messages.length > 0 && (
            <GradientButton variant="outline" size="sm" onClick={handleNewChat}>
              New Chat
            </GradientButton>
          )}
          <ModeToggle />
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto">
        {isEmpty ? (
          <div className="flex flex-col items-center justify-center h-full gap-4 text-center px-4">
            <div className="rounded-full bg-gradient-to-br from-blue-600 to-indigo-600 p-4 dark:from-blue-500 dark:to-indigo-500">
              <IconCpu2 className="size-8 text-white" />
            </div>
            <div>
              <h2 className="text-2xl font-semibold mb-2">How can I help you today?</h2>
              <p className="text-muted-foreground text-sm max-w-md">
                {selectedInstance
                  ? `Using ${selectedInstance.title} (${selectedInstance.turLLMVendor?.id ?? ""})`
                  : "Select a language model to start chatting."}
              </p>
            </div>
          </div>
        ) : (
          <div className="max-w-3xl mx-auto py-6 px-4 space-y-6">
            {messages.map((message) => (
              <div key={message.id} className="flex gap-3">
                <div className="shrink-0 pt-0.5">
                  <GradientAvatar className="size-7">
                    <GradientAvatarFallback
                      variant={message.role === "assistant" ? "info" : "secondary"}
                    >
                      {message.role === "assistant" ? (
                        <IconCpu2 className="size-4" />
                      ) : (
                        <IconUser className="size-4" />
                      )}
                    </GradientAvatarFallback>
                  </GradientAvatar>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground mb-1">
                    <span>
                      {message.role === "assistant"
                        ? selectedInstance?.title ?? "Assistant"
                        : "You"}
                    </span>
                    {message.role === "assistant" && isLoading && message === messages.at(-1) && (
                      <IconLoader2 className="size-3 animate-spin" />
                    )}
                  </div>
                  {message.attachments && message.attachments.length > 0 && (
                    <div className="flex flex-wrap gap-2 mb-2">
                      {message.attachments.map((att, idx) => (
                        <div
                          key={idx}
                          className="flex items-center gap-1.5 rounded-lg border bg-muted/50 px-2.5 py-1.5 text-xs text-muted-foreground"
                        >
                          <IconFile className="size-3.5 shrink-0" />
                          <span className="truncate max-w-[150px]">{att.name}</span>
                          <span className="text-muted-foreground/60">{formatFileSize(att.size)}</span>
                        </div>
                      ))}
                    </div>
                  )}
                  {message.role === "assistant" ? (
                    <div className="text-sm leading-relaxed prose prose-sm dark:prose-invert max-w-none break-words prose-p:my-2 prose-pre:my-2 prose-ul:my-2 prose-ol:my-2 prose-headings:my-3 prose-code:before:content-none prose-code:after:content-none prose-code:bg-muted prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-pre:bg-muted prose-pre:border prose-pre:rounded-lg">
                      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                        {message.content}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    <div className="text-sm leading-relaxed whitespace-pre-wrap break-words">
                      {message.content}
                    </div>
                  )}
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input */}
      <div className="shrink-0 border-t bg-background">
        <div className="max-w-3xl mx-auto px-4 py-4">
          {/* Attached files chips */}
          {attachedFiles.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-2">
              {attachedFiles.map((file, idx) => (
                <div
                  key={idx}
                  className="flex items-center gap-1.5 rounded-lg border bg-muted/50 px-2.5 py-1.5 text-xs"
                >
                  <IconFile className="size-3.5 shrink-0 text-muted-foreground" />
                  <span className="truncate max-w-[150px]">{file.name}</span>
                  <span className="text-muted-foreground/60">{formatFileSize(file.size)}</span>
                  <button
                    type="button"
                    onClick={() => removeFile(idx)}
                    title={`Remove ${file.name}`}
                    className="ml-0.5 rounded-full p-0.5 hover:bg-muted-foreground/20 transition-colors"
                  >
                    <IconX className="size-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
          <div className="relative flex items-end rounded-xl border bg-muted/50 focus-within:ring-2 focus-within:ring-blue-500/50 focus-within:border-blue-500/50 transition-all">
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="hidden"
              title="Attach files"
              onChange={handleFileInputChange}
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={!selectedLlmId || isLoading}
              className="p-3 text-muted-foreground hover:text-foreground transition-colors disabled:opacity-50 disabled:pointer-events-none"
              title="Attach files"
            >
              <IconPaperclip className="size-5" />
            </button>
            <Textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={
                selectedLlmId
                  ? "Send a message..."
                  : "Select a model to start..."
              }
              disabled={!selectedLlmId || isLoading}
              className="flex-1 resize-none border-0 bg-transparent px-2 py-3 text-sm focus-visible:ring-0 focus-visible:ring-offset-0 min-h-[44px] max-h-[200px]"
              rows={1}
            />
            <div className="p-2">
              <GradientButton
                size="icon-sm"
                disabled={(!input.trim() && attachedFiles.length === 0) || isLoading || !selectedLlmId}
                onClick={handleSend}
                className="rounded-lg"
              >
                {isLoading ? (
                  <IconLoader2 className="size-4 animate-spin" />
                ) : (
                  <IconArrowUp className="size-4" />
                )}
              </GradientButton>
            </div>
          </div>
          {/* Footer: model info + context bar */}
          <div className="flex items-center justify-between mt-2 gap-4">
            <span className="text-xs text-muted-foreground shrink-0">
              {selectedInstance
                ? `${selectedInstance.turLLMVendor?.id ?? ""} · ${selectedInstance.modelName ?? ""}`
                : "No model selected"}
            </span>
            {!isEmpty && (
              <div className="flex items-center gap-2 flex-1 min-w-0 justify-end">
                {/* Context bar */}
                <div className="flex items-center gap-2 max-w-xs w-full">
                  <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${contextBarColor}`}
                      style={{ width: `${contextUsage.percentage}%` }}
                    />
                  </div>
                  <span className="text-xs text-muted-foreground whitespace-nowrap">
                    {contextUsage.percentage}%
                  </span>
                </div>
                {/* Compact button */}
                <button
                  type="button"
                  onClick={handleCompact}
                  disabled={isLoading || isCompacting || messages.length < 4}
                  title={`${100 - contextUsage.percentage}% of context remaining (${formatTokenCount(contextUsage.tokens)}/${formatTokenCount(contextWindow)} tokens). Click to compact.`}
                  className="flex items-center gap-1 rounded-md border px-2 py-1 text-xs text-muted-foreground hover:text-foreground hover:bg-muted transition-colors disabled:opacity-50 disabled:pointer-events-none shrink-0"
                >
                  {isCompacting ? (
                    <IconLoader2 className="size-3 animate-spin" />
                  ) : (
                    <IconBolt className="size-3" />
                  )}
                  Compact
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
