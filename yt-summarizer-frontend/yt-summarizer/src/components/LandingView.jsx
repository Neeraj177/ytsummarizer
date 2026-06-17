import { useState } from "react";
import { Link2, AlertTriangle, Sparkles } from "lucide-react";
import YouTubeLogo from "./YouTubeLogo";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "https://ytsummarizer-backend-20ho.onrender.com";

function extractVideoId(url) {
  const match = url.match(/(?:v=|youtu\.be\/)([^&\n?#]+)/);
  return match ? match[1] : null;
}

export default function LandingView({ onJobCreated }) {
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!url.trim()) return;
    setError("");
    setLoading(true);

    try {
      const videoId = extractVideoId(url.trim());
      let transcript = "";

      // Browser se transcript fetch karo
      if (videoId) {
        try {
          const proxyRes = await fetch(
            `https://corsproxy.io/?https://www.youtube.com/api/timedtext?v=${videoId}&lang=en&fmt=json3`
          );
          if (proxyRes.ok) {
            const ytData = await proxyRes.json();
            transcript = ytData?.events
              ?.map(ev => ev.segs?.map(s => s.utf8).join(""))
              .filter(Boolean)
              .join(" ") || "";
            console.log("Transcript fetched! Length:", transcript.length);
          }
        } catch (e) {
          console.log("Browser transcript fetch failed, backend will handle");
        }
      }

      const res = await fetch(`${API_BASE}/api/videos/process`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          url: url.trim(),
          transcript: transcript || null
        }),
      });

      if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText || `Server returned ${res.status}`);
      }

      const data = await res.json();
      onJobCreated(data, url.trim());
    } catch (err) {
      if (err.message.includes("Failed to fetch") || err.message.includes("NetworkError")) {
        setError("Cannot reach the backend server. Ensure your Spring Boot application is running.");
      } else {
        setError(err.message || "Something went wrong. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen radial-bg flex items-center justify-center px-4 relative overflow-hidden">
      {/* Ambient orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full bg-indigo-600/5 blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full bg-emerald-500/5 blur-3xl pointer-events-none" />

      <div className="w-full max-w-xl slide-up">
        {/* Card */}
        <div className="glass rounded-2xl p-8 relative overflow-hidden glow-border-indigo">
          {/* Subtle top gradient line */}
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-indigo-500/50 to-transparent" />

          {/* Header */}
          <div className="flex flex-col items-center mb-8">
            <div className="pulse-ring mb-5">
              <YouTubeLogo className="w-16 h-11" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-center gradient-text leading-tight mb-2">
              AI YouTube Summarizer
              <br />& Chat
            </h1>
            <p className="text-slate-400 text-sm text-center mt-1 max-w-xs leading-relaxed">
              Drop any YouTube link. Get an instant technical summary and chat with the video's content.
            </p>
          </div>

          {/* Error banner */}
          {error && (
            <div className="mb-5 flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/8 px-4 py-3 text-sm text-red-300 fade-in">
              <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="relative group">
              <div className="absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none">
                <Link2 className="w-4 h-4 text-slate-500 group-focus-within:text-indigo-400 transition-colors" />
              </div>
              <input
                type="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://www.youtube.com/watch?v=..."
                disabled={loading}
                className="w-full bg-slate-900/60 border border-slate-700/60 rounded-xl pl-10 pr-4 py-3.5
                           text-sm text-slate-200 placeholder-slate-500
                           focus:outline-none focus:border-indigo-500/70 focus:bg-slate-900/80
                           focus:shadow-[0_0_0_3px_rgba(99,102,241,0.1)]
                           disabled:opacity-50 transition-all duration-200"
              />
            </div>

            <button
              type="submit"
              disabled={loading || !url.trim()}
              className="w-full gradient-btn rounded-xl py-3.5 text-sm font-semibold text-white
                         disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:transform-none
                         disabled:hover:shadow-none flex items-center justify-center gap-2.5"
            >
              {loading ? (
                <>
                  <svg className="spin-slow w-4 h-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
                    <path className="opacity-90" fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  <span>Spawning analysis threads…</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Analyze Video</span>
                </>
              )}
            </button>
          </form>

          {/* Footer hint */}
          <p className="text-center text-xs text-slate-600 mt-5">
            Powered by yt-dlp · FFmpeg · Spring AI RAG
          </p>
        </div>
      </div>
    </div>
  );
}