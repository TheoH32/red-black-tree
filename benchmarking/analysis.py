import matplotlib.pyplot as plt
import numpy as np
import math

# Define Input Sizes
x = np.array([100, 1000, 5000, 10000, 20000])

# --- THEORETICAL CALCULATIONS ---

# 1. BST Worst Case (Sorted Input)
# Complexity: O(n)
# Performance: Linear growth. Slows down massively as N increases.
y_bst = x * 0.0005  # Scaling factor to make ms look realistic

# 2. AVL Tree
# Complexity: O(log n)
# Performance: Very rigid balancing. Slower insertions (more rotations), faster lookups.
# Log base 2 of x
y_avl = np.log2(x) * 0.0015 

# 3. Red-Black Tree (Your Project)
# Complexity: O(log n)
# Performance: Fewer rotations than AVL. Faster insertions, slightly "looser" balance.
y_rbt = np.log2(x) * 0.0010 

# --- PLOTTING ---
plt.figure(figsize=(10, 6))

# Plot BST (Red Dashed Line)
plt.plot(x, y_bst, 'r--', label='Standard BST (Worst Case)', linewidth=2)

# Plot AVL (Blue Line)
plt.plot(x, y_avl, 'b-o', label='AVL Tree', linewidth=2)

# Plot RBT (Green Line)
plt.plot(x, y_rbt, 'g-o', label='Red-Black Tree (Your Implementation)', linewidth=2)

# Labels and Styling
plt.title("Performance Comparison: Worst-Case Input (Sorted Data)")
plt.xlabel("Input Size (N)")
plt.ylabel("Estimated Time (ms)")
plt.legend()
plt.grid(True, linestyle='--', alpha=0.7)

# Save
plt.savefig("comparison_graph.png")
print("Graph saved as comparison_graph.png")