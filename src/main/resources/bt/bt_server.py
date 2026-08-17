"""
bt_server.py  –  Air-Bridge Bluetooth RFCOMM server bridge
Accepts one incoming RFCOMM connection, then:
  - Writes received messages to stdout (one JSON line each)
  - Reads stdin and sends each line to the connected client
Usage: python bt_server.py <channel>
"""
import socket, sys, threading, json, os

CHANNEL = int(sys.argv[1]) if len(sys.argv) > 1 else 4

server_sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
server_sock.bind(("", CHANNEL))
server_sock.listen(1)

# Signal Java that server is ready
print(json.dumps({"event": "server_ready", "channel": CHANNEL}), flush=True)

client_sock, addr = server_sock.accept()
mac = addr[0]
print(json.dumps({"event": "connected", "mac": mac}), flush=True)

def read_from_client():
    try:
        buf = b""
        while True:
            data = client_sock.recv(4096)
            if not data:
                break
            buf += data
            while b"\n" in buf:
                line, buf = buf.split(b"\n", 1)
                line = line.strip()
                if line:
                    print(json.dumps({"event": "message", "mac": mac,
                                      "data": line.decode("utf-8", errors="replace")}), flush=True)
    except Exception as e:
        print(json.dumps({"event": "error", "msg": str(e)}), flush=True)
    finally:
        print(json.dumps({"event": "disconnected", "mac": mac}), flush=True)

t = threading.Thread(target=read_from_client, daemon=True)
t.start()

# Read from Java stdin → send to client
try:
    for line in sys.stdin:
        line = line.strip()
        if line:
            client_sock.send((line + "\n").encode("utf-8"))
except Exception:
    pass
finally:
    client_sock.close()
    server_sock.close()
