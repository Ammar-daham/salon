import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The Docker image (Dockerfile) runs a self-contained server; `npm run build` and `npm start` stay as they are.
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
};

export default nextConfig;
