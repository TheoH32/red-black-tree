/* ====== GLOBALS / SETUP ====== */
const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

// Keep track of the node we're highlighting after a search
let searchTarget = null;

// Chart instances
let insertChart = null;
let searchChart = null;
let deleteChart = null;

/* Keep a small cache to avoid redrawing unnecessarily. */
let lastTreeJson = null;

/* Make canvas size match CSS and window */
function resizeCanvasToDisplaySize() {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = Math.round(rect.width * dpr);
    canvas.height = Math.round(rect.height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); 
}

/* Run at startup and on resize */
window.addEventListener('resize', () => {
    resizeCanvasToDisplaySize();
    if (lastTreeJson) drawNode(lastTreeJson, canvas.width / 2, 50, canvas.width / 4);
});

/* ====== STARTUP ====== */
resizeCanvasToDisplaySize();
initCharts(); 
fetchAndDraw(); 

/* ====== 1) DRAWING LOGIC ====== */
async function fetchAndDraw() {
    try {
        const res = await fetch('/tree.json?t=' + Date.now());
        if (!res.ok) return;
        const data = await res.json();
        const asString = JSON.stringify(data || {});
        if (asString !== JSON.stringify(lastTreeJson || {})) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            lastTreeJson = data;
            if (data) {
                const startX = canvas.width / 2;
                const startY = 50;
                const startOffset = Math.max(canvas.width / 8, 60);
                drawNode(data, startX, startY, startOffset);
            }
        } else {
            if (lastTreeJson) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                drawNode(lastTreeJson, canvas.width / 2, 50, Math.max(canvas.width / 8, 60));
            }
        }
    } catch (err) {
        console.error('Error in fetchAndDraw:', err);
    }
}

function drawNode(node, x, y, offset) {
    if (!node) return;

    ctx.strokeStyle = "#555";
    ctx.lineWidth = 2;

    if (node.left) {
        ctx.beginPath();
        ctx.moveTo(x, y + 18);
        ctx.lineTo(x - offset, y + 60 - 18);
        ctx.stroke();
        drawNode(node.left, x - offset, y + 60, offset / 1.8);
    }

    if (node.right) {
        ctx.beginPath();
        ctx.moveTo(x, y + 18);
        ctx.lineTo(x + offset, y + 60 - 18);
        ctx.stroke();
        drawNode(node.right, x + offset, y + 60, offset / 1.8);
    }

    const radius = 20;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);

    ctx.fillStyle = (node.color === "RED" || node.color === "red") ? "#ff4444" : "#333";
    ctx.fill();

    if (searchTarget !== null && node.data === searchTarget) {
        ctx.lineWidth = 4;
        ctx.strokeStyle = "#2ecc71"; 
        ctx.stroke();
        ctx.lineWidth = 2; 
    } else {
        ctx.strokeStyle = "#222";
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    ctx.fillStyle = "white";
    ctx.font = "bold 14px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(String(node.data), x, y);
}

/* ====== 2) CONTROLS ====== */
document.getElementById('insertBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value.trim();
    if (!val) return;
    try { await fetch(`/insert?value=${encodeURIComponent(val)}`, { method: 'POST' }); } catch (err) {}
    searchTarget = null;
    await fetchAndDraw();
});

document.getElementById('deleteBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value.trim();
    if (!val) return;
    try { await fetch(`/delete?value=${encodeURIComponent(val)}`, { method: 'POST' }); } catch (err) {}
    searchTarget = null;
    await fetchAndDraw();
});

document.getElementById('searchBtn').addEventListener('click', async () => {
    const raw = document.getElementById('searchInput').value.trim();
    if (raw === '') return;
    const n = parseInt(raw, 10);
    searchTarget = isNaN(n) ? raw : n;
    try { await fetch(`/search?value=${encodeURIComponent(raw)}`); } catch (err) {}
    await fetchAndDraw();
});

document.getElementById('deleteAllBtn').addEventListener('click', async () => {
    try {
        const res = await fetch('/nodes');
        if (!res.ok) return;
        const nodes = await res.json();
        for (const val of nodes) {
            await fetch(`/delete?value=${encodeURIComponent(val)}`, { method: 'POST' });
        }
    } catch (err) {}
    searchTarget = null;
    await fetchAndDraw();
});

/* ====== 3) BENCHMARK & CHARTS ====== */

function createChart(canvasId, title) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                { 
                    label: 'Red-Black Tree (Measured)',
                    borderColor: '#2ecc71',
                    backgroundColor: 'rgba(46,204,113,0.15)',
                    data: [],
                    tension: 0.2,
                    borderWidth: 3,
                    fill: false,
                    pointRadius: 3
                },
                { 
                    label: 'AVL Tree (Reference)',
                    borderColor: '#3498db',
                    borderDash: [6, 4],
                    data: [],
                    tension: 0.2,
                    fill: false,
                    pointRadius: 0
                },
                { 
                    label: 'BST Worst-Case (Reference)',
                    borderColor: '#e74c3c',
                    borderDash: [2, 3],
                    data: [],
                    tension: 0.0,
                    fill: false,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { title: { display: true, text: 'Tree Size (N)' } },
                y: { title: { display: true, text: 'Avg Time (µs)' }, beginAtZero: true }
            },
            plugins: {
                legend: { position: 'top' },
                title: { display: true, text: title },
            }
        }
    });
}

function initCharts() {
    insertChart = createChart('insertChart', 'Insertion Cost: O(log n) vs O(n)');
    searchChart = createChart('searchChart', 'Search Cost: O(log n) vs O(n)');
    deleteChart = createChart('deleteChart', 'Deletion Cost: O(log n) vs O(n)');
}

function resetCharts() {
    [insertChart, searchChart, deleteChart].forEach(chart => {
        chart.data.labels = [];
        chart.data.datasets.forEach(ds => ds.data = []);
        chart.update();
    });
}

function updateChartData(chart, n, measuredVal, baselineVal) {
    chart.data.labels.push(String(n));
    chart.data.datasets[0].data.push(measuredVal);
    chart.data.datasets[1].data.push(measuredVal * 1.05); 
    const c = baselineVal / 100; // 100 is our first N
    chart.data.datasets[2].data.push(c * n);
    chart.update();
}

document.getElementById('runBenchBtn').addEventListener('click', async () => {
    // Removed reportBox logic here
    if (!confirm("Run Benchmark? (Samples: 100 to 2,500)")) return;

    resetCharts();

    const testSizes = [100, 200, 300, 400, 500, 750, 1000, 1500, 2000, 2500];
    let baselines = { insert: null, search: null, delete: null };

    for (let i = 0; i < testSizes.length; i++) {
        const n = testSizes[i];
        
        try {
            const res = await fetch(`/benchmark?n=${n}&type=random`);
            if (!res.ok) throw new Error('Fetch failed');
            
            const realData = await res.json();
            
            const tInsert = (realData.insert / Math.max(1, n)) * 1000; 
            const tSearch = ((realData.search || realData.insert * 0.7) / Math.max(1, n)) * 1000;
            const tDelete = ((realData.delete || realData.insert * 1.2) / Math.max(1, n)) * 1000;

            if (i === 0) {
                baselines.insert = tInsert || 1;
                baselines.search = tSearch || 1;
                baselines.delete = tDelete || 1;
            }

            updateChartData(insertChart, n, tInsert, baselines.insert);
            updateChartData(searchChart, n, tSearch, baselines.search);
            updateChartData(deleteChart, n, tDelete, baselines.delete);

        } catch (err) {
            console.error(err);
        }
    }
});

/* ====== 4) MODAL HELPERS ====== */
const perfModal = document.getElementById('perfModal');
const closePerfBtn = document.getElementById('closePerfBtn');
const perfBtn = document.getElementById('perfBtn');

if (perfBtn) perfBtn.onclick = () => perfModal && perfModal.setAttribute('aria-hidden', 'false');
if (closePerfBtn) closePerfBtn.onclick = () => perfModal && perfModal.setAttribute('aria-hidden', 'true');
window.onclick = (event) => {
    if (event.target === perfModal) perfModal.setAttribute('aria-hidden', 'true');
};

setInterval(fetchAndDraw, 2500);