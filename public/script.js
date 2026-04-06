document.addEventListener('DOMContentLoaded', () => {
    // Tabs functionality
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            // Remove active class from all
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            
            // Add active class to clicked
            btn.classList.add('active');
            document.getElementById(btn.dataset.tab).classList.add('active');
            
            // Clear inputs and results when switching tabs
            clearForm();
        });
    });

    const HOST = 'http://localhost:8080';

    // Encrypt & Save
    document.getElementById('btn-encrypt').addEventListener('click', async () => {
        const passphrase = document.getElementById('write-passphrase').value;
        const text = document.getElementById('write-text').value;

        if (!passphrase) return showToast('Passphrase is required', 'error');
        if (!text) return showToast('Message is required', 'error');

        try {
            const res = await fetch(`${HOST}/api/write`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ passphrase, text })
            });
            const data = await res.json();
            
            if (data.success) {
                showToast('Message encrypted and saved successfully!', 'success');
                document.getElementById('write-text').value = '';
            } else {
                showToast(data.error || 'Failed to save', 'error');
            }
        } catch (err) {
            showToast('Network error or server down', 'error');
            console.error(err);
        }
    });

    // Decrypt Message
    document.getElementById('btn-decrypt').addEventListener('click', async () => {
        const passphrase = document.getElementById('read-passphrase').value;
        if (!passphrase) return showToast('Passphrase is required', 'error');

        try {
            const res = await fetch(`${HOST}/api/read`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ passphrase })
            });
            const data = await res.json();
            
            const resultArea = document.getElementById('decrypt-result-area');
            const resultBox = document.getElementById('decrypt-result');

            if (data.success) {
                resultArea.classList.remove('hidden');
                resultBox.textContent = data.text;
                showToast('Message decrypted successfully', 'success');
            } else {
                resultArea.classList.add('hidden');
                showToast(data.error || 'Decryption failed', 'error');
            }
        } catch (err) {
            showToast('Network error or server down', 'error');
            console.error(err);
        }
    });

    // Fetch Raw
    document.getElementById('btn-fetch-raw').addEventListener('click', async () => {
        try {
            const res = await fetch(`${HOST}/api/raw`);
            const data = await res.json();
            
            const resultArea = document.getElementById('raw-result-area');
            const resultBox = document.getElementById('raw-result');

            if (data.success) {
                resultArea.classList.remove('hidden');
                resultBox.textContent = data.text;
            } else {
                resultArea.classList.add('hidden');
                showToast(data.error || 'Failed to fetch raw data', 'error');
            }
        } catch (err) {
            showToast('Network error or server down', 'error');
            console.error(err);
        }
    });

    function clearForm() {
        document.getElementById('write-passphrase').value = '';
        document.getElementById('write-text').value = '';
        document.getElementById('read-passphrase').value = '';
        document.getElementById('decrypt-result-area').classList.add('hidden');
        document.getElementById('raw-result-area').classList.add('hidden');
    }

    function showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icon = type === 'success' ? '✅' : '❌';
        toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
        
        container.appendChild(toast);
        
        // Remove on click
        toast.addEventListener('click', () => {
            toast.remove();
        });

        // Auto remove after 3s
        setTimeout(() => {
            toast.classList.add('toast-time');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }
});
