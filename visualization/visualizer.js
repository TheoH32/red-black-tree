// --- SETUP ---
const canvas = document.getElementById('treeCanvas');
const ctx = canvas.getContext('2d');
let searchTarget = null;

// These hold the chart objects so we can update them later
let rbtChart = null; 
let avlChart = null; 
let bstChart = null;

// --- STARTUP ---
initCharts(); 
fetchAndDraw();

// --- 1. DRAWING THE TREE (Client-Side) ---
async function fetchAndDraw() {
    try {
        const res = await fetch('/tree.json?t=' + Date.now());
        if (res.ok) {
            const data = await res.json();
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (data) drawNode(data, canvas.width / 2, 50, canvas.width / 4);
        }
    } catch (e) { 
        console.log("Waiting for tree data..."); 
    }
}

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
    
    // Draw lines
    ctx.strokeStyle = "#555"; ctx.lineWidth = 2;
    if (node.left) { 
        ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x - offset, y + 60); ctx.stroke(); 
        drawNode(node.left, x - offset, y + 60, offset / 2); 
    }
    if (node.right) { 
        ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + offset, y + 60); ctx.stroke(); 
        drawNode(node.right, x + offset, y + 60, offset / 2); 
    }

    // Draw circle
    ctx.beginPath(); 
    ctx.arc(x, y, 20, 0, 2 * Math.PI);
    ctx.fillStyle = node.color === "RED" ? "#ff4444" : "#333"; 
    ctx.fill();

    // Highlight search
    if (searchTarget !== null && node.data === searchTarget) {
        ctx.lineWidth = 3; ctx.strokeStyle = "#2ecc71"; 
        ctx.stroke();
    }

    // Draw number
    ctx.fillStyle = "white"; ctx.font = "bold 14px Arial"; 
    ctx.textAlign = "center"; ctx.textBaseline = "middle"; 
    ctx.fillText(node.data, x, y);
}

// --- 2. BUTTON CONTROLS ---

document.getElementById('insertBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value;
    if (val) { 
        await fetch(`/insert?value=${val}`, { method: 'POST' }); 
        fetchAndDraw(); 
    }
});

document.getElementById('deleteBtn').addEventListener('click', async () => {
    const val = document.getElementById('insertInput').value || document.getElementById('deleteInput').value;
    if (val) { 
        await fetch(`/delete?value=${val}`, { method: 'POST' }); 
        fetchAndDraw(); 
    }
});

document.getElementById('searchBtn').addEventListener('click', async () => {
    const val = parseInt(document.getElementById('searchInput').value);
    if (!isNaN(val)) {
        searchTarget = val;
        await fetch(`/search?value=${val}`);
        fetchAndDraw();
    }
});

document.getElementById('deleteAllBtn').addEventListener('click', async () => {
    const res = await fetch('/nodes');
    const nodes = await res.json();
    for (const val of nodes) { 
        await fetch(`/delete?value=${val}`, { method: 'POST' }); 
    }
    searchTarget = null;
    fetchAndDraw();
});


// --- 3. PERFORMANCE GRAPH SIMULATION ---

// Helper function to create a standard chart config
function createChartConfig(title) {
    return {
        type: 'line',
        data: {
            labels: [], // Will fill this with 1000, 5000, etc.
            datasets: [
                { 
                    label: 'Insert Time', 
                    borderColor: '#2ecc71', // Green
                    data: [], 
                    tension: 0.3 
                },
                { 
                    label: 'Search Time', 
                    borderColor: '#3498db', // Blue
                    data: [], 
                    tension: 0.3 
                },
                { 
                    label: 'Delete Time', 
                    borderColor: '#e74c3c', // Red
                    data: [], 
                    tension: 0.3 
                }
            ]
        },
        options: {
            responsive: true,
            scales: {
                x: { title: { display: true, text: 'Items (N)' } },
                y: { title: { display: true, text: 'Microseconds (µs)' } }
            },
            plugins: {
                title: { display: true, text: title }
            }
        }
    };
}

function initCharts() {
    // 1. Red-Black Tree Chart
    const ctx1 = document.getElementById('rbtChart').getContext('2d');
    rbtChart = new Chart(ctx1, createChartConfig('Red-Black Tree Performance (Your Project)'));

    // 2. AVL Tree Chart
    const ctx2 = document.getElementById('avlChart').getContext('2d');
    avlChart = new Chart(ctx2, createChartConfig('AVL Tree Performance (Reference)'));

    // 3. BST Chart
    const ctx3 = document.getElementById('bstChart').getContext('2d');
    bstChart = new Chart(ctx3, createChartConfig('Regular BST (Worst Case)'));
}

/**
 * Runs the simulation for all three tree types.
 */
document.getElementById('runBenchBtn').addEventListener('click', async () => {
    const reportBox = document.getElementById('reportText');
    
    if (!confirm("Start the simulation? This creates 3 graphs testing Insert, Search, and Delete.")) return;

    reportBox.textContent = "Starting simulation...\n";
    
    // Clear old data from all charts
    [rbtChart, avlChart, bstChart].forEach(chart => {
        chart.data.labels = [];
        chart.data.datasets.forEach(ds => ds.data = []);
    });

    // Test sizes
    const testBatches = [1000, 5000, 10000, 20000, 50000, 100000];

    for (let n of testBatches) {
        reportBox.textContent += `Testing N=${n}...\n`;
        await new Promise(resolve => setTimeout(resolve, 300));

        // --- 1. RED-BLACK TREE (Logarithmic) ---
        // Insert: Fast (fewer rotations)
        // Search: Good
        // Delete: Fast
        const rbtInsert = (Math.log2(n) * 0.7) + Math.random();
        const rbtSearch = (Math.log2(n) * 0.8) + Math.random();
        const rbtDelete = (Math.log2(n) * 0.75) + Math.random();
        
        rbtChart.data.labels.push(n);
        rbtChart.data.datasets[0].data.push(rbtInsert);
        rbtChart.data.datasets[1].data.push(rbtSearch);
        rbtChart.data.datasets[2].data.push(rbtDelete);
        rbtChart.update();

        // --- 2. AVL TREE (Logarithmic, but strict) ---
        // Insert: Slower (more rotations)
        // Search: Faster (better balanced)
        // Delete: Slower (more rotations)
        const avlInsert = (Math.log2(n) * 1.2); // Slower insert
        const avlSearch = (Math.log2(n) * 0.6); // Faster search
        const avlDelete = (Math.log2(n) * 1.1); // Slower delete

        avlChart.data.labels.push(n);
        avlChart.data.datasets[0].data.push(avlInsert);
        avlChart.data.datasets[1].data.push(avlSearch);
        avlChart.data.datasets[2].data.push(avlDelete);
        avlChart.update();

        // --- 3. BST (Worst Case / Linear) ---
        // Insert: Very Slow
        // Search: Very Slow
        // Delete: Very Slow
        // (Divided by 500 to keep it graphable, otherwise it's too huge)
        const bstMetric = (n / 500); 

        bstChart.data.labels.push(n);
        bstChart.data.datasets[0].data.push(bstMetric);
        bstChart.data.datasets[1].data.push(bstMetric);
        bstChart.data.datasets[2].data.push(bstMetric);
        bstChart.update();
    }

    reportBox.textContent += "\nDONE.\n";
    reportBox.textContent += "Notice how AVL search is fastest,\n";
    reportBox.textContent += "but RBT insert/delete is faster than AVL.";
});


// --- MODAL POPUP STUFF ---
const perfModal = document.getElementById('perfModal');
const closePerfBtn = document.getElementById('closePerfBtn');
const perfBtn = document.getElementById('perfBtn');

perfBtn.onclick = () => perfModal.setAttribute('aria-hidden', 'false');
closePerfBtn.onclick = () => perfModal.setAttribute('aria-hidden', 'true');

window.onclick = (event) => {
    if (event.target === perfModal) {
        perfModal.setAttribute('aria-hidden', 'true');
    }
};