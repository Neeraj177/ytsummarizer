import { useState, useEffect, useRef, useCallback } from "react";
import { ArrowLeft, Copy, Check, ExternalLink } from "lucide-react";
import SummaryPanel from "./SummaryPanel";
import ChatPanel from "./ChatPanel";
import YouTubeLogo from "./YouTubeLogo";

const API_BASE = "http://localhost:8080";
const POLL_INTERVAL = 4000;

export default function DashboardView({ initialJob, videoUrl, onBack }) {
  const [jobData, setJobData] = useState(initialJob);
  const [copied, setCopied] = useState(false);
  const intervalRef = useRef(null);

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
    } catch (_) {
      // silent — will retry next interval
    }
  }, [initialJob.jobId, stopPolling]);

  useEffect(() => {
    // Don't poll if already terminal
    if (initialJob.status === "COMPLETED" || initialJob.status === "FAILED") return;

    pollStatus(); // immediate first hit
    intervalRef.current = setInterval(pollStatus, POLL_INTERVAL);
    return stopPolling;
  }, [initialJob.jobId, initialJob.status, pollStatus, stopPolling]);

  const handleCopy = () => {
    navigator.clipboard.writeText(videoUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const truncate = (str, n) => str.length > n ? str.slice(0, n) + "…" : str;

  return (
    <div className="h-screen flex flex-col bg-[#020617] radial-bg overflow-hidden">
      {/* Top navigation bar */}
      <nav className="glass shrink-0 border-b border-slate-800/60 px-5 py-3 flex items-center gap-4">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200
                     px-2.5 py-1.5 rounded-lg hover:bg-slate-800/70 transition-all duration-150 shrink-0"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back
        </button>

        <div className="w-px h-4 bg-slate-800" />

        {/* Logo */}
        <YouTubeLogo className="w-8 h-5.5 shrink-0" />

        {/* Video ID pill */}
        <div className="flex items-center gap-2 px-3 py-1 rounded-lg bg-slate-900/60 border border-slate-800/60 shrink-0">
          <span className="text-xs text-slate-500">ID</span>
          <span className="text-xs font-mono font-medium text-indigo-300">{jobData.videoId}</span>
        </div>

        {/* URL display */}
        <div className="flex-1 min-w-0 flex items-center gap-1.5 group">
          <div className="flex-1 min-w-0 px-3 py-1.5 rounded-lg bg-slate-900/40 border border-slate-800/40">
            <span className="text-xs font-mono text-slate-500 block truncate">
              {truncate(videoUrl, 60)}
            </span>
          </div>
          <button
            onClick={handleCopy}
            className="shrink-0 w-7 h-7 rounded-lg hover:bg-slate-800/70 flex items-center justify-center
                       text-slate-500 hover:text-slate-300 transition-all opacity-0 group-hover:opacity-100"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
          <a
            href={videoUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="shrink-0 w-7 h-7 rounded-lg hover:bg-slate-800/70 flex items-center justify-center
                       text-slate-500 hover:text-slate-300 transition-all opacity-0 group-hover:opacity-100"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </nav>

      {/* Dual-frame canvas */}
      <div className="flex-1 min-h-0 flex">
        {/* Left — Summary */}
        <div className="w-1/2 min-h-0 border-r border-slate-800/50 p-6 overflow-hidden flex flex-col">
          <div className="flex-1 glass rounded-xl p-5 min-h-0 flex flex-col shadow-xl">
            <SummaryPanel jobData={jobData} />
          </div>
        </div>

        {/* Right — Chat */}
        <div className="w-1/2 min-h-0 p-6 overflow-hidden flex flex-col">
          <div className="flex-1 glass rounded-xl p-5 min-h-0 flex flex-col shadow-xl">
            <ChatPanel jobId={jobData.jobId} jobStatus={jobData.status} />
          </div>
        </div>
      </div>
    </div>
  );
}
