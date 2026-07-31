package xd.harm.utils;

import xd.harm.Harmony;
import xd.harm.utils.client.IMinecraft;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@UtilityClass
public class SoundUtil implements IMinecraft {

    private static AudioInputStream stream;
    private static final List<Clip> CLIPS_LIST = new ArrayList<>();
    public static final ResourceLocation YUNYUN_ENEMY_SOUND = new ResourceLocation("harmony.yunyun_enemy");
    public static final ResourceLocation YUNYUN_LOW_HP_SOUND = new ResourceLocation("harmony.yunyun_lowhp");
    public static final ResourceLocation YUNYUN_TOTEM_SOUND = new ResourceLocation("harmony.yunyun_totem");
    public static final ResourceLocation YUNYUN_SUGGEST_SOUND = new ResourceLocation("harmony.yunyun_suggest");
    public static final String YUNYUN_ENEMY_WAV = "yunyun_enemy.wav";
    public static final String YUNYUN_LOW_HP_WAV = "yunyun_lowhp.wav";
    public static final String YUNYUN_TOTEM_WAV = "yunyun_totem.wav";
    public static final String YUNYUN_SUGGEST_WAV = "yunyun_suggest.wav";

    public static void playRegisteredSound(ResourceLocation soundLocation, float volume) {
        if (soundLocation == null || volume <= 0.0F) {
            return;
        }
        float safeVolume = Math.max(0.0F, Math.min(volume, 1.0F));
        mc.getSoundHandler().play(SimpleSound.master(new SoundEvent(soundLocation), 1.0F, safeVolume));
    }

    public static void playYunyunSound(ResourceLocation soundLocation, float volume) {
        if (YUNYUN_ENEMY_SOUND.equals(soundLocation)) {
            playClientSound(YUNYUN_ENEMY_WAV, volume);
        } else if (YUNYUN_LOW_HP_SOUND.equals(soundLocation)) {
            playClientSound(YUNYUN_LOW_HP_WAV, volume);
        } else if (YUNYUN_TOTEM_SOUND.equals(soundLocation)) {
            playClientSound(YUNYUN_TOTEM_WAV, volume);
        } else if (YUNYUN_SUGGEST_SOUND.equals(soundLocation)) {
            playClientSound(YUNYUN_SUGGEST_WAV, volume);
        }
    }

    public static void playClientSound(String location, float volume) {
        if (location == null || location.isEmpty() || volume <= 0.0F) {
            return;
        }

        try {
            Clip clip = AudioSystem.getClip();
            InputStream inputStream = mc.getResourceManager().getResource(new ResourceLocation("harmony/sounds/" + location)).getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
            if (audioInputStream == null) {
                System.out.println("Sound not found!");
                return;
            }

            clip.open(audioInputStream);
            clip.start();
            setClipVolume(clip, volume);
            CLIPS_LIST.add(clip);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    CLIPS_LIST.remove(clip);
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void setClipVolume(Clip clip, float volume) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float safeVolume = Math.max(0.0001F, Math.min(volume, 1.0F));
        float dbValue = (float) (Math.log10(safeVolume) * 20.0D);
        dbValue = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), dbValue));
        volumeControl.setValue(dbValue);
    }

    public static void playSound(final String location, double volume, boolean checkClientTune) {
        if (checkClientTune && (!Harmony.getInstance().getModuleManager().getClientTune().isState() || !Harmony.getInstance().getModuleManager().getClientTune().other.get())) return;
        List<Clip> mutableClips = new ArrayList<>(CLIPS_LIST);
        mutableClips.stream().filter(Objects::nonNull).filter(Line::isOpen).filter(clip -> !clip.isRunning()).forEach(Clip::close);
        mutableClips.stream().filter(Objects::nonNull).filter(clip -> !(clip.isOpen() && clip.isRunning())).forEach(Clip::stop);
        mutableClips.removeIf(clip -> !clip.isRunning());
        try {
            stream = AudioSystem.getAudioInputStream(new BufferedInputStream(SoundUtil.class.getResourceAsStream("/assets/minecraft/harmony/sounds/" + location)));
        } catch (final Exception ignored) {
        }
        assert stream != null;
        try {
            mutableClips.add(AudioSystem.getClip());
        } catch (final Exception exception) {
            System.out.println("Client:SoundUtil:" + exception.getMessage());
        }
        mutableClips.stream().filter(Objects::nonNull).filter(clip -> !clip.isOpen()).forEach(clip -> {
            try {
                clip.open(stream);
            } catch (final Exception ignored) {
            }
        });
        mutableClips.stream().filter(Objects::nonNull).filter(Clip::isOpen).forEach(clip -> {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            int dbValue = (int) (Math.log((volume < 0.D ? 0.D : Math.min(volume, 1.D)) * .5D) / Math.log(10.D) * 20.D);
            volumeControl.setValue(dbValue);
        });
        mutableClips.stream().filter(Objects::nonNull).filter(Clip::isOpen).filter(clip -> !clip.isRunning()).forEach(Clip::start);
    }


    public static void playSound(final String location, double volume) {
        playSound(location, volume, true);
    }

    public static void playSound(final String location) {
        playSound(location, .25D, true);
    }

    public static class AudioClipPlayController {
        private final AudioClip audioClip;
        private Supplier<Boolean> playIf;
        private boolean stopIsAPause;
        private boolean started;

        private AudioClipPlayController(AudioClip audioClip, Supplier<Boolean> playIf, boolean stopIsAPause) {
            this.audioClip = audioClip;
            this.playIf = playIf;
            this.stopIsAPause = stopIsAPause;
        }

        public static AudioClipPlayController build(AudioClip audioClip, Supplier<Boolean> playIf, boolean stopIsAPause) {
            return new AudioClipPlayController(audioClip, playIf, stopIsAPause);
        }

        public void setPlayIf(Supplier<Boolean> playIf) {
            this.playIf = playIf;
        }

        public void setStopIsAPauseMode(boolean stopIsAPause) {
            this.stopIsAPause = stopIsAPause;
        }

        public void updatePlayingStatus() {
            if (started && audioClip.clip == null && playIf.get()) {
                started = false;
            }
            if (!started && playIf.get()) {
                audioClip.startPlayingAudio();
                started = true;
            }
            if (stopIsAPause) {
                audioClip.setPause(!playIf.get());
                return;
            }
            if (audioClip.isPlaying() != playIf.get()) {
                if (playIf.get()) audioClip.startPlayingAudio();
                else audioClip.stopPlayingAudio();
            }
        }

        public AudioClip getAudioClip() {
            return this.audioClip;
        }

        public boolean isSucessPlaying() {
            return this.audioClip.isPlaying();
        }
    }

    public static class AudioClip {
        private final boolean loop;
        private boolean pause;
        private long currentPlayTime;
        @Getter
        private String soundName;
        private Clip clip;

        private AudioClip(String soundName, boolean loop) {
            this.soundName = soundName;
            this.loop = loop;
        }

        public static AudioClip build(String soundName, boolean loop) {
            return new AudioClip(soundName, loop);
        }

        public boolean isPlaying() {
            return this.clip != null && this.clip.isOpen() && this.clip.isRunning();
        }

        public void changeAudioTrack(String soundName) {
            this.soundName = soundName;
            stopPlayingAudio();
            startPlayingAudio();
        }

        public void setLoop(boolean loop) {
            if (this.clip == null) return;
            this.clip.loop(loop ? Clip.LOOP_CONTINUOUSLY : 0);
        }

        public boolean isLoop() {
            return this.loop && clip != null && clip.isOpen();
        }

        public void setPause(boolean pause) {
            if (this.pause != pause && clip != null && clip.isOpen() && clip.getMicrosecondLength() != 0) {
                if (pause) {
                    currentPlayTime = clip.getMicrosecondPosition();
                    clip.stop();
                } else {
                    clip.setMicrosecondPosition(currentPlayTime);
                    this.setVolume(this.getVolume());
                    this.setLoop(this.isLoop());
                    clip.start();
                }
                this.pause = pause;
            }
        }

        public boolean isPaused() {
            return this.pause && clip != null && !clip.isRunning();
        }

        public void setVolume(float volume) {
            if (this.clip == null) return;
            double dbValue = Math.log(volume * 0.5D) / Math.log(10.D) * 20.D;
            FloatControl control = ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN));
            if (control.getValue() != (int) dbValue) control.setValue((int) dbValue);
        }

        private float getVolume() {
            FloatControl control = ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN));
            return control.getValue();
        }

        public void startPlayingAudio() {
            this.stopPlayingAudio();
            try {
                this.clip = AudioSystem.getClip();
                String resourcePath = "/assets/minecraft/harmony/sounds/" + this.soundName;
                InputStream audioSrc = SoundUtil.class.getResourceAsStream(resourcePath);
                assert audioSrc != null;
                try {
                    BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(bufferedIn);
                    clip.open(inputStream);
                    this.setVolume(this.getVolume());
                    this.setLoop(this.isLoop());
                    clip.start();
                } catch (Exception exception) {
                    System.out.println(exception.getLocalizedMessage());
                }
            } catch (Exception exception) {
                System.out.println(exception.getLocalizedMessage());
            }
        }

        public void stopPlayingAudio() {
            if (this.clip == null) return;
            if (this.clip.isRunning()) this.clip.stop();
            if (this.clip.isOpen()) this.clip.close();
            this.clip = null;
        }
    }
}
