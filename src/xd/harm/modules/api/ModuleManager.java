package xd.harm.modules.api;

import xd.harm.modules.impl.combat.*;
import xd.harm.modules.impl.misc.*;
import xd.harm.modules.impl.movement.*;
import xd.harm.modules.impl.player.*;
import xd.harm.modules.impl.render.*;
import com.google.common.eventbus.Subscribe;

import xd.harm.Harmony;
import xd.harm.events.input.EventKey;

import xd.harm.utils.render.font.Font;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Getter
public class ModuleManager {
    private final List<Module> modules = new CopyOnWriteArrayList<>();

    private PlayerHelper playerHelper;
    private NoServerDesync noServerDesync;
    private HitAura hitAura;
    private AutoGapple autoGapple;
    private AutoSprint autoSprint;
    private WaterSpeed WaterSpeed;
    private ShaderHand ShaderHand;
    private BaseFinder BaseFinder;
    private NoRender noRender;
    private InventoryPlus inventoryPlus;
    private ShieldBreaker ShieldBreaker;
    private ElytraHelper elytrahelper;
    private AutoBuy autoBuy;
    private PotionThrower autopotion;
    private TriggerBot triggerbot;
    private ClickFriend clickfriend;
    private TargetESP TargetESP;
    private FTHelper FTHelper;
    private ItemCooldown itemCooldown;
    private ClickPearl clickPearl;
    private AutoSwap autoSwap;
    private EntityBox entityBox;
    private AntiPush antiPush;
    private FreeCam freeCam;
    private ChestStealer chestStealer;
    private AutoLeave autoLeave;
    private AutoRegister autoRegister;
    private AutoSkin autoSkin;
    private AutoSolver autoSolver;
    private ClientTune clientTune;
    private Assistant assistant;
    private AutoThrow AutoThrow;
    private PlayerFinder PlayerFinder;
    private AutoTotem autoTotem;
    private Crosshair crosshair;
    private Particles particles;
    private JumpCircle jumpCircle;
    private ItemPhysic itemPhysic;
    private Predictions predictions;
    private NoEntityTrace noEntityTrace;
    private Spider spider;
    private StreamerMode StreamerMode;
    private NoInteract noInteract;
    private Visuality visuality;
    private SeeInvisibles seeInvisibles;
    private NoFriendHurt noFriendHurt;
    private ClickGui clickGui;
    private WorldTweaks worldTweaks;
    private EnchantmentColor enchantmentColor;
    private Arrows arrows;
    private Nuker Nuker;
    private ChatHelper chatHelper;
    private Notifications Notifications;
    private AutoActions autoActions;
    private AutoBrew AutoBrew;
    private FullBright fullBright;
    private SwingAnimation swingAnimation;
    private HUD hud;
    private AhHelper ahHelper;
    private AimHelper AimHelper;
    private FastEXP fastEXP;
    private TpLootMyst TpLootMyst;
    private HitMarkers HitMarkers;
    private RetroDamage damagePopupRetro;
    private HitBubble hitBubble;
    private LineGlyphs lineGlyphs;
    private WavePlayer WavePlayer;
    private FiguraCosmetic figuraCosmetic;
    private NameTags nameTags;
    private ShulkerVisible ShulkerVisible;
    private FakeLag FakeLag;
    private Theme theme;
    private AutoPilot AutoPilot;
    private MineViewer MineViewer;
    private TestPlayer TestPlayer;
    private DragonFlight DragonFlight;
    private GuiMove GuiMove;
    private AntiBot antiBot;
    private LootPickup LootPickup;
    private ElytraFly ElytraFly;
    private ElytraBooster ElytraBooster;
    private ElytraTarget ElytraTarget;
    private ElytraMotion ElytraMotion;
    private ElytraPredict ElytraPredict;
    private AntiTarget AntiTarget;
    private SpookyJoiner SpookyJoiner;
    private AutoExplosion AutoExplosion;
    private NoSlow noSlow;
    private LootBubbles LootBubbles;
    private AirStuck airStuck;
    private VoiceChat VoiceChat;
    private AttackEffects AttackEffects;
    private TrashTalk trashTalk;
    private Velocity Velocity;
    private Scaffold Scaffold;
    private ScaffoldLiquid ScaffoldLiquid;
    private ScaffoldLiquidTwo ScaffoldLiquidTwo;
    private TimerModule Timer;
    private MovementFix movementFix;
    private Flight Flight;
    private Disabler Disabler;
    private AutoBuyMLegacy autoBuyMLegacy;
    public void init() {
        registerAll(
                (Module) (noSlow = new NoSlow()),
                VoiceChat = new VoiceChat(),
                AttackEffects = new AttackEffects(),
                trashTalk = new TrashTalk(),
                Velocity = new Velocity(),
                Scaffold = new Scaffold(),
                ScaffoldLiquid = new ScaffoldLiquid(),
                ScaffoldLiquidTwo = new ScaffoldLiquidTwo(),
                Timer = new TimerModule(),
                autoBuyMLegacy = new AutoBuyMLegacy(),
                movementFix = new MovementFix(),
                Flight = new Flight(),
                Disabler = new Disabler(),
                Notifications = new Notifications(),
                (Module) (autopotion = new PotionThrower()),
                GuiMove = new GuiMove(),
                HitMarkers = new HitMarkers(),
                damagePopupRetro = new RetroDamage(),
                hitBubble = new HitBubble(),
                lineGlyphs = new LineGlyphs(),
                AutoExplosion = new AutoExplosion(),
                WavePlayer = new WavePlayer(),
                FakeLag = new FakeLag(),
                particles = new Particles(),
                LootBubbles = new LootBubbles(),
                ShieldBreaker = new ShieldBreaker(),
                MineViewer = new MineViewer(),
                theme = new Theme(),
                TpLootMyst = new TpLootMyst(),
                LootPickup = new LootPickup(),
                nameTags = new NameTags(),
                fastEXP = new FastEXP(),
                autoBuy = new AutoBuy(),
                ahHelper = new AhHelper(),
                ShaderHand = new ShaderHand(),
                Nuker = new Nuker(),
                BaseFinder = new BaseFinder(),
                hud = new HUD(),
                swingAnimation = new SwingAnimation(),
                assistant = new Assistant(),
                AutoThrow = new AutoThrow(),
                fullBright = new FullBright(),
                AimHelper = new AimHelper(),
                PlayerFinder = new PlayerFinder(),
                autoActions = new AutoActions(),
                TargetESP = new TargetESP(),
                chatHelper = new ChatHelper(),
                arrows = new Arrows(),
                WaterSpeed = new WaterSpeed(),
                noServerDesync = new NoServerDesync(),
                playerHelper = new PlayerHelper(),
                worldTweaks = new WorldTweaks(),
                enchantmentColor = new EnchantmentColor(),
                AutoBrew = new AutoBrew(),
                clickGui = new ClickGui(),
                noFriendHurt = new NoFriendHurt(),
                autoGapple = new AutoGapple(),
                autoSprint = new AutoSprint(),
                noRender = new NoRender(),
                inventoryPlus = new InventoryPlus(),
                seeInvisibles = new SeeInvisibles(),
                elytrahelper = new ElytraHelper(),
                antiBot = new AntiBot(),
                figuraCosmetic = new FiguraCosmetic(),
                triggerbot = new TriggerBot(),
                clickfriend = new ClickFriend(),
                FTHelper = new FTHelper(),
                entityBox = new EntityBox(),
                antiPush = new AntiPush(),
                freeCam = new FreeCam(),
                chestStealer = new ChestStealer(),
                autoLeave = new AutoLeave(),
                autoRegister = new AutoRegister(),
                autoSkin = new AutoSkin(),
                autoSolver = new AutoSolver(),
                clientTune = new ClientTune(),
                crosshair = new Crosshair(),
                autoTotem = new AutoTotem(),
                itemCooldown = new ItemCooldown(),
                hitAura = new HitAura(),
                clickPearl = new ClickPearl(itemCooldown),
                autoSwap = new AutoSwap(),
                TestPlayer = new TestPlayer(),
                jumpCircle = new JumpCircle(),
                itemPhysic = new ItemPhysic(),
                predictions = new Predictions(),
                noEntityTrace = new NoEntityTrace(),
                DragonFlight = new DragonFlight(),
                AutoPilot = new AutoPilot(),
                airStuck = new AirStuck(),
                spider = new Spider(),
                StreamerMode = new StreamerMode(),
                noInteract = new NoInteract(),
                SpookyJoiner = new SpookyJoiner(),
                visuality = new Visuality(),
                ShulkerVisible = new ShulkerVisible(),
                ElytraBooster = new ElytraBooster(),
                ElytraTarget = new ElytraTarget(),
                ElytraMotion = new ElytraMotion(),
                ElytraPredict = new ElytraPredict(),
                AntiTarget = new AntiTarget(),
                ElytraFly = new ElytraFly(),
                new BWJoinHelper(),
                new BWAutoLeave(),
                new BotAttack(),
                new InvManager());

        sortModulesAlphabetically();

        Harmony.getInstance().getEventBus().register(this);
    }

    private void registerAll(Module... modules) {
        this.modules.addAll(List.of(modules));
    }

    private void sortModulesAlphabetically() {
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Module> getSorted(Font font, float size) {
        return modules.stream()
                .sorted((f1, f2) -> Float.compare(font.getWidth(f2.getName(), size), font.getWidth(f1.getName(), size)))
                .toList();
    }

    public List<Module> getSortedAlphabetically() {
        return modules.stream()
                .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public int countEnabledModules() {
        int enabledModules = 0;
        for (Module module : modules) {
            if (module.isState()) {
                enabledModules++;
            }
        }
        return enabledModules;
    }

    public List<Module> getModules() {
        return modules;
    }

    public PlayerHelper getPlayerHelper() {
        return playerHelper;
    }

    public AutoBuy getAutoBuy() {
        return autoBuy;
    }

    public AutoRegister getAutoRegister() {
        return autoRegister;
    }

    public AutoSkin getAutoSkin() {
        return autoSkin;
    }

    public AutoSolver getAutoSolver() {
        return autoSolver;
    }

    public AutoSwap getAutoSwap() {
        return autoSwap;
    }

    public AntiPush getAntiPush() {
        return antiPush;
    }

    public FreeCam getFreeCam() {
        return freeCam;
    }

    public ClientTune getClientTune() {
        return clientTune;
    }

    public ItemCooldown getItemCooldown() {
        return itemCooldown;
    }

    public Particles getParticles() {
        return particles;
    }

    public WorldTweaks getWorldTweaks() {
        return worldTweaks;
    }

    public EnchantmentColor getEnchantmentColor() {
        return enchantmentColor;
    }

    public Notifications getNotifications() {
        return Notifications;
    }

    public AutoActions getAutoActions() {
        return autoActions;
    }

    public HUD getHud() {
        return hud;
    }

    public StreamerMode getStreamerMode() {
        return StreamerMode;
    }

    public Visuality getVisuality() {
        return visuality;
    }

    public NoRender getNoRender() {
        return noRender;
    }

    public AirStuck getAirStuck() {
        return airStuck;
    }

    public NoServerDesync getNoServerDesync() {
        return noServerDesync;
    }

    public AutoGapple getAutoGapple() {
        return autoGapple;
    }

    public AutoSprint getAutoSprint() {
        return autoSprint;
    }

    public WaterSpeed getWaterSpeed() {
        return WaterSpeed;
    }

    public ShaderHand getShaderHand() {
        return ShaderHand;
    }

    public BaseFinder getBaseFinder() {
        return BaseFinder;
    }

    public InventoryPlus getInventoryPlus() {
        return inventoryPlus;
    }

    public ShieldBreaker getShieldBreaker() {
        return ShieldBreaker;
    }

    public ElytraHelper getElytrahelper() {
        return elytrahelper;
    }

    public PotionThrower getAutopotion() {
        return autopotion;
    }

    public TriggerBot getTriggerbot() {
        return triggerbot;
    }

    public ClickFriend getClickfriend() {
        return clickfriend;
    }

    public TargetESP getTargetESP() {
        return TargetESP;
    }

    public ClickPearl getClickPearl() {
        return clickPearl;
    }

    public EntityBox getEntityBox() {
        return entityBox;
    }

    public ChestStealer getChestStealer() {
        return chestStealer;
    }

    public AutoLeave getAutoLeave() {
        return autoLeave;
    }

    public AutoThrow getAutoThrow() {
        return AutoThrow;
    }

    public PlayerFinder getPlayerFinder() {
        return PlayerFinder;
    }

    public AutoTotem getAutoTotem() {
        return autoTotem;
    }

    public Crosshair getCrosshair() {
        return crosshair;
    }

    public JumpCircle getJumpCircle() {
        return jumpCircle;
    }

    public ItemPhysic getItemPhysic() {
        return itemPhysic;
    }

    public Predictions getPredictions() {
        return predictions;
    }

    public NoEntityTrace getNoEntityTrace() {
        return noEntityTrace;
    }

    public NameTags getNameTags() {
        return nameTags;
    }

    public Spider getSpider() {
        return spider;
    }

    public NoInteract getNoInteract() {
        return noInteract;
    }

    public SeeInvisibles getSeeInvisibles() {
        return seeInvisibles;
    }

    public NoFriendHurt getNoFriendHurt() {
        return noFriendHurt;
    }

    public ClickGui getClickGui() {
        return clickGui;
    }

    public Arrows getArrows() {
        return arrows;
    }

    public Nuker getNuker() {
        return Nuker;
    }

    public ChatHelper getChatHelper() {
        return chatHelper;
    }

    public AutoBrew getAutoBrew() {
        return AutoBrew;
    }

    public FullBright getFullBright() {
        return fullBright;
    }

    public SwingAnimation getSwingAnimation() {
        return swingAnimation;
    }

    public AhHelper getAhHelper() {
        return ahHelper;
    }

    public AimHelper getAimHelper() {
        return AimHelper;
    }

    public FastEXP getFastEXP() {
        return fastEXP;
    }

    public TpLootMyst getTpLootMyst() {
        return TpLootMyst;
    }

    public HitMarkers getHitMarkers() {
        return HitMarkers;
    }

    public RetroDamage getDamagePopupRetro() {
        return damagePopupRetro;
    }

    public HitBubble getHitBubble() {
        return hitBubble;
    }

    public LineGlyphs getLineGlyphs() {
        return lineGlyphs;
    }

    public ShulkerVisible getShulkerVisible() {
        return ShulkerVisible;
    }

    public FakeLag getFakeLag() {
        return FakeLag;
    }

    public Theme getTheme() {
        return theme;
    }

    public AutoPilot getAutoPilot() {
        return AutoPilot;
    }

    public MineViewer getMineViewer() {
        return MineViewer;
    }

    public TestPlayer getTestPlayer() {
        return TestPlayer;
    }

    public DragonFlight getDragonFlight() {
        return DragonFlight;
    }

    public GuiMove getGuiMove() {
        return GuiMove;
    }

    public AntiBot getAntiBot() {
        return antiBot;
    }

    public LootPickup getLootPickup() {
        return LootPickup;
    }

    public ElytraFly getElytraFly() {
        return ElytraFly;
    }

    public ElytraBooster getElytraBooster() {
        return ElytraBooster;
    }

    public Assistant getAssistant() {
        return assistant;
    }

    public HitAura getHitAura() {
        return hitAura;
    }

    public FTHelper getFTHelper() {
        return FTHelper;
    }

    public WavePlayer getWavePlayer() {
        return WavePlayer;
    }


    public ElytraTarget getElytraTarget() {
        return ElytraTarget;
    }

    public ElytraMotion getElytraMotion() {
        return ElytraMotion;
    }

    public ElytraPredict getElytraPredict() {
        return ElytraPredict;
    }

    public AntiTarget getAntiTarget() {
        return AntiTarget;
    }

    public SpookyJoiner getSpookyJoiner() {
        return SpookyJoiner;
    }

    public AutoExplosion getAutoExplosion() {
        return AutoExplosion;
    }

    public NoSlow getNoSlow() {
        return noSlow;
    }

    public LootBubbles getLootBubbles() {
        return LootBubbles;
    }

    public VoiceChat getVoiceChat() {
        return VoiceChat;
    }

    public TimerModule getTimer() {
        return Timer;
    }

    public MovementFix getMovementFix() {
        return movementFix;
    }

    public Flight getFlight() {
        return Flight;
    }

    public Disabler getDisabler() {
        return Disabler;
    }

    public TrashTalk getTrashTalk() {
        return trashTalk;
    }

    @Subscribe
    private void onKey(EventKey e) {
        for (Module Module : modules) {
            if (Module.getBind() == e.getKey()) {
                Module.toggle();
            }
        }
    }

    // Explicit getters preserve the original Lombok-generated ModuleManager API.
    public FiguraCosmetic getFiguraCosmetic() { return figuraCosmetic; }
    public AttackEffects getAttackEffects() { return AttackEffects; }
    public Velocity getVelocity() { return Velocity; }
    public Scaffold getScaffold() { return Scaffold; }
    public ScaffoldLiquid getScaffoldLiquid() { return ScaffoldLiquid; }
    public ScaffoldLiquidTwo getScaffoldLiquidTwo() { return ScaffoldLiquidTwo; }
    public AutoBuyMLegacy getAutoBuyMLegacy() { return autoBuyMLegacy; }
}
