import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type { TuringClient } from "../client.js";
import { createChatController, type ChatMessage } from "../controllers/chat.js";

/**
 * T1258 — `initialMessages` seeds a persisted chat that has nothing stored yet, so a
 * host (the WordPress plugin's search answer) can open its assistant with the first
 * exchange already in it. A stored conversation still wins.
 */
// The controller asks whether chat is on when it is created; the answer does not matter here.
const client = { get: () => Promise.resolve({ data: { enabled: true } }) } as unknown as TuringClient;
const KEY = "turing.chat.tarnfell.assistant";

const seed: ChatMessage[] = [
  { id: "q", role: "user", content: "how does the library work?", timestamp: 1 },
  { id: "a", role: "assistant", content: "It opens at 8.", timestamp: 2 },
];

function memoryStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => void data.delete(key),
    setItem: (key, value) => void data.set(key, String(value)),
  };
}

describe("createChatController — T1258 initialMessages with persist", () => {
  let previous: Storage | undefined;

  beforeEach(() => {
    previous = globalThis.sessionStorage;
    Object.defineProperty(globalThis, "sessionStorage", { value: memoryStorage(), configurable: true });
  });

  afterEach(() => {
    Object.defineProperty(globalThis, "sessionStorage", { value: previous, configurable: true });
  });

  it("seeds a persisted chat that has nothing stored", () => {
    const chat = createChatController(client, { site: "tarnfell", persist: "assistant", initialMessages: seed });

    expect(chat.getState().messages.map((m) => m.content)).toEqual([
      "how does the library work?",
      "It opens at 8.",
    ]);
    chat.destroy();
  });

  it("keeps a stored conversation over the seed", () => {
    sessionStorage.setItem(
      KEY,
      JSON.stringify({
        conversationId: "c-1",
        messages: [{ id: "old", role: "user", content: "earlier", timestamp: 0 }],
      }),
    );

    const chat = createChatController(client, { site: "tarnfell", persist: "assistant", initialMessages: seed });

    expect(chat.getState().messages.map((m) => m.content)).toEqual(["earlier"]);
    expect(chat.getState().conversationId).toBe("c-1");
    chat.destroy();
  });

  it("seeds again after a reset emptied the stored conversation", () => {
    const first = createChatController(client, { site: "tarnfell", persist: "assistant" });
    first.reset();
    first.destroy();

    const chat = createChatController(client, { site: "tarnfell", persist: "assistant", initialMessages: seed });

    expect(chat.getState().messages).toHaveLength(2);
    chat.destroy();
  });
});
