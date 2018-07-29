package com.github.tankist88.carpenter.generator.dto;

import java.util.*;

public class Node<T> {
    private T value;
    private Node<T> parent;
    private List<Node<T>> childs;

    private Comparator<T> comparator;

    public Node() {
        this(null, null);
    }

    public Node(T value, Node parent) {
        this(value, parent, null);
    }

    public Node(T value, Node parent, Comparator<T> comparator) {
        this.value = value;
        this.parent = parent;
        this.comparator = comparator;
    }

    public Node<T> getParent() {
        return parent;
    }

    public List<Node<T>> getChilds() {
        if(childs == null) {
            childs = new ArrayList<>();
        }
        return childs;
    }

    public Node<T> addChild(T value) {
        return addChild(value, null);
    }

    public Node<T> addChild(T value, Comparator<T> customComparator) {
        Node<T> node = new Node<>(value, this, customComparator);
        getChilds().add(node);
        Collections.sort(getChilds(), getComparator());
        return node;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Comparator<Node<T>> getComparator() {
        if (comparator != null) {
            return new Comparator<Node<T>>() {
                @Override
                public int compare(Node<T> o1, Node<T> o2) {
                    return comparator.compare(o1.value, o2.value);
                }
            };
        } else {
            return new Comparator<Node<T>>() {
                @Override
                public int compare(Node<T> o1, Node<T> o2) {
                    Comparable c1 = (Comparable) o1.value;
                    Comparable c2 = (Comparable) o2.value;
                    return c1.compareTo(c2);
                }
            };
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node<?> node = (Node<?>) o;
        return Objects.equals(value, node.value) &&
                Objects.equals(parent, node.parent);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value, parent);
    }
}
