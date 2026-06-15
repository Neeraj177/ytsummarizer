export default function YouTubeLogo({ className = "" }) {
  return (
    <svg
      viewBox="0 0 120 84"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      <defs>
        <linearGradient id="ytGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#ff4444" />
          <stop offset="100%" stopColor="#cc0000" />
        </linearGradient>
        <filter id="ytGlow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur" />
          <feMerge>
            <feMergeNode in="coloredBlur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <rect
        x="2" y="2" width="116" height="80" rx="18"
        fill="url(#ytGrad)"
        filter="url(#ytGlow)"
        opacity="0.95"
      />
      <polygon
        points="46,24 46,60 82,42"
        fill="white"
        opacity="0.95"
      />
    </svg>
  );
}
