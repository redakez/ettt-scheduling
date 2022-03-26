package util;

import java.util.*;

/**
 * Simple implementation of an interval tree with int intervals.
 * Includes insert, remove and intersects methods.
 */
public class IntervalTree {

    private Node root; //Root node of the tree, it is null if the tree is empty

    private class Node {

        //Lower number in an interval
        int lo;

        //Higher number in an interval
        int hi;

        //Maximum hi value in current subtree
        int max;

        //Parent node
        Node parent;

        //Left child
        Node left;

        //Right child
        Node right;

        public Node(int lo, int hi) {
            this.lo = lo;
            this.hi = hi;
            this.max = hi;
        }

        /**
         * @param otherLo lower number of an interval
         * @param otherHi higher number of an interval
         * @return if this node intersects the given interval
         */
        boolean intersects(int otherLo, int otherHi) {
            return otherHi >= lo && otherLo <= hi;
        }

        /**
         * Replaces this node in its parent with a new child.
         * Also sets the parent in the new child node.
         * @param newChild new child to replace this node
         */
        void changeParentsChildTo(Node newChild) {
            if (parent == null) {
                root = newChild;
            } else {
                boolean leftChild = lo <= parent.lo;
                if (leftChild) {
                    parent.left = newChild;
                } else {
                    parent.right = newChild;
                }
                if (newChild != null) {
                    newChild.parent = parent;
                }
            }
        }

        @Override
        public String toString() {
            return lo + "," + hi + "|" + max;
        }
    }

    /**
     * Adds a new interval into the interval tree.
     * The arguments can be provided in the wrong order.
     * @param lo lower number in the given interval
     * @param hi higher number in the given interval
     */
    public void add(int lo, int hi) {
        if (lo > hi) {
            int tmp = lo;
            lo = hi;
            hi = tmp;
        }
        Node toInsertNode = new Node(lo, hi);
        if (root == null) {
            root = toInsertNode;
            return;
        }
        Node cur = root;
        while (true) {
            if (lo > cur.lo) {
                if (cur.right == null) {
                    cur.right = toInsertNode;
                    break;
                }
                cur = cur.right;
            } else {
                if (cur.left == null) {
                    cur.left = toInsertNode;
                    break;
                }
                cur = cur.left;
            }
        }
        toInsertNode.parent = cur;
        while (cur != null && cur.max < toInsertNode.max) {
            cur.max = toInsertNode.max;
            cur = cur.parent;
        }
    }

    /**
     * @param lo lower number in a given interval
     * @param hi higher number in a given interval
     * @return if the given interval intersects any other interval inserted into the tree
     */
    public boolean intersects(int lo, int hi) {
        Node cur = root;
        while (cur != null) {
            if (cur.intersects(lo, hi)) {
                return true;
            } else if (cur.left == null || cur.left.max < lo) {
                cur = cur.right;
            } else {
                cur = cur.left;
            }
        }
        return false;
    }

    /**
     * Tries to find a node with given interval and removes it if it is found.
     * @param lo lower number in a given interval
     * @param hi higher number in a given interval
     * @return if the node was found and removed
     */
    public boolean remove(int lo, int hi) {
        Node cur = root;
        while (cur != null) {
            if (lo > cur.lo) {
                cur = cur.right;
            } else if (lo < cur.lo) {
                cur = cur.left;
            } else {
                if (cur.hi == hi) {
                    removeNode(cur);
                    return true;
                } else {
                    cur = cur.left;
                }
            }
        }
        return false;
    }

    /**
     * Removes a specified node in the tree
     * @param toDelete node to delete from the tree
     */
    private void removeNode(Node toDelete) {
        if (toDelete.left == null && toDelete.right == null) { //Node has no child
            if (toDelete == root) {
                root = null;
            } else {
                toDelete.changeParentsChildTo(null);
                propagateSmallerMaxToParents(toDelete.parent);
            }
        } else if (toDelete.left == null) { //Node has only right child
            if (toDelete == root) {
                root = toDelete.right;
            } else {
                toDelete.changeParentsChildTo(toDelete.right);
                propagateSmallerMaxToParents(toDelete.parent);
            }
        } else if (toDelete.right == null) { //Node has only left child
            if (toDelete == root) {
                root = toDelete.left;
            } else {
                toDelete.changeParentsChildTo(toDelete.left);
                propagateSmallerMaxToParents(toDelete.parent);
            }
        } else { //Node has 2 children
            Node cur = toDelete.left;
            while (cur.right != null) {
                cur = cur.right;
            }
            int deletedLo = cur.lo;
            int deletedHi = cur.hi;
            removeNode(cur);
            toDelete.lo = deletedLo;
            toDelete.hi = deletedHi;
            propagateSmallerMaxToParents(toDelete);
        }
    }

    /**
     * Updates max of a node and then propagates the change to parents if needed.
     * @param cur node of which the max should be updated
     */
    private void propagateSmallerMaxToParents(Node cur) {
        while (cur != null) {
            int overallMax = cur.hi;
            if (cur.left != null && cur.left.max > overallMax) {
                overallMax = cur.left.max;
            }
            if (cur.right != null && cur.right.max > overallMax) {
                overallMax = cur.right.max;
            }
            if (overallMax != cur.max) {
                cur.max = overallMax;
                cur = cur.parent;
            } else {
                break;
            }
        }
    }

    /**
     * Debugging method.
     * @return if the tree is structurally correct and all parent/child variable connections are correct
     */
    public boolean testTreeCorrectness() {
        if (root == null) {
            return true;
        }
        Queue<Node> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            Node curNode = q.poll();
            if (curNode.left != null & curNode.right != null) { //curNode has 2 children
                if (Collections.max(Arrays.asList(curNode.left.max,curNode.right.max,curNode.hi)) != curNode.max) {
                    return false; //incorrect max
                }
                if (!curNode.left.parent.equals(curNode)) {
                    return false; //wrong parent<->child connection for the left child
                }
                if (!curNode.right.parent.equals(curNode)) {
                    return false; //wrong parent<->child connection for the right child
                }
                q.add(curNode.right);
                q.add(curNode.left);
            } else if (curNode.left != null) { //curNode has only left child
                if (curNode.left.lo > curNode.lo || curNode.max != Math.max(curNode.left.max, curNode.hi)) {
                    return false; //incorrect max
                }
                if (!curNode.left.parent.equals(curNode)) {
                    return false; //wrong parent<->child connection
                }
                q.add(curNode.left);
            } else if (curNode.right != null) { //curNode has only right child
                if (curNode.right.lo <= curNode.lo || curNode.max != Math.max(curNode.right.max, curNode.hi)) {
                    return false; //incorrect max
                }
                if (!curNode.right.parent.equals(curNode)) {
                    return false; //wrong parent<->child connection
                }
                q.add(curNode.right);
            }
        }
        return true;
    }

    /**
     * Debugging method.
     * Prints the tree into stdout. Only viable for trees with smaller depth (5 or less)
     */
    public void printTree() {
        if (root == null) {
            System.out.println("Interval tree is empty!");
            return;
        }
        int padding = 2 << (getTreeDepth() + 2);
        ArrayList<Node> curLevelNodes = new ArrayList<>();
        ArrayList<Node> nextLevelNodes = new ArrayList<>();
        curLevelNodes.add(root);
        while (true) {
            System.out.print(getNCopiesOfString(padding/2, "."));
            for (Node node : curLevelNodes) {
                if (node != null) {
                    System.out.printf("%2d,%2d|%2d", node.lo, node.hi,node.max);
                    nextLevelNodes.add(node.left);
                    nextLevelNodes.add(node.right);
                } else {
                    System.out.print("  null  ");
                    nextLevelNodes.add(null);
                    nextLevelNodes.add(null);
                }
                System.out.print(getNCopiesOfString(padding - 8, " "));
            }
            System.out.print("\n\n");

            curLevelNodes = nextLevelNodes;
            nextLevelNodes = new ArrayList<>();
            boolean whileBreak = true;
            for (Node node : curLevelNodes) {
                if (node != null) {
                    whileBreak = false;
                    break;
                }
            }
            if (whileBreak) {
                break;
            }

            padding /= 2;
        }

    }

    /**
     * Debugging method.
     * @return depth of the interval tree
     */
    public int getTreeDepth() {
        if (root == null) {
            return 0;
        }
        int ret = 0;
        ArrayList<Node> curLevelNodes = new ArrayList<>();
        ArrayList<Node> nextLevelNodes = new ArrayList<>();
        curLevelNodes.add(root);
        while (!curLevelNodes.isEmpty()) {
            for (Node node : curLevelNodes) {
                if (node.left != null) {
                    nextLevelNodes.add(node.left);
                }
                if (node.right != null) {
                    nextLevelNodes.add(node.right);
                }
            }
            curLevelNodes = nextLevelNodes;
            nextLevelNodes = new ArrayList<>();
            ret++;
        }
        return ret;
    }

    /**
     * Debugging method. Used by printTree() method.
     * @param n number of copies
     * @param s string to be copied
     * @return string containing n copies of s
     */
    private String getNCopiesOfString(int n, String s) {
        if (n <= 0) {
            return "";
        }
        return new String(new char[n]).replace("\0", s);
    }

}
