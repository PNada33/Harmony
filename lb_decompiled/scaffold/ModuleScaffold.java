/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.IndexedValue
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.ContinuationInterceptor
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.CoroutineContext$Key
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.Boxing
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.JvmField
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.functions.Function2
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.MutablePropertyReference1
 *  kotlin.jvm.internal.MutablePropertyReference1Impl
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Ref$BooleanRef
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.Mode
 *  net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.CoroutineTickerKt
 *  net.ccbluex.liquidbounce.event.Event
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.EventManager
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$DiscardLatest
 *  net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
 *  net.ccbluex.liquidbounce.event.events.WorldChangeEvent
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.ClientModule
 *  net.ccbluex.liquidbounce.features.module.ModuleCategories
 *  net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
 *  net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedGeometry
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedLineSegment
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedPoint
 *  net.ccbluex.liquidbounce.render.engine.type.Color4b
 *  net.ccbluex.liquidbounce.utils.aiming.RotationManager
 *  net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtilKt
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.block.SwingMode
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.clicking.Clicker
 *  net.ccbluex.liquidbounce.utils.client.SilentHotbar
 *  net.ccbluex.liquidbounce.utils.client.Timer
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.item.ItemExtensionsKt
 *  net.ccbluex.liquidbounce.utils.item.PreferAverageHardBlocks
 *  net.ccbluex.liquidbounce.utils.item.PreferFavourableBlocks
 *  net.ccbluex.liquidbounce.utils.item.PreferFullCubeBlocks
 *  net.ccbluex.liquidbounce.utils.item.PreferSolidBlocks
 *  net.ccbluex.liquidbounce.utils.item.PreferStackSize
 *  net.ccbluex.liquidbounce.utils.item.PreferWalkableBlocks
 *  net.ccbluex.liquidbounce.utils.kotlin.Priority
 *  net.ccbluex.liquidbounce.utils.math.ShapeExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
 *  net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.IndexedValue;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.ContinuationInterceptor;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.JvmField;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.MutablePropertyReference1;
import kotlin.jvm.internal.MutablePropertyReference1Impl;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.Mode;
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.CoroutineTickerKt;
import net.ccbluex.liquidbounce.event.Event;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent;
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.ClientModule;
import net.ccbluex.liquidbounce.features.module.ModuleCategories;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldMovementPlanner;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAccelerationFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoBlockFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldBlinkFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldJumpStrafe;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeFeatureKt;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSpeedLimiterFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStrafeFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldBreezilyTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldExpandTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldGodBridgeTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTower;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerNone;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerVulcan;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtilKt;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.block.SwingMode;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.clicking.Clicker;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.ccbluex.liquidbounce.utils.client.Timer;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.item.ItemExtensionsKt;
import net.ccbluex.liquidbounce.utils.item.PreferAverageHardBlocks;
import net.ccbluex.liquidbounce.utils.item.PreferFavourableBlocks;
import net.ccbluex.liquidbounce.utils.item.PreferFullCubeBlocks;
import net.ccbluex.liquidbounce.utils.item.PreferSolidBlocks;
import net.ccbluex.liquidbounce.utils.item.PreferStackSize;
import net.ccbluex.liquidbounce.utils.item.PreferWalkableBlocks;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import net.ccbluex.liquidbounce.utils.math.ShapeExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer;
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u00d8\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0006\u0085\u0001\u0086\u0001\u0087\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010M\u001a\u00020NH\u0016J\b\u0010O\u001a\u00020NH\u0016J\b\u0010P\u001a\u00020NH\u0002J\u0017\u0010U\u001a\u00020N2\b\u0010V\u001a\u0004\u0018\u00010?H\u0002\u00a2\u0006\u0002\u0010WJ\u0014\u0010q\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020K0s0rH\u0002J\u000f\u0010t\u001a\u0004\u0018\u00010?H\u0002\u00a2\u0006\u0002\u0010uJ\u0015\u0010v\u001a\u00020!2\u0006\u0010w\u001a\u00020xH\u0000\u00a2\u0006\u0002\byJ\u0015\u0010z\u001a\u00020{2\u0006\u0010|\u001a\u00020{H\u0000\u00a2\u0006\u0002\b}J\u001e\u0010~\u001a\u00020!2\b\u0010\u007f\u001a\u0004\u0018\u00010x2\n\u0010\u0080\u0001\u001a\u0005\u0018\u00010\u0081\u0001H\u0002J\u001b\u0010\u0082\u0001\u001a\u00020!2\u0007\u0010\u0083\u0001\u001a\u00020!2\u0007\u0010\u0084\u0001\u001a\u00020!H\u0002R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\t\u001a\u0004\b\f\u0010\rR\u001b\u0010\u000f\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\t\u001a\u0004\b\u0010\u0010\rR\u001a\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u001b\u0010\u0017\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001b\u0010\u001c\u001a\u0004\b\u0019\u0010\u001aR\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0016R\u0014\u0010 \u001a\u00020!8@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\"\u0010#R\u000e\u0010$\u001a\u00020!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010%\u001a\u00020\u00148BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b&\u0010'R\u001a\u0010(\u001a\b\u0012\u0004\u0012\u00020)0\u0013X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b*\u0010\u0003R\u0010\u0010+\u001a\u0004\u0018\u00010,X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010-\u001a\u00020.8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b1\u0010\u001c\u001a\u0004\b/\u00100R\u001b\u00102\u001a\u00020!8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b4\u00105\u001a\u0004\b3\u0010#R+\u00107\u001a\u00020!2\u0006\u00106\u001a\u00020!8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b;\u00105\u001a\u0004\b8\u0010#\"\u0004\b9\u0010:R\u000e\u0010<\u001a\u00020=X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010>\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010@\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010A\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010B\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010C\u001a\u0004\u0018\u00010DX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0011\u0010E\u001a\u00020?8F\u00a2\u0006\u0006\u001a\u0004\bF\u0010GR\u0011\u0010H\u001a\u00020!8F\u00a2\u0006\u0006\u001a\u0004\bH\u0010#R\u0014\u0010I\u001a\b\u0012\u0004\u0012\u00020K0JX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010L\u001a\b\u0012\u0004\u0012\u00020K0J8\u0006X\u0087\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010Q\u001a\b\u0012\u0004\u0012\u00020S0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bT\u0010\u0003R\u001a\u0010X\u001a\b\u0012\u0004\u0012\u00020Y0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bZ\u0010\u0003R\u001c\u0010[\u001a\u0004\u0018\u00010\\X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b]\u0010^\"\u0004\b_\u0010`R\u001a\u0010a\u001a\u00020bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bc\u0010d\"\u0004\be\u0010fR\u001a\u0010g\u001a\b\u0012\u0004\u0012\u00020h0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bi\u0010\u0003R\u001a\u0010j\u001a\b\u0012\u0004\u0012\u00020h0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bk\u0010\u0003R\u001a\u0010l\u001a\b\u0012\u0004\u0012\u00020m0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bn\u0010\u0003R\u001a\u0010o\u001a\b\u0012\u0004\u0012\u00020m0RX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\bp\u0010\u0003\u00a8\u0006\u0088\u0001"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold;", "Lnet/ccbluex/liquidbounce/features/module/ClientModule;", "<init>", "()V", "delay", "Lkotlin/ranges/IntRange;", "getDelay", "()Lkotlin/ranges/IntRange;", "delay$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "minDist", "", "getMinDist", "()F", "minDist$delegate", "timer", "getTimer", "timer$delegate", "technique", "Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "getTechnique$liquidbounce", "()Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "sameYMode", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode;", "getSameYMode", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode;", "sameYMode$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "towerMode", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "getTowerMode", "isTowering", "", "isTowering$liquidbounce", "()Z", "wasTowering", "activeTechnique", "getActiveTechnique", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "safeWalkMode", "Lnet/ccbluex/liquidbounce/config/types/group/Mode;", "getSafeWalkMode$annotations", "currentTarget", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "swingMode", "Lnet/ccbluex/liquidbounce/utils/block/SwingMode;", "getSwingMode", "()Lnet/ccbluex/liquidbounce/utils/block/SwingMode;", "swingMode$delegate", "autoSpeed", "getAutoSpeed", "autoSpeed$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "<set-?>", "ledge", "getLedge", "setLedge", "(Z)V", "ledge$delegate", "renderer", "Lnet/ccbluex/liquidbounce/utils/render/placement/PlacementRenderer;", "placementY", "", "forceSneak", "startY", "jumps", "nextBlock", "Lnet/minecraft/world/level/block/Block;", "blockCount", "getBlockCount", "()I", "isBlockBelow", "BLOCK_COMPARATOR_FOR_HOTBAR", "Lnet/ccbluex/liquidbounce/utils/sorting/ComparatorChain;", "Lnet/minecraft/world/item/ItemStack;", "BLOCK_COMPARATOR_FOR_INVENTORY", "onEnabled", "", "onDisabled", "reset", "worldChangeHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/WorldChangeEvent;", "getWorldChangeHandler$annotations", "updateRenderCount", "count", "(Ljava/lang/Integer;)V", "rotationUpdateHandler", "Lnet/ccbluex/liquidbounce/event/events/RotationUpdateEvent;", "getRotationUpdateHandler$annotations", "currentOptimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "getCurrentOptimalLine", "()Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "setCurrentOptimalLine", "(Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;)V", "rawInput", "Lnet/ccbluex/liquidbounce/utils/movement/DirectionalInput;", "getRawInput", "()Lnet/ccbluex/liquidbounce/utils/movement/DirectionalInput;", "setRawInput", "(Lnet/ccbluex/liquidbounce/utils/movement/DirectionalInput;)V", "handleMovementInput", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getHandleMovementInput$annotations", "movementInputHandler", "getMovementInputHandler$annotations", "timerHandler", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getTimerHandler$annotations", "tickHandler", "getTickHandler$annotations", "findPlaceableSlots", "", "Lkotlin/collections/IndexedValue;", "findBestValidHotbarSlotForTarget", "()Ljava/lang/Integer;", "isValidCrosshairTarget", "rayTraceResult", "Lnet/minecraft/world/phys/BlockHitResult;", "isValidCrosshairTarget$liquidbounce", "getTargetedPosition", "Lnet/minecraft/core/BlockPos;", "blockPos", "getTargetedPosition$liquidbounce", "simulatePlacementAttempts", "hitResult", "suitableHand", "Lnet/minecraft/world/InteractionHand;", "handleSilentBlockSelection", "hasBlockInMainHand", "hasBlockInOffHand", "SameYMode", "ScaffoldRotationValueGroup", "SimulatePlacementAttempts", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nModuleScaffold.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 4 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 5 ModuleDebug.kt\nnet/ccbluex/liquidbounce/features/module/modules/render/ModuleDebug\n+ 6 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 7 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n+ 8 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n+ 9 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n*L\n1#1,740:1\n1#2:741\n1#2:780\n777#3:742\n873#3,2:743\n111#4:745\n196#4:757\n269#5,6:746\n269#5,5:752\n274#5:758\n558#6:759\n216#7:760\n96#8,4:761\n57#9,7:765\n77#9,8:772\n85#9,2:781\n66#9:783\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold\n*L\n-1#1:780\n641#1:742\n641#1:743,2\n650#1:745\n398#1:757\n387#1:746,6\n396#1:752,5\n396#1:758\n-1#1:759\n-1#1:760\n-1#1:761,4\n-1#1:765,7\n-1#1:772,8\n-1#1:781,2\n-1#1:783\n*E\n"})
public final class ModuleScaffold
extends ClientModule {
    @NotNull
    public static final ModuleScaffold INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue delay$delegate;
    @NotNull
    private static final RangedValue minDist$delegate;
    @NotNull
    private static final RangedValue timer$delegate;
    @NotNull
    private static final ModeValueGroup<ScaffoldTechnique> technique;
    @NotNull
    private static final ChoiceListValue sameYMode$delegate;
    @NotNull
    private static final ModeValueGroup<ScaffoldTower> towerMode;
    private static boolean wasTowering;
    @NotNull
    private static final ModeValueGroup<Mode> safeWalkMode;
    @Nullable
    private static BlockPlacementTarget currentTarget;
    @NotNull
    private static final ChoiceListValue swingMode$delegate;
    @NotNull
    private static final Value autoSpeed$delegate;
    @NotNull
    private static final Value ledge$delegate;
    @NotNull
    private static final PlacementRenderer renderer;
    private static int placementY;
    private static int forceSneak;
    private static int startY;
    private static int jumps;
    @Nullable
    private static Block nextBlock;
    @NotNull
    private static final ComparatorChain<ItemStack> BLOCK_COMPARATOR_FOR_HOTBAR;
    @JvmField
    @NotNull
    public static final ComparatorChain<ItemStack> BLOCK_COMPARATOR_FOR_INVENTORY;
    @NotNull
    private static final EventHook<WorldChangeEvent> worldChangeHandler;
    @NotNull
    private static final EventHook<RotationUpdateEvent> rotationUpdateHandler;
    @Nullable
    private static Line currentOptimalLine;
    @NotNull
    private static DirectionalInput rawInput;
    @NotNull
    private static final EventHook<MovementInputEvent> handleMovementInput;
    @NotNull
    private static final EventHook<MovementInputEvent> movementInputHandler;
    @NotNull
    private static final EventHook<GameTickEvent> timerHandler;
    @NotNull
    private static final EventHook<GameTickEvent> tickHandler;

    private ModuleScaffold() {
        super("Scaffold", ModuleCategories.WORLD, 0, null, false, false, false, false, null, false, 1020, null);
    }

    private final IntRange getDelay() {
        return (IntRange)delay$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final float getMinDist() {
        return ((Number)minDist$delegate.getValue((Object)this, $$delegatedProperties[1])).floatValue();
    }

    private final float getTimer() {
        return ((Number)timer$delegate.getValue((Object)this, $$delegatedProperties[2])).floatValue();
    }

    @NotNull
    public final ModeValueGroup<ScaffoldTechnique> getTechnique$liquidbounce() {
        return technique;
    }

    private final SameYMode getSameYMode() {
        return (SameYMode)((Object)sameYMode$delegate.getValue((Object)this, $$delegatedProperties[3]));
    }

    @NotNull
    public final ModeValueGroup<ScaffoldTower> getTowerMode() {
        return towerMode;
    }

    public final boolean isTowering$liquidbounce() {
        boolean bl;
        if (!Intrinsics.areEqual((Object)towerMode.getActiveMode(), (Object)((Object)ScaffoldTowerNone.INSTANCE)) && this.getMc().options.keyJump.isDown()) {
            wasTowering = true;
            bl = true;
        } else {
            bl = false;
        }
        return bl;
    }

    private final ScaffoldTechnique getActiveTechnique() {
        return this.isTowering$liquidbounce() ? (ScaffoldTechnique)ScaffoldNormalTechnique.INSTANCE : (ScaffoldTechnique)technique.getActiveMode();
    }

    private static /* synthetic */ void getSafeWalkMode$annotations() {
    }

    private final SwingMode getSwingMode() {
        return (SwingMode)swingMode$delegate.getValue((Object)this, $$delegatedProperties[4]);
    }

    public final boolean getAutoSpeed() {
        return (Boolean)autoSpeed$delegate.getValue((Object)this, $$delegatedProperties[5]);
    }

    private final boolean getLedge() {
        return (Boolean)ledge$delegate.getValue((Object)this, $$delegatedProperties[6]);
    }

    private final void setLedge(boolean bl) {
        ledge$delegate.setValue((Object)this, $$delegatedProperties[6], (Object)bl);
    }

    /*
     * WARNING - void declaration
     */
    public final int getBlockCount() {
        int n;
        ItemStack itemStack = this.getPlayer().getOffhandItem();
        Intrinsics.checkNotNullExpressionValue((Object)itemStack, (String)"getOffhandItem(...)");
        int n2 = ModuleScaffold._get_blockCount_$blockCount(itemStack);
        if (ScaffoldAutoBlockFeature.INSTANCE.getEnabled()) {
            int n3;
            Iterable iterable = this.findPlaceableSlots();
            int n4 = n2;
            int n5 = 0;
            for (Object t : iterable) {
                void it;
                IndexedValue indexedValue = (IndexedValue)t;
                n3 = n5;
                boolean bl = false;
                int n6 = ModuleScaffold._get_blockCount_$blockCount((ItemStack)it.getValue());
                n5 = n3 + n6;
            }
            n3 = n5;
            n2 = n4;
            n = n3;
        } else {
            ItemStack itemStack2 = this.getPlayer().getInventory().getItem(this.getPlayer().getInventory().getSelectedSlot());
            Intrinsics.checkNotNullExpressionValue((Object)itemStack2, (String)"getItem(...)");
            n = ModuleScaffold._get_blockCount_$blockCount(itemStack2);
        }
        return n2 + n;
    }

    public final boolean isBlockBelow() {
        Iterable iterable = this.getWorld().getBlockCollisions((Entity)this.getPlayer(), this.getPlayer().getBoundingBox().inflate(0.5, 0.0, 0.5).move(0.0, -1.05, 0.0));
        Intrinsics.checkNotNullExpressionValue((Object)iterable, (String)"getBlockCollisions(...)");
        return !ShapeExtensionsKt.allEmpty((Iterable)iterable);
    }

    public void onEnabled() {
        placementY = this.getPlayer().blockPosition().getY() - 1;
        startY = this.getPlayer().blockPosition().getY();
        jumps = 2;
        ScaffoldMovementPlanner.INSTANCE.reset();
        super.onEnabled();
    }

    public void onDisabled() {
        this.reset();
    }

    private final void reset() {
        NoFallBlink.INSTANCE.setWaitUntilGround(false);
        ScaffoldMovementPlanner.INSTANCE.reset();
        ScaffoldMovementPrediction.INSTANCE.reset();
        SilentHotbar.INSTANCE.resetSlot((Object)this);
        nextBlock = null;
        this.updateRenderCount(null);
        forceSneak = 0;
        currentTarget = null;
        renderer.clearSilently();
    }

    private static /* synthetic */ void getWorldChangeHandler$annotations() {
    }

    private final void updateRenderCount(Integer count) {
        EventManager.INSTANCE.callEvent((Event)new BlockCountChangeEvent(nextBlock, count));
    }

    private static /* synthetic */ void getRotationUpdateHandler$annotations() {
    }

    @Nullable
    public final Line getCurrentOptimalLine() {
        return currentOptimalLine;
    }

    public final void setCurrentOptimalLine(@Nullable Line line) {
        currentOptimalLine = line;
    }

    @NotNull
    public final DirectionalInput getRawInput() {
        return rawInput;
    }

    public final void setRawInput(@NotNull DirectionalInput directionalInput) {
        Intrinsics.checkNotNullParameter((Object)directionalInput, (String)"<set-?>");
        rawInput = directionalInput;
    }

    private static /* synthetic */ void getHandleMovementInput$annotations() {
    }

    private static /* synthetic */ void getMovementInputHandler$annotations() {
    }

    private static /* synthetic */ void getTimerHandler$annotations() {
    }

    private static /* synthetic */ void getTickHandler$annotations() {
    }

    private final List<IndexedValue<ItemStack>> findPlaceableSlots() {
        List list;
        List $this$findPlaceableSlots_u24lambda_u240 = list = CollectionsKt.createListBuilder((int)9);
        boolean bl = false;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack;
            Intrinsics.checkNotNullExpressionValue((Object)INSTANCE.getPlayer().getInventory().getItem(i), (String)"getItem(...)");
            if (!ScaffoldBlockItemSelection.INSTANCE.isValidBlock(stack)) continue;
            $this$findPlaceableSlots_u24lambda_u240.add(new IndexedValue(i, (Object)stack));
        }
        return CollectionsKt.build((List)list);
    }

    /*
     * WARNING - void declaration
     */
    private final Integer findBestValidHotbarSlotForTarget() {
        void $this$filterTo$iv$iv;
        void $this$filter$iv;
        List<IndexedValue<ItemStack>> placeableSlots = this.findPlaceableSlots();
        int doNotUseBelowCount = ScaffoldAutoBlockFeature.INSTANCE.getDoNotUseBelowCount();
        Iterable iterable = placeableSlots;
        boolean $i$f$filter = false;
        void var6_5 = $this$filter$iv;
        Collection destination$iv$iv = new ArrayList();
        boolean $i$f$filterTo = false;
        for (Object element$iv$iv : $this$filterTo$iv$iv) {
            IndexedValue indexedValue = (IndexedValue)element$iv$iv;
            boolean bl = false;
            ItemStack stack = (ItemStack)indexedValue.component2();
            if (!(stack.getCount() > doNotUseBelowCount)) continue;
            destination$iv$iv.add(element$iv$iv);
        }
        IndexedValue indexedValue = (IndexedValue)CollectionsKt.maxWithOrNull((Iterable)((List)destination$iv$iv), (arg_0, arg_1) -> ModuleScaffold.findBestValidHotbarSlotForTarget$lambda$2(ModuleScaffold::findBestValidHotbarSlotForTarget$lambda$1, arg_0, arg_1));
        if (indexedValue == null && (indexedValue = (IndexedValue)CollectionsKt.maxWithOrNull((Iterable)placeableSlots, (arg_0, arg_1) -> ModuleScaffold.findBestValidHotbarSlotForTarget$lambda$4(ModuleScaffold::findBestValidHotbarSlotForTarget$lambda$3, arg_0, arg_1))) == null) {
            return null;
        }
        int slot = indexedValue.component1();
        return slot;
    }

    /*
     * WARNING - void declaration
     */
    public final boolean isValidCrosshairTarget$liquidbounce(@NotNull BlockHitResult rayTraceResult) {
        void $this$minus$iv;
        Intrinsics.checkNotNullParameter((Object)rayTraceResult, (String)"rayTraceResult");
        Vec3 vec3 = rayTraceResult.getLocation();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getLocation(...)");
        Vec3 vec32 = vec3;
        Vec3 vec33 = this.getPlayer().getEyePosition();
        Intrinsics.checkNotNullExpressionValue((Object)vec33, (String)"getEyePosition(...)");
        Position other$iv = (Position)vec33;
        boolean $i$f$minus = false;
        Vec3 vec34 = $this$minus$iv.subtract(other$iv.x(), other$iv.y(), other$iv.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec34, (String)"subtract(...)");
        Vec3 diff = vec34;
        Direction direction = rayTraceResult.getDirection();
        Intrinsics.checkNotNullExpressionValue((Object)direction, (String)"getDirection(...)");
        Direction side = direction;
        if (side.getAxis() != Direction.Axis.Y) {
            double dist;
            double d = dist = side == Direction.NORTH || side == Direction.SOUTH ? diff.z : diff.x;
            if (Math.abs(dist) < (double)this.getMinDist()) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public final BlockPos getTargetedPosition$liquidbounce(@NotNull BlockPos blockPos) {
        BlockPos blockPos2;
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        if (this.isTowering$liquidbounce() || wasTowering) {
            blockPos2 = ((ScaffoldTower)towerMode.getActiveMode()).getTargetedPosition(blockPos);
        } else if (ScaffoldDownFeature.INSTANCE.getRunning() && ScaffoldDownFeature.INSTANCE.getShouldGoDown()) {
            BlockPos blockPos3 = blockPos.offset(0, -2, 0);
            blockPos2 = blockPos3;
            Intrinsics.checkNotNullExpressionValue((Object)blockPos3, (String)"offset(...)");
        } else if (ScaffoldCeilingFeature.INSTANCE.getRunning() && ScaffoldCeilingFeature.INSTANCE.canConstructCeiling()) {
            BlockPos blockPos4 = blockPos.offset(0, 3, 0);
            blockPos2 = blockPos4;
            Intrinsics.checkNotNullExpressionValue((Object)blockPos4, (String)"offset(...)");
        } else if (this.getPlayer().input.keyPresses.jump() && (!EntityExtensionsKt.getMoving((LocalPlayer)this.getPlayer()) || this.getPlayer().horizontalCollision)) {
            BlockPos blockPos5 = blockPos.offset(0, -1, 0);
            blockPos2 = blockPos5;
            Intrinsics.checkNotNullExpressionValue((Object)blockPos5, (String)"offset(...)");
        } else {
            blockPos2 = (BlockPos)this.getSameYMode().getGetTargetedBlockPos().invoke((Object)blockPos);
            if (blockPos2 == null) {
                BlockPos blockPos6 = blockPos.offset(0, -1, 0);
                blockPos2 = blockPos6;
                Intrinsics.checkNotNullExpressionValue((Object)blockPos6, (String)"offset(...)");
            }
        }
        return blockPos2;
    }

    private final boolean simulatePlacementAttempts(BlockHitResult hitResult, InteractionHand suitableHand) {
        boolean bl;
        InteractionHand interactionHand;
        block10: {
            block9: {
                interactionHand = suitableHand;
                if (interactionHand == null) break block9;
                InteractionHand interactionHand2 = interactionHand;
                LocalPlayer localPlayer = this.getPlayer();
                InteractionHand p0 = interactionHand2;
                boolean bl2 = false;
                ItemStack itemStack = localPlayer.getItemInHand(p0);
                interactionHand = itemStack;
                if (itemStack != null) break block10;
            }
            return false;
        }
        InteractionHand stack = interactionHand;
        if (hitResult == null || !SimulatePlacementAttempts.INSTANCE.getEnabled()) {
            return false;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        UseOnContext context = new UseOnContext((Player)this.getPlayer(), suitableHand, hitResult);
        Item item = stack.getItem();
        Intrinsics.checkNotNull((Object)item, (String)"null cannot be cast to non-null type net.minecraft.world.item.BlockItem");
        boolean canPlaceOnFace = ((BlockItem)item).getPlacementState(new BlockPlaceContext(context)) != null;
        if (SimulatePlacementAttempts.INSTANCE.getFailedAttemptsOnly()) {
            bl = !canPlaceOnFace;
        } else if (this.getSameYMode() != SameYMode.OFF) {
            bl = !(context.getClickedPos().getY() != placementY || hitResult.getDirection() == Direction.UP && canPlaceOnFace);
        } else {
            boolean isTargetUnderPlayer = context.getClickedPos().getY() <= this.getPlayer().getBlockY() - 1;
            boolean isTowering = context.getClickedPos().getY() == this.getPlayer().getBlockY() - 1 && canPlaceOnFace && context.getClickedFace() == Direction.UP;
            bl = isTargetUnderPlayer && !isTowering;
        }
        return bl;
    }

    private final boolean handleSilentBlockSelection(boolean hasBlockInMainHand, boolean hasBlockInOffHand) {
        if (ScaffoldAutoBlockFeature.INSTANCE.getEnabled() && !hasBlockInMainHand && !hasBlockInOffHand) {
            Integer bestMainHandSlot = this.findBestValidHotbarSlotForTarget();
            if (bestMainHandSlot != null) {
                SilentHotbar.INSTANCE.selectSlotSilently((Object)this, bestMainHandSlot.intValue(), ScaffoldAutoBlockFeature.INSTANCE.getSlotResetDelay());
                return true;
            }
            SilentHotbar.INSTANCE.resetSlot((Object)this);
        } else {
            SilentHotbar.INSTANCE.resetSlot((Object)this);
        }
        return hasBlockInMainHand;
    }

    private static final ScaffoldTower[] towerMode$lambda$0(ModeValueGroup it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        ScaffoldTower[] scaffoldTowerArray = new ScaffoldTower[]{ScaffoldTowerNone.INSTANCE, ScaffoldTowerMotion.INSTANCE, ScaffoldTowerPulldown.INSTANCE, ScaffoldTowerKarhu.INSTANCE, ScaffoldTowerVulcan.INSTANCE, ScaffoldTowerHypixel.INSTANCE};
        return scaffoldTowerArray;
    }

    private static final int _get_blockCount_$blockCount(ItemStack $this$_get_blockCount__u24blockCount) {
        return ScaffoldBlockItemSelection.INSTANCE.isValidBlock($this$_get_blockCount__u24blockCount) ? $this$_get_blockCount__u24blockCount.getCount() : 0;
    }

    private static final void worldChangeHandler$lambda$0(WorldChangeEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        INSTANCE.reset();
    }

    /*
     * Unable to fully structure code
     */
    private static final void rotationUpdateHandler$lambda$0(RotationUpdateEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        NoFallBlink.INSTANCE.setWaitUntilGround(true);
        blockInHotbar = ModuleScaffold.INSTANCE.findBestValidHotbarSlotForTarget();
        if (blockInHotbar == null) {
            ModuleScaffold.nextBlock = null;
            v0 = new ItemStack((ItemLike)Items.SANDSTONE, 64);
        } else {
            it = var4_2 = ModuleScaffold.INSTANCE.getPlayer().getInventory().getItem(blockInHotbar.intValue());
            $i$a$-also-ModuleScaffold$rotationUpdateHandler$1$bestStack$1 = false;
            Intrinsics.checkNotNull((Object)it);
            ModuleScaffold.nextBlock = ItemExtensionsKt.getBlock((ItemStack)it);
            var3_6 = var4_2;
            Intrinsics.checkNotNull((Object)var3_6);
            v0 = var3_6;
        }
        bestStack = v0;
        optimalLine = ModuleScaffold.currentOptimalLine;
        v1 = ScaffoldMovementPrediction.INSTANCE.getPredictedPlacementPos(optimalLine);
        if (v1 == null) {
            v2 = ModuleScaffold.INSTANCE.getPlayer().position();
            v1 = v2;
            Intrinsics.checkNotNullExpressionValue((Object)v2, (String)"position(...)");
        }
        predictedPos = v1;
        if (!ScaffoldEagleFeature.INSTANCE.getEnabled()) ** GOTO lbl-1000
        v3 = ModuleScaffold.INSTANCE.getPlayer().input;
        Intrinsics.checkNotNullExpressionValue((Object)v3, (String)"input");
        if (ScaffoldEagleFeature.INSTANCE.shouldEagle(new DirectionalInput(v3))) {
            v4 = Pose.CROUCHING;
        } else lbl-1000:
        // 2 sources

        {
            v4 = Pose.STANDING;
        }
        predictedPose = v4;
        $i$a$-also-ModuleScaffold$rotationUpdateHandler$1$bestStack$1 = ModuleDebug.INSTANCE;
        var7_8 = (DebuggedOwner)ModuleScaffold.INSTANCE;
        name$iv = "predictedPos";
        $i$f$debugGeometry = false;
        if (this_$iv.getRunning()) {
            var25_12 = name$iv;
            var24_13 = $this$debugGeometry$iv;
            var23_14 = this_$iv;
            $i$a$-debugGeometry-ModuleScaffold$rotationUpdateHandler$1$1 = false;
            var26_17 = (ModuleDebug.DebuggedGeometry)new ModuleDebug.DebuggedPoint(predictedPos, Color4b.GREEN, 0.1);
            var23_14.debugGeometry((DebuggedOwner)var24_13, var25_12, var26_17);
        }
        technique = ModuleScaffold.INSTANCE.getActiveTechnique();
        it = name$iv = technique.findPlacementTarget(predictedPos, predictedPose, optimalLine, bestStack);
        $i$a$-also-ModuleScaffold$rotationUpdateHandler$1$target$1 = false;
        ModuleScaffold.currentTarget = it;
        target = name$iv;
        name$iv = ModuleDebug.INSTANCE;
        it = (DebuggedOwner)ModuleScaffold.INSTANCE;
        name$iv = "lineToBlock";
        $i$f$debugGeometry = false;
        if (this_$iv.getRunning()) {
            var25_12 = name$iv;
            var24_13 = $this$debugGeometry$iv;
            var23_14 = this_$iv;
            $i$a$-debugGeometry-ModuleScaffold$rotationUpdateHandler$1$2 = false;
            v5 = target;
            if (v5 == null || (v5 = v5.getPlacedBlock()) == null) {
                v6 = null;
            } else {
                var14_21 = (Vec3i)v5;
                var15_22 = 0.5;
                var17_23 = 1.0;
                zOffset$iv = 0.5;
                $i$f$toVec3d = false;
                b = new Vec3((double)$this$toVec3d$iv.getX() + xOffset$iv, (double)$this$toVec3d$iv.getY() + yOffset$iv, (double)$this$toVec3d$iv.getZ() + zOffset$iv);
                v7 = optimalLine;
                if (v7 == null || (v7 = v7.getNearestPointTo(b)) == null) {
                    v6 = null;
                } else {
                    a = v7;
                    v6 = (ModuleDebug.DebuggedGeometry)new ModuleDebug.DebuggedLineSegment((Vec3)a, b, Color4b.RED);
                }
            }
            var26_17 = v6;
            var23_14.debugGeometry((DebuggedOwner)var24_13, var25_12, var26_17);
        }
        if (ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.NORMAL) {
            rotation = technique.getRotations((BlockPlacementTarget)target);
            var9_11 = RotationManager.INSTANCE;
            v8 = rotation;
            if (v8 == null) {
                return;
            }
            var10_16 = v8;
            var11_18 = ScaffoldRotationValueGroup.INSTANCE.getConsiderInventory();
            var12_20 = ScaffoldRotationValueGroup.INSTANCE;
            var13_27 = ModuleScaffold.INSTANCE;
            var14_21 = Priority.IMPORTANT_FOR_PLAYER_LIFE;
            RotationManager.setRotationTarget$default((RotationManager)var9_11, (Rotation)var10_16, (boolean)var11_18, (RotationsValueGroup)var12_20, (Priority)var14_21, (ClientModule)var13_27, null, (int)32, null);
        }
    }

    private static final void handleMovementInput$lambda$0(MovementInputEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        currentOptimalLine = null;
        rawInput = event.getDirectionalInput();
        DirectionalInput currentInput = event.getDirectionalInput();
        if (Intrinsics.areEqual((Object)currentInput, (Object)DirectionalInput.NONE)) {
            return;
        }
        currentOptimalLine = ScaffoldMovementPlanner.INSTANCE.getOptimalMovementLine(event.getDirectionalInput());
    }

    private static final void movementInputHandler$lambda$0(MovementInputEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        if (forceSneak > 0) {
            event.setSneak(true);
            int n = forceSneak;
            forceSneak = n + -1;
        }
        if (INSTANCE.getLedge()) {
            LedgeAction ledgeAction;
            ScaffoldTechnique technique = INSTANCE.getActiveTechnique();
            Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
            if (rotation == null) {
                rotation = EntityExtensionsKt.getRotation((Entity)((Entity)INSTANCE.getPlayer()));
            }
            if ((ledgeAction = ScaffoldLedgeFeatureKt.ledge(currentTarget, rotation, technique instanceof ScaffoldLedgeExtension ? (ScaffoldLedgeExtension)((Object)technique) : null)).jump()) {
                event.setJump(true);
            }
            if (ledgeAction.stopInput()) {
                event.setDirectionalInput(DirectionalInput.NONE);
            }
            if (ledgeAction.stepBack()) {
                event.setDirectionalInput(DirectionalInput.copy$default((DirectionalInput)event.getDirectionalInput(), (boolean)false, (boolean)true, (boolean)false, (boolean)false, (int)12, null));
            }
            if (ledgeAction.sneakTime() > forceSneak) {
                event.setSneak(true);
                forceSneak = ledgeAction.sneakTime();
            }
        }
    }

    private static final void timerHandler$lambda$0(GameTickEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (!(INSTANCE.getTimer() == 1.0f)) {
            Timer.requestTimerSpeed$default((Timer)Timer.INSTANCE, (float)INSTANCE.getTimer(), (Priority)Priority.IMPORTANT_FOR_USAGE_1, (ClientModule)INSTANCE, (int)0, (int)8, null);
        }
    }

    private static final void tickHandler$lambda$0$commonPlaceSucceed(BlockPos placed) {
        ScaffoldMovementPlanner.INSTANCE.trackPlacedBlock(placed);
        PlacementRenderer.addBlock$default((PlacementRenderer)renderer, (BlockPos)placed, (boolean)false, null, (int)0, (int)14, null);
        ScaffoldEagleFeature.INSTANCE.onBlockPlacement();
        ScaffoldBlinkFeature.INSTANCE.onBlockPlacement();
        ScaffoldSprintControlFeature.INSTANCE.onBlockPlacement();
    }

    private static final int findBestValidHotbarSlotForTarget$lambda$1(IndexedValue o1, IndexedValue o2) {
        return BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.getValue(), o2.getValue());
    }

    private static final int findBestValidHotbarSlotForTarget$lambda$2(Function2 $tmp0, Object p0, Object p1) {
        return ((Number)$tmp0.invoke(p0, p1)).intValue();
    }

    private static final int findBestValidHotbarSlotForTarget$lambda$3(IndexedValue o1, IndexedValue o2) {
        return BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.getValue(), o2.getValue());
    }

    private static final int findBestValidHotbarSlotForTarget$lambda$4(Function2 $tmp0, Object p0, Object p1) {
        return ((Number)$tmp0.invoke(p0, p1)).intValue();
    }

    public static final /* synthetic */ void access$updateRenderCount(ModuleScaffold $this, Integer count) {
        $this.updateRenderCount(count);
    }

    public static final /* synthetic */ void access$setPlacementY$p(int n) {
        placementY = n;
    }

    public static final /* synthetic */ void access$setWasTowering$p(boolean bl) {
        wasTowering = bl;
    }

    public static final /* synthetic */ void access$setStartY$p(int n) {
        startY = n;
    }

    public static final /* synthetic */ boolean access$getWasTowering$p() {
        return wasTowering;
    }

    public static final /* synthetic */ BlockPlacementTarget access$getCurrentTarget$p() {
        return currentTarget;
    }

    public static final /* synthetic */ ScaffoldTechnique access$getActiveTechnique(ModuleScaffold $this) {
        return $this.getActiveTechnique();
    }

    public static final /* synthetic */ IntRange access$getDelay(ModuleScaffold $this) {
        return $this.getDelay();
    }

    public static final /* synthetic */ boolean access$handleSilentBlockSelection(ModuleScaffold $this, boolean hasBlockInMainHand, boolean hasBlockInOffHand) {
        return $this.handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand);
    }

    public static final /* synthetic */ boolean access$simulatePlacementAttempts(ModuleScaffold $this, BlockHitResult hitResult, InteractionHand suitableHand) {
        return $this.simulatePlacementAttempts(hitResult, suitableHand);
    }

    public static final /* synthetic */ void access$tickHandler$lambda$0$commonPlaceSucceed(BlockPos placed) {
        ModuleScaffold.tickHandler$lambda$0$commonPlaceSucceed(placed);
    }

    public static final /* synthetic */ SwingMode access$getSwingMode(ModuleScaffold $this) {
        return $this.getSwingMode();
    }

    public static final /* synthetic */ void access$setCurrentTarget$p(BlockPlacementTarget blockPlacementTarget) {
        currentTarget = blockPlacementTarget;
    }

    /*
     * WARNING - void declaration
     */
    static {
        void behavior$iv$iv;
        void $this$suspendHandler_u24default$iv$iv;
        ContinuationInterceptor continuationInterceptor;
        short priority$iv$iv;
        Function3 handler$iv$iv;
        block3: {
            void context$iv$iv;
            block2: {
                EventListener $this$handler$iv;
                EventListener $this$handler_u24default$iv;
                String name$iv;
                Comparator[] this_$iv;
                Object[] objectArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "delay", "getDelay()Lkotlin/ranges/IntRange;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "minDist", "getMinDist()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "timer", "getTimer()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "sameYMode", "getSameYMode()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "swingMode", "getSwingMode()Lnet/ccbluex/liquidbounce/utils/block/SwingMode;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.class, "autoSpeed", "getAutoSpeed()Z", 0))), Reflection.mutableProperty1((MutablePropertyReference1)((MutablePropertyReference1)new MutablePropertyReference1Impl(ModuleScaffold.class, "ledge", "getLedge()Z", 0)))};
                $$delegatedProperties = objectArray;
                INSTANCE = new ModuleScaffold();
                delay$delegate = ValueGroup.intRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Delay", (IntRange)new IntRange(0, 0), (IntRange)new IntRange(0, 40), (String)"ticks", null, (int)16, null);
                minDist$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"MinDist", (float)0.0f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)0.25f), null, null, (int)24, null);
                timer$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Timer", (float)1.0f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.01f, (float)10.0f), null, null, (int)24, null);
                INSTANCE.tree(ScaffoldBlockItemSelection.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldAutoBlockFeature.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldMovementPrediction.INSTANCE);
                objectArray = new ScaffoldTechnique[]{ScaffoldNormalTechnique.INSTANCE, ScaffoldExpandTechnique.INSTANCE, ScaffoldGodBridgeTechnique.INSTANCE, ScaffoldBreezilyTechnique.INSTANCE};
                objectArray = INSTANCE.choices("Technique", ScaffoldNormalTechnique.INSTANCE, (Mode[])objectArray);
                Object object = INSTANCE;
                Value p0 = (Value)objectArray;
                boolean bl = false;
                object.tagBy(p0);
                technique = objectArray;
                objectArray = (ValueGroup)INSTANCE;
                object = "SameY";
                Enum default$iv = SameYMode.OFF;
                boolean $i$f$enumChoice = false;
                Tagged tagged = (Tagged)default$iv;
                boolean $i$f$enumSetAllOf = false;
                EnumSet<SameYMode> enumSet = EnumSet.allOf(SameYMode.class);
                Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
                sameYMode$delegate = this_$iv.enumChoice(name$iv, tagged, (Set)enumSet);
                towerMode = INSTANCE.choices("Tower", 0, ModuleScaffold::towerMode$lambda$0);
                safeWalkMode = INSTANCE.choices("SafeWalk", 1, (Function1)new Function1<ModeValueGroup<Mode>, Mode[]>((Object)ModuleSafeWalk.INSTANCE){

                    public final Mode[] invoke(ModeValueGroup<Mode> p0) {
                        Intrinsics.checkNotNullParameter(p0, (String)"p0");
                        return ((ModuleSafeWalk)this.receiver).safeWalkChoices(p0);
                    }
                });
                this_$iv = (Comparator[])INSTANCE;
                name$iv = "Swing";
                default$iv = (Enum)SwingMode.DO_NOT_HIDE;
                $i$f$enumChoice = false;
                Tagged tagged2 = (Tagged)default$iv;
                $i$f$enumSetAllOf = false;
                EnumSet<SwingMode> enumSet2 = EnumSet.allOf(SwingMode.class);
                Intrinsics.checkNotNullExpressionValue(enumSet2, (String)"allOf(...)");
                swingMode$delegate = this_$iv.enumChoice(name$iv, tagged2, (Set)enumSet2);
                INSTANCE.tree((ValueGroup)ScaffoldRotationValueGroup.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldSprintControlFeature.INSTANCE);
                INSTANCE.tree((ValueGroup)SimulatePlacementAttempts.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldAccelerationFeature.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldStrafeFeature.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldJumpStrafe.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldSpeedLimiterFeature.INSTANCE);
                INSTANCE.tree((ValueGroup)ScaffoldBlinkFeature.INSTANCE);
                autoSpeed$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"AutoSpeed", (boolean)false, null, (int)4, null);
                ledge$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Ledge", (boolean)true, null, (int)4, null);
                renderer = (PlacementRenderer)INSTANCE.tree((ValueGroup)new PlacementRenderer("Render", true, (EventListener)INSTANCE, false, false, null, 48, null));
                this_$iv = new Comparator[]{PreferFavourableBlocks.INSTANCE, PreferSolidBlocks.INSTANCE, PreferFullCubeBlocks.INSTANCE, PreferWalkableBlocks.INSTANCE, new PreferAverageHardBlocks(true), PreferStackSize.PREFER_MORE, new PreferAverageHardBlocks(false)};
                BLOCK_COMPARATOR_FOR_HOTBAR = new ComparatorChain(this_$iv);
                this_$iv = new Comparator[]{PreferFavourableBlocks.INSTANCE, PreferSolidBlocks.INSTANCE, PreferFullCubeBlocks.INSTANCE, PreferWalkableBlocks.INSTANCE, new PreferAverageHardBlocks(true), PreferStackSize.PREFER_FEWER, new PreferAverageHardBlocks(false)};
                BLOCK_COMPARATOR_FOR_INVENTORY = new ComparatorChain(this_$iv);
                this_$iv = (Comparator[])INSTANCE;
                Consumer<WorldChangeEvent> handler$iv = ModuleScaffold::worldChangeHandler$lambda$0;
                short priority$iv = 0;
                boolean $i$f$handler = false;
                worldChangeHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, WorldChangeEvent.class, (short)priority$iv, handler$iv);
                $this$handler_u24default$iv = (EventListener)INSTANCE;
                handler$iv = ModuleScaffold::rotationUpdateHandler$lambda$0;
                priority$iv = 0;
                $i$f$handler = false;
                rotationUpdateHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, RotationUpdateEvent.class, (short)priority$iv, handler$iv);
                rawInput = DirectionalInput.NONE;
                $this$handler_u24default$iv = (EventListener)INSTANCE;
                priority$iv = -10;
                handler$iv = ModuleScaffold::handleMovementInput$lambda$0;
                $i$f$handler = false;
                handleMovementInput = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
                $this$handler$iv = (EventListener)INSTANCE;
                priority$iv = -50;
                handler$iv = ModuleScaffold::movementInputHandler$lambda$0;
                $i$f$handler = false;
                movementInputHandler = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
                $this$handler$iv = (EventListener)INSTANCE;
                handler$iv = ModuleScaffold::timerHandler$lambda$0;
                priority$iv = 0;
                $i$f$handler = false;
                timerHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, GameTickEvent.class, (short)priority$iv, handler$iv);
                EventListener $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
                ContinuationInterceptor dispatcher$iv = null;
                Runnable onCancellation$iv = null;
                boolean $i$f$tickHandler = false;
                EventListener $i$f$enumSetAllOf2 = $this$tickHandler_u24default$iv;
                CoroutineContext coroutineContext = (CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$tickHandler_u24default$iv, dispatcher$iv);
                SuspendHandlerBehavior suspendHandlerBehavior = (SuspendHandlerBehavior)new SuspendHandlerBehavior.DiscardLatest(onCancellation$iv);
                handler$iv$iv = (Function3)new Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object>(null){
                    int label;
                    private /* synthetic */ Object L$0;
                    Object L$1;
                    Object L$2;
                    Object L$3;
                    Object L$4;
                    Object L$5;
                    Object L$6;
                    Object L$7;
                    Object L$8;
                    Object L$9;
                    Object L$10;
                    int I$0;
                    int I$1;
                    boolean Z$0;
                    boolean Z$1;

                    /*
                     * Unable to fully structure code
                     */
                    public final Object invokeSuspend(Object $result) {
                        var2_2 = (CoroutineScope)this.L$0;
                        var3_3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                        switch (this.label) {
                            case 0: {
                                ResultKt.throwOnFailure((Object)$result);
                                var4_4 = (Continuation)this;
                                $this$tickHandler_u24lambda_u240 = $this$suspendHandler;
                                $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1 = 0;
                                ModuleScaffold.access$updateRenderCount(ModuleScaffold.INSTANCE, Boxing.boxInt((int)ModuleScaffold.INSTANCE.getBlockCount()));
                                if (ModuleScaffold.INSTANCE.getPlayer().onGround()) {
                                    ModuleScaffold.access$setPlacementY$p(ModuleScaffold.INSTANCE.getPlayer().blockPosition().getY() - 1);
                                    var7_10 = ModuleScaffold.access$getJumps$p();
                                    ModuleScaffold.access$setJumps$p(var7_10 + 1);
                                    ModuleScaffold.access$setWasTowering$p(false);
                                }
                                if (ModuleScaffold.INSTANCE.getMc().options.keyJump.isDown()) {
                                    ModuleScaffold.access$setStartY$p(ModuleScaffold.INSTANCE.getPlayer().blockPosition().getY());
                                    ModuleScaffold.access$setJumps$p(2);
                                }
                                var7_11 = ModuleDebug.INSTANCE;
                                var8_13 = (DebuggedOwner)ModuleScaffold.INSTANCE;
                                name$iv = "IsTowering";
                                $i$f$debugParameter = false;
                                if (this_$iv.getRunning()) {
                                    var11_20 = name$iv;
                                    var12_21 = $this$debugParameter$iv;
                                    var13_22 = this_$iv;
                                    $i$a$-debugParameter-ModuleScaffold$tickHandler$1$1 = false;
                                    var15_25 = Boxing.boxBoolean((boolean)ModuleScaffold.INSTANCE.isTowering$liquidbounce());
                                    var13_22.debugParameter(var12_21, var11_20, (Object)var15_25);
                                }
                                this_$iv = ModuleDebug.INSTANCE;
                                $this$debugParameter$iv = (DebuggedOwner)ModuleScaffold.INSTANCE;
                                name$iv = "WasTowering";
                                $i$f$debugParameter = false;
                                if (this_$iv.getRunning()) {
                                    var11_20 = name$iv;
                                    var12_21 = $this$debugParameter$iv;
                                    var13_22 = this_$iv;
                                    $i$a$-debugParameter-ModuleScaffold$tickHandler$1$2 = false;
                                    var15_25 = Boxing.boxBoolean((boolean)ModuleScaffold.access$getWasTowering$p());
                                    var13_22.debugParameter(var12_21, var11_20, (Object)var15_25);
                                }
                                target = ModuleScaffold.access$getCurrentTarget$p();
                                technique = ModuleScaffold.access$getActiveTechnique(ModuleScaffold.INSTANCE);
                                if ((ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK || ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) && target != null) {
                                    v0 = technique.getRotations(target);
                                    if (v0 == null && (v0 = RotationManager.INSTANCE.getCurrentRotation()) == null) {
                                        v0 = EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer()));
                                    }
                                } else {
                                    v0 = RotationManager.INSTANCE.getCurrentRotation();
                                    if (v0 == null) {
                                        v0 = EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer()));
                                    }
                                }
                                currentRotation = v0.normalize();
                                currentCrosshairTarget = technique.getCrosshairTarget(target, currentRotation);
                                currentDelay = RangesKt.random((IntRange)ModuleScaffold.access$getDelay(ModuleScaffold.INSTANCE), (Random)((Random)Random.Default));
                                hasBlockInMainHand = ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getInventory().getItem(ModuleScaffold.INSTANCE.getPlayer().getInventory().getSelectedSlot()));
                                hasBlockInOffHand = ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getOffhandItem());
                                if (ScaffoldAutoBlockFeature.INSTANCE.getAlwaysHoldBlock()) {
                                    hasBlockInMainHand = ModuleScaffold.access$handleSilentBlockSelection(ModuleScaffold.INSTANCE, hasBlockInMainHand != false, hasBlockInOffHand != false);
                                }
                                $this$firstOrNull$iv = (Iterable)EntriesMappings.entries$0;
                                $i$f$firstOrNull = false;
                                for (T element$iv : $this$firstOrNull$iv) {
                                    it = (InteractionHand)element$iv;
                                    $i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1 = false;
                                    if (!ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getItemInHand(it))) continue;
                                    v1 = element$iv;
                                    ** GOTO lbl68
                                }
                                v1 = null;
lbl68:
                                // 2 sources

                                suitableHand = v1;
                                if (ModuleScaffold.access$simulatePlacementAttempts(ModuleScaffold.INSTANCE, currentCrosshairTarget, suitableHand) && EntityExtensionsKt.getMoving((LocalPlayer)ModuleScaffold.INSTANCE.getPlayer()) && SimulatePlacementAttempts.INSTANCE.getClicker().isClickTick()) {
                                    SimulatePlacementAttempts.INSTANCE.getClicker().click((Function0)new Function0<Boolean>(currentCrosshairTarget, suitableHand){
                                        final /* synthetic */ BlockHitResult $currentCrosshairTarget;
                                        final /* synthetic */ InteractionHand $suitableHand;
                                        {
                                            this.$currentCrosshairTarget = $currentCrosshairTarget;
                                            this.$suitableHand = $suitableHand;
                                        }

                                        public final Boolean invoke() {
                                            BlockHitResult blockHitResult = this.$currentCrosshairTarget;
                                            Intrinsics.checkNotNull((Object)blockHitResult);
                                            InteractionHand interactionHand = this.$suitableHand;
                                            Intrinsics.checkNotNull((Object)interactionHand);
                                            BlockExtensionsKt.doPlacement$default((BlockHitResult)blockHitResult, (InteractionHand)interactionHand, (Function0)((Function0)new Function0<Boolean>(this.$currentCrosshairTarget){
                                                final /* synthetic */ BlockHitResult $currentCrosshairTarget;
                                                {
                                                    this.$currentCrosshairTarget = $currentCrosshairTarget;
                                                }

                                                public final Boolean invoke() {
                                                    ModuleScaffold.access$tickHandler$lambda$0$commonPlaceSucceed(BlockExtensionsKt.getTargetBlockPos((BlockHitResult)this.$currentCrosshairTarget));
                                                    return true;
                                                }
                                            }), null, (SwingMode)ModuleScaffold.access$getSwingMode(ModuleScaffold.INSTANCE), (int)8, null);
                                            return true;
                                        }
                                    });
                                }
                                if (target != null && currentCrosshairTarget != null && target.doesCrosshairTargetMatchRequirements(currentCrosshairTarget) && ModuleScaffold.INSTANCE.isValidCrosshairTarget$liquidbounce(currentCrosshairTarget)) {
                                    if (!ScaffoldAutoBlockFeature.INSTANCE.getAlwaysHoldBlock()) {
                                        hasBlockInMainHand = ModuleScaffold.access$handleSilentBlockSelection(ModuleScaffold.INSTANCE, hasBlockInMainHand != false, hasBlockInOffHand != false);
                                    }
                                    if (hasBlockInMainHand || hasBlockInOffHand) {
                                        handToInteractWith = hasBlockInMainHand != false ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                                        wasSuccessful = new Ref.BooleanRef();
                                        if (ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK || ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) {
                                            if (!Intrinsics.areEqual((Object)currentRotation, (Object)RotationManager.INSTANCE.getServerRotation())) {
                                                ModuleScaffold.INSTANCE.getNetwork().send((Packet)new ServerboundMovePlayerPacket.PosRot(ModuleScaffold.INSTANCE.getPlayer().getX(), ModuleScaffold.INSTANCE.getPlayer().getY(), ModuleScaffold.INSTANCE.getPlayer().getZ(), currentRotation.yaw(), currentRotation.pitch(), ModuleScaffold.INSTANCE.getPlayer().onGround(), ModuleScaffold.INSTANCE.getPlayer().horizontalCollision));
                                            }
                                            if (ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) {
                                                var20_35 = RotationManager.INSTANCE;
                                                var21_38 = ScaffoldRotationValueGroup.INSTANCE.getConsiderInventory();
                                                var22_39 = ScaffoldRotationValueGroup.INSTANCE;
                                                $i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1 = ModuleScaffold.INSTANCE;
                                                var25_45 = Priority.IMPORTANT_FOR_PLAYER_LIFE;
                                                RotationManager.setRotationTarget$default((RotationManager)var20_35, (Rotation)currentRotation, (boolean)(var21_38 != false), (RotationsValueGroup)var22_39, (Priority)var25_45, (ClientModule)$i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1, null, (int)32, null);
                                            }
                                        }
                                        v2 = ModuleScaffold.INSTANCE.getCurrentOptimalLine();
                                        if (v2 != null) {
                                            l = v2;
                                            $i$a$-let-ModuleScaffold$tickHandler$1$previousFallOffPos$1 = false;
                                            v3 = ScaffoldMovementPrediction.INSTANCE.getFallOffPositionOnLine(l);
                                        } else {
                                            v3 = null;
                                        }
                                        previousFallOffPos = v3;
                                        BlockExtensionsKt.doPlacement$default((BlockHitResult)currentCrosshairTarget, (InteractionHand)handToInteractWith, (Function0)((Function0)new Function0<Boolean>(target, wasSuccessful){
                                            final /* synthetic */ BlockPlacementTarget $target;
                                            final /* synthetic */ Ref.BooleanRef $wasSuccessful;
                                            {
                                                this.$target = $target;
                                                this.$wasSuccessful = $wasSuccessful;
                                            }

                                            public final Boolean invoke() {
                                                ModuleScaffold.access$tickHandler$lambda$0$commonPlaceSucceed(this.$target.getPlacedBlock());
                                                ModuleScaffold.access$setCurrentTarget$p(null);
                                                this.$wasSuccessful.element = true;
                                                return true;
                                            }
                                        }), null, (SwingMode)ModuleScaffold.access$getSwingMode(ModuleScaffold.INSTANCE), (int)8, null);
                                        if (ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK && !Intrinsics.areEqual((Object)RotationManager.INSTANCE.getServerRotation(), (Object)EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer())))) {
                                            ModuleScaffold.INSTANCE.getNetwork().send((Packet)new ServerboundMovePlayerPacket.PosRot(ModuleScaffold.INSTANCE.getPlayer().getX(), ModuleScaffold.INSTANCE.getPlayer().getY(), ModuleScaffold.INSTANCE.getPlayer().getZ(), RotationUtilKt.withFixedYaw((LocalPlayer)ModuleScaffold.INSTANCE.getPlayer(), (Rotation)currentRotation), ModuleScaffold.INSTANCE.getPlayer().getXRot(), ModuleScaffold.INSTANCE.getPlayer().onGround(), ModuleScaffold.INSTANCE.getPlayer().horizontalCollision));
                                        }
                                        if (wasSuccessful.element) {
                                            ScaffoldMovementPrediction.INSTANCE.onPlace(ModuleScaffold.INSTANCE.getCurrentOptimalLine(), previousFallOffPos);
                                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)$this$suspendHandler);
                                            this.L$1 = SpillingKt.nullOutSpilledVariable((Object)$completion);
                                            this.L$2 = SpillingKt.nullOutSpilledVariable((Object)$this$tickHandler_u24lambda_u240);
                                            this.L$3 = SpillingKt.nullOutSpilledVariable((Object)target);
                                            this.L$4 = SpillingKt.nullOutSpilledVariable((Object)technique);
                                            this.L$5 = SpillingKt.nullOutSpilledVariable((Object)currentRotation);
                                            this.L$6 = SpillingKt.nullOutSpilledVariable((Object)currentCrosshairTarget);
                                            this.L$7 = SpillingKt.nullOutSpilledVariable((Object)handToInteractWith);
                                            this.L$8 = SpillingKt.nullOutSpilledVariable((Object)wasSuccessful);
                                            this.L$9 = SpillingKt.nullOutSpilledVariable((Object)previousFallOffPos);
                                            this.L$10 = SpillingKt.nullOutSpilledVariable((Object)suitableHand);
                                            this.I$0 = $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1;
                                            this.I$1 = currentDelay;
                                            this.Z$0 = hasBlockInMainHand;
                                            this.Z$1 = hasBlockInOffHand;
                                            this.label = 1;
                                            v4 = CoroutineTickerKt.waitTicks((int)currentDelay, (Continuation)this);
                                            if (v4 == var3_3) {
                                                return var3_3;
                                            }
                                        }
                                    }
                                }
                                ** GOTO lbl137
                            }
                            case 1: {
                                hasBlockInOffHand = this.Z$1;
                                hasBlockInMainHand = this.Z$0;
                                currentDelay = this.I$1;
                                $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1 = this.I$0;
                                suitableHand = (InteractionHand)this.L$10;
                                previousFallOffPos = (Vec3)this.L$9;
                                wasSuccessful = (Ref.BooleanRef)this.L$8;
                                handToInteractWith = (InteractionHand)this.L$7;
                                currentCrosshairTarget = (BlockHitResult)this.L$6;
                                currentRotation = (Rotation)this.L$5;
                                technique = (ScaffoldTechnique)this.L$4;
                                target = (BlockPlacementTarget)this.L$3;
                                $this$tickHandler_u24lambda_u240 = (CoroutineScope)this.L$2;
                                $completion = (Continuation)this.L$1;
                                ResultKt.throwOnFailure((Object)$result);
                                v4 = $result;
lbl137:
                                // 2 sources

                                return Unit.INSTANCE;
                            }
                        }
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                    }

                    public final Object invoke(CoroutineScope p1, GameTickEvent p2, Continuation<? super Unit> p3) {
                        var var4_4 = new /* invalid duplicate definition of identical inner class */;
                        var4_4.L$0 = p1;
                        return var4_4.invokeSuspend(Unit.INSTANCE);
                    }
                };
                priority$iv$iv = 0;
                boolean $i$f$suspendHandler = false;
                continuationInterceptor = (ContinuationInterceptor)context$iv$iv.get((CoroutineContext.Key)ContinuationInterceptor.Key);
                if (continuationInterceptor == null) break block2;
                ContinuationInterceptor it$iv$iv = continuationInterceptor;
                boolean bl2 = false;
                CoroutineContext coroutineContext2 = context$iv$iv.plus((CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$suspendHandler_u24default$iv$iv, (ContinuationInterceptor)it$iv$iv));
                continuationInterceptor = coroutineContext2;
                if (coroutineContext2 != null) break block3;
            }
            continuationInterceptor = context$iv$iv;
        }
        ContinuationInterceptor context$iv$iv = continuationInterceptor;
        void $this$suspendHandler_u24lambda_u241$iv$iv = behavior$iv$iv;
        boolean bl = false;
        tickHandler = $this$suspendHandler_u24lambda_u241$iv$iv.createEventHook((EventListener)$this$suspendHandler_u24default$iv$iv, GameTickEvent.class, (CoroutineContext)context$iv$iv, priority$iv$iv, handler$iv$iv);
    }

    @Metadata(mv={2, 3, 0}, k=3, xi=50)
    public static final class EntriesMappings {
        public static final /* synthetic */ EnumEntries<InteractionHand> entries$0;

        static {
            entries$0 = EnumEntriesKt.enumEntries((Enum[])((Enum[])InteractionHand.values()));
        }
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B'\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0014\u0010\u0005\u001a\u0010\u0012\u0004\u0012\u00020\u0007\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006\u00a2\u0006\u0004\b\b\u0010\tR\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001f\u0010\u0005\u001a\u0010\u0012\u0004\u0012\u00020\u0007\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011\u00a8\u0006\u0012"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "getTargetedBlockPos", "Lkotlin/Function1;", "Lnet/minecraft/core/BlockPos;", "<init>", "(Ljava/lang/String;ILjava/lang/String;Lkotlin/jvm/functions/Function1;)V", "getTag", "()Ljava/lang/String;", "getGetTargetedBlockPos", "()Lkotlin/jvm/functions/Function1;", "OFF", "ON", "FALLING", "HYPIXEL", "liquidbounce"})
    @SourceDebugExtension(value={"SMAP\nModuleScaffold.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,740:1\n78#2:741\n78#2:742\n78#2:744\n78#2:745\n1#3:743\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode\n*L\n155#1:741\n160#1:742\n170#1:744\n172#1:745\n*E\n"})
    private static final class SameYMode
    extends Enum<SameYMode>
    implements Tagged {
        @NotNull
        private final String tag;
        @NotNull
        private final Function1<BlockPos, BlockPos> getTargetedBlockPos;
        public static final /* enum */ SameYMode OFF = new SameYMode("Off", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)SameYMode::_init_$lambda$0));
        public static final /* enum */ SameYMode ON = new SameYMode("On", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)SameYMode::_init_$lambda$1));
        public static final /* enum */ SameYMode FALLING = new SameYMode("Falling", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)SameYMode::_init_$lambda$2));
        public static final /* enum */ SameYMode HYPIXEL = new SameYMode("Hypixel", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)SameYMode::_init_$lambda$3));
        private static final /* synthetic */ SameYMode[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private SameYMode(String tag, Function1<? super BlockPos, ? extends BlockPos> getTargetedBlockPos) {
            this.tag = tag;
            this.getTargetedBlockPos = getTargetedBlockPos;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        @NotNull
        public final Function1<BlockPos, BlockPos> getGetTargetedBlockPos() {
            return this.getTargetedBlockPos;
        }

        public static SameYMode[] values() {
            return (SameYMode[])$VALUES.clone();
        }

        public static SameYMode valueOf(String value) {
            return Enum.valueOf(SameYMode.class, value);
        }

        @NotNull
        public static EnumEntries<SameYMode> getEntries() {
            return $ENTRIES;
        }

        private static final BlockPos _init_$lambda$0(BlockPos it) {
            Intrinsics.checkNotNullParameter((Object)it, (String)"it");
            return null;
        }

        /*
         * WARNING - void declaration
         */
        private static final BlockPos _init_$lambda$1(BlockPos blockPos) {
            void $this$copy_u24default$iv;
            Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
            BlockPos blockPos2 = blockPos;
            int y$iv = placementY;
            int x$iv = $this$copy_u24default$iv.getX();
            int z$iv = $this$copy_u24default$iv.getZ();
            boolean $i$f$copy = false;
            return new BlockPos(x$iv, y$iv, z$iv);
        }

        /*
         * WARNING - void declaration
         */
        private static final BlockPos _init_$lambda$2(BlockPos blockPos) {
            void $this$copy_u24default$iv;
            Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
            BlockPos blockPos2 = blockPos;
            int y$iv = placementY;
            int x$iv = $this$copy_u24default$iv.getX();
            int z$iv = $this$copy_u24default$iv.getZ();
            boolean $i$f$copy = false;
            BlockPos it = blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
            boolean bl = false;
            return ModuleScaffold.INSTANCE.getPlayer().getDeltaMovement().y < 0.2 ? blockPos2 : null;
        }

        /*
         * WARNING - void declaration
         */
        private static final BlockPos _init_$lambda$3(BlockPos blockPos) {
            BlockPos blockPos2;
            Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
            if (ModuleScaffold.INSTANCE.getPlayer().getDeltaMovement().y == -0.15233518685055708 && jumps >= 2) {
                void $this$copy_u24default$iv;
                jumps = 0;
                BlockPos blockPos3 = blockPos;
                int y$iv = startY;
                int x$iv = $this$copy_u24default$iv.getX();
                int z$iv = $this$copy_u24default$iv.getZ();
                boolean $i$f$copy = false;
                blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
            } else {
                BlockPos $this$copy_u24default$iv = blockPos;
                int y$iv = startY - 1;
                int x$iv = $this$copy_u24default$iv.getX();
                int z$iv = $this$copy_u24default$iv.getZ();
                boolean $i$f$copy = false;
                blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
            }
            return blockPos2;
        }

        static {
            $VALUES = sameYModeArray = new SameYMode[]{SameYMode.OFF, SameYMode.ON, SameYMode.FALLING, SameYMode.HYPIXEL};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u00c0\u0002\u0018\u00002\u00020\u0001:\u0001\u0010B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0011"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup;", "Lnet/ccbluex/liquidbounce/utils/aiming/RotationsValueGroup;", "<init>", "()V", "considerInventory", "", "getConsiderInventory", "()Z", "considerInventory$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "rotationTiming", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "getRotationTiming", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "rotationTiming$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "RotationTimingMode", "liquidbounce"})
    @SourceDebugExtension(value={"SMAP\nModuleScaffold.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup\n+ 2 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 3 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n*L\n1#1,740:1\n558#2:741\n216#3:742\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup\n*L\n-1#1:741\n-1#1:742\n*E\n"})
    public static final class ScaffoldRotationValueGroup
    extends RotationsValueGroup {
        @NotNull
        public static final ScaffoldRotationValueGroup INSTANCE;
        static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
        @NotNull
        private static final Value considerInventory$delegate;
        @NotNull
        private static final ChoiceListValue rotationTiming$delegate;

        private ScaffoldRotationValueGroup() {
            super((EventListener)INSTANCE, null, false, 6, null);
        }

        public final boolean getConsiderInventory() {
            return (Boolean)considerInventory$delegate.getValue((Object)this, $$delegatedProperties[0]);
        }

        @NotNull
        public final RotationTimingMode getRotationTiming() {
            return (RotationTimingMode)((Object)rotationTiming$delegate.getValue((Object)this, $$delegatedProperties[1]));
        }

        /*
         * WARNING - void declaration
         */
        static {
            void name$iv;
            void this_$iv;
            ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldRotationValueGroup.class, "considerInventory", "getConsiderInventory()Z", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldRotationValueGroup.class, "rotationTiming", "getRotationTiming()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", 0)))};
            $$delegatedProperties = valueGroup;
            INSTANCE = new ScaffoldRotationValueGroup();
            considerInventory$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"ConsiderInventory", (boolean)false, null, (int)4, null);
            valueGroup = (ValueGroup)INSTANCE;
            String string = "RotationTiming";
            Enum default$iv = RotationTimingMode.NORMAL;
            boolean $i$f$enumChoice = false;
            Tagged tagged = (Tagged)default$iv;
            boolean $i$f$enumSetAllOf = false;
            EnumSet<RotationTimingMode> enumSet = EnumSet.allOf(RotationTimingMode.class);
            Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
            rotationTiming$delegate = this_$iv.enumChoice((String)name$iv, tagged, (Set)enumSet);
        }

        @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000b\u00a8\u0006\f"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "NORMAL", "ON_TICK", "ON_TICK_SNAP", "liquidbounce"})
        public static final class RotationTimingMode
        extends Enum<RotationTimingMode>
        implements Tagged {
            @NotNull
            private final String tag;
            public static final /* enum */ RotationTimingMode NORMAL = new RotationTimingMode("Normal");
            public static final /* enum */ RotationTimingMode ON_TICK = new RotationTimingMode("OnTick");
            public static final /* enum */ RotationTimingMode ON_TICK_SNAP = new RotationTimingMode("OnTickSnap");
            private static final /* synthetic */ RotationTimingMode[] $VALUES;
            private static final /* synthetic */ EnumEntries $ENTRIES;

            private RotationTimingMode(String tag) {
                this.tag = tag;
            }

            @NotNull
            public String getTag() {
                return this.tag;
            }

            public static RotationTimingMode[] values() {
                return (RotationTimingMode[])$VALUES.clone();
            }

            public static RotationTimingMode valueOf(String value) {
                return Enum.valueOf(RotationTimingMode.class, value);
            }

            @NotNull
            public static EnumEntries<RotationTimingMode> getEntries() {
                return $ENTRIES;
            }

            static {
                $VALUES = rotationTimingModeArray = new RotationTimingMode[]{RotationTimingMode.NORMAL, RotationTimingMode.ON_TICK, RotationTimingMode.ON_TICK_SNAP};
                $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
            }
        }
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u00c2\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u000f"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SimulatePlacementAttempts;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "clicker", "Lnet/ccbluex/liquidbounce/utils/clicking/Clicker;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold;", "getClicker", "()Lnet/ccbluex/liquidbounce/utils/clicking/Clicker;", "failedAttemptsOnly", "", "getFailedAttemptsOnly", "()Z", "failedAttemptsOnly$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "liquidbounce"})
    private static final class SimulatePlacementAttempts
    extends ToggleableValueGroup {
        @NotNull
        public static final SimulatePlacementAttempts INSTANCE;
        static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
        @NotNull
        private static final Clicker<ModuleScaffold> clicker;
        @NotNull
        private static final Value failedAttemptsOnly$delegate;

        private SimulatePlacementAttempts() {
            super((EventListener)INSTANCE, "SimulatePlacementAttempts", false, null, 8, null);
        }

        @NotNull
        public final Clicker<ModuleScaffold> getClicker() {
            return clicker;
        }

        public final boolean getFailedAttemptsOnly() {
            return (Boolean)failedAttemptsOnly$delegate.getValue((Object)this, $$delegatedProperties[0]);
        }

        static {
            KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(SimulatePlacementAttempts.class, "failedAttemptsOnly", "getFailedAttemptsOnly()Z", 0)))};
            $$delegatedProperties = kPropertyArray;
            INSTANCE = new SimulatePlacementAttempts();
            EventListener eventListener = (EventListener)INSTANCE;
            KeyMapping keyMapping = SimulatePlacementAttempts.INSTANCE.getMc().options.keyUse;
            Intrinsics.checkNotNullExpressionValue((Object)keyMapping, (String)"keyUse");
            clicker = (Clicker)INSTANCE.tree((ValueGroup)new Clicker(eventListener, keyMapping, null, 100, null, false, 48, null));
            failedAttemptsOnly$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"FailedAttemptsOnly", (boolean)true, null, (int)4, null);
        }
    }
}

