package com.dozuki.ifixit.model.topic;

import java.io.Serializable;
import java.util.ArrayList;

public class TopicNode implements Serializable {
   private static final long serialVersionUID = 1L;
   protected static final String ROOT_NAME = "ROOT";

   private String mName;
   private ArrayList<TopicNode> mChildren;

   public TopicNode() {
      this(ROOT_NAME);
   }

   public TopicNode(String name) {
      mName = name;
   }

   public String getName() {
      return mName;
   }

   public ArrayList<TopicNode> getChildren() {
      return mChildren;
   }

   public void setChildren(ArrayList<TopicNode> children) {
      mChildren = children;
   }

   public boolean isLeaf() {
      return mChildren == null;
   }

   public boolean isRoot() {
      return mName.equals(ROOT_NAME);
   }

   public boolean equals(Object other) {
      return other instanceof TopicNode &&
       ((TopicNode)other).getName().equals(mName);
   }

   public static boolean isRootName(String name) {
      return ROOT_NAME.equals(name);
   }
}
