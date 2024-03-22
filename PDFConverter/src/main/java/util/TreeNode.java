package util;

public class TreeNode<T> {
    private T data;
    private TreeNode<T> parent;
    private TreeNode<T>[] children;
    private boolean isTitle;
    private int fileIndex;

    public TreeNode() {
        this.data = null;
        this.parent = null;
        this.children = null;
    }

    public TreeNode(T data, boolean isTitle, int fileIndex) {
        this.setData(data);
        this.isTitle = isTitle;
        this.fileIndex = fileIndex;
    }

    public TreeNode(TreeNode<T> parent, T data, boolean isTitle, int fileIndex) {
        this.setParent(parent);
        if (this.getParent() != null)
            this.getParent().addChild(this);
        this.setData(data);
        this.isTitle = isTitle;
        this.fileIndex = isTitle ? -1 : fileIndex;
    }

    public void insert(TreeNode<T> child) {
        child.setParent(this);
        if (this.children != null) {
            for (TreeNode<T> node : this.children) {
                child.addChild(node);
            }
            this.removeAllChildren();
        }
        this.addChild(child);
    }

    public void insert(TreeNode<T> child, int index) {
        TreeNode<T> existingChild = findNodeWithData(child.getData());
        if (existingChild != null) {
            // Merge the children of the new child into the existing child
            mergeChildren(existingChild, child);
        } else {
            // If the child does not exist, insert it at the specified index
            child.setParent(this);
            this.addChild(child, index);
        }
    }

    private TreeNode<T> findNodeWithData(T data) {
        for (TreeNode<T> child : this.children) {
            if (child.getData().equals(data)) {
                return child;
            }
        }
        return null;
    }

    private void mergeChildren(TreeNode<T> existingChild, TreeNode<T> newChild) {
        for (TreeNode<T> newGrandChild : newChild.getChildren()) {
            TreeNode<T> existingGrandChild = existingChild.findNodeWithData(newGrandChild.getData());

            if (existingGrandChild != null) {
                // If a matching grandchild is found, merge its children
                for (TreeNode<T> greatGrandChild : newGrandChild.getChildren()) {
                    existingGrandChild.addChild(greatGrandChild);
                    greatGrandChild.setParent(existingGrandChild);
                }
            } else {
                // If no matching grandchild is found, add the new grandchild
                existingChild.addChild(newGrandChild);
                newGrandChild.setParent(existingChild);
            }
        }
    }



    public void addChild(TreeNode<T> child, int index) {
        if (this.children == null) {
            this.children = new TreeNode[1];
            this.children[0] = child;
        } else {
            TreeNode<T>[] temp = this.children;
            this.children = new TreeNode[temp.length + 1];
            for (int i = 0; i < index; i++) {
                this.children[i] = temp[i];
            }
            this.children[index] = child;
            for (int i = index + 1; i < this.children.length; i++) {
                this.children[i] = temp[i - 1];
            }
        }
    }
    public void addChild(TreeNode<T> child) {
        if (this.equals(null)) return;
        child.setParent(this);
        if (this.children == null) {
            this.children = new TreeNode[1];
            this.children[0] = child;
        } else {
            TreeNode<T>[] temp = this.children;
            this.children = new TreeNode[temp.length + 1];
            System.arraycopy(temp, 0, this.children, 0, temp.length);
            this.children[temp.length] = child;
        }
    }

    public void removeAllChildren() {
        this.children = null;
    }

    public void removeLastChild() {
        if (this.children != null) {
            TreeNode<T>[] temp = this.children;
            this.children = new TreeNode[temp.length - 1];
            System.arraycopy(temp, 0, this.children, 0, temp.length - 1);
        }
    }

    public TreeNode<T> popLastChild() {
        if (this.children != null) {
            TreeNode<T> temp = this.children[this.children.length - 1];
            TreeNode<T>[] newChildren = new TreeNode[this.children.length - 1];
            System.arraycopy(this.children, 0, newChildren, 0, this.children.length - 1);
            this.children = newChildren;
            return temp;
        }
        return null;
    }


    public boolean containsData(T data) {
        if (this.data.equals(data)) {
            return true;
        } else if (this.children != null) {
            for (TreeNode<T> node : this.children) {
                if (node.containsData(data)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsTitle(T data) {
        if (this.equals(null)) return false;
        if (this.isTitle && this.data.equals(data)) {
            return true;
        } else if (this.children != null) {
            for (TreeNode<T> node : this.children) {
                if (node.containsTitle(data)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TreeNode<T> getNode(T data) {
        if (this.data != null && this.data.equals(data)) {
            return this;
        } else if (this.children != null) {
            for (TreeNode<T> node : this.children) {
                TreeNode<T> temp = node.getNode(data);
                if (temp != null) {
                    return temp;
                }
            }
        }
        return null;
    }
    public int getIndex(T data) {
        if (this.data != null && this.data.equals(data)) {
            return 0;
        } else if (this.children != null) {
            for (int i = 0; i < this.children.length; i++) {
                if (this.children[i].getData() != null && this.children[i].getData().equals(data)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public TreeNode<T> getParent() {
        return this.parent;
    }
    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getFileIndex() {
        return this.fileIndex;
    }

    public boolean isTitle() {
        return this.isTitle;
    }

    public TreeNode<T>[] getChildren() {
        return this.children;
    }

}
