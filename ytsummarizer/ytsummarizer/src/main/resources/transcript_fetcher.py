import sys
import json
import requests
from http.cookiejar import MozillaCookieJar
from youtube_transcript_api import YouTubeTranscriptApi

def main():
    video_id = sys.argv[1]
    cookies_path = sys.argv[2] if len(sys.argv) > 2 else None

    session = requests.Session()

    if cookies_path:
        try:
            jar = MozillaCookieJar(cookies_path)
            jar.load(ignore_discard=True, ignore_expires=True)
            session.cookies = jar
            print("Cookies loaded successfully", file=sys.stderr)
        except Exception as e:
            print(f"Cookie load failed: {e}", file=sys.stderr)

    ytt_api = YouTubeTranscriptApi(http_client=session)

    try:
        transcript = ytt_api.fetch(video_id, languages=['en', 'hi', 'en-IN'])
        text = " ".join([snippet.text for snippet in transcript])
        print(text)
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()