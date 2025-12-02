const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

document.getElementById('insertBtn').addEventListener('click', () => {
    const val = document.getElementById('insertInput').value;
    if (val === '') return;
    fetch('/insert?value=' + encodeURIComponent(val), { method: 'POST' })
        .then(res => {
            if (!res.ok) return Promise.reject('Insert failed: ' + res.status);
            return res.json();
        })
        .then(() => fetchAndDraw())
        .catch(err => console.error("Insert failed:", err));
});

document.getElementById('deleteBtn').addEventListener('click', () => {
    const val = document.getElementById('deleteInput').value;
    if (val === '') return;
    fetch('/delete?value=' + encodeURIComponent(val), { method: 'POST' })
        .then(res => {
            if (!res.ok) return Promise.reject('Delete failed: ' + res.status);
            return res.json();
        })
        .then(() => fetchAndDraw())
        .catch(err => console.error("Delete failed:", err));
});

document.getElementById('deleteAllBtn').addEventListener('click', () => {
    // Delete All -> reuse /clear endpoint
    fetch('/clear', { method: 'POST' })
        .then(res => {
            if (!res.ok) return Promise.reject('Delete All failed: ' + res.status);
            return res.text();
        })
        .then(() => fetchAndDraw())
        .catch(err => console.error("Delete All failed:", err));
});

document.getElementById('clearBtn').addEventListener('click', () => {
    fetch('/clear', { method: 'POST' })
        .then(() => fetchAndDraw())
        .catch(err => console.error("Clear failed:", err));
});

document.getElementById('refreshBtn').addEventListener('click', fetchAndDraw);

function fetchAndDraw() {
    // Request current tree JSON from server
    fetch('/tree.json?t=' + new Date().getTime())
        .then(res => res.text())
        .then(text => {
            let data = null;
            try { data = JSON.parse(text); } catch (e) { /* JSON.parse(null) -> null; other errors will be caught */ }
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (data) drawNode(data, canvas.width / 2, 50, canvas.width / 4);
        })
        .catch(err => console.error("Error loading tree data:", err));
}

function drawNode(node, x, y, offset) {
    if (!node) return;

    // Draw edges first (so they appear behind nodes)
    ctx.strokeStyle = "#555";
    ctx.lineWidth = 2;

    if (node.left) {
        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.lineTo(x - offset, y + 60);
        ctx.stroke();
        drawNode(node.left, x - offset, y + 60, offset / 2);
    }
    if (node.right) {
        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.lineTo(x + offset, y + 60);
        ctx.stroke();
        drawNode(node.right, x + offset, y + 60, offset / 2);
    }

    // Draw Node Circle
    ctx.beginPath();
    ctx.arc(x, y, 20, 0, 2 * Math.PI);
    ctx.fillStyle = node.color === "RED" ? "#ff4444" : "#333";
    ctx.fill();
    ctx.stroke();

    // Draw Text
    ctx.fillStyle = "white";
    ctx.font = "bold 14px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(node.data, x, y);
}

// Initial draw
fetchAndDraw();