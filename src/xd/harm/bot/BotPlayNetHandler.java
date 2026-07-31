package xd.harm.bot;

import io.netty.buffer.Unpooled;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.network.play.NetworkPlayerInfoAccessor;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;

public class BotPlayNetHandler implements IClientPlayNetHandler {
    private final BotSessionManager manager;
    private final BotSession session;
    private final NetworkManager networkManager;
    private final Minecraft mc = Minecraft.getInstance();

    public BotPlayNetHandler(BotSessionManager manager, BotSession session, NetworkManager networkManager) {
        this.manager = manager;
        this.session = session;
        this.networkManager = networkManager;
    }

    private void onMainThread(Runnable runnable) {
        if (mc.isOnExecutionThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    private void sendPacket(IPacket<?> packet) {
        networkManager.sendPacket(packet);
    }

    private int getModelPartFlags() {
        int flags = 0;
        for (PlayerModelPart part : mc.gameSettings.getModelParts()) {
            flags |= part.getPartMask();
        }
        return flags;
    }

    @Override
    public void handleJoinGame(SJoinGamePacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            session.setPlayerEntityId(packetIn.getPlayerId());
            session.setState(BotSession.State.PLAY, "In game");
            session.clearHudData();
            sendPacket(new CClientSettingsPacket(
                    mc.gameSettings.language,
                    mc.gameSettings.renderDistanceChunks,
                    mc.gameSettings.chatVisibility,
                    mc.gameSettings.chatColor,
                    getModelPartFlags(),
                    mc.gameSettings.mainHand
            ));
            sendPacket(new CCustomPayloadPacket(
                    CCustomPayloadPacket.BRAND,
                    (new PacketBuffer(Unpooled.buffer())).writeString(ClientBrandRetriever.getClientModName())
            ));
        });
    }

    @Override
    public void handleKeepAlive(SKeepAlivePacket packetIn) {
        session.touchPacket();
        sendPacket(new CKeepAlivePacket(packetIn.getId()));
    }

    @Override
    public void handlePlayerPosLook(SPlayerPositionLookPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            double x = packetIn.getX();
            double y = packetIn.getY();
            double z = packetIn.getZ();
            float yaw = packetIn.getYaw();
            float pitch = packetIn.getPitch();

            if (packetIn.getFlags().contains(SPlayerPositionLookPacket.Flags.X)) {
                x += session.getPosX();
            }
            if (packetIn.getFlags().contains(SPlayerPositionLookPacket.Flags.Y)) {
                y += session.getPosY();
            }
            if (packetIn.getFlags().contains(SPlayerPositionLookPacket.Flags.Z)) {
                z += session.getPosZ();
            }
            if (packetIn.getFlags().contains(SPlayerPositionLookPacket.Flags.Y_ROT)) {
                yaw += session.getYaw();
            }
            if (packetIn.getFlags().contains(SPlayerPositionLookPacket.Flags.X_ROT)) {
                pitch += session.getPitch();
            }

            session.updatePosition(x, y, z, yaw, pitch, false);
            sendPacket(new CConfirmTeleportPacket(packetIn.getTeleportId()));
            sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, false));
        });
    }

    @Override
    public void handleDisconnect(SDisconnectPacket packetIn) {
        session.touchPacket();
        this.networkManager.closeChannel(packetIn.getReason());
        manager.onSessionDisconnected(session, packetIn.getReason().getString());
    }

    @Override
    public void handleChat(SChatPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> manager.onBotChat(session, packetIn.getChatComponent()));
    }

    @Override
    public void handleRespawn(SRespawnPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            session.clearPosition();
            session.setState(BotSession.State.PLAY, "Respawned");
        });
    }

    @Override
    public void handlePlayerListHeaderFooter(SPlayerListHeaderFooterPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            session.setTabHeader(packetIn.getHeader().getString().isEmpty() ? null : packetIn.getHeader());
            session.setTabFooter(packetIn.getFooter().getString().isEmpty() ? null : packetIn.getFooter());
        });
    }

    @Override
    public void handlePlayerListItem(SPlayerListItemPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            synchronized (session.getLock()) {
                for (SPlayerListItemPacket.AddPlayerData entry : packetIn.getEntries()) {
                    if (packetIn.getAction() == SPlayerListItemPacket.Action.REMOVE_PLAYER) {
                        session.getPlayerInfoMap().remove(entry.getProfile().getId());
                        continue;
                    }

                    NetworkPlayerInfo info = session.getPlayerInfoMap().get(entry.getProfile().getId());
                    if (packetIn.getAction() == SPlayerListItemPacket.Action.ADD_PLAYER) {
                        info = new NetworkPlayerInfo(entry);
                        session.getPlayerInfoMap().put(info.getGameProfile().getId(), info);
                    }

                    if (info == null) {
                        continue;
                    }

                    switch (packetIn.getAction()) {
                        case ADD_PLAYER:
                            NetworkPlayerInfoAccessor.setGameType(info, entry.getGameMode());
                            NetworkPlayerInfoAccessor.setResponseTime(info, entry.getPing());
                            info.setDisplayName(entry.getDisplayName());
                            break;
                        case UPDATE_GAME_MODE:
                            NetworkPlayerInfoAccessor.setGameType(info, entry.getGameMode());
                            break;
                        case UPDATE_LATENCY:
                            NetworkPlayerInfoAccessor.setResponseTime(info, entry.getPing());
                            break;
                        case UPDATE_DISPLAY_NAME:
                            info.setDisplayName(entry.getDisplayName());
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void handleUpdateBossInfo(SUpdateBossInfoPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            synchronized (session.getLock()) {
                if (packetIn.getOperation() == SUpdateBossInfoPacket.Operation.ADD) {
                    session.getBossInfos().put(packetIn.getUniqueId(), new net.minecraft.client.gui.ClientBossInfo(packetIn));
                } else if (packetIn.getOperation() == SUpdateBossInfoPacket.Operation.REMOVE) {
                    session.getBossInfos().remove(packetIn.getUniqueId());
                } else {
                    net.minecraft.client.gui.ClientBossInfo info = session.getBossInfos().get(packetIn.getUniqueId());
                    if (info != null) {
                        info.updateFromPacket(packetIn);
                    }
                }
            }
        });
    }

    @Override
    public void handleScoreboardObjective(SScoreboardObjectivePacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            Scoreboard scoreboard = session.getScoreboard();
            String name = packetIn.getObjectiveName();

            if (packetIn.getAction() == 0) {
                scoreboard.addObjective(name, ScoreCriteria.DUMMY, packetIn.getDisplayName(), packetIn.getRenderType());
            } else if (scoreboard.hasObjective(name)) {
                ScoreObjective objective = scoreboard.getObjective(name);
                if (packetIn.getAction() == 1) {
                    scoreboard.removeObjective(objective);
                } else if (packetIn.getAction() == 2) {
                    objective.setRenderType(packetIn.getRenderType());
                    objective.setDisplayName(packetIn.getDisplayName());
                }
            }
        });
    }

    @Override
    public void handleDisplayObjective(SDisplayObjectivePacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            Scoreboard scoreboard = session.getScoreboard();
            String name = packetIn.getName();
            ScoreObjective objective = name == null ? null : scoreboard.getOrCreateObjective(name);
            scoreboard.setObjectiveInDisplaySlot(packetIn.getPosition(), objective);
        });
    }

    @Override
    public void handleUpdateScore(SUpdateScorePacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            Scoreboard scoreboard = session.getScoreboard();
            String objectiveName = packetIn.getObjectiveName();
            if (objectiveName == null) {
                return;
            }

            if (packetIn.getAction() == ServerScoreboard.Action.CHANGE) {
                ScoreObjective objective = scoreboard.getOrCreateObjective(objectiveName);
                Score score = scoreboard.getOrCreateScore(packetIn.getPlayerName(), objective);
                score.setScorePoints(packetIn.getScoreValue());
                return;
            }

            if (packetIn.getAction() == ServerScoreboard.Action.REMOVE) {
                scoreboard.removeObjectiveFromEntity(packetIn.getPlayerName(), scoreboard.getObjective(objectiveName));
            }
        });
    }

    @Override
    public void handleTeams(STeamsPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            Scoreboard scoreboard = session.getScoreboard();
            ScorePlayerTeam team;
            if (packetIn.getAction() == 0) {
                team = scoreboard.createTeam(packetIn.getName());
            } else {
                team = scoreboard.getTeam(packetIn.getName());
            }

            if ((packetIn.getAction() == 0 || packetIn.getAction() == 2) && team != null) {
                team.setDisplayName(packetIn.getDisplayName());
                team.setColor(packetIn.getColor());
                team.setFriendlyFlags(packetIn.getFriendlyFlags());

                Team.Visible visible = Team.Visible.getByName(packetIn.getNameTagVisibility());
                if (visible != null) {
                    team.setNameTagVisibility(visible);
                }

                Team.CollisionRule collisionRule = Team.CollisionRule.getByName(packetIn.getCollisionRule());
                if (collisionRule != null) {
                    team.setCollisionRule(collisionRule);
                }

                team.setPrefix(packetIn.getPrefix());
                team.setSuffix(packetIn.getSuffix());
            }

            if ((packetIn.getAction() == 0 || packetIn.getAction() == 3) && team != null) {
                for (String player : packetIn.getPlayers()) {
                    scoreboard.addPlayerToTeam(player, team);
                }
            }

            if (packetIn.getAction() == 4 && team != null) {
                for (String player : packetIn.getPlayers()) {
                    scoreboard.removePlayerFromTeam(player, team);
                }
            }

            if (packetIn.getAction() == 1 && team != null) {
                scoreboard.removeTeam(team);
            }
        });
    }

    @Override
    public void handleResourcePack(SSendResourcePackPacket packetIn) {
        session.touchPacket();
        sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
        sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
    }

    @Override
    public void onDisconnect(ITextComponent reason) {
        manager.onSessionDisconnected(session, reason.getString());
    }

    @Override
    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    public void handleSpawnObject(SSpawnObjectPacket packetIn) {
    }

    @Override
    public void handleSpawnExperienceOrb(SSpawnExperienceOrbPacket packetIn) {
    }

    @Override
    public void handleSpawnMob(SSpawnMobPacket packetIn) {
    }

    @Override
    public void handleSpawnPainting(SSpawnPaintingPacket packetIn) {
    }

    @Override
    public void handleSpawnPlayer(SSpawnPlayerPacket packetIn) {
        session.touchPacket();
    }

    @Override
    public void handleAnimation(SAnimateHandPacket packetIn) {
    }

    @Override
    public void handleStatistics(SStatisticsPacket packetIn) {
    }

    @Override
    public void handleRecipeBook(SRecipeBookPacket packetIn) {
    }

    @Override
    public void handleBlockBreakAnim(SAnimateBlockBreakPacket packetIn) {
    }

    @Override
    public void handleSignEditorOpen(SOpenSignMenuPacket packetIn) {
    }

    @Override
    public void handleUpdateTileEntity(SUpdateTileEntityPacket packetIn) {
    }

    @Override
    public void handleBlockAction(SBlockActionPacket packetIn) {
    }

    @Override
    public void handleBlockChange(SChangeBlockPacket packetIn) {
    }

    @Override
    public void handleMultiBlockChange(SMultiBlockChangePacket packetIn) {
    }

    @Override
    public void handleMaps(SMapDataPacket packetIn) {
    }

    @Override
    public void handleConfirmTransaction(SConfirmTransactionPacket packetIn) {
    }

    @Override
    public void handleCloseWindow(SCloseWindowPacket packetIn) {
    }

    @Override
    public void handleWindowItems(SWindowItemsPacket packetIn) {
    }

    @Override
    public void handleOpenHorseWindow(SOpenHorseWindowPacket packetIn) {
    }

    @Override
    public void handleWindowProperty(SWindowPropertyPacket packetIn) {
    }

    @Override
    public void handleSetSlot(SSetSlotPacket packetIn) {
    }

    @Override
    public void handleCustomPayload(SCustomPayloadPlayPacket packetIn) {
    }

    @Override
    public void handleEntityStatus(SEntityStatusPacket packetIn) {
    }

    @Override
    public void handleEntityAttach(SMountEntityPacket packetIn) {
    }

    @Override
    public void handleSetPassengers(SSetPassengersPacket packetIn) {
    }

    @Override
    public void handleExplosion(SExplosionPacket packetIn) {
    }

    @Override
    public void handleChangeGameState(SChangeGameStatePacket packetIn) {
    }

    @Override
    public void handleChunkData(SChunkDataPacket packetIn) {
        session.touchPacket();
    }

    @Override
    public void processChunkUnload(SUnloadChunkPacket packetIn) {
    }

    @Override
    public void handleEffect(SPlaySoundEventPacket packetIn) {
    }

    @Override
    public void handleEntityMovement(SEntityPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            if (packetIn.entityId == session.getPlayerEntityId()) {
                double x = session.getPosX();
                double y = session.getPosY();
                double z = session.getPosZ();
                if (packetIn.func_229745_h_() && session.hasPosition()) {
                    Vector3d moved = packetIn.func_244300_a(new Vector3d(session.getPosX(), session.getPosY(), session.getPosZ()));
                    x = moved.x;
                    y = moved.y;
                    z = moved.z;
                }

                float yaw = session.getYaw();
                float pitch = session.getPitch();
                if (packetIn.isRotating()) {
                    yaw = (float) (packetIn.getYaw() * 360) / 256.0F;
                    pitch = (float) (packetIn.getPitch() * 360) / 256.0F;
                }

                session.updatePosition(x, y, z, yaw, pitch, packetIn.getOnGround());
            }
        });
    }

    @Override
    public void handleParticles(SSpawnParticlePacket packetIn) {
    }

    @Override
    public void handlePlayerAbilities(SPlayerAbilitiesPacket packetIn) {
    }

    @Override
    public void handleDestroyEntities(SDestroyEntitiesPacket packetIn) {
    }

    @Override
    public void handleRemoveEntityEffect(SRemoveEntityEffectPacket packetIn) {
    }

    @Override
    public void handleEntityHeadLook(SEntityHeadLookPacket packetIn) {
    }

    @Override
    public void handleHeldItemChange(SHeldItemChangePacket packetIn) {
    }

    @Override
    public void handleEntityMetadata(SEntityMetadataPacket packetIn) {
    }

    @Override
    public void handleEntityVelocity(SEntityVelocityPacket packetIn) {
    }

    @Override
    public void handleEntityEquipment(SEntityEquipmentPacket packetIn) {
    }

    @Override
    public void handleSetExperience(SSetExperiencePacket packetIn) {
    }

    @Override
    public void handleUpdateHealth(SUpdateHealthPacket packetIn) {
    }

    @Override
    public void func_230488_a_(SWorldSpawnChangedPacket p_230488_1_) {
    }

    @Override
    public void handleTimeUpdate(SUpdateTimePacket packetIn) {
    }

    @Override
    public void handleSoundEffect(SPlaySoundEffectPacket packetIn) {
    }

    @Override
    public void handleSpawnMovingSoundEffect(SSpawnMovingSoundEffectPacket packetIn) {
    }

    @Override
    public void handleCustomSound(SPlaySoundPacket packetIn) {
    }

    @Override
    public void handleCollectItem(SCollectItemPacket packetIn) {
    }

    @Override
    public void handleEntityTeleport(SEntityTeleportPacket packetIn) {
        session.touchPacket();
        onMainThread(() -> {
            if (packetIn.getEntityId() != session.getPlayerEntityId()) {
                return;
            }
            float yaw = (float) (packetIn.getYaw() * 360) / 256.0F;
            float pitch = (float) (packetIn.getPitch() * 360) / 256.0F;
            session.updatePosition(packetIn.getX(), packetIn.getY(), packetIn.getZ(), yaw, pitch, packetIn.isOnGround());
        });
    }

    @Override
    public void handleEntityProperties(SEntityPropertiesPacket packetIn) {
    }

    @Override
    public void handleEntityEffect(SPlayEntityEffectPacket packetIn) {
    }

    @Override
    public void handleTags(STagsListPacket packetIn) {
    }

    @Override
    public void handleCombatEvent(SCombatPacket packetIn) {
    }

    @Override
    public void handleServerDifficulty(SServerDifficultyPacket packetIn) {
    }

    @Override
    public void handleCamera(SCameraPacket packetIn) {
    }

    @Override
    public void handleWorldBorder(SWorldBorderPacket packetIn) {
    }

    @Override
    public void handleTitle(STitlePacket packetIn) {
    }

    @Override
    public void handleCooldown(SCooldownPacket packetIn) {
    }

    @Override
    public void handleMoveVehicle(SMoveVehiclePacket packetIn) {
    }

    @Override
    public void handleAdvancementInfo(SAdvancementInfoPacket packetIn) {
    }

    @Override
    public void handleSelectAdvancementsTab(SSelectAdvancementsTabPacket packetIn) {
    }

    @Override
    public void handlePlaceGhostRecipe(SPlaceGhostRecipePacket packetIn) {
    }

    @Override
    public void handleCommandList(SCommandListPacket packetIn) {
    }

    @Override
    public void handleStopSound(SStopSoundPacket packetIn) {
    }

    @Override
    public void handleTabComplete(STabCompletePacket packetIn) {
    }

    @Override
    public void handleUpdateRecipes(SUpdateRecipesPacket packetIn) {
    }

    @Override
    public void handlePlayerLook(SPlayerLookPacket packetIn) {
    }

    @Override
    public void handleNBTQueryResponse(SQueryNBTResponsePacket packetIn) {
    }

    @Override
    public void handleUpdateLight(SUpdateLightPacket packetIn) {
    }

    @Override
    public void handleOpenBookPacket(SOpenBookWindowPacket packetIn) {
    }

    @Override
    public void handleOpenWindowPacket(SOpenWindowPacket packetIn) {
    }

    @Override
    public void handleMerchantOffers(SMerchantOffersPacket packetIn) {
    }

    @Override
    public void handleUpdateViewDistancePacket(SUpdateViewDistancePacket packetIn) {
    }

    @Override
    public void handleChunkPositionPacket(SUpdateChunkPositionPacket packetIn) {
    }

    @Override
    public void handleAcknowledgePlayerDigging(SPlayerDiggingPacket packetIn) {
    }
}
