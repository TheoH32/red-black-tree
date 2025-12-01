const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

function fetchAndDraw() {
    // Add a timestamp to prevent browser caching of the JSON file
    fetch('tree_data.json?t=' + new Date().getTime())
        .then(res => res.json())
        .then(data => {
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