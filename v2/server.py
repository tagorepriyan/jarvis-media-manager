import os
import json
import asyncio
import time
import logging
import socket
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from telethon import TelegramClient
from FastTelethonhelper import fast_upload, fast_download
from message import send_text_message
import uvicorn

# Suppress noisy MTProto socket disconnect warnings from Telethon console
logging.getLogger('telethon.network.mtprotosender').setLevel(logging.CRITICAL)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")

app = FastAPI()

# Prevents Python 3.11 Garbage Collector from destroying active background tasks
active_tasks = set()

# Global Worker State
task_state = {
    "is_running": False,
    "current_file": "Idle",
    "percentage": 0.0,
    "speed_mb": 0.0,
    "transferred_mb": 0.0,
    "total_mb": 0.0,
    "current_percentage": 0.0,
    "completed_files": 0,
    "total_files": 0,
    "failed_files": 0,
    "is_paused": False,
    "eta": "--:--",
    "logs": []
}

cancel_event = asyncio.Event()
pause_event = asyncio.Event()
pause_event.set()

class DummyMessage:
    """Tricks FastTelethonhelper into executing progress callbacks without needing an initial Telegram message object"""
    async def edit(self, *args, **kwargs):
        pass

def add_log(message: str):
    timestamp = time.strftime('%H:%M:%S')
    entry = f"[{timestamp}] {message}"
    task_state["logs"].append(entry)
    if len(task_state["logs"]) > 200:
        task_state["logs"].pop(0)
    print(entry)

def clean_path(path_str: str) -> str:
    if not path_str: return ""
    cleaned = path_str.strip().strip('"').strip("'")
    return os.path.normpath(cleaned)

def parse_chat_id(chat_str: str):
    chat_str = chat_str.strip()
    if chat_str.startswith("https://t.me/"):
        chat_str = chat_str.replace("https://t.me/", "")
    if chat_str.lstrip('-').isdigit():
        return int(chat_str)
    return chat_str

# --- Pydantic Data Schemas ---
class ConfigData(BaseModel):
    api_id: str
    api_hash: str
    bot_token: str
    saved_chats: List[dict]

class ScanRequest(BaseModel):
    folder_path: str

class UploadRequest(BaseModel):
    api_id: int
    api_hash: str
    auth_mode: str
    bot_token: Optional[str] = ""
    chat_id: str
    folder_path: str
    grouped: bool = True
    # Kept in the model so the UI slider doesn't crash the payload, 
    # but we will intentionally ignore it to prevent FastTelethonhelper from throwing an error.
    connection_count: int = 0  
    selected_files: Optional[List[str]] = None

class DownloadRequest(BaseModel):
    api_id: int
    api_hash: str
    auth_mode: str
    bot_token: Optional[str] = ""
    chat_id: str
    save_folder: str
    limit: int = 10

class MessageRequest(BaseModel):
    bot_token: str
    chat_id: str
    message: str
    disable_notification: bool = False

# --- Web Routes ---
@app.get("/")
def serve_html(): return FileResponse(os.path.join(BASE_DIR, "index.html"))

@app.get("/style.css")
def serve_css(): return FileResponse(os.path.join(BASE_DIR, "style.css"))

@app.get("/script.js")
def serve_js(): return FileResponse(os.path.join(BASE_DIR, "script.js"))

@app.get("/api/config")
def get_config():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, "r") as f:
            return json.load(f)
    return {"api_id": "", "api_hash": "", "bot_token": "", "saved_chats": []}

@app.post("/api/config")
def save_config(data: ConfigData):
    with open(CONFIG_FILE, "w") as f:
        json.dump(data.model_dump(), f, indent=4)
    return {"status": "Config saved"}

@app.get("/api/status")
def get_status(): return task_state

@app.post("/api/send_message")
async def send_message(req: MessageRequest):
    try:
        result = await asyncio.to_thread(
            send_text_message,
            req.bot_token,
            parse_chat_id(req.chat_id),
            req.message,
            req.disable_notification,
        )
        return {"status": "Message sent", "message_id": result.get("result", {}).get("message_id")}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Message could not be sent: {e}")

@app.post("/api/scan_folder")
def scan_folder(req: ScanRequest):
    path = clean_path(req.folder_path)
    if not path or not os.path.exists(path):
        raise HTTPException(status_code=400, detail=f"Path not found: '{path}'")
    
    if os.path.isfile(path):
        folder = os.path.dirname(path)
        filename = os.path.basename(path)
        size = os.path.getsize(path)
        return {"folder": folder, "files": [filename], "file_details": [{"name": filename, "size": size}], "count": 1, "total_size": size}

    files = [f for f in os.listdir(path) if os.path.isfile(os.path.join(path, f))]
    files.sort()
    file_details = [
        {"name": filename, "size": os.path.getsize(os.path.join(path, filename))}
        for filename in files
    ]
    return {"folder": path, "files": files, "file_details": file_details, "count": len(files), "total_size": sum(item["size"] for item in file_details)}

@app.post("/api/cancel")
def cancel_task():
    if task_state["is_running"]:
        cancel_event.set()
        add_log("Cancellation requested by user...")
        return {"status": "Cancelling..."}
    return {"status": "No active task"}

@app.post("/api/pause")
def pause_task():
    if not task_state["is_running"]:
        raise HTTPException(status_code=400, detail="No active task")
    if pause_event.is_set():
        pause_event.clear()
        task_state["is_paused"] = True
    else:
        pause_event.set()
        task_state["is_paused"] = False
    return {"paused": task_state["is_paused"]}

@app.post("/api/start_upload")
async def start_upload(req: UploadRequest):
    if task_state["is_running"]:
        raise HTTPException(status_code=400, detail="Task already running.")
    path = clean_path(req.folder_path)
    if not path or not os.path.exists(path):
        raise HTTPException(status_code=400, detail=f"Path not found: '{path}'")
    cancel_event.clear()
    task = asyncio.create_task(upload_worker(req))
    active_tasks.add(task)
    task.add_done_callback(active_tasks.discard)
    return {"status": "Upload started"}

@app.post("/api/start_download")
async def start_download(req: DownloadRequest):
    if task_state["is_running"]:
        raise HTTPException(status_code=400, detail="Task already running.")
    cancel_event.clear()
    task = asyncio.create_task(download_worker(req))
    active_tasks.add(task)
    task.add_done_callback(active_tasks.discard)
    return {"status": "Download started"}

# --- Background Workers ---
async def upload_worker(req: UploadRequest):
    task_state["is_running"] = True
    pause_event.set()
    task_state["is_paused"] = False
    task_state["logs"] = []
    add_log("Initializing upload process...")

    path = clean_path(req.folder_path)
    target_entity_raw = parse_chat_id(req.chat_id)
    
    if os.path.isfile(path):
        folder = os.path.dirname(path)
        files = [os.path.basename(path)]
    else:
        folder = path
        files = [f for f in os.listdir(folder) if os.path.isfile(os.path.join(folder, f))]
        files.sort()

    if req.selected_files is not None:
        selected = set(req.selected_files)
        files = [filename for filename in files if filename in selected]

    task_state["total_files"] = len(files)
    task_state["completed_files"] = 0
    task_state["failed_files"] = 0
    task_state["percentage"] = 0.0

    session_name = "bot_web_session" if req.auth_mode == "bot" else "user_web_session"
    client = TelegramClient(session_name, req.api_id, req.api_hash, connection_retries=10, timeout=60)
    dummy_msg = DummyMessage()

    try:
        if req.auth_mode == "bot": await client.start(bot_token=req.bot_token)
        else: await client.start()
        add_log("Connected to Telegram successfully!")

        try:
            add_log(f"Resolving destination chat: {target_entity_raw}...")
            target_entity = await client.get_entity(target_entity_raw)
            resolved_name = getattr(target_entity, 'title', getattr(target_entity, 'username', target_entity.id))
            add_log(f"Resolved destination successfully: {resolved_name}")
        except Exception as e:
            add_log(f"Error resolving chat ID: {e}. Please check the ID or username.")
            raise e

        total = len(files)
        batch = []

        for idx, filename in enumerate(files, 1):
            if cancel_event.is_set(): break
            while not pause_event.is_set() and not cancel_event.is_set():
                await asyncio.sleep(0.2)
            if cancel_event.is_set(): break
            file_path = os.path.join(folder, filename)
            task_state["current_file"] = f"({idx}/{total}) {filename}"
            add_log(f"Uploading file: {filename}")
            
            start_time = time.time()
            last_math = [time.time()]

            def cb(current, total_bytes):
                now = time.time()
                if (now - last_math[0]) < 0.5 and current < total_bytes: return "Updating"
                last_math[0] = now
                elapsed = max(now - start_time, 0.001)
                speed = current / elapsed
                
                current_percentage = (current / total_bytes) * 100 if total_bytes > 0 else 0
                task_state["current_percentage"] = round(current_percentage, 1)
                task_state["percentage"] = round(((idx - 1 + current_percentage / 100) / total) * 100, 1) if total else 0
                task_state["speed_mb"] = round(speed / (1024 * 1024), 2)
                task_state["transferred_mb"] = round(current / (1024 * 1024), 2)
                task_state["total_mb"] = round(total_bytes / (1024 * 1024), 2)
                
                eta_secs = int((total_bytes - current) / speed) if speed > 0 else 0
                task_state["eta"] = f"{eta_secs // 60}m {eta_secs % 60}s"
                return "Updating"

            uploaded_file = None
            try:
                # We revert to default FastTelethonhelper behavior without the invalid keyword.
                # It will run at its maximum multi-threaded speed (~12 MB/s).
                uploaded_file = await fast_upload(
                    client,       
                    file_path,    
                    dummy_msg,    
                    filename,     
                    cb
                )
            except Exception as e:
                # Silence the harmless "0 bytes read" spam from Telethon drops
                if "0 bytes read" not in str(e):
                    add_log(f"Upload Warning for {filename}: {e}")
                task_state["failed_files"] += 1

            if cancel_event.is_set() or uploaded_file is None: break

            if req.grouped:
                batch.append(uploaded_file)
                if len(batch) == 10:
                    add_log("Sending batch of 10 files...")
                    await client.send_file(entity=target_entity, file=batch, force_document=True)
                    batch = []
            else:
                add_log(f"Sending '{filename}' individually...")
                await client.send_file(entity=target_entity, file=uploaded_file, force_document=True)

            task_state["completed_files"] = idx

        if req.grouped and batch and not cancel_event.is_set():
            add_log(f"Sending final batch of {len(batch)} files...")
            await client.send_file(entity=target_entity, file=batch, force_document=True)

        if not cancel_event.is_set(): add_log("All uploads completed successfully!")
    except Exception as e:
        add_log(f"Critical Error: {e}")
    finally:
        task_state["is_running"] = False
        task_state["is_paused"] = False
        pause_event.set()
        task_state["current_file"] = "Idle"
        await client.disconnect()

async def download_worker(req: DownloadRequest):
    task_state["is_running"] = True
    pause_event.set()
    task_state["is_paused"] = False
    task_state["logs"] = []
    add_log("Initializing download process...")

    save_folder = clean_path(req.save_folder)
    os.makedirs(save_folder, exist_ok=True)
    target_entity_raw = parse_chat_id(req.chat_id)
    
    session_name = "bot_web_session" if req.auth_mode == "bot" else "user_web_session"
    client = TelegramClient(session_name, req.api_id, req.api_hash, connection_retries=10, timeout=60)
    dummy_msg = DummyMessage()

    try:
        if req.auth_mode == "bot": await client.start(bot_token=req.bot_token)
        else: await client.start()
        
        try:
            target_entity = await client.get_entity(target_entity_raw)
        except Exception as e:
            add_log(f"Error resolving chat ID: {e}. Please check the ID or username.")
            raise e

        add_log(f"Scanning last {req.limit} messages...")

        messages = await client.get_messages(target_entity, limit=req.limit)
        media_messages = [msg for msg in messages if msg.media]
        total = len(media_messages)
        task_state["total_files"] = total
        task_state["completed_files"] = 0
        task_state["failed_files"] = 0
        task_state["percentage"] = 0.0

        if total == 0:
            add_log("No media found.")
            return

        for idx, msg in enumerate(reversed(media_messages), 1):
            if cancel_event.is_set(): break
            while not pause_event.is_set() and not cancel_event.is_set():
                await asyncio.sleep(0.2)
            if cancel_event.is_set(): break
            filename = getattr(msg.file, 'name', f"file_msg_{msg.id}")
            task_state["current_file"] = f"({idx}/{total}) {filename}"
            add_log(f"Downloading: {filename}")

            start_time = time.time()
            last_math = [time.time()]

            def cb(current, total_bytes):
                now = time.time()
                if (now - last_math[0]) < 0.5 and current < total_bytes: return "Updating"
                last_math[0] = now
                elapsed = max(now - start_time, 0.001)
                speed = current / elapsed
                
                current_percentage = (current / total_bytes) * 100 if total_bytes > 0 else 0
                task_state["current_percentage"] = round(current_percentage, 1)
                task_state["percentage"] = round(((idx - 1 + current_percentage / 100) / total) * 100, 1) if total else 0
                task_state["speed_mb"] = round(speed / (1024 * 1024), 2)
                task_state["transferred_mb"] = round(current / (1024 * 1024), 2)
                task_state["total_mb"] = round(total_bytes / (1024 * 1024), 2)
                
                eta_secs = int((total_bytes - current) / speed) if speed > 0 else 0
                task_state["eta"] = f"{eta_secs // 60}m {eta_secs % 60}s"
                return "Updating"

            try:
                await fast_download(
                    client,       
                    msg,          
                    dummy_msg,    
                    save_folder,  
                    cb            
                )
            except Exception as e:
                if "0 bytes read" not in str(e):
                    add_log(f"Download Warning for {filename}: {e}")
                task_state["failed_files"] += 1
                continue
                    
            add_log(f"Downloaded: {filename}")
            task_state["completed_files"] = idx

        if not cancel_event.is_set(): add_log("Downloads completed!")
    except Exception as e:
        add_log(f"Critical Error: {e}")
    finally:
        task_state["is_running"] = False
        task_state["is_paused"] = False
        pause_event.set()
        task_state["current_file"] = "Idle"
        await client.disconnect()

if __name__ == "__main__":
    local_ip = "127.0.0.1"
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
    except Exception:
        pass

    print("\n" + "="*55)
    print(" 🚀 FAST-TELETHON SERVER IS RUNNING!")
    print("="*55)
    print(f" 💻 Access on this Laptop: http://127.0.0.1:8000")
    print(f" 📱 Access on your Phone:  http://{local_ip}:8000")
    print("="*55)
    print(" (Type the Phone link exactly as shown into Safari/Chrome)\n")

    # Run Uvicorn silently to keep the console clean
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="error")