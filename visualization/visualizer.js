const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');

let searchTarget = null; // integer or null

// --- Persistence for samples -------------------------------------------------
const STORAGE_KEY = 'rbt_perf_samples';
// samples shape: { INSERT: [{n:..., t:...}, ...], DELETE: [...], SEARCH: [...], DELETE_ALL: [...] }
function loadSamples() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : { INSERT: [], DELETE: [], SEARCH: [], DELETE_ALL: [] };
    } catch (e) {
        return { INSERT: [], DELETE: [], SEARCH: [], DELETE_ALL: [] };
    }
}
function saveSamples(samples) {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(samples));
    } catch (e) {
        // ignore quota errors
    }
}
const samples = loadSamples();

// --- Helpers: fetch tree JSON (cache-buster) --------------------------------
async function fetchTreeWithTiming() {
    const url = '/tree.json?t=' + new Date().getTime();
    const start = Date.now();
    try {
        const res = await fetch(url);
        const text = await res.text();
        const duration = Date.now() - start;
        let data = null;
        try { data = JSON.parse(text); } catch (_) { data = null; }
        return { data, duration };
    } catch (err) {
        const duration = Date.now() - start;
        console.error("Error loading tree data:", err);
        return { data: null, duration };
    }
}

async function fetchAndDraw() {
    const { data } = await fetchTreeWithTiming();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (data) drawNode(data, canvas.width / 2, 50, canvas.width / 4);
    return data;
}

// --- Utility: count nodes in returned JSON tree ------------------------------
function getNodeCountFromTree(node) {
    if (!node) return 0;
    let count = 1;
    if (node.left) count += getNodeCountFromTree(node.left);
    if (node.right) count += getNodeCountFromTree(node.right);
    return count;
}

// --- Model-fitting / complexity estimator -----------------------------------
// Candidate functions (proportional models): 1, log2(n), n, n*log2(n), n^2
function f_const(n) { return 1; }
function f_log(n) { return Math.log2(Math.max(1, n)); }
function f_n(n) { return n; }
function f_nlogn(n) { return n * Math.log2(Math.max(1, n)); }
function f_n2(n) { return n * n; }

// Given samples [{n,t}, ...], determine best-fitting model among candidates.
// Uses simple linear proportional fit t ~ a * f(n), chooses model with minimum MSE.
function chooseBestModelForSamples(samplesArr) {
    if (!samplesArr || samplesArr.length === 0) return { label: 'O(?)', used: 0 };

    const funcs = [
        { name: 'O(1)', fn: f_const },
        { name: 'O(log n)', fn: f_log },
        { name: 'O(n)', fn: f_n },
        { name: 'O(n log n)', fn: f_nlogn },
        { name: 'O(n^2)', fn: f_n2 }
    ];

    // Prepare arrays
    const ns = samplesArr.map(s => Math.max(0, s.n));
    const ts = samplesArr.map(s => Math.max(0.0001, s.t)); // avoid zeros

    let best = null;

    for (const cand of funcs) {
        const fs = ns.map(n => cand.fn(n));
        // compute scalar a = sum(t_i * f_i) / sum(f_i^2)
        let num = 0, den = 0;
        for (let i = 0; i < fs.length; i++) {
            num += ts[i] * fs[i];
            den += fs[i] * fs[i];
        }
        if (den === 0) continue;
        const a = num / den;
        // compute MSE
        let mse = 0;
        for (let i = 0; i < fs.length; i++) {
            const pred = a * fs[i];
            const err = ts[i] - pred;
            mse += err * err;
        }
        mse = mse / fs.length;
        // record normalized MSE by variance of ts to compare across scales
        let varT = 0;
        const meanT = ts.reduce((s,v)=>s+v,0)/ts.length;
        for (const v of ts) varT += (v - meanT)*(v - meanT);
        varT = varT / ts.length;
        // if varT is 0 (all ts equal), use raw mse
        const score = varT > 0 ? mse / varT : mse;
        // select minimum score
        if (best === null || score < best.score) {
            best = { name: cand.name, a, mse, score };
        }
    }
    if (!best) return { label: 'O(?)', used: samplesArr.length };
    return { label: best.name, used: samplesArr.length };
}

// --- Logging UI --------------------------------------------------------------
const logListEl = document.getElementById('logList');
function addLogEntry(operation, affected, n, measuredMs) {
    // store sample
    const opKey = operation === 'DELETE_ALL' ? 'DELETE_ALL' : operation;
    if (!samples[opKey]) samples[opKey] = [];
    // store a simple sample {n, t}
    samples[opKey].push({ n: n || 0, t: measuredMs });
    // cap samples to, say, 200
    if (samples[opKey].length > 200) samples[opKey].shift();
    saveSamples(samples);

    // compute best model using all samples for this op
    const estimator = chooseBestModelForSamples(samples[opKey]);

    // create entry and display computed label with sample count
    const entry = document.createElement('div');
    // add operation class for color styling (insert/search/delete)
    const opClassMap = { 'INSERT': 'insert', 'SEARCH': 'search', 'DELETE': 'delete', 'DELETE_ALL': 'delete' };
    entry.className = 'log-entry ' + (opClassMap[operation] || '');
    entry.innerHTML = `
        <span class="log-op">${operation}</span>
        <span class="log-affected">${affected}</span>
        <span class="log-complexity">${estimator.label} (calculated from ${estimator.used} sample${estimator.used === 1 ? '' : 's'})</span>
    `;
    // Include measured ms as a hover tooltip on the complexity element
    const compSpan = entry.querySelector('.log-complexity');
    compSpan.title = `Most recent measured time: ${Math.round(measuredMs)} ms (client-side round-trip)`;

    logListEl.appendChild(entry);
    const box = document.getElementById('logBox');
    box.scrollTop = box.scrollHeight;
    // trim UI entries to keep it snappy
    const maxEntries = 500;
    while (logListEl.children.length > maxEntries) {
        logListEl.removeChild(logListEl.firstChild);
    }
}

// clear logs button also clears samples (optional)
document.getElementById('clearLogBtn').addEventListener('click', () => {
    logListEl.innerHTML = '';
    // keep samples if you want; clear them as well:
    // for complete reset uncomment next lines:
    // samples.INSERT = []; samples.DELETE = []; samples.SEARCH = []; samples.DELETE_ALL = [];
    // saveSamples(samples);
});

// --- Tree operations: measure actual time for each request -------------------

// Insert
document.getElementById('insertBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value;
    if (val === '') return;
    try {
        const start = Date.now();
        const res = await fetch('/insert?value=' + encodeURIComponent(val), { method: 'POST' });
        const t = Date.now() - start;
        if (!res.ok) {
            // try to refresh tree to get n
            const tree = await fetchAndDraw();
            const n = getNodeCountFromTree(tree);
            addLogEntry('INSERT', 'NONE FOUND', n, t);
            return;
        }
        await res.json().catch(()=>{});
        const tree = await fetchAndDraw();
        const n = getNodeCountFromTree(tree);
        addLogEntry('INSERT', val, n, t);
    } catch (err) {
        console.error("Insert failed:", err);
    }
});

// Delete
document.getElementById('deleteBtn').addEventListener('click', async () => {
    const val = document.getElementById('deleteInput').value;
    if (val === '') return;
    try {
        const start = Date.now();
        const res = await fetch('/delete?value=' + encodeURIComponent(val), { method: 'POST' });
        const t = Date.now() - start;
        const tree = await fetchAndDraw();
        const n = getNodeCountFromTree(tree);
        if (!res.ok) {
            addLogEntry('DELETE', 'NONE FOUND', n, t);
        } else {
            addLogEntry('DELETE', val, n, t);
            if (searchTarget !== null && parseInt(val, 10) === searchTarget) {
                searchTarget = null;
            }
        }
        await res.json().catch(()=>{});
    } catch (err) {
        console.error("Delete failed:", err);
    }
});

// Delete All - measure each delete individually and log
document.getElementById('deleteAllBtn').addEventListener('click', async () => {
    try {
        const listRes = await fetch('/nodes');
        if (!listRes.ok) throw new Error('Failed to fetch node list: ' + listRes.status);
        const nodes = await listRes.json(); // expecting an array of ints
        const initialN = nodes.length;
        for (const v of nodes) {
            const start = Date.now();
            const delRes = await fetch('/delete?value=' + encodeURIComponent(v), { method: 'POST' });
            const t = Date.now() - start;
            const currentTree = await fetchAndDraw();
            const n = getNodeCountFromTree(currentTree);
            if (!delRes.ok) {
                addLogEntry('DELETE_ALL', 'NONE FOUND', initialN, t);
            } else {
                addLogEntry('DELETE_ALL', v, initialN, t);
            }
            await delRes.json().catch(()=>{});
        }
        searchTarget = null;
    } catch (err) {
        console.error("Delete All failed:", err);
    }
});

// Search: measure fetching/parsing the tree and then check presence
document.getElementById('searchBtn').addEventListener('click', async () => {
    const valStr = document.getElementById('searchInput').value;
    if (valStr === '') return;
    const val = parseInt(valStr, 10);
    const start = Date.now();
    const { data } = await fetchTreeWithTiming(); // already times fetch
    const t = Date.now() - start;
    searchTarget = val;
    await fetchAndDraw();
    const n = getNodeCountFromTree(data);
    const found = treeContains(data, val);
    addLogEntry('SEARCH', found ? val : 'NONE FOUND', n, t);
});
document.getElementById('clearSearchBtn').addEventListener('click', () => {
    searchTarget = null;
    document.getElementById('searchInput').value = '';
    fetchAndDraw();
});
document.getElementById('searchInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        document.getElementById('searchBtn').click();
    }
});

// Helper to traverse JSON tree and find a value (assumes node.data holds numeric or string)
function treeContains(node, value) {
    if (!node) return false;
    try {
        if (parseInt(node.data, 10) === value) return true;
    } catch (e) { /* ignore parse error */ }
    if (node.left && treeContains(node.left, value)) return true;
    if (node.right && treeContains(node.right, value)) return true;
    return false;
}

// --- Drawing code (unchanged except for location) ---------------------------
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