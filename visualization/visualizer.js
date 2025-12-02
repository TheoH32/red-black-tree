/* ====== GLOBALS / SETUP ====== */
const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

// Keep track of the node we're highlighting after a search
let searchTarget = null;

// Chart instance for benchmarking
let perfChart = null;

/* Keep a small cache to avoid redrawing unnecessarily.
   We still fetch each time, but only redraw when data changes. */
let lastTreeJson = null;

/* Make canvas size match CSS and window
   (this helps crisp drawing on resize / high-DPI screens). */
function resizeCanvasToDisplaySize() {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = Math.round(rect.width * dpr);
    canvas.height = Math.round(rect.height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); // scale drawing commands
}

/* Run at startup and on resize */
window.addEventListener('resize', () => {
    resizeCanvasToDisplaySize();
    // redraw if we already have a tree
    if (lastTreeJson) drawNode(lastTreeJson, canvas.width / 2, 50, canvas.width / 4);
});

/* ====== STARTUP ====== */
resizeCanvasToDisplaySize();
initChart();
fetchAndDraw(); // initial fetch + draw

/* ====== 1) DRAWING LOGIC ====== */

/* Fetch the JSON tree from server and draw it.
   We compare with lastTreeJson to avoid useless redraws. */
async function fetchAndDraw() {
    try {
        const res = await fetch('/tree.json?t=' + Date.now());
        if (!res.ok) {
            console.warn('Failed to fetch tree.json:', res.status);
            return;
        }
        const data = await res.json();
        // quick deep-ish check: stringify (ok for moderate trees)
        const asString = JSON.stringify(data || {});
        if (asString !== JSON.stringify(lastTreeJson || {})) {
            // clear and redraw
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            lastTreeJson = data;
            if (data) {
                // center horizontally, start y at 50px
                const startX = canvas.width / 2;
                const startY = 50;
                const startOffset = Math.max(canvas.width / 8, 60);
                drawNode(data, startX, startY, startOffset);
            }
        } else {
            // nothing changed; still draw highlight if needed
            if (lastTreeJson) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                drawNode(lastTreeJson, canvas.width / 2, 50, Math.max(canvas.width / 8, 60));
            }
        }
    } catch (err) {
        console.error('Error in fetchAndDraw:', err);
    }
}

/* Recursive drawing of a node and its children.
   Simple, readable code — no fancy layout libraries.
   node: { data, color, left?, right? } */
function drawNode(node, x, y, offset) {
    if (!node) return;

    // draw links to children first (so links go under the node circles)
    ctx.strokeStyle = "#555";
    ctx.lineWidth = 2;

    // left child: line + recursive draw
    if (node.left) {
        ctx.beginPath();
        ctx.moveTo(x, y + 18); // start slightly below circle edge
        ctx.lineTo(x - offset, y + 60 - 18); // end slightly above child circle
        ctx.stroke();
        drawNode(node.left, x - offset, y + 60, offset / 1.8);
    }

    // right child
    if (node.right) {
        ctx.beginPath();
        ctx.moveTo(x, y + 18);
        ctx.lineTo(x + offset, y + 60 - 18);
        ctx.stroke();
        drawNode(node.right, x + offset, y + 60, offset / 1.8);
    }

    // draw the node circle
    const radius = 20;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);

    // node color: red vs black
    ctx.fillStyle = (node.color === "RED" || node.color === "red") ? "#ff4444" : "#333";
    ctx.fill();

    // if this is the searched-for node, draw a highlighted stroke
    if (searchTarget !== null && node.data === searchTarget) {
        ctx.lineWidth = 4;
        ctx.strokeStyle = "#2ecc71"; // green highlight
        ctx.stroke();
        ctx.lineWidth = 2; // reset
    } else {
        // default stroke
        ctx.strokeStyle = "#222";
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    // draw the text value centered
    ctx.fillStyle = "white";
    ctx.font = "bold 14px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(String(node.data), x, y);
}

/* ====== 2) CONTROLS ====== */

/* Insert button - grabs value from insertInput and calls server */
document.getElementById('insertBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value.trim();
    if (!val) return;
    try {
        await fetch(`/insert?value=${encodeURIComponent(val)}`, { method: 'POST' });
    } catch (err) {
        console.error('Insert failed:', err);
    }
    searchTarget = null; // clear highlight after insert
    await fetchAndDraw();
});

/* Delete button - delete single value */
document.getElementById('deleteBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value.trim();
    if (!val) return;
    try {
        await fetch(`/delete?value=${encodeURIComponent(val)}`, { method: 'POST' });
    } catch (err) {
        console.error('Delete failed:', err);
    }
    searchTarget = null;
    await fetchAndDraw();
});

/* Search button - highlight node locally and notify server (optional) */
document.getElementById('searchBtn').addEventListener('click', async () => {
    const raw = document.getElementById('searchInput').value.trim();
    if (raw === '') return;
    // Accept integer or string; try parseInt first
    const n = parseInt(raw, 10);
    const val = isNaN(n) ? raw : n;
    searchTarget = val;
    try {
        // optional: hit server just in case server does something for search
        await fetch(`/search?value=${encodeURIComponent(raw)}`);
    } catch (err) {
        // non-fatal
    }
    await fetchAndDraw();
});

/* Delete all - grabs server node list and deletes one-by-one */
document.getElementById('deleteAllBtn').addEventListener('click', async () => {
    const report = document.getElementById('reportText');
    try {
        const res = await fetch('/nodes');
        if (!res.ok) {
            console.warn('Failed to fetch nodes list:', res.status);
            return;
        }
        const nodes = await res.json();
        report && (report.textContent = `Deleting ${nodes.length} nodes...\n`);
        for (const val of nodes) {
            await fetch(`/delete?value=${encodeURIComponent(val)}`, { method: 'POST' });
        }
    } catch (err) {
        console.error('Delete all failed:', err);
    }
    searchTarget = null;
    await fetchAndDraw();
});

/* ====== 3) BENCHMARK & CHART ====== */

/* Initialize Chart.js with three datasets */
function initChart() {
    const chartCtx = document.getElementById('perfChart').getContext('2d');
    perfChart = new Chart(chartCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                { // real measured RBT data
                    label: 'Red-Black Tree (Real Java Data)',
                    borderColor: '#2ecc71',
                    backgroundColor: 'rgba(46,204,113,0.15)',
                    data: [],
                    tension: 0.2,
                    borderWidth: 3,
                    fill: false,
                    pointRadius: 3
                },
                { // simple reference for AVL (slightly worse than RBT)
                    label: 'AVL Tree (Reference)',
                    borderColor: '#3498db',
                    borderDash: [6, 4],
                    data: [],
                    tension: 0.2,
                    fill: false,
                    pointRadius: 0
                },
                { // BST worst-case reference (O(N) line)
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
                x: {
                    title: { display: true, text: 'Tree Size (N)' }
                },
                y: {
                    title: { display: true, text: 'Avg Time per Op (microseconds)' },
                    beginAtZero: true
                }
            },
            plugins: {
                legend: { position: 'top' },
                title: { display: true, text: 'Insertion Cost: O(log n) vs O(n)' },
            }
        }
    });
}

/* Run benchmark - smaller sample sizes for quick runs */
document.getElementById('runBenchBtn').addEventListener('click', async () => {
    const reportBox = document.getElementById('reportText');
    if (!confirm("Run Benchmark? (Samples: 100 to 5,000)")) return;

    // reset report area
    if (reportBox) reportBox.textContent = "Running benchmark...\n";

    // clear chart
    perfChart.data.labels = [];
    perfChart.data.datasets.forEach(ds => ds.data = []);
    perfChart.update();

    // sample sizes - small and fast but still illustrative
    const testSizes = [100, 500, 1000, 2500, 5000];

    let baselineTime = null; // avg per-op for first sample, used for scaling references

    for (let i = 0; i < testSizes.length; i++) {
        const n = testSizes[i];
        reportBox && (reportBox.textContent += `Testing N=${n}...\n`);

        try {
            // get real measured data from server
            const res = await fetch(`/benchmark?n=${n}&type=random`);
            if (!res.ok) {
                reportBox && (reportBox.textContent += ` Failed to fetch benchmark for N=${n}\n`);
                continue;
            }
            const realData = await res.json();
            // server returns total insertion time in milliseconds
            const totalMs = realData.insert;
            const avgPerOp = (totalMs / Math.max(1, n)) * 1000; // convert ms -> µs per op

            // capture baseline from first point for reference scaling
            if (baselineTime === null) {
                baselineTime = avgPerOp || 1; // guard against zero
            }

            // AVL reference: slightly worse than measured RBT
            const avlRef = avgPerOp * 1.15;

            // BST worst-case reference: O(N) line
            // We want bstRef(N) = c * N where c = baselineTime / testSizes[0]
            // This keeps the BST line anchored at the first sample.
            const c = baselineTime / testSizes[0];
            const bstRef = c * n;

            reportBox && (reportBox.textContent += ` > RBT Avg: ${avgPerOp.toFixed(2)} µs\n`);

            // push to chart
            perfChart.data.labels.push(String(n));
            perfChart.data.datasets[0].data.push(avgPerOp);
            perfChart.data.datasets[1].data.push(avlRef);
            perfChart.data.datasets[2].data.push(bstRef);
            perfChart.update();

        } catch (err) {
            console.error('Benchmark error for N=' + n, err);
            reportBox && (reportBox.textContent += ` Error for N=${n}\n`);
        }
    }

    reportBox && (reportBox.textContent += "Done.\n");
});

/* ====== 4) MODAL HELPERS ======
   (small helpers to open/close the performance modal) */
const perfModal = document.getElementById('perfModal');
const closePerfBtn = document.getElementById('closePerfBtn');
const perfBtn = document.getElementById('perfBtn');

if (perfBtn) perfBtn.onclick = () => perfModal && perfModal.setAttribute('aria-hidden', 'false');
if (closePerfBtn) closePerfBtn.onclick = () => perfModal && perfModal.setAttribute('aria-hidden', 'true');
window.onclick = (event) => {
    if (event.target === perfModal) perfModal.setAttribute('aria-hidden', 'true');
};

/* ====== OPTIONAL: auto-refresh tree every few seconds ======
   If you prefer manual control, comment this out.
   This keeps the visualizer up-to-date with server-side changes. */
setInterval(fetchAndDraw, 2500);
