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
        // After all deletes, refresh the view
        fetchAndDraw();
    } catch (err) {
        console.error("Delete All failed:", err);
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