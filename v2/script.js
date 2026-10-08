let appConfig = { api_id: "", api_hash: "", bot_token: "", saved_chats: [] };
let queueFiles = [];

function formatBytes(bytes) {
    if (!bytes) return '0 MB';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / (1024 ** index)).toFixed(index > 1 ? 2 : 0)} ${units[index]}`;
}

function updateQueueSummary() {
    const selected = queueFiles.filter(file => file.selected);
    document.getElementById('fileCountLabel').textContent = queueFiles.length
        ? `${selected.length} of ${queueFiles.length} file(s) selected.`
        : 'Ready to scan...';
    document.getElementById('queueSizeLabel').textContent = formatBytes(selected.reduce((total, file) => total + file.size, 0));
    document.getElementById('selectAllBtn').textContent = selected.length === queueFiles.length && queueFiles.length ? 'Deselect all' : 'Select all';
}

function showToast(message, duration = 3500) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.add('visible');
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => toast.classList.remove('visible'), duration);
}

// Boot Sequence
document.addEventListener('DOMContentLoaded', async () => {
    // Initial Stagger Animations
    gsap.from(".fade-in", { y: 20, opacity: 0, duration: 0.6, stagger: 0.1, ease: "power3.out" });

    // Fetch Permanent Configuration
    try {
        const res = await fetch('/api/config');
        appConfig = await res.json();
        
        document.getElementById('apiId').value = appConfig.api_id || '';
        document.getElementById('apiHash').value = appConfig.api_hash || '';
        document.getElementById('botToken').value = appConfig.bot_token || '';
        
        if (!appConfig.saved_chats || appConfig.saved_chats.length === 0) {
            appConfig.saved_chats = [{ name: "Saved Messages", link: "me" }];
        }
        populateChatSelects();
        startPolling();
    } catch (e) {
        console.error("Failed to load config:", e);
        showToast("Could not load local configuration.");
    }
});

// Slider Value Label Sync
const connSlider = document.getElementById('connectionCount');
const connLabel = document.getElementById('connCountLabel');
connSlider.addEventListener('input', (e) => {
    connLabel.textContent = e.target.value == 0 ? "Auto" : e.target.value;
});

// Save Config Helper
async function saveConfigToBackend() {
    appConfig.api_id = document.getElementById('apiId').value;
    appConfig.api_hash = document.getElementById('apiHash').value;
    appConfig.bot_token = document.getElementById('botToken').value;
    await fetch('/api/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(appConfig)
    });
}

// Auth Mode Switching
document.querySelectorAll('input[name="authMode"]').forEach(radio => {
    radio.addEventListener('change', (e) => {
        const container = document.getElementById('botTokenContainer');
        if (e.target.value === 'bot') {
            container.classList.remove('hidden');
            gsap.fromTo(container, { height: 0, opacity: 0 }, { height: "auto", opacity: 1, duration: 0.3, ease: "power2.out" });
        } else {
            gsap.to(container, { height: 0, opacity: 0, duration: 0.3, ease: "power2.in", onComplete: () => container.classList.add('hidden') });
        }
    });
});

// Tab Switching
const tabs = { upload: document.getElementById('tabUploadBtn'), download: document.getElementById('tabDownloadBtn') };
const contents = { upload: document.getElementById('uploadTabContent'), download: document.getElementById('downloadTabContent') };

function switchTab(target) {
    const isUpload = target === 'upload';

    gsap.killTweensOf([contents.upload, contents.download, ".tab-indicator"]);
    
    gsap.to(".tab-indicator", { left: isUpload ? "0%" : "50%", duration: 0.4, ease: "power3.inOut" });
    
    tabs.upload.classList.toggle('active', isUpload);
    tabs.download.classList.toggle('active', !isUpload);
    tabs.upload.setAttribute('aria-selected', String(isUpload));
    tabs.download.setAttribute('aria-selected', String(!isUpload));
    
    const outContent = isUpload ? contents.download : contents.upload;
    const inContent = isUpload ? contents.upload : contents.download;

    outContent.classList.remove('active-tab');
    outContent.classList.add('hidden');
    inContent.classList.remove('hidden');
    inContent.classList.add('active-tab');
    gsap.fromTo(inContent, { opacity: 0, y: 8 }, { opacity: 1, y: 0, duration: 0.2, ease: "power2.out" });
}

tabs.upload.addEventListener('click', () => switchTab('upload'));
tabs.download.addEventListener('click', () => switchTab('download'));

// Chat Select Population
function populateChatSelects() {
    const selects = [document.getElementById('upChatSelect'), document.getElementById('downChatSelect'), document.getElementById('messageChatSelect')];
    selects.forEach(select => {
        const currentVal = select.value;
        select.innerHTML = '';
        appConfig.saved_chats.forEach(chat => {
            const opt = document.createElement('option');
            opt.value = chat.link;
            opt.textContent = `${chat.name} (${chat.link})`;
            select.appendChild(opt);
        });
        if (currentVal && appConfig.saved_chats.some(c => c.link === currentVal)) {
            select.value = currentVal;
        }
    });
}

// Modal Management
const modal = document.getElementById('chatModal');
const modalInner = modal.querySelector('.modal-card');
let modalReturnFocus;

function openModal() {
    modalReturnFocus = document.activeElement;
    renderModalList();
    modal.hidden = false;
    modal.classList.add('active');
    modal.setAttribute('aria-hidden', 'false');
    document.getElementById('newChatName').focus();
    gsap.fromTo(modalInner, { y: 30, opacity: 0, scale: 0.95 }, { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.2)" });
}

function closeModal() {
    gsap.to(modalInner, { y: 20, opacity: 0, scale: 0.95, duration: 0.3, ease: "power2.in", onComplete: () => {
        modal.classList.remove('active');
        modal.setAttribute('aria-hidden', 'true');
        modal.hidden = true;
        modalReturnFocus?.focus();
    }});
}

document.getElementById('manageChatsBtn1').addEventListener('click', openModal);
document.getElementById('manageChatsBtn2').addEventListener('click', openModal);
document.getElementById('closeModalBtn').addEventListener('click', closeModal);
modal.addEventListener('click', (event) => {
    if (event.target === modal) closeModal();
});
document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && modal.classList.contains('active')) closeModal();
});
document.getElementById('clearLogsBtn').addEventListener('click', () => {
    document.getElementById('consoleLog').textContent = '';
});
document.getElementById('settingsBtn').addEventListener('click', () => {
    const panel = document.getElementById('settingsPanel');
    const isHidden = panel.classList.toggle('settings-collapsed');
    const button = document.getElementById('settingsBtn');
    button.setAttribute('aria-expanded', String(!isHidden));
    button.innerHTML = `${isHidden ? 'Show access' : 'Access'} <span class="header-button-icon">${isHidden ? '⌄' : '⌃'}</span>`;
});
document.getElementById('selectAllBtn').addEventListener('click', () => {
    const shouldSelect = queueFiles.some(file => !file.selected);
    queueFiles.forEach(file => { file.selected = shouldSelect; });
    document.querySelectorAll('#fileList input[type="checkbox"]').forEach(input => { input.checked = shouldSelect; });
    updateQueueSummary();
});

function renderModalList() {
    const list = document.getElementById('modalChatList');
    list.innerHTML = '';
    appConfig.saved_chats.forEach((chat, idx) => {
        const li = document.createElement('li');
        li.innerHTML = `<span><strong>${chat.name}</strong><br><span style="font-size:0.8rem;color:gray;">${chat.link}</span></span> <button class="delete-chat-btn" onclick="deleteChat(${idx})">Delete</button>`;
        list.appendChild(li);
    });
}

window.deleteChat = (idx) => {
    appConfig.saved_chats.splice(idx, 1);
    saveConfigToBackend();
    renderModalList();
    populateChatSelects();
};

document.getElementById('addChatBtn').addEventListener('click', () => {
    const name = document.getElementById('newChatName').value.trim();
    const link = document.getElementById('newChatLink').value.trim();
    if (name && link) {
        appConfig.saved_chats.push({ name, link });
        saveConfigToBackend();
        document.getElementById('newChatName').value = '';
        document.getElementById('newChatLink').value = '';
        renderModalList();
        populateChatSelects();
    }
});

document.getElementById('sendMessageBtn').addEventListener('click', async () => {
    const message = document.getElementById('messageText').value.trim();
    const botToken = document.getElementById('botToken').value.trim();
    const chatId = document.getElementById('messageChatSelect').value;
    if (!botToken) return showToast('Enter a bot token in Settings first.');
    if (!chatId || !message) return showToast('Choose a chat and write a message first.');

    const button = document.getElementById('sendMessageBtn');
    button.disabled = true;
    button.firstChild.textContent = 'SENDING... ';
    try {
        const res = await fetch('/api/send_message', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bot_token: botToken,
                chat_id: chatId,
                message,
                disable_notification: document.getElementById('silentMessage').checked
            })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || 'Message could not be sent');
        document.getElementById('messageText').value = '';
        showToast('Message sent successfully.');
    } catch (error) {
        showToast(error.message);
    } finally {
        button.disabled = false;
        button.firstChild.textContent = 'SEND MESSAGE ';
    }
});

// Scan Path / Folder
document.getElementById('scanBtn').addEventListener('click', async () => {
    const folderPath = document.getElementById('folderPath').value;
    const btn = document.getElementById('scanBtn');
    btn.textContent = "Scanning...";
    
    const list = document.getElementById('fileList');
    const label = document.getElementById('fileCountLabel');
    const emptyState = document.getElementById('queueEmpty');
    list.innerHTML = '';
    queueFiles = [];
    try {
        const res = await fetch('/api/scan_folder', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ folder_path: folderPath })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || 'Unable to scan path');
        emptyState.classList.add('hidden');
        queueFiles = (data.file_details || data.files.map(name => ({ name, size: 0 }))).map(file => ({ ...file, selected: true }));
        label.textContent = `Scanned ${data.count} file(s) ready.`;
        queueFiles.forEach((file, index) => {
            const li = document.createElement('li');
            li.innerHTML = `<label class="queue-item"><input type="checkbox" data-queue-index="${index}" checked><span class="queue-file-name"></span><span class="queue-file-size">${formatBytes(file.size)}</span></label>`;
            li.querySelector('.queue-file-name').textContent = file.name;
            list.appendChild(li);
        });
        updateQueueSummary();
        gsap.from(".file-list li", { x: -10, opacity: 0, duration: 0.3, stagger: 0.05 });
        showToast(`${data.count} file(s) queued for transfer.`);
    } catch (error) {
        queueFiles = [];
        updateQueueSummary();
        emptyState.classList.remove('hidden');
        label.textContent = `Error: ${error.message}`;
        showToast(error.message);
    } finally {
        btn.textContent = "Scan";
    }
});

document.getElementById('fileList').addEventListener('change', event => {
    const checkbox = event.target.closest('input[data-queue-index]');
    if (!checkbox) return;
    queueFiles[Number(checkbox.dataset.queueIndex)].selected = checkbox.checked;
    updateQueueSummary();
});

// Start Transfer
document.getElementById('startBtn').addEventListener('click', async () => {
    const apiId = parseInt(document.getElementById('apiId').value, 10);
    const apiHash = document.getElementById('apiHash').value.trim();
    const authMode = document.querySelector('input[name="authMode"]:checked').value;
    const activeChat = tabs.upload.classList.contains('active') ? document.getElementById('upChatSelect').value : document.getElementById('downChatSelect').value;
    if (!apiId || !apiHash || !activeChat || (authMode === 'bot' && !document.getElementById('botToken').value.trim())) {
        showToast('Complete the Telegram credentials and choose a chat first.');
        return;
    }
    if (tabs.upload.classList.contains('active') && !document.getElementById('folderPath').value.trim()) {
        showToast('Choose a source file or folder first.');
        return;
    }
    if (tabs.upload.classList.contains('active') && queueFiles.length && !queueFiles.some(file => file.selected)) {
        showToast('Select at least one file to upload.');
        return;
    }

    setTransferControls(true);
    try {
        await saveConfigToBackend();
    } catch (error) {
        setTransferControls(false);
        showToast('Could not save configuration.');
        return;
    }

    const isUpload = tabs.upload.classList.contains('active');
    const endpoint = isUpload ? '/api/start_upload' : '/api/start_download';

    let payload = {
        api_id: apiId,
        api_hash: appConfig.api_hash,
        auth_mode: authMode,
        bot_token: appConfig.bot_token
    };

    if (isUpload) {
        payload.chat_id = document.getElementById('upChatSelect').value;
        payload.folder_path = document.getElementById('folderPath').value;
        payload.grouped = document.getElementById('groupedToggle').checked;
        payload.connection_count = parseInt(document.getElementById('connectionCount').value);
        if (queueFiles.length) payload.selected_files = queueFiles.filter(file => file.selected).map(file => file.name);
    } else {
        payload.chat_id = document.getElementById('downChatSelect').value;
        payload.save_folder = document.getElementById('savePath').value;
        payload.limit = parseInt(document.getElementById('scanLimit').value) || 10;
    }

    try {
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || 'Transfer could not start');
        document.getElementById('startBtn').disabled = true;
        document.getElementById('pauseBtn').disabled = false;
        document.getElementById('cancelBtn').disabled = false;
        document.getElementById('connectionLabel').textContent = 'Transferring';
        lastWidth = 0;
        showToast('Transfer started.');
        startPolling();
    } catch (error) {
        setTransferControls(false);
        showToast(error.message);
    }
});

document.getElementById('pauseBtn').addEventListener('click', async () => {
    const button = document.getElementById('pauseBtn');
    try {
        const res = await fetch('/api/pause', { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || 'Could not change pause state');
        button.querySelector('span').textContent = data.paused ? 'RESUME' : 'PAUSE';
        document.getElementById('connectionLabel').textContent = data.paused ? 'Paused' : 'Transferring';
        showToast(data.paused ? 'Transfer paused between files.' : 'Transfer resumed.');
    } catch (error) {
        showToast(error.message);
    }
});

// Cancel Task
document.getElementById('cancelBtn').addEventListener('click', async () => {
    try {
        await fetch('/api/cancel', { method: 'POST' });
        document.getElementById('cancelBtn').disabled = true;
        document.getElementById('pauseBtn').disabled = true;
        document.getElementById('cancelBtn').textContent = "Aborting...";
        showToast('Cancellation requested.');
    } catch (error) {
        showToast('Could not cancel the transfer.');
    }
});

// Metrics Poller
let pollInterval;
let lastWidth = 0;

function setTransferControls(isRunning, isPaused = false) {
    document.getElementById('startBtn').disabled = isRunning;
    document.getElementById('cancelBtn').disabled = !isRunning;
    document.getElementById('pauseBtn').disabled = !isRunning;
    document.getElementById('pauseBtn').querySelector('span').textContent = isPaused ? 'RESUME' : 'PAUSE';
    document.querySelectorAll('#settingsPanel input, #settingsPanel select, #uploadTabContent input, #uploadTabContent select, #downloadTabContent input, #downloadTabContent select, #scanBtn, #manageChatsBtn1, #manageChatsBtn2, #tabUploadBtn, #tabDownloadBtn').forEach(control => {
        control.disabled = isRunning;
    });
}

function startPolling() {
    if (pollInterval) clearInterval(pollInterval);
    document.getElementById('cancelBtn').textContent = "ABORT";

    const updateStatus = async () => {
        try {
            const res = await fetch('/api/status');
            const data = await res.json();

            document.getElementById('currentFile').textContent = data.current_file;
            document.getElementById('statPct').textContent = `${data.percentage}%`;
            document.getElementById('statSpeed').textContent = `${data.speed_mb} MB/s`;
            document.getElementById('statSize').textContent = `${data.transferred_mb} / ${data.total_mb} MB`;
            document.getElementById('statEta').textContent = data.eta;
            document.getElementById('statJob').textContent = `${data.completed_files || 0} / ${data.total_files || 0} files`;
            document.getElementById('statRemaining').textContent = `${Math.max(0, (data.total_mb || 0) - (data.transferred_mb || 0)).toFixed(2)} MB`;
            document.getElementById('connectionLabel').textContent = data.is_paused ? 'Paused' : (data.is_running ? 'Transferring' : 'Ready');
            setTransferControls(data.is_running, data.is_paused);

            if (data.percentage !== lastWidth) {
                gsap.to("#progressBar", { width: `${data.percentage}%`, duration: 0.5, ease: "power1.out" });
                lastWidth = data.percentage;
            }

            const logBox = document.getElementById('consoleLog');
            const isScrolledToBottom = logBox.scrollHeight - logBox.clientHeight <= logBox.scrollTop + 10;
            logBox.textContent = data.logs.join('\n');
            if (isScrolledToBottom) logBox.scrollTop = logBox.scrollHeight;

            if (!data.is_running) {
                document.getElementById('cancelBtn').textContent = "ABORT";
                clearInterval(pollInterval);
            }
        } catch (error) {
            document.getElementById('connectionLabel').textContent = 'Offline';
            setTransferControls(false);
        }
    };

    updateStatus();
    pollInterval = setInterval(updateStatus, 500);
}