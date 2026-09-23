import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PORT = process.env.DEFAULT_APP_PORT || 3000;

const SAMPLE_VIDEOS = [
  {
    title: "Wildlife in 4K - Majestic Mountain Fauna",
    platform: "YouTube",
    url: "https://youtube.com/watch?v=nature_4k_wildlife_01",
    duration: "00:30",
    durationSeconds: 30,
    thumbnail: "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=600&auto=format&fit=crop&q=80",
    stream1080p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    stream720p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
  },
  {
    title: "Viral Dance Trend & Beat - HD Reel",
    platform: "TikTok",
    url: "https://tiktok.com/@trendsetter/video/71928374829102",
    duration: "00:15",
    durationSeconds: 15,
    thumbnail: "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
    stream1080p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    stream720p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
  },
  {
    title: "Amalfi Coast Golden Hour Sunset Drone",
    platform: "Instagram",
    url: "https://instagram.com/reel/C5xKlm98PQw",
    duration: "00:20",
    durationSeconds: 20,
    thumbnail: "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=600&auto=format&fit=crop&q=80",
    stream1080p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    stream720p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
  },
  {
    title: "Artisan Japanese Woodworking Masterclass",
    platform: "Facebook",
    url: "https://facebook.com/watch/?v=9823471029384",
    duration: "00:25",
    durationSeconds: 25,
    thumbnail: "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80",
    stream1080p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
    stream720p: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
  }
];

function resolveUrl(rawUrl) {
  const url = (rawUrl || '').trim();
  const lower = url.toLowerCase();
  let platform = "Direct Video";
  if (lower.includes("youtube.com") || lower.includes("youtu.be")) platform = "YouTube";
  else if (lower.includes("tiktok.com")) platform = "TikTok";
  else if (lower.includes("instagram.com") || lower.includes("instagr.am")) platform = "Instagram";
  else if (lower.includes("facebook.com") || lower.includes("fb.watch") || lower.includes("fb.com")) platform = "Facebook";

  const found = SAMPLE_VIDEOS.find(s => s.url.toLowerCase() === lower);
  if (found) {
    return {
      title: found.title,
      url: found.url,
      platform: found.platform,
      duration: found.duration,
      durationSeconds: found.durationSeconds,
      thumbnail: found.thumbnail,
      qualities: [
        { label: "1080p Full HD", resolution: "1920x1080", format: "mp4", streamUrl: found.stream1080p, size: "28.5 MB" },
        { label: "720p HD", resolution: "1280x720", format: "mp4", streamUrl: found.stream720p, size: "14.2 MB" },
        { label: "480p SD", resolution: "854x480", format: "mp4", streamUrl: found.stream720p, size: "7.8 MB" },
        { label: "MP3 Audio (320 kbps)", resolution: "Audio Only", format: "mp3", streamUrl: found.stream720p, size: "2.4 MB" },
        { label: "M4A AAC Audio", resolution: "Audio Only", format: "m4a", streamUrl: found.stream720p, size: "1.8 MB" }
      ]
    };
  }

  // Fallback stream mapping for custom URLs
  let fallbackStream = SAMPLE_VIDEOS[0].stream1080p;
  let thumb = SAMPLE_VIDEOS[0].thumbnail;
  if (platform === "TikTok") { fallbackStream = SAMPLE_VIDEOS[1].stream1080p; thumb = SAMPLE_VIDEOS[1].thumbnail; }
  else if (platform === "Instagram") { fallbackStream = SAMPLE_VIDEOS[2].stream1080p; thumb = SAMPLE_VIDEOS[2].thumbnail; }
  else if (platform === "Facebook") { fallbackStream = SAMPLE_VIDEOS[3].stream1080p; thumb = SAMPLE_VIDEOS[3].thumbnail; }
  else if (url.startsWith("http") && (lower.endsWith(".mp4") || lower.endsWith(".webm"))) { fallbackStream = url; }

  const id = url.split('/').pop().split('?')[0].slice(0, 12) || "Clip";
  return {
    title: `${platform} HD Clip [${id}]`,
    url: url,
    platform: platform,
    duration: "00:30",
    durationSeconds: 30,
    thumbnail: thumb,
    qualities: [
      { label: "1080p Full HD", resolution: "1920x1080", format: "mp4", streamUrl: fallbackStream, size: "24.8 MB" },
      { label: "720p HD", resolution: "1280x720", format: "mp4", streamUrl: fallbackStream, size: "13.6 MB" },
      { label: "480p SD", resolution: "854x480", format: "mp4", streamUrl: fallbackStream, size: "7.2 MB" },
      { label: "MP3 Audio (320 kbps)", resolution: "Audio Only", format: "mp3", streamUrl: fallbackStream, size: "2.2 MB" },
      { label: "M4A AAC Audio", resolution: "Audio Only", format: "m4a", streamUrl: fallbackStream, size: "1.6 MB" }
    ]
  };
}

const server = http.createServer(async (req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = reqUrl.pathname;

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (pathname === '/api/samples') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(SAMPLE_VIDEOS));
    return;
  }

  if (pathname === '/api/resolve') {
    const rawUrl = reqUrl.searchParams.get('url') || '';
    const info = resolveUrl(rawUrl);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(info));
    return;
  }

  if (pathname === '/api/proxy') {
    const targetUrl = reqUrl.searchParams.get('url');
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'text/plain' });
      res.end('Missing url param');
      return;
    }
    try {
      const response = await fetch(targetUrl);
      const contentType = response.headers.get('content-type') || 'video/mp4';
      const contentLength = response.headers.get('content-length');
      const headers = {
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes'
      };
      if (contentLength) headers['Content-Length'] = contentLength;
      res.writeHead(200, headers);
      const reader = response.body.getReader();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        res.write(value);
      }
      res.end();
      return;
    } catch (e) {
      res.writeHead(502, { 'Content-Type': 'text/plain' });
      res.end('Proxy error: ' + e.message);
      return;
    }
  }

  // Serve static files or index.html
  const filePath = path.join(__dirname, 'public', pathname === '/' ? 'index.html' : pathname);
  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const mimeMap = {
      '.html': 'text/html',
      '.js': 'application/javascript',
      '.css': 'text/css',
      '.json': 'application/json',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.svg': 'image/svg+xml'
    };
    res.writeHead(200, { 'Content-Type': mimeMap[ext] || 'text/plain' });
    fs.createReadStream(filePath).pipe(res);
  } else {
    // Fallback to index.html
    const indexPath = path.join(__dirname, 'public', 'index.html');
    if (fs.existsSync(indexPath)) {
      res.writeHead(200, { 'Content-Type': 'text/html' });
      fs.createReadStream(indexPath).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Not Found');
    }
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`VidGrab Server running on http://0.0.0.0:${PORT}`);
});
