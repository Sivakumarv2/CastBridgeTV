package com.castbridge.tv;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CastServer {
    private final int port;
    private ServerSocket server;
    private volatile boolean running;
    private Thread acceptThread;
    private final List<Client> clients = new CopyOnWriteArrayList<>();

    public CastServer(int port) { this.port = port; }

    public void start() throws IOException {
        if (running) return;
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));
        running = true;
        acceptThread = new Thread(() -> {
            while (running) {
                try { Socket s = server.accept(); new Thread(() -> handle(s), "cast-client").start(); }
                catch (IOException e) { if (running) e.printStackTrace(); }
            }
        }, "cast-accept");
        acceptThread.start();
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        for (Client c : clients) c.close();
        clients.clear();
    }

    public int getPort() { return port; }

    public void broadcast(byte[] jpeg) {
        for (Client c : clients) {
            try { c.sendBinary(jpeg); } catch (Exception e) { c.close(); clients.remove(c); }
        }
    }

    private void handle(Socket socket) {
        try {
            socket.setTcpNoDelay(true);
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String request = r.readLine();
            if (request == null) { socket.close(); return; }
            Map<String,String> headers = new HashMap<>();
            String line;
            while ((line = r.readLine()) != null && !line.isEmpty()) {
                int p = line.indexOf(':'); if (p > 0) headers.put(line.substring(0,p).trim().toLowerCase(Locale.US), line.substring(p+1).trim());
            }
            String path = request.split(" ")[1];
            if (path.startsWith("/ws") && "websocket".equalsIgnoreCase(headers.get("upgrade"))) {
                String key = headers.get("sec-websocket-key");
                String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
                OutputStream out = socket.getOutputStream();
                String response = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.US_ASCII)); out.flush();
                Client c = new Client(socket); clients.add(c); c.readLoop();
                clients.remove(c); c.close();
            } else {
                byte[] body = receiverHtml().getBytes(StandardCharsets.UTF_8);
                OutputStream out = socket.getOutputStream();
                String h = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
                out.write(h.getBytes(StandardCharsets.US_ASCII)); out.write(body); out.flush(); socket.close();
            }
        } catch (Exception e) { try { socket.close(); } catch (Exception ignored) {} }
    }

    private String receiverHtml() {
        return "<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>CastBridge TV</title>"+
        "<style>html,body{margin:0;width:100%;height:100%;background:#000;overflow:hidden;font-family:Arial;color:#fff}#s{width:100%;height:100%;object-fit:contain;background:#000}#m{position:fixed;left:50%;top:50%;transform:translate(-50%,-50%);text-align:center;opacity:.85}h1{font-size:28px;margin:0 0 8px}p{color:#aaa}</style></head>"+
        "<body><div id=m><h1>CastBridge TV</h1><p>Waiting for screen…</p></div><img id=s alt='Screen'><script>"+
        "const img=document.getElementById('s'),m=document.getElementById('m');let ws=new WebSocket('ws://'+location.host+'/ws');ws.binaryType='blob';ws.onopen=()=>m.style.display='none';ws.onclose=()=>{m.style.display='block';m.querySelector('p').textContent='Disconnected. Waiting…';};ws.onmessage=e=>{const u=URL.createObjectURL(e.data);img.onload=()=>URL.revokeObjectURL(u);img.src=u};"+
        "</script></body></html>";
    }

    private static class Client {
        final Socket socket; final OutputStream out; final InputStream in;
        Client(Socket s) throws IOException { socket=s; out=s.getOutputStream(); in=s.getInputStream(); }
        synchronized void sendBinary(byte[] data) throws IOException {
            int len=data.length; ByteArrayOutputStream b=new ByteArrayOutputStream(); b.write(0x82);
            if(len<126){b.write(len);} else if(len<=65535){b.write(126); b.write((len>>8)&255); b.write(len&255);} else {b.write(127); for(int i=7;i>=0;i--) b.write((len >>> (8*i))&255);}
            b.write(data); out.write(b.toByteArray()); out.flush();
        }
        void readLoop() throws IOException { byte[] buf=new byte[256]; while(in.read(buf)>=0){} }
        void close(){try{socket.close();}catch(Exception ignored){}}
    }
}
