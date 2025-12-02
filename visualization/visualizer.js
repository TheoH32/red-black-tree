const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

let searchTarget = null; // integer or null

// add/restore this function near the top of visualizer.js
function fetchAndDraw() {
    // Request current tree JSON from server (cache-buster)
    fetch('/tree.json?t=' + new Date().getTime())
        .then(res => res.text())
        .then(text => {
            let data = null;
            try { data = JSON.parse(text); } catch (e) { data = null; }
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (data) drawNode(data, canvas.width / 2, 50, canvas.width / 4);
        })
        .catch(err => console.error("Error loading tree data:", err));
}

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
        .then(() => {
            // if deleted node was the highlighted one, clear highlight
            if (searchTarget !== null && parseInt(val, 10) === searchTarget) {
                searchTarget = null;
            }
            fetchAndDraw();
        })
        .catch(err => console.error("Delete failed:", err));
});

document.getElementById('deleteAllBtn').addEventListener('click', async () => {
    try {
        const res = await fetch('/nodes');
        if (!res.ok) throw new Error('Failed to fetch node list: ' + res.status);
        const nodes = await res.json(); // expecting an array of ints
        // Delete nodes sequentially to avoid concurrency issues in deletion logic
        for (const v of nodes) {
            const delRes = await fetch('/delete?value=' + encodeURIComponent(v), { method: 'POST' });
            if (!delRes.ok) {
                console.warn('Failed to delete value', v, 'status', delRes.status);
            }
        }
        // After all deletes, refresh the view and clear search
        searchTarget = null;
        fetchAndDraw();
    } catch (err) {
        console.error("Delete All failed:", err);
    }
});

// Search functionality
document.getElementById('searchBtn').addEventListener('click', () => {
    const val = document.getElementById('searchInput').value;
    if (val === '') return;
    searchTarget = parseInt(val, 10);
    fetchAndDraw();
});
document.getElementById('clearSearchBtn').addEventListener('click', () => {
    searchTarget = null;
    document.getElementById('searchInput').value = '';
    fetchAndDraw();
});
// allow pressing Enter to search
document.getElementById('searchInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        document.getElementById('searchBtn').click();
    }
});

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

    // If this node matches the search target, draw a highlight ring behind the node
    const isMatch = (searchTarget !== null) && (parseInt(node.data, 10) === searchTarget);
    if (isMatch) {
        ctx.beginPath();
        ctx.arc(x, y, 26, 0, 2 * Math.PI); // slightly bigger than node circle
        ctx.lineWidth = 4;
        ctx.strokeStyle = "#00cc66";
        ctx.shadowColor = "rgba(0,204,102,0.4)";
        ctx.shadowBlur = 12;
        ctx.stroke();
        // reset shadow so other drawings aren't affected
        ctx.shadowBlur = 0;
    }

    // Draw Node Circle
    ctx.beginPath();
    ctx.arc(x, y, 20, 0, 2 * Math.PI);
    ctx.fillStyle = node.color === "RED" ? "#ff4444" : "#333";
    ctx.fill();
    ctx.strokeStyle = "#222";
    ctx.lineWidth = 1;
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