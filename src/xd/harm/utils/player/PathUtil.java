package xd.harm.utils.player;

import net.minecraft.block.AirBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    public Vector3d start;
    public Vector3d end;
    public List<Vector3d> path = new ArrayList<>();

    public PathUtil(Vector3d from, Vector3d to) {
        this.start = from;
        this.end = to;
    }

    public Vector3d getStart() {
        return start;
    }

    public Vector3d getEnd() {
        return end;
    }

    public List<Vector3d> getPath() {
        return path;
    }

    public void calculatePath(float step) {
        float totalDistance = (float) start.distanceTo(end);

        for (float i = 0; i <= totalDistance; i += step) {
            float x = (float) (start.x + i * (end.x - start.x) / totalDistance);
            float z = (float) (start.z + i * (end.z - start.z) / totalDistance);

            float t = i / totalDistance;
            float currentY = (float) (start.y * (1 - t) + end.y * t);

            while (!(Minecraft.getInstance().world.getBlockState(new BlockPos(x, currentY, z)).getBlock() instanceof AirBlock) && !(Minecraft.getInstance().world.getBlockState(new BlockPos(x, currentY, z)).getBlock() instanceof FlowingFluidBlock)) {
                currentY += 1;
            }

            path.add(new Vector3d(x, currentY, z));
        }
    }

}
