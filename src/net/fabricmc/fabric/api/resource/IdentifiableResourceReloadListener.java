package net.fabricmc.fabric.api.resource;

import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.util.ResourceLocation;

public interface IdentifiableResourceReloadListener extends IFutureReloadListener {
    ResourceLocation getFabricId();
}
