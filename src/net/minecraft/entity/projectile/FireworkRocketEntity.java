package net.minecraft.entity.projectile;

import java.util.OptionalInt;
import javax.annotation.Nullable;


import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IRendersAsItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xd.harm.Harmony;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.impl.movement.ElytraBooster;

public class FireworkRocketEntity extends ProjectileEntity implements IRendersAsItem
{
    private static final DataParameter<ItemStack> FIREWORK_ITEM = EntityDataManager.createKey(FireworkRocketEntity.class, DataSerializers.ITEMSTACK);
    private static final DataParameter<OptionalInt> BOOSTED_ENTITY_ID = EntityDataManager.createKey(FireworkRocketEntity.class, DataSerializers.OPTIONAL_VARINT);
    private static final DataParameter<Boolean> field_213895_d = EntityDataManager.createKey(FireworkRocketEntity.class, DataSerializers.BOOLEAN);
    private int fireworkAge;
    private int lifetime;
    private LivingEntity boostedEntity;

    public FireworkRocketEntity(EntityType <? extends FireworkRocketEntity > p_i50164_1_, World p_i50164_2_)
    {
        super(p_i50164_1_, p_i50164_2_);
    }

    public FireworkRocketEntity(World worldIn, double x, double y, double z, ItemStack givenItem)
    {
        super(EntityType.FIREWORK_ROCKET, worldIn);
        this.fireworkAge = 0;
        this.setPosition(x, y, z);
        int i = 1;

        if (!givenItem.isEmpty() && givenItem.hasTag())
        {
            this.dataManager.set(FIREWORK_ITEM, givenItem.copy());
            i += givenItem.getOrCreateChildTag("Fireworks").getByte("Flight");
        }

        this.setMotion(this.rand.nextGaussian() * 0.001D, 0.05D, this.rand.nextGaussian() * 0.001D);
        this.lifetime = 10 * i + this.rand.nextInt(6) + this.rand.nextInt(7);
    }

    public FireworkRocketEntity(World p_i231581_1_, @Nullable Entity p_i231581_2_, double p_i231581_3_, double p_i231581_5_, double p_i231581_7_, ItemStack p_i231581_9_)
    {
        this(p_i231581_1_, p_i231581_3_, p_i231581_5_, p_i231581_7_, p_i231581_9_);
        this.setShooter(p_i231581_2_);
    }

    public FireworkRocketEntity(World p_i47367_1_, ItemStack p_i47367_2_, LivingEntity p_i47367_3_)
    {
        this(p_i47367_1_, p_i47367_3_, p_i47367_3_.getPosX(), p_i47367_3_.getPosY(), p_i47367_3_.getPosZ(), p_i47367_2_);
        this.dataManager.set(BOOSTED_ENTITY_ID, OptionalInt.of(p_i47367_3_.getEntityId()));
        this.boostedEntity = p_i47367_3_;
    }

    public FireworkRocketEntity(World p_i50165_1_, ItemStack p_i50165_2_, double p_i50165_3_, double p_i50165_5_, double p_i50165_7_, boolean p_i50165_9_)
    {
        this(p_i50165_1_, p_i50165_3_, p_i50165_5_, p_i50165_7_, p_i50165_2_);
        this.dataManager.set(field_213895_d, p_i50165_9_);
    }

    public FireworkRocketEntity(World p_i231582_1_, ItemStack p_i231582_2_, Entity p_i231582_3_, double p_i231582_4_, double p_i231582_6_, double p_i231582_8_, boolean p_i231582_10_)
    {
        this(p_i231582_1_, p_i231582_2_, p_i231582_4_, p_i231582_6_, p_i231582_8_, p_i231582_10_);
        this.setShooter(p_i231582_3_);
    }

    protected void registerData()
    {
        this.dataManager.register(FIREWORK_ITEM, ItemStack.EMPTY);
        this.dataManager.register(BOOSTED_ENTITY_ID, OptionalInt.empty());
        this.dataManager.register(field_213895_d, false);
    }

    /**
     * Checks if the entity is in range to render.
     */
    public boolean isInRangeToRenderDist(double distance)
    {
        return distance < 4096.0D && !this.isAttachedToEntity();
    }

    public boolean isInRangeToRender3d(double x, double y, double z)
    {
        return super.isInRangeToRender3d(x, y, z) && !this.isAttachedToEntity();
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void tick()
    {
        super.tick();

        if (this.isAttachedToEntity())
        {
            if (this.boostedEntity == null)
            {
                this.dataManager.get(BOOSTED_ENTITY_ID).ifPresent((p_213891_1_) ->
                {
                    Entity entity = this.world.getEntityByID(p_213891_1_);

                    if (entity instanceof LivingEntity)
                    {
                        this.boostedEntity = (LivingEntity)entity;
                    }
                });
            }

            if (this.boostedEntity != null) {
                if (this.boostedEntity.isElytraFlying()) {
                    HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
                    ElytraBooster elytraBooster = Harmony.getInstance().getModuleManager().getElytraBooster();
                    SpeedData speedData = getElytraSpeedData(elytraBooster, this.boostedEntity.rotationPitch, this.boostedEntity.rotationYaw);
                    boolean auraCorrection = hitAura != null
                            && hitAura.isState()
                            && hitAura.getOptions().getValueByName("Коррекция движения").get()
                            && boostedEntity instanceof ClientPlayerEntity;
                    Vector3d vector3d = auraCorrection
                            ? this.getVectorForRotation(hitAura.rotateVector.y, hitAura.rotateVector.x)
                            : this.boostedEntity.getLookVec();
                    Vector3d vector3d1 = this.boostedEntity.getMotion();
                    double verticalLerp = auraCorrection ? speedData.speedY : 0.5D;
                    this.boostedEntity.setMotion(vector3d1.add(
                            vector3d.x * 0.1D + (vector3d.x * speedData.speedXZ - vector3d1.x) * 0.5D,
                            vector3d.y * 0.1D + (vector3d.y * speedData.speedY - vector3d1.y) * verticalLerp,
                            vector3d.z * 0.1D + (vector3d.z * speedData.speedXZ - vector3d1.z) * 0.5D
                    ));
                }

                this.setPosition(this.boostedEntity.getPosX(), this.boostedEntity.getPosY(), this.boostedEntity.getPosZ());
                this.setMotion(this.boostedEntity.getMotion());
            }
        }
        else
        {
            if (!this.func_213889_i())
            {
                double d2 = this.collidedHorizontally ? 1.0D : 1.15D;
                this.setMotion(this.getMotion().mul(d2, 1.0D, d2).add(0.0D, 0.04D, 0.0D));
            }

            Vector3d vector3d2 = this.getMotion();
            this.move(MoverType.SELF, vector3d2);
            this.setMotion(vector3d2);
        }

        RayTraceResult raytraceresult = ProjectileHelper.func_234618_a_(this, this::func_230298_a_);

        if (!this.noClip)
        {
            this.onImpact(raytraceresult);
            this.isAirBorne = true;
        }

        this.func_234617_x_();

        if (this.fireworkAge == 0 && !this.isSilent())
        {
            this.world.playSound((PlayerEntity)null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        ++this.fireworkAge;

        if (this.world.isRemote && this.fireworkAge % 2 < 2)
        {
            this.world.addParticle(ParticleTypes.FIREWORK, this.getPosX(), this.getPosY() - 0.3D, this.getPosZ(), this.rand.nextGaussian() * 0.05D, -this.getMotion().y * 0.5D, this.rand.nextGaussian() * 0.05D);
        }

        if (!this.world.isRemote && this.fireworkAge > this.lifetime)
        {
            this.func_213893_k();
        }
    }

    private SpeedData getElytraSpeedData(ElytraBooster booster, float pitch, float yaw) {
        float speedXZ = 1.5f;
        float speedY = 1.5f;
        if (booster == null || !booster.isState()) {
            return new SpeedData(speedXZ, speedY);
        }
        float normalizedYaw = normalizeAngle(yaw);
        float normalizedPitch = normalizeAngle(pitch);
        if (booster.mode.is("Кастомный")) {
            if (booster.maxspeed.get()) {
                speedXZ = getSpeedCustom(booster, normalizedYaw, normalizedPitch);
                speedY = getSpeedCustomY(booster, normalizedPitch);
            } else {
                speedXZ = booster.speedxz.get();
                speedY = booster.speedy.get();
            }
        } else if (booster.mode.is("РиллиВорлд")) {
            speedXZ = getSpeedForPitchRange(normalizedPitch, normalizedYaw);
            speedY = getSpeedYForPitchRange(normalizedPitch);
        } else {
            speedXZ = getSpeedForPitch(normalizedPitch, normalizedYaw);
            speedY = getSpeedYForPitch(normalizedPitch);
        }
        return new SpeedData(speedXZ, speedY);
    }

    private float normalizeAngle(float angle) {
        float normalized = angle % 180.0f;
        if (normalized > 90.0f) {
            normalized -= 180.0f;
        } else if (normalized < -90.0f) {
            normalized += 180.0f;
        }
        return normalized;
    }

    private float getSpeedForPitch(float pitch, float yaw) {
        float absPitch = Math.abs(pitch);
        float absYaw = Math.abs(yaw);
        float speed;
        if (absPitch >= 38 && absPitch <= 52) speed = 2f;
        else if (absPitch >= 32 && absPitch <= 58) speed = 1.96f;
        else if (absPitch >= 28 && absPitch <= 62) speed = 1.95f;
        else if ((absYaw >= 29 && absYaw <= 61) || (absPitch >= 29 && absPitch <= 61)) speed = 1.963f;
        else if ((absYaw >= 28 && absYaw <= 60) || (absPitch >= 28 && absPitch <= 60)) speed = 1.954f;
        else if ((absYaw >= 26 && absYaw <= 64) || (absPitch >= 26 && absPitch <= 64)) speed = 1.874f;
        else if ((absYaw >= 24 && absYaw <= 66) || (absPitch >= 24 && absPitch <= 66)) speed = 1.77f;
        else if ((absYaw >= 15 && absYaw <= 75) || (absPitch >= 15 && absPitch <= 75)) speed = 1.70f;
        else if ((absYaw >= 13 && absYaw <= 77) || (absPitch >= 13 && absPitch <= 77)) speed = 1.68f;
        else if ((absYaw >= 12 && absYaw <= 78) || (absPitch >= 12 && absPitch <= 78)) speed = 1.68f;
        else if ((absYaw >= 8 && absYaw <= 82) || (absPitch >= 11 && absPitch <= 79)) speed = 1.66f;
        else if ((absYaw >= 5 && absYaw <= 85) || (absPitch >= 8 && absPitch <= 82)) speed = 1.635f;
        else if (absYaw <= 90 || absPitch <= 90) speed = 1.622f;
        else speed = 1.621f;
        return pitch > 15 ? speed - 0.068f : speed;
    }

    private float getSpeedYForPitch(float pitch) {
        if (Math.abs(pitch) >= 37 && Math.abs(pitch) <= 38) return 2.03f;
        if (Math.abs(pitch) >= 25 && Math.abs(pitch) <= 30) return 2f;
        if (Math.abs(pitch) >= 35 && Math.abs(pitch) <= 45) return 1.99f;
        if (Math.abs(pitch) >= 40 && Math.abs(pitch) <= 50) return 1.97f;
        if (Math.abs(pitch) >= 50 && Math.abs(pitch) <= 60) return 1.96f;
        if (Math.abs(pitch) >= 51 && Math.abs(pitch) <= 61) return 1.87f;
        if (Math.abs(pitch) >= 52 && Math.abs(pitch) <= 65) return 1.70f;
        return 1.59f;
    }

    private float getSpeedForPitchRange(float pitch, float yaw) {
        if (Math.abs(pitch) >= 30 && Math.abs(pitch) <= 40) return 1.85f;
        if (Math.abs(pitch) >= 35 && Math.abs(pitch) <= 45) return 1.8f;
        if (Math.abs(pitch) >= 45 && Math.abs(pitch) <= 55) return 1.76f;
        if ((Math.abs(yaw) >= 30 && Math.abs(yaw) <= 60) || (Math.abs(pitch) >= 30 && Math.abs(pitch) <= 60)) return 1.74f;
        if ((Math.abs(yaw) >= 28 && Math.abs(yaw) <= 62) || (Math.abs(pitch) >= 28 && Math.abs(pitch) <= 62)) return 1.71f;
        if ((Math.abs(yaw) >= 26 && Math.abs(yaw) <= 64) || (Math.abs(pitch) >= 26 && Math.abs(pitch) <= 64)) return 1.7f;
        if ((Math.abs(yaw) >= 24 && Math.abs(yaw) <= 66) || (Math.abs(pitch) >= 24 && Math.abs(pitch) <= 66)) return 1.69f;
        if ((Math.abs(yaw) >= 22 && Math.abs(yaw) <= 68) || (Math.abs(pitch) >= 22 && Math.abs(pitch) <= 68)) return 1.68f;
        if ((Math.abs(yaw) >= 20 && Math.abs(yaw) <= 70) || (Math.abs(pitch) >= 20 && Math.abs(pitch) <= 70)) return 1.67f;
        if ((Math.abs(yaw) >= 18 && Math.abs(yaw) <= 72) || (Math.abs(pitch) >= 18 && Math.abs(pitch) <= 72)) return 1.66f;
        if ((Math.abs(yaw) >= 16 && Math.abs(yaw) <= 74) || (Math.abs(pitch) >= 16 && Math.abs(pitch) <= 74)) return 1.65f;
        if ((Math.abs(yaw) >= 12 && Math.abs(yaw) <= 78) || (Math.abs(pitch) >= 12 && Math.abs(pitch) <= 78)) return 1.64f;
        if ((Math.abs(yaw) >= 10 && Math.abs(yaw) <= 80) || (Math.abs(pitch) >= 10 && Math.abs(pitch) <= 80)) return 1.63f;
        if ((Math.abs(yaw) >= 8 && Math.abs(yaw) <= 82) || (Math.abs(pitch) >= 8 && Math.abs(pitch) <= 82)) return 1.62f;
        return 1.61f;
    }

    private float getSpeedYForPitchRange(float pitch) {
        if (Math.abs(pitch) >= 30 && Math.abs(pitch) <= 40) return 1.69f;
        if (Math.abs(pitch) >= 35 && Math.abs(pitch) <= 45) return 1.68f;
        if (Math.abs(pitch) >= 45 && Math.abs(pitch) <= 55) return 1.65f;
        return 1.59f;
    }

    private float getSpeedCustom(ElytraBooster booster, float yaw, float pitch) {
        if ((Math.abs(yaw) >= 40 && Math.abs(yaw) <= 50) || (Math.abs(pitch) >= 40 && Math.abs(pitch) <= 50)) return booster.speed40.get();
        if ((Math.abs(yaw) >= 35 && Math.abs(yaw) <= 55) || (Math.abs(pitch) >= 35 && Math.abs(pitch) <= 55)) return booster.speed35.get();
        if ((Math.abs(yaw) >= 30 && Math.abs(yaw) <= 60) || (Math.abs(pitch) >= 30 && Math.abs(pitch) <= 60)) return booster.speed30.get();
        if ((Math.abs(yaw) >= 25 && Math.abs(yaw) <= 65) || (Math.abs(pitch) >= 25 && Math.abs(pitch) <= 65)) return booster.speed25.get();
        if ((Math.abs(yaw) >= 20 && Math.abs(yaw) <= 70) || (Math.abs(pitch) >= 20 && Math.abs(pitch) <= 70)) return booster.speed20.get();
        if ((Math.abs(yaw) >= 15 && Math.abs(yaw) <= 75) || (Math.abs(pitch) >= 15 && Math.abs(pitch) <= 75)) return booster.speed15.get();
        if ((Math.abs(yaw) >= 10 && Math.abs(yaw) <= 80) || (Math.abs(pitch) >= 10 && Math.abs(pitch) <= 80)) return booster.speed10.get();
        return booster.speed5.get();
    }

    private float getSpeedCustomY(ElytraBooster booster, float pitch) {
        if (Math.abs(pitch) >= 40 && Math.abs(pitch) <= 50) return booster.speed40y.get();
        if (Math.abs(pitch) >= 35 && Math.abs(pitch) <= 55) return booster.speed35y.get();
        if (Math.abs(pitch) >= 30 && Math.abs(pitch) <= 60) return booster.speed30y.get();
        if (Math.abs(pitch) >= 25 && Math.abs(pitch) <= 65) return booster.speed25y.get();
        if (Math.abs(pitch) >= 20 && Math.abs(pitch) <= 70) return booster.speed20y.get();
        if (Math.abs(pitch) >= 15 && Math.abs(pitch) <= 75) return booster.speed15y.get();
        if (Math.abs(pitch) >= 10 && Math.abs(pitch) <= 80) return booster.speed10y.get();
        return booster.speed5y.get();
    }

    private static class SpeedData {
        private final float speedXZ;
        private final float speedY;

        private SpeedData(float speedXZ, float speedY) {
            this.speedXZ = speedXZ;
            this.speedY = speedY;
        }
    }

    private void func_213893_k()
    {
        this.world.setEntityState(this, (byte)17);
        this.dealExplosionDamage();
        this.remove();
    }

    /**
     * Called when the arrow hits an entity
     */
    protected void onEntityHit(EntityRayTraceResult p_213868_1_)
    {
        super.onEntityHit(p_213868_1_);

        if (!this.world.isRemote)
        {
            this.func_213893_k();
        }
    }

    protected void func_230299_a_(BlockRayTraceResult p_230299_1_)
    {
        BlockPos blockpos = new BlockPos(p_230299_1_.getPos());
        this.world.getBlockState(blockpos).onEntityCollision(this.world, blockpos, this);

        if (!this.world.isRemote() && this.func_213894_l())
        {
            this.func_213893_k();
        }

        super.func_230299_a_(p_230299_1_);
    }

    private boolean func_213894_l()
    {
        ItemStack itemstack = this.dataManager.get(FIREWORK_ITEM);
        CompoundNBT compoundnbt = itemstack.isEmpty() ? null : itemstack.getChildTag("Fireworks");
        ListNBT listnbt = compoundnbt != null ? compoundnbt.getList("Explosions", 10) : null;
        return listnbt != null && !listnbt.isEmpty();
    }

    private void dealExplosionDamage()
    {
        float f = 0.0F;
        ItemStack itemstack = this.dataManager.get(FIREWORK_ITEM);
        CompoundNBT compoundnbt = itemstack.isEmpty() ? null : itemstack.getChildTag("Fireworks");
        ListNBT listnbt = compoundnbt != null ? compoundnbt.getList("Explosions", 10) : null;

        if (listnbt != null && !listnbt.isEmpty())
        {
            f = 5.0F + (float)(listnbt.size() * 2);
        }

        if (f > 0.0F)
        {
            if (this.boostedEntity != null)
            {
                this.boostedEntity.attackEntityFrom(DamageSource.func_233548_a_(this, this.func_234616_v_()), 5.0F + (float)(listnbt.size() * 2));
            }

            double d0 = 5.0D;
            Vector3d vector3d = this.getPositionVec();

            for (LivingEntity livingentity : this.world.getEntitiesWithinAABB(LivingEntity.class, this.getBoundingBox().grow(5.0D)))
            {
                if (livingentity != this.boostedEntity && !(this.getDistanceSq(livingentity) > 25.0D))
                {
                    boolean flag = false;

                    for (int i = 0; i < 2; ++i)
                    {
                        Vector3d vector3d1 = new Vector3d(livingentity.getPosX(), livingentity.getPosYHeight(0.5D * (double)i), livingentity.getPosZ());
                        RayTraceResult raytraceresult = this.world.rayTraceBlocks(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));

                        if (raytraceresult.getType() == RayTraceResult.Type.MISS)
                        {
                            flag = true;
                            break;
                        }
                    }

                    if (flag)
                    {
                        float f1 = f * (float)Math.sqrt((5.0D - (double)this.getDistance(livingentity)) / 5.0D);
                        livingentity.attackEntityFrom(DamageSource.func_233548_a_(this, this.func_234616_v_()), f1);
                    }
                }
            }
        }
    }

    private boolean isAttachedToEntity()
    {
        return this.dataManager.get(BOOSTED_ENTITY_ID).isPresent();
    }

    public boolean func_213889_i()
    {
        return this.dataManager.get(field_213895_d);
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    public void handleStatusUpdate(byte id)
    {
        if (id == 17 && this.world.isRemote)
        {
            if (!this.func_213894_l())
            {
                for (int i = 0; i < this.rand.nextInt(3) + 2; ++i)
                {
                    this.world.addParticle(ParticleTypes.POOF, this.getPosX(), this.getPosY(), this.getPosZ(), this.rand.nextGaussian() * 0.05D, 0.005D, this.rand.nextGaussian() * 0.05D);
                }
            }
            else
            {
                ItemStack itemstack = this.dataManager.get(FIREWORK_ITEM);
                CompoundNBT compoundnbt = itemstack.isEmpty() ? null : itemstack.getChildTag("Fireworks");
                Vector3d vector3d = this.getMotion();
                this.world.makeFireworks(this.getPosX(), this.getPosY(), this.getPosZ(), vector3d.x, vector3d.y, vector3d.z, compoundnbt);
            }
        }

        super.handleStatusUpdate(id);
    }

    public void writeAdditional(CompoundNBT compound)
    {
        super.writeAdditional(compound);
        compound.putInt("Life", this.fireworkAge);
        compound.putInt("LifeTime", this.lifetime);
        ItemStack itemstack = this.dataManager.get(FIREWORK_ITEM);

        if (!itemstack.isEmpty())
        {
            compound.put("FireworksItem", itemstack.write(new CompoundNBT()));
        }

        compound.putBoolean("ShotAtAngle", this.dataManager.get(field_213895_d));
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditional(CompoundNBT compound)
    {
        super.readAdditional(compound);
        this.fireworkAge = compound.getInt("Life");
        this.lifetime = compound.getInt("LifeTime");
        ItemStack itemstack = ItemStack.read(compound.getCompound("FireworksItem"));

        if (!itemstack.isEmpty())
        {
            this.dataManager.set(FIREWORK_ITEM, itemstack);
        }

        if (compound.contains("ShotAtAngle"))
        {
            this.dataManager.set(field_213895_d, compound.getBoolean("ShotAtAngle"));
        }
    }

    public ItemStack getItem()
    {
        ItemStack itemstack = this.dataManager.get(FIREWORK_ITEM);
        return itemstack.isEmpty() ? new ItemStack(Items.FIREWORK_ROCKET) : itemstack;
    }

    /**
     * Returns true if it's possible to attack this entity with an item.
     */
    public boolean canBeAttackedWithItem()
    {
        return false;
    }

    public IPacket<?> createSpawnPacket()
    {
        return new SSpawnObjectPacket(this);
    }
}
