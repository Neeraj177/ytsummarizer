import { useState, useEffect, useRef, useCallback } from "react";
import { Send, Bot, Sparkles } from "lucide-react";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "https://ytsummarizer-backend-20ho.onrender.com";

const ThinkingIndicator = () => (
  <div className="flex items-start gap-3 fade-in">
    <div className="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700/60 flex items-center justify-center shrink-0">
      <Bot className="w-3.5 h-3.5 text-slate-400" />
    </div>
    <div className="chat-bubble-ai rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-1.5">
      <span className="thinking-dot w-1.5 h-1.5 rounded-full bg-slate-400 inline-block" />
      <span className="thinking-dot w-1.5 h-1.5 rounded-full bg-slate-400 inline-block" />
      <span className="thinking-dot w-1.5 h-1.5 rounded-full bg-slate-400 inline-block" />
    </div>
  </div>
);

export default function ChatPanel({ jobId, jobStatus }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  const scrollToBottom = useCallback(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  // Load chat history
  useEffect(() => {
    if (!jobId) return;
    const load = async () => {
      try {
        const res = await fetch(`${API_BASE}/api/v1/chat/${jobId}`);
        if (res.ok) {
          const data = await res.json();
          const history = Array.isArray(data)
            ? data.map((m) => ({
                role: m.role || (m.question ? "user" : "assistant"),
                content: m.question || m.answer || m.content || "",
              }))
            : [];
          setMessages(history);
        }
      } catch (_) {
        // History load failure is non-critical
      } finally {
        setLoadingHistory(false);
      }
    };
    load();
  }, [jobId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  const handleSend = async () => {
    const q = input.trim();
    if (!q || sending) return;

    const userMsg = { role: "user", content: q };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setSending(true);

    try {
      const res = await fetch(`${API_BASE}/api/v1/chat/${jobId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: q }),
      });

      if (!res.ok) throw new Error("Chat request failed");
      const data = await res.json();
      const aiMsg = { role: "assistant", content: data.answer || "No response received." };
      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      const errMsg = {
        role: "assistant",
        content: "⚠️ Failed to reach the AI assistant. Please check your backend connection.",
        isError: true,
      };
      setMessages((prev) => [...prev, errMsg]);
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const isReady = jobStatus === "COMPLETED";

  return (
    <div className="h-full flex flex-col">
      {/* Panel header */}
      <div className="flex items-center justify-between mb-5 shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center">
            <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
          </div>
          <span className="text-sm font-semibold text-slate-200">Contextual AI Assistant</span>
        </div>
        <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${
          isReady
            ? "text-emerald-400 bg-emerald-400/10 border-emerald-400/20"
            : "text-slate-500 bg-slate-800/50 border-slate-700/40"
        }`}>
          {isReady ? "Ready" : "Waiting for summary…"}
        </span>
      </div>

      {/* Messages feed */}
      <div className="flex-1 overflow-y-auto min-h-0 space-y-4 pr-1 pb-2">
        {loadingHistory && (
          <div className="flex justify-center pt-8">
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <svg className="spin-slow w-3.5 h-3.5" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              Loading conversation history…
            </div>
          </div>
        )}

        {!loadingHistory && messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full gap-3 py-8 text-center">
            <div className="w-10 h-10 rounded-xl bg-slate-800/80 border border-slate-700/50 flex items-center justify-center">
              <Bot className="w-5 h-5 text-slate-500" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400 mb-1">
                {isReady ? "Ask anything about this video" : "Chat unlocks after summary completes"}
              </p>
              <p className="text-xs text-slate-600">
                {isReady
                  ? "Your questions are grounded in the video's actual content."
                  : "The AI needs the full summary before it can answer accurately."}
              </p>
            </div>
          </div>
        )}

        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex items-start gap-3 fade-in ${msg.role === "user" ? "flex-row-reverse" : ""}`}
          >
            {msg.role === "assistant" && (
              <div className="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700/60 flex items-center justify-center shrink-0">
                <Bot className="w-3.5 h-3.5 text-slate-400" />
              </div>
            )}
            <div
              className={`max-w-[82%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
                msg.role === "user"
                  ? "chat-bubble-user rounded-tr-sm text-white"
                  : `chat-bubble-ai rounded-tl-sm ${msg.isError ? "text-red-300" : "text-slate-300"}`
              }`}
            >
              {msg.content}
            </div>
          </div>
        ))}

        {sending && <ThinkingIndicator />}
        <div ref={bottomRef} />
      </div>

      {/* Input bar */}
      <div className="shrink-0 pt-4 border-t border-slate-800/60">
        <div className="relative flex items-end gap-2">
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isReady ? "Ask a question about the video…" : "Waiting for processing to complete…"}
            disabled={!isReady || sending}
            rows={1}
            className="flex-1 bg-slate-900/60 border border-slate-700/60 rounded-xl px-4 py-3 pr-12
                       text-sm text-slate-200 placeholder-slate-600 resize-none
                       focus:outline-none focus:border-indigo-500/60
                       focus:shadow-[0_0_0_3px_rgba(99,102,241,0.08)]
                       disabled:opacity-40 transition-all duration-200 max-h-28 min-h-[46px]"
            style={{ lineHeight: "1.5" }}
            onInput={(e) => {
              e.target.style.height = "auto";
              e.target.style.height = Math.min(e.target.scrollHeight, 112) + "px";
            }}
          />
          <button
            onClick={handleSend}
            disabled={!isReady || sending || !input.trim()}
            className="absolute right-2 bottom-2 w-8 h-8 rounded-lg gradient-btn flex items-center justify-center
                       disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:transform-none
                       disabled:hover:shadow-none transition-all"
          >
            <Send className="w-3.5 h-3.5 text-white" />
          </button>
        </div>
        <p className="text-xs text-slate-600 mt-1.5 text-right">Enter to send · Shift+Enter for newline</p>
      </div>
    </div>
  );
}
