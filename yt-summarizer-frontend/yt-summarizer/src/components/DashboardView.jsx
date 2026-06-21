import { useState, useEffect, useRef, useCallback } from "react";
import { ArrowLeft, Copy, Check, ExternalLink, FileText, Sparkles, Sun, Moon } from "lucide-react";
import SummaryPanel from "./SummaryPanel";
import ChatPanel from "./ChatPanel";
import YouTubeLogo from "./YouTubeLogo";
import { useTheme } from "../ThemeContext";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "https://ytsummarizer-backend-20ho.onrender.com";
const POLL_INTERVAL = 4000;

export default function DashboardView({ initialJob, videoUrl, onBack }) {
  const [jobData, setJobData] = useState(initialJob);
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState("summary"); // mobile tab state
  const intervalRef = useRef(null);
  const { theme, toggleTheme } = useTheme();

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const pollStatus = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/api/videos/status/${initialJob.jobId}`);
      if (!res.ok) return;
      const data = await res.json();
      setJobData(data);
      if (data.status === "COMPLETED" || data.status === "FAILED") {
        stopPolling();
      }
    } catch (_) {}
  }, [initialJob.jobId, stopPolling]);

  useEffect(() => {
    if (initialJob.status === "COMPLETED" || initialJob.status === "FAILED") return;
    pollStatus();
    intervalRef.current = setInterval(pollStatus, POLL_INTERVAL);
    return stopPolling;
  }, [initialJob.jobId, initialJob.status, pollStatus, stopPolling]);

  const handleCopy = () => {
    navigator.clipboard.writeText(videoUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const truncate = (str, n) => (str.length > n ? str.slice(0, n) + "…" : str);

  return (
    <div
      className="h-screen flex flex-col radial-bg overflow-hidden transition-colors duration-300"
      style={{ background: "var(--bg-primary)" }}
    >
      {/* Top navigation bar */}
      <nav
        className="glass shrink-0 border-b px-3 sm:px-5 py-3 flex items-center gap-2 sm:gap-4"
        style={{ borderColor: "var(--border-primary)" }}
      >
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 text-xs px-2 sm:px-2.5 py-1.5 rounded-lg hover:bg-slate-800/40 transition-all duration-150 shrink-0"
          style={{ color: "var(--text-secondary)" }}
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Back</span>
        </button>

        <div className="w-px h-4 hidden sm:block" style={{ background: "var(--border-primary)" }} />

        {/* Logo — hidden on very small screens */}
        <YouTubeLogo className="w-7 h-5 sm:w-8 sm:h-5.5 shrink-0 hidden xs:block" />

        {/* Video ID pill */}
        <div
          className="flex items-center gap-1.5 px-2 sm:px-3 py-1 rounded-lg border shrink-0"
          style={{ background: "var(--bg-tertiary)", borderColor: "var(--border-secondary)" }}
        >
          <span className="text-[10px] sm:text-xs hidden sm:inline" style={{ color: "var(--text-tertiary)" }}>ID</span>
          <span className="text-[10px] sm:text-xs font-mono font-medium text-indigo-400">
            {jobData.videoId}
          </span>
        </div>

        {/* URL display — hidden on mobile to save space */}
        <div className="flex-1 min-w-0 hidden md:flex items-center gap-1.5 group">
          <div
            className="flex-1 min-w-0 px-3 py-1.5 rounded-lg border"
            style={{ background: "var(--bg-tertiary)", borderColor: "var(--border-secondary)" }}
          >
            <span className="text-xs font-mono block truncate" style={{ color: "var(--text-tertiary)" }}>
              {truncate(videoUrl, 60)}
            </span>
          </div>
          <button
            onClick={handleCopy}
            className="shrink-0 w-7 h-7 rounded-lg hover:bg-slate-800/40 flex items-center justify-center transition-all opacity-0 group-hover:opacity-100"
            style={{ color: "var(--text-tertiary)" }}
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>

<a
                      href={videoUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="shrink-0 w-7 h-7 rounded-lg hover:bg-slate-800/40 flex items-center justify-center transition-all opacity-0 group-hover:opacity-100"
                      style={{ color: "var(--text-tertiary)" }}
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
        </div>

        {/* Spacer pushes theme toggle to the right on mobile */}
        <div className="flex-1 md:flex-none" />

        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          aria-label="Toggle theme"
          className="shrink-0 w-8 h-8 rounded-lg hover:bg-slate-800/40 flex items-center justify-center transition-all"
          style={{ color: "var(--text-secondary)" }}
        >
          {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
      </nav>

      {/* Mobile tab switcher — only visible below md breakpoint */}
      <div
        className="flex md:hidden border-b shrink-0"
        style={{ borderColor: "var(--border-primary)" }}
      >
        <button
          onClick={() => setActiveTab("summary")}
          className="flex-1 flex items-center justify-center gap-1.5 py-3 text-xs font-medium transition-all"
          style={{
            color: activeTab === "summary" ? "var(--text-primary)" : "var(--text-tertiary)",
            borderBottom: activeTab === "summary" ? "2px solid #6366f1" : "2px solid transparent",
          }}
        >
          <FileText className="w-3.5 h-3.5" />
          Summary
        </button>
        <button
          onClick={() => setActiveTab("chat")}
          className="flex-1 flex items-center justify-center gap-1.5 py-3 text-xs font-medium transition-all"
          style={{
            color: activeTab === "chat" ? "var(--text-primary)" : "var(--text-tertiary)",
            borderBottom: activeTab === "chat" ? "2px solid #6366f1" : "2px solid transparent",
          }}
        >
          <Sparkles className="w-3.5 h-3.5" />
          Chat
        </button>
      </div>

      {/* Dual-frame canvas — side by side on desktop, tabbed on mobile */}
      <div className="flex-1 min-h-0 flex">
        {/* Summary panel */}
        <div
          className={`min-h-0 p-3 sm:p-6 overflow-hidden flex flex-col w-full md:w-1/2 md:border-r ${
            activeTab === "summary" ? "flex" : "hidden md:flex"
          }`}
          style={{ borderColor: "var(--border-secondary)" }}
        >
          <div
            className="flex-1 glass rounded-xl p-4 sm:p-5 min-h-0 flex flex-col shadow-xl border"
            style={{ borderColor: "var(--border-secondary)" }}
          >
            <SummaryPanel jobData={jobData} />
          </div>
        </div>

        {/* Chat panel */}
        <div
          className={`min-h-0 p-3 sm:p-6 overflow-hidden flex flex-col w-full md:w-1/2 ${
            activeTab === "chat" ? "flex" : "hidden md:flex"
          }`}
        >
          <div
            className="flex-1 glass rounded-xl p-4 sm:p-5 min-h-0 flex flex-col shadow-xl border"
            style={{ borderColor: "var(--border-secondary)" }}
          >
            <ChatPanel jobId={jobData.jobId} jobStatus={jobData.status} />
          </div>
        </div>
      </div>
    </div>
  );
}