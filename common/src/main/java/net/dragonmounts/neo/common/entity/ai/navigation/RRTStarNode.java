// File: RRTStarNode.java
package net.dragonmounts.neo.common.entity.ai.navigation;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class RRTStarNode {
    public final Vec3 pos;
    public RRTStarNode parent;
    public double cost; // cost from root to this node
    public final List<RRTStarNode> children = new ArrayList<>();

    public RRTStarNode(Vec3 pos) {
        this.pos = pos;
        this.parent = null;
        this.cost = 0.0;
    }

    public void addChild(RRTStarNode child) {
        children.add(child);
        child.parent = this;
    }
}
