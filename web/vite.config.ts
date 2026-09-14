/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo, /api va a la API local (o a la del iMac por Tailscale cambiando el target).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { "/api": { target: process.env.API_TARGET ?? "http://localhost:8080", changeOrigin: true } },
  },
  test: { environment: "jsdom", globals: true },
});
