package com.github.tankist88.carpenter.generator.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PackageTree {
    private static final String DELIMETER = "\\.";

    private Node<String> root;
    private Comparator<String> packageComparator;

    public PackageTree() {
        this(null);
    }

    public PackageTree(Comparator<String> packageComparator) {
        this.packageComparator = packageComparator;
        this.root = new Node<>(null, null, packageComparator);
    }

    public void addPackage(String element) {
        String[] packages = element.split(DELIMETER);
        Node<String> prevNode = root;
        for (String p : packages) {
            Node<String> node = searchNode(p, prevNode);
            prevNode = node == null ? addChild(prevNode, p) : node;
        }
    }

    private Node<String> addChild(Node<String> node, String value) {
        return node.addChild(value, packageComparator);
    }

    private Node<String> searchNode(String value, Node<String> node) {
        if (value.equals(node.getValue())) return node;
        if (node.getChilds().size() == 0) return null;
        for (Node<String> c : node.getChilds()) {
            if (value.equals(c.getValue())) return c;
        }
        return null;
    }

    private Node<String> searchNodeRecursive(String value, Node<String> node) {
        if (value.equals(node.getValue())) return node;
        if (node.getChilds().size() == 0) return null;
        for (Node<String> c : node.getChilds()) {
            Node<String> res = searchNodeRecursive(value, c);
            if (res != null) return res;
        }
        return null;
    }

    private List<Node<String>> getNodesWithoutChilds(Node<String> node) {
        List<Node<String>> result = new ArrayList<>();
        for (Node<String> n : node.getChilds()) {
            if (n.getChilds().size() == 0) {
                result.add(n);
            } else {
                result.addAll(getNodesWithoutChilds(n));
            }
        }
        return result;
    }

    private String getPackage(Node<String> node) {
        StringBuilder sb = new StringBuilder();
        if (node.getValue() != null) {
            sb.insert(0, node.getValue());
        }
        if (node.getParent() != null && node.getParent().getValue() != null) {
            sb.insert(0, ".");
            sb.insert(0, getPackage(node.getParent()));
        }
        return sb.toString();
    }

    public List<String> toList() {
        List<String> res = new ArrayList<>();
        for (Node<String> node : getNodesWithoutChilds(root)) {
            res.add(getPackage(node));
        }
        return res;
    }

    public List<String> cutOne() {
        List<String> res = new ArrayList<>();
        for (Node<String> node : getNodesWithoutChilds(root)) {
            int familySize = node.getParent().getChilds().size();
            String packageStr = familySize > 1 ? getPackage(node.getParent()) + ".*;" : getPackage(node);
            if (!res.contains(packageStr)) res.add(packageStr);
        }
        return res;
    }
}
