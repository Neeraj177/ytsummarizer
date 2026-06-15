import ReactMarkdown from "react-markdown";
import { FileText, Clock, CheckCircle2, XCircle, Loader2 } from "lucide-react";

const StatusBadge = ({ status }) => {
  const map = {
    PENDING:    { icon: Clock,         color: "text-amber-400",   bg: "bg-amber-400/10 border-amber-400/20",  label: "Pending" },
    PROCESSING: { icon: Loader2,       color: "text-indigo-400",  bg: "bg-indigo-400/10 border-indigo-400/20", label: "Processing" },
    COMPLETED:  { icon: CheckCircle2,  color: "text-emerald-400", bg: "bg-emerald-400/10 border-emerald-400/20", label: "Completed" },
    FAILED:     { icon: XCircle,       color: "text-red-400",     bg: "bg-red-400/10 border-red-400/20",     label: "Failed" },
  };
  const cfg = map[status] || map.PENDING;
  const Icon = cfg.icon;

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-xs font-medium ${cfg.bg} ${cfg.color}`}>
      <Icon className={`w-3 h-3 ${status === "PROCESSING" ? "animate-spin" : ""}`} />
      {cfg.label}
    </span>
  );
};

export default function SummaryPanel({ jobData }) {
  const { status, summary, videoId } = jobData;

  return (
    <div className="h-full flex flex-col">
      {/* Panel header */}
      <div className="flex items-center justify-between mb-5 shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center">
            <FileText className="w-3.5 h-3.5 text-indigo-400" />
          </div>
          <span className="text-sm font-semibold text-slate-200">Technical Summary</span>
        </div>
        <StatusBadge status={status} />
      </div>

      {/* Content area */}
      <div className="flex-1 overflow-y-auto min-h-0">
        {!summary && (status === "PENDING" || status === "PROCESSING") && (
          <div className="flex flex-col items-center justify-center h-full gap-4 py-12">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border border-indigo-500/30 animate-ping absolute inset-0" />
              <div className="w-12 h-12 rounded-full border border-indigo-500/20 flex items-center justify-center relative z-10">
                <Loader2 className="w-5 h-5 text-indigo-400 animate-spin" />
              </div>
            </div>
            <div className="text-center">
              <p className="text-sm font-medium text-slate-300 mb-1">
                {status === "PENDING" ? "Queued for processing…" : "Extracting & analyzing frames…"}
              </p>
              <p className="text-xs text-slate-500">This may take a minute for longer videos</p>
            </div>
            {/* Skeleton lines */}
            <div className="w-full max-w-xs space-y-2 mt-4">
              {[100, 80, 90, 65, 75].map((w, i) => (
                <div
                  key={i}
                  className="h-3 rounded-full bg-slate-800/80 animate-pulse"
                  style={{ width: `${w}%`, animationDelay: `${i * 0.15}s` }}
                />
              ))}
            </div>
          </div>
        )}

        {status === "FAILED" && (
          <div className="flex flex-col items-center justify-center h-full gap-3 py-12">
            <XCircle className="w-10 h-10 text-red-400/70" />
            <p className="text-sm text-slate-400 text-center">Processing failed for video <span className="text-slate-200 font-mono">{videoId}</span>.</p>
          </div>
        )}

        {summary && (
          <div className="fade-in prose-custom">
            <ReactMarkdown
              components={{
                h1: ({ children }) => (
                  <h1 className="text-lg font-bold text-slate-100 mb-3 mt-5 first:mt-0 border-b border-slate-800 pb-2">{children}</h1>
                ),
                h2: ({ children }) => (
                  <h2 className="text-base font-semibold text-slate-200 mb-2 mt-4 first:mt-0">{children}</h2>
                ),
                h3: ({ children }) => (
                  <h3 className="text-sm font-semibold text-indigo-300 mb-1.5 mt-3">{children}</h3>
                ),
                p: ({ children }) => (
                  <p className="text-sm text-slate-300 leading-relaxed mb-3">{children}</p>
                ),
                ul: ({ children }) => (
                  <ul className="space-y-1.5 mb-3 pl-1">{children}</ul>
                ),
                li: ({ children }) => (
                  <li className="flex items-start gap-2 text-sm text-slate-300">
                    <span className="w-1 h-1 rounded-full bg-indigo-400 mt-2 shrink-0" />
                    <span className="leading-relaxed">{children}</span>
                  </li>
                ),
                strong: ({ children }) => (
                  <strong className="font-semibold text-slate-100">{children}</strong>
                ),
                code: ({ children }) => (
                  <code className="text-xs bg-slate-800/80 text-emerald-300 px-1.5 py-0.5 rounded font-mono">{children}</code>
                ),
                blockquote: ({ children }) => (
                  <blockquote className="border-l-2 border-indigo-500/50 pl-3 py-0.5 my-3 text-slate-400 italic text-sm">{children}</blockquote>
                ),
              }}
            >
              {summary}
            </ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  );
}
