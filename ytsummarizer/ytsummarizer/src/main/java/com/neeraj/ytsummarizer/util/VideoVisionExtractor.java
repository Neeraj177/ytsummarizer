package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VideoVisionExtractor {

    public static List<File> extractVideoFrames(String videoId) {
        List<File> extractedFrames = new ArrayList<>();
        String tempDir = System.getProperty("java.io.tmpdir");
        if (!tempDir.endsWith(File.separator)) tempDir += File.separator;

        String uniqueFolder = tempDir + "yt_frames_" + UUID.randomUUID();
        File folder = new File(uniqueFolder);
        folder.mkdirs();

        System.out.println("[Vision-Engine] Target extraction folder: " + uniqueFolder);

        String videoPath = uniqueFolder + File.separator + "video.mp4";
        String framePattern = uniqueFolder + File.separator + "frame_%03d.jpg";

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String dlpCommand = osName.contains("win") ? "yt-dlp.exe" : "yt-dlp";

            System.out.println("[Vision-Engine] Executing video download via: " + dlpCommand);

            ProcessBuilder dlpPb = new ProcessBuilder(
                    dlpCommand,
                    "-f", "worst[ext=mp4]/worst",
                    "--no-playlist",
                    "--ies", "Invidious,InvidiousPlaylist,default,-youtube,-youtubeplaylist",
                    "--extractor-args", "invidious:instance=yewtu.be",  // ← Working instance specify karo
                    "-o", videoPath,
                    "https://www.youtube.com/watch?v=" + videoId
            );

            dlpPb.redirectErrorStream(true);
            Process dlpProcess = dlpPb.start();
            BufferedReader dlpReader = new BufferedReader(new InputStreamReader(dlpProcess.getInputStream()));
            String line;
            while ((line = dlpReader.readLine()) != null) {
                System.out.println("[yt-dlp-stream] " + line);
            }
            dlpReader.close();
            int dlpExitCode = dlpProcess.waitFor();
            System.out.println("[Vision-Engine] yt-dlp exit execution code: " + dlpExitCode);

            File downloadedVideo = new File(videoPath);
            if (!downloadedVideo.exists() || downloadedVideo.length() == 0) {
                System.err.println("[Vision-Engine] Failsafe Guard Triggered: video.mp4 disk par nahi mili!");
                File altVideo = new File(videoPath + ".mp4");
                if (altVideo.exists()) {
                    altVideo.renameTo(downloadedVideo);
                    System.out.println("[Vision-Engine] Resolved double extension anomaly successfully.");
                } else {
                    throw new RuntimeException("yt-dlp download pipeline collapsed. Video file missing on temp drive.");
                }
            }

            String ffmpegCmd = osName.contains("win") ? "ffmpeg.exe" : "ffmpeg";
            ProcessBuilder ffmpegPb = new ProcessBuilder(
                    ffmpegCmd,
                    "-y",
                    "-i", videoPath,
                    "-vf", "fps=1/10",
                    "-q:v", "2",
                    "-frames:v", "10",
                    framePattern
            );
            ffmpegPb.redirectErrorStream(true);
            Process ffmpegProcess = ffmpegPb.start();
            BufferedReader ffmpegReader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
            while ((line = ffmpegReader.readLine()) != null) {
                System.out.println("[ffmpeg] " + line);
            }
            ffmpegReader.close();
            ffmpegProcess.waitFor();

            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg")) {
                        extractedFrames.add(f);
                    }
                }
            }

            System.out.println("[Vision-Engine] Extracted " + extractedFrames.size() + " frames.");

        } catch (Exception e) {
            System.err.println("[Vision-Engine] Frame extraction crashed: " + e.getMessage());
        }

        return extractedFrames;
    }

    public static void cleanUpFrames(List<File> frames) {
        if (frames == null || frames.isEmpty()) return;
        try {
            File parentFolder = frames.get(0).getParentFile();
            for (File f : frames) {
                if (f.exists()) f.delete();
            }
            if (parentFolder != null && parentFolder.exists()) {
                parentFolder.delete();
            }
            System.out.println("[Vision-Engine] Temporary visual frames storage cache cleared.");
        } catch (Exception e) {
            System.err.println("[Vision-Engine] Cache clean up failed: " + e.getMessage());
        }
    }
}