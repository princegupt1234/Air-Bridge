# ✈ Air-Bridge

Air-Bridge is an **offline-first, Bluetooth-based messaging application** built with Spring Boot. Two devices can communicate directly over Bluetooth RFCOMM — no internet, no router, no cloud. The web UI is served locally via an embedded Tomcat server and accessed through any browser on the same machine.

---

## How It Works

```
Browser  ──►  Spring Boot (port 8080)  ──►  MySQL (air_bridge_db)
                      │
                      ▼
         Python RFCOMM Bridge (bt_server.py / bt_client.py)
              socket.AF_BLUETOOTH + BTPROTO_RFCOMM
                      │
                      ▼
              Bluetooth RFCOMM (other device)
```

1. Spring Boot serves the web UI at `http://localhost:8080`.
2. Users register and log in through the browser.
3. For Bluetooth messaging, the app spawns a Python subprocess (`bt_server.py` or `bt_client.py`) that opens a native RFCOMM socket using Python's `socket.AF_BLUETOOTH`.
4. Messages are serialized as JSON and streamed over the RFCOMM socket line by line.
5. Incoming messages are pushed to the browser in real-time via WebSocket (STOMP over SockJS) on `/topic/bluetooth/{mac}`.
6. Device discovery uses PowerShell `Get-PnpDevice` — works on Windows 10/11 with any modern Bluetooth adapter.
7. All user data, chats, contacts, and notifications are persisted in a local MySQL database.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.4 |
| Security | Spring Security 6 (BCrypt, form login) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Real-time | WebSocket (STOMP + SockJS) |
| Templating | Thymeleaf 3 + Thymeleaf Security extras |
| Bluetooth | Python 3 RFCOMM bridge (`socket.AF_BLUETOOTH`) |
| BT Discovery | PowerShell `Get-PnpDevice` (Windows 10/11) |
| Build | Maven 3.8+ (Maven Wrapper included) |
| UI | Inter font, custom CSS (dark theme) |

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.8+ (or use included `mvnw`) |
| MySQL | 8+ |
| Python | 3.6+ |
| OS | Windows 10 / Windows 11 |
| Bluetooth | Any adapter supported by Windows (Intel, Qualcomm, etc.) |

> **Why Windows only?** The Python bridge uses `socket.AF_BLUETOOTH` with `BTPROTO_RFCOMM`, which is exposed natively by the Windows Bluetooth stack. Linux/macOS support can be added by adjusting the socket constants in the bridge scripts.

---

## Setup & Run

### 1. Clone the repository

```bash
git clone https://github.com/your-username/air-bridge.git
cd air-bridge
```

### 2. Create the database

```sql
CREATE DATABASE air_bridge_db;
```

> The JDBC URL includes `createDatabaseIfNotExist=true` so this step is optional if your MySQL user has `CREATE` privileges.

### 3. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/air_bridge_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. Build and run

```bash
mvnw spring-boot:run
```

The app starts at `http://localhost:8080`.

> Hibernate auto-creates all tables on first run (`ddl-auto=update`). No SQL scripts needed.

---

## Application Routes

### Auth

| Method | Route | Description |
|---|---|---|
| GET | `/` | Landing page |
| GET | `/register` | Registration form |
| POST | `/register` | Submit registration |
| GET | `/login` | Login form |
| POST | `/login` | Submit login |
| GET | `/logout` | Logout |
| GET | `/forgot-password` | Forgot password page |

### Dashboard & Profile

| Method | Route | Description |
|---|---|---|
| GET | `/dashboard` | Main dashboard |
| GET | `/profile` | View your profile |
| GET | `/profile/edit` | Edit profile form |
| POST | `/profile/edit` | Save profile changes |

### Chat

| Method | Route | Description |
|---|---|---|
| GET | `/chat` | List all chats |
| GET | `/chat/new` | New chat page |
| POST | `/chat/start?userId=` | Start or open a chat with a user |
| GET | `/chat/{chatId}` | Open a chat room |
| POST | `/chat/{chatId}/send` | Send a message |

### Contacts

| Method | Route | Description |
|---|---|---|
| GET | `/contacts` | View contacts |
| GET | `/contacts/add` | Add contact form |
| POST | `/contacts/add` | Add contact by email |
| POST | `/contacts/remove/{id}` | Remove a contact |
| POST | `/contacts/block/{id}` | Block a contact |

### Bluetooth

| Method | Route | Description |
|---|---|---|
| GET | `/bluetooth` | Bluetooth home — connected & known devices |
| GET | `/bluetooth/scan` | Scan page |
| POST | `/bluetooth/scan` | Trigger a device scan (PowerShell) |
| POST | `/bluetooth/connect` | Connect to a device by MAC address |
| POST | `/bluetooth/disconnect` | Disconnect from a device |
| GET | `/bluetooth/chat/{mac}` | Open Bluetooth chat with a device |
| POST | `/bluetooth/send` | Send a message over RFCOMM |
| POST | `/bluetooth/server/start` | Start the RFCOMM server (listen mode) |
| POST | `/bluetooth/server/stop` | Stop the RFCOMM server |

### Notifications

| Method | Route | Description |
|---|---|---|
| GET | `/notifications` | View all notifications (auto-marks as read) |

---

## Bluetooth Messaging — How It Works

### Why not BlueCove?

BlueCove (the standard Java Bluetooth library) uses a JNI native DLL that calls the old `BluetoothFindFirstDevice` Win32 API. This API is blocked on Windows 10/11 with modern Intel/Qualcomm Bluetooth stacks. Air-Bridge replaces it entirely with a Python bridge.

### Python Bridge Architecture

Air-Bridge uses two Python scripts as a bridge between Java and the OS Bluetooth stack:

| Script | Role |
|---|---|
| `bt_server.py` | Runs on the receiving device. Binds to RFCOMM channel 4 and waits for an incoming connection. |
| `bt_client.py` | Runs on the sending device. Connects to a remote device by MAC address and channel. |

Both scripts use Python's native `socket.AF_BLUETOOTH` + `socket.BTPROTO_RFCOMM` — available on Windows 10/11 without any extra libraries.

### Communication Protocol

Java spawns the Python script as a subprocess and communicates via **stdin/stdout** using newline-delimited JSON:

**Python → Java (stdout):**
```json
{"event": "server_ready", "channel": 4}
{"event": "connected",    "mac": "AA:BB:CC:DD:EE:FF"}
{"event": "message",      "mac": "AA:BB:CC:DD:EE:FF", "data": "{...}"}
{"event": "disconnected", "mac": "AA:BB:CC:DD:EE:FF"}
{"event": "error",        "msg": "..."}
```

**Java → Python (stdin):**
```json
{"senderEmail":"user@example.com","senderName":"Alice","content":"Hello!","type":"TEXT","timestamp":1234567890}
```

### Device Discovery

Device discovery uses PowerShell `Get-PnpDevice -Class Bluetooth` which reads the Windows device registry. MAC addresses are extracted from the `InstanceId` field (e.g. `BLUETOOTHDEVICE_E6CFAB5EE7F1` → `E6:CF:AB:5E:E7:F1`). This works with any paired device on Windows 10/11.

### Step-by-Step: Connecting Two Devices

| Machine A (server) | Machine B (client) |
|---|---|
| Open `http://localhost:8080/bluetooth` | Open `http://localhost:8080/bluetooth` |
| Click **▶ Start Server** | Click **🔍 Scan Devices** |
| Wait — server is now listening on RFCOMM channel 4 | Find Machine A in the list, click **Connect** |
| Browser shows Machine B as connected | Browser shows Machine A as connected |
| Go to `/bluetooth/chat/{mac}` | Go to `/bluetooth/chat/{mac}` |
| Type a message and send | Type a message and send |
| Messages arrive via RFCOMM — no internet used | Messages arrive via RFCOMM — no internet used |

---

## Project Structure

```
src/main/java/com/airbridge/
├── AirBridgeApplication.java       # Spring Boot entry point
├── config/
│   ├── MvcConfig.java              # Static resource mapping (/uploads/**)
│   ├── SecurityConfig.java         # BCrypt, form login, CSRF disabled
│   └── WebSocketConfig.java        # STOMP over SockJS at /ws
├── controller/
│   ├── AuthController.java         # /login, /register, /forgot-password
│   ├── BluetoothController.java    # /bluetooth/**
│   ├── ChatController.java         # /chat/**
│   ├── ContactController.java      # /contacts/**
│   ├── MainController.java         # /, /dashboard
│   ├── NotificationController.java # /notifications
│   └── ProfileController.java      # /profile/**
├── dto/
│   ├── BluetoothDeviceDTO.java
│   ├── BluetoothMessagePayload.java
│   ├── ChatDTO.java
│   ├── MessageDTO.java
│   ├── NotificationDTO.java
│   ├── RegisterRequest.java
│   └── UserDTO.java
├── exception/
│   ├── EmailAlreadyExistsException.java
│   └── PasswordMismatchException.java
├── model/
│   ├── BluetoothDevice.java        # BT device entity (MAC, name, connected)
│   ├── Chat.java                   # One-to-one chat session
│   ├── Contact.java                # Contact list with block support
│   ├── Message.java                # Chat message (TEXT/IMAGE/FILE/VOICE)
│   ├── Notification.java           # In-app notification
│   └── User.java                   # Registered user account
├── repository/                     # Spring Data JPA repositories (one per entity)
└── service/
    ├── impl/
    │   ├── AuthServiceImpl.java
    │   ├── BluetoothServiceImpl.java   # PowerShell discovery + Python RFCOMM bridge
    │   ├── ChatServiceImpl.java
    │   ├── ContactServiceImpl.java
    │   ├── NotificationServiceImpl.java
    │   └── UserServiceImpl.java        # Implements UserDetailsService
    ├── AuthService.java
    ├── BluetoothService.java
    ├── ChatService.java
    ├── ContactService.java
    ├── NotificationService.java
    └── UserService.java

src/main/resources/
├── bt/
│   ├── bt_server.py                # RFCOMM server bridge (Python)
│   └── bt_client.py                # RFCOMM client bridge (Python)
├── static/
│   └── css/
│       └── style.css               # Dark theme UI (Inter font)
├── templates/
│   ├── auth/                       # login, register, forgot-password
│   ├── bluetooth/                  # bluetooth-scan, connected-devices, pair-device
│   ├── chat/                       # chat-list, chat-room, new-chat
│   ├── contacts/                   # contacts, add-contact
│   ├── home/                       # index (landing), dashboard
│   ├── notification/               # notifications
│   └── profile/                    # profile, edit-profile
└── application.properties
```

---

## Database Schema

All tables are auto-generated by Hibernate on startup.

| Table | Description |
|---|---|
| `users` | Registered user accounts (email, BCrypt password, profile info) |
| `chats` | One-to-one chat sessions between two users |
| `messages` | Messages within a chat — column `is_read` (reserved keyword workaround) |
| `contacts` | User contact list with block support, unique constraint on (owner, contact) |
| `bluetooth_devices` | Known/paired Bluetooth devices (MAC, name, connected status) |
| `notifications` | In-app notifications per user — column `is_read` |

> **MySQL reserved keyword note:** The Java field `read` maps to column `is_read` via `@Column(name="is_read")` in both `Message` and `Notification` entities to avoid MySQL 8 reserved keyword conflicts.

---

## Security

- Passwords hashed with **BCrypt**.
- CSRF disabled to support WebSocket and Bluetooth form submissions.
- Session-based authentication via Spring Security form login.
- The following routes are public (no login required):

```
/  /login  /register  /forgot-password
/css/**  /js/**  /images/**  /icons/**
/ws/**  /bluetooth/**
```

- All other routes require an authenticated session and redirect to `/login`.

---

## Known Limitations

| Limitation | Detail |
|---|---|
| Windows only | Python `socket.AF_BLUETOOTH` with `BTPROTO_RFCOMM` is Windows-specific in this implementation |
| Classic BT only | Uses RFCOMM (Classic Bluetooth), not BLE |
| One connection per server | `bt_server.py` accepts one client at a time |
| No message persistence for BT | Bluetooth messages are delivered in real-time via WebSocket but not saved to the DB |
| BlueCove on classpath | BlueCove JAR is a compile dependency but its socket APIs are not used — only the Python bridge is used for RFCOMM |

---

## Troubleshooting

**"Adapter not found" on `/bluetooth`**
- Make sure Bluetooth is enabled in Windows Settings → Bluetooth & devices.
- Run `Get-PnpDevice -Class Bluetooth` in PowerShell — your adapter should show `Status: OK`.
- BlueCove is not used for detection; the check is done via PowerShell.

**"Connection failed" when clicking Connect**
- The remote device must be running Air-Bridge with the server started (`▶ Start Server`).
- Both devices must be paired in Windows Bluetooth settings first.
- RFCOMM channel 4 must not be blocked by another application.

**Blank scan results**
- Only devices already paired in Windows show up in the scan (Windows registry-based).
- Pair the device first via Windows Settings → Bluetooth & devices → Add device.

**Build fails with Lombok errors**
- The `pom.xml` includes explicit `annotationProcessorPaths` for Lombok in `maven-compiler-plugin`. If you see missing getter/setter errors, run `mvnw clean compile` (not just `compile`).

**MySQL connection refused**
- Ensure MySQL 8 is running on port 3306.
- The JDBC URL includes `createDatabaseIfNotExist=true` — the database is created automatically if your user has `CREATE` privileges.

---

## Development Notes

- `spring.thymeleaf.cache=false` — templates reload without restart during development.
- `spring.jpa.show-sql=true` — all SQL queries are logged to console.
- DevTools is included — Java class changes hot-reload, but `application.properties` changes require a full restart.
- Python scripts are extracted from the classpath to a temp file at runtime so they work both in IDE and from a packaged JAR.
