const HOST = 'http://localhost:8080';

// State
let state = {
    displayName: '',
    passphrase: '',
    isAdmin: false,
    pollInterval: null,
    lastMessageCount: 0
};

document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const loginContainer = document.getElementById('login-container');
    const chatContainer = document.getElementById('chat-container');
    const chatMessages = document.getElementById('chat-messages');
    const chatInput = document.getElementById('chat-input');
    
    // Login
    document.getElementById('btn-join').addEventListener('click', () => {
        const name = document.getElementById('display-name').value.trim();
        const pass = document.getElementById('room-passphrase').value;
        
        if (!name || !pass) {
            return showToast('Both name and passphrase are required', 'error');
        }
        
        state.displayName = name;
        state.passphrase = pass;
        state.isAdmin = false;
        
        enterChat(`Logged in as ${name}`);
    });

    // Admin Login
    document.getElementById('btn-admin-login').addEventListener('click', () => {
        state.isAdmin = true;
        state.passphrase = '';
        state.displayName = 'Admin (View Only)';
        
        // Hide input area for admin
        document.getElementById('chat-input-area').style.display = 'none';
        
        enterChat('Logged in as Admin');
    });

    // Send Message
    const sendMessage = async () => {
        if (state.isAdmin) return;
        
        const rawText = chatInput.value.trim();
        if (!rawText) return;
        
        try {
            // Encrypt the message locally
            const encryptedText = CryptoJS.AES.encrypt(rawText, state.passphrase).toString();
            
            const payload = {
                sender: state.displayName,
                text: encryptedText,
                timestamp: new Date().toISOString()
            };

            // Optimistic clear input
            chatInput.value = '';

            const res = await fetch(`${HOST}/api/messages`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            
            if (data.success) {
                pollMessages(); // Fetch immediately
            } else {
                showToast(data.error || 'Failed to send', 'error');
            }
        } catch (err) {
            showToast('Failed to encrypt or send message', 'error');
            console.error(err);
        }
    };

    document.getElementById('btn-send').addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });

    // Leave Chat
    document.getElementById('btn-leave').addEventListener('click', () => {
        if (state.pollInterval) {
            clearInterval(state.pollInterval);
        }
        state = { displayName: '', passphrase: '', isAdmin: false, pollInterval: null, lastMessageCount: 0 };
        
        document.getElementById('display-name').value = '';
        document.getElementById('room-passphrase').value = '';
        document.getElementById('chat-input-area').style.display = 'flex';
        
        chatContainer.classList.add('hidden');
        loginContainer.classList.remove('hidden');
    });

    function enterChat(toastMsg) {
        loginContainer.classList.add('hidden');
        chatContainer.classList.remove('hidden');
        
        document.getElementById('user-info').textContent = state.isAdmin ? 'Admin View - Encrypted' : state.displayName;
        
        showToast(toastMsg, 'success');
        
        // Initial fetch
        pollMessages();
        
        // Start polling
        state.pollInterval = setInterval(pollMessages, 2000);
    }

    async function pollMessages() {
        try {
            const res = await fetch(`${HOST}/api/messages`);
            const messages = await res.json();
            
            // Render if count changed (naive check)
            if (messages.length !== state.lastMessageCount) {
                state.lastMessageCount = messages.length;
                renderMessages(messages);
            }
        } catch (err) {
            console.error('Polling error', err);
        }
    }

    function renderMessages(messages) {
        chatMessages.innerHTML = '';
        
        if (messages.length === 0) {
            chatMessages.innerHTML = '<div class="empty-chat">No messages yet...</div>';
            return;
        }

        messages.forEach(msg => {
            const wrapper = document.createElement('div');
            wrapper.className = `message ${msg.sender === state.displayName ? 'sent' : 'received'}`;
            
            if (state.isAdmin) {
                // Admin sees encrypted data
                wrapper.innerHTML = `
                    <div class="msg-sender">${msg.sender}</div>
                    <div class="msg-bubble raw-text">${msg.text}</div>
                `;
            } else {
                // User tries to decrypt
                let displayTxt = '';
                let isError = false;
                try {
                    const bytes = CryptoJS.AES.decrypt(msg.text, state.passphrase);
                    displayTxt = bytes.toString(CryptoJS.enc.Utf8);
                    if (!displayTxt) throw new Error('Empty');
                } catch (e) {
                    displayTxt = '🔒 [Encrypted / Wrong Passphrase]';
                    isError = true;
                }
                
                wrapper.innerHTML = `
                    <div class="msg-sender">${msg.sender}</div>
                    <div class="msg-bubble ${isError ? 'error-bubble' : ''}">${escapeHtml(displayTxt)}</div>
                `;
            }
            chatMessages.appendChild(wrapper);
        });

        // Scroll to bottom
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function escapeHtml(unsafe) {
        return unsafe.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    function showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icon = type === 'success' ? '✅' : '❌';
        toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
        
        container.appendChild(toast);
        
        toast.addEventListener('click', () => toast.remove());
        
        setTimeout(() => {
            toast.classList.add('toast-time');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }
});
