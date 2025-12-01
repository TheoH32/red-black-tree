// TESTS:

// Root Property: The root of the tree is always black.
// Red Property: Red nodes cannot have red children (no two consecutive red nodes on any path).
// Black Property: Every path from a node to its descendant null nodes (leaves) has the same number of black nodes.
// Leaf Property: All leaves (NIL nodes) are black.
// Search	O(log n)
// Insert	O(log n)
// Delete	O(log n)