"""
bt_client.py  –  Air-Bridge Bluetooth RFCOMM client bridge
Connects to a remote RFCOMM server, then:
  - Writes received messages to stdout (one JSON line each)
  - Reads stdin and sends each line to the server
Usage: python bt_client.py <mac_address> <channel>
"""
import socket, sys, threading, json

if len(sys.argv) < 3:
    print(json.dumps({"event": "error", "msg": "Usage: bt_client.py <mac> <channel>"}), flush=True)
    sys.exit(1)

MAC     = sys.argv[1]
CHANNEL = int(sys.argv[2])

try:
    sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
    sock.settimeout(15)
    sock.connect((MAC, CHANNEL))
    sock.settimeout(None)
    print(json.dumps({"event": "connected", "mac": MAC}), flush=True)
except Exception as e:
    print(json.dumps({"event": "error", "msg": str(e)}), flush=True)
    sys.exit(1)

def read_from_server():
    try:
        buf = b""
        while True:
            data = sock.recv(4096)
            if not data:
                break
            buf += data
            while b"\n" in buf:
                line, buf = buf.split(b"\n", 1)
                line = line.strip()
                if line:
                    print(json.dumps({"event": "message", "mac": MAC,
                                      "data": line.decode("utf-8", errors="replace")}), flush=True)
    except Exception as e:
        print(json.dumps({"event": "error", "msg": str(e)}), flush=True)
    finally:
        print(json.dumps({"event": "disconnected", "mac": MAC}), flush=True)

t = threading.Thread(target=read_from_server, daemon=True)
t.start()

try:
    for line in sys.stdin:
        line = line.strip()
        if line:
            sock.send((line + "\n").encode("utf-8"))
except Exception:
    pass
finally:
    sock.close()
