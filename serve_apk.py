import http.server
import socketserver
import os
import sys

PORT = 3000
DIRECTORY = "/app/applet/public"

class CustomHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        if self.path.endswith(".apk"):
            self.send_header("Content-Disposition", 'attachment; filename="app-release.apk"')
            self.send_header("Content-Type", "application/vnd.android.package-archive")
        super().end_headers()

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), CustomHandler) as httpd:
        print(f"Server started on port {PORT}")
        sys.stdout.flush()
        httpd.serve_forever()
