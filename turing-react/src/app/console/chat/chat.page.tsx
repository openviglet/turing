"use client"
import { IconArrowUp, IconCpu2, IconLoader2, IconUser } from "@tabler/icons-react"
import { useCallback, useEffect, useRef, useState } from "react"
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
import type { TurLLMInstance } from "@/models/llm/llm-instance.model"
import { type ChatMessageItem, TurChatService } from "@/services/chat/chat.service"

const turChatService = new TurChatService()

interface ChatMessage extends ChatMessageItem {
  id: string
}

const generateId = () => crypto.randomUUID()

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([])
  const [selectedLlmId, setSelectedLlmId] = useState<string>("")
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

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

  const handleSend = async () => {
    const trimmed = input.trim()
    if (!trimmed || isLoading || !selectedLlmId) return

    const userMessage: ChatMessage = {
      id: generateId(),
      role: "user",
      content: trimmed,
    }

    const updatedMessages = [...messages, userMessage]
    setMessages(updatedMessages)
    setInput("")
    setIsLoading(true)

    try {
      const apiMessages: ChatMessageItem[] = updatedMessages.map(({ role, content }) => ({
        role,
        content,
      }))
      const response = await turChatService.send(selectedLlmId, apiMessages)
      const assistantMessage: ChatMessage = {
        id: generateId(),
        role: "assistant",
        content: response.content,
      }
      setMessages((prev) => [...prev, assistantMessage])
    } catch {
      toast.error("Failed to get a response. Please check your LLM configuration.")
    } finally {
      setIsLoading(false)
    }
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
  }

  const isEmpty = messages.length === 0

  return (
    <div className="flex flex-col h-[calc(100vh-2rem)] max-h-screen">
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
                  <div className="text-xs font-medium text-muted-foreground mb-1">
                    {message.role === "assistant"
                      ? selectedInstance?.title ?? "Assistant"
                      : "You"}
                  </div>
                  <div className="text-sm leading-relaxed whitespace-pre-wrap break-words">
                    {message.content}
                  </div>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex gap-3">
                <div className="shrink-0 pt-0.5">
                  <GradientAvatar className="size-7">
                    <GradientAvatarFallback variant="info">
                      <IconCpu2 className="size-4" />
                    </GradientAvatarFallback>
                  </GradientAvatar>
                </div>
                <div className="flex-1">
                  <div className="text-xs font-medium text-muted-foreground mb-1">
                    {selectedInstance?.title ?? "Assistant"}
                  </div>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <IconLoader2 className="size-4 animate-spin" />
                    <span>Thinking...</span>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input */}
      <div className="shrink-0 border-t bg-background">
        <div className="max-w-3xl mx-auto px-4 py-4">
          <div className="relative flex items-end rounded-xl border bg-muted/50 focus-within:ring-2 focus-within:ring-blue-500/50 focus-within:border-blue-500/50 transition-all">
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
              className="flex-1 resize-none border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0 focus-visible:ring-offset-0 min-h-[44px] max-h-[200px]"
              rows={1}
            />
            <div className="p-2">
              <GradientButton
                size="icon-sm"
                disabled={!input.trim() || isLoading || !selectedLlmId}
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
          <div className="text-center mt-2">
            <span className="text-xs text-muted-foreground">
              {selectedInstance
                ? `${selectedInstance.turLLMVendor?.id ?? ""} · ${selectedInstance.modelName ?? ""}`
                : "No model selected"}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}
