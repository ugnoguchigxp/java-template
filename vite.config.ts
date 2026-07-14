import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
	root: "web",
	plugins: [tailwindcss(), react()],
	resolve: {
		alias: {
			"@": path.resolve(__dirname, "./web/src"),
			"@web": path.resolve(__dirname, "./web/src"),
			"@shared": path.resolve(__dirname, "./shared"),
		},
	},
	server: {
		host: "127.0.0.1",
		port: 5173,
		proxy: {
			"/api": {
				target: "http://127.0.0.1:8080",
				changeOrigin: true,
			},
		},
	},
	build: {
		outDir: "../dist-web",
		emptyOutDir: true,
	},
});
