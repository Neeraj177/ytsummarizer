import { useState } from "react";
import LandingView from "./components/LandingView";
import DashboardView from "./components/DashboardView";

const SCREENS = {
  LANDING: "LANDING",
  DASHBOARD: "DASHBOARD",
};

export default function App() {
  const [screen, setScreen] = useState(SCREENS.LANDING);
  const [jobData, setJobData] = useState(null);
  const [videoUrl, setVideoUrl] = useState("");

  const handleJobCreated = (data, url) => {
    setJobData(data);
    setVideoUrl(url);
    setScreen(SCREENS.DASHBOARD);
  };

  const handleBack = () => {
    setScreen(SCREENS.LANDING);
    setJobData(null);
    setVideoUrl("");
  };

  if (screen === SCREENS.DASHBOARD && jobData) {
    return (
      <DashboardView
        initialJob={jobData}
        videoUrl={videoUrl}
        onBack={handleBack}
      />
    );
  }

  return <LandingView onJobCreated={handleJobCreated} />;
}
