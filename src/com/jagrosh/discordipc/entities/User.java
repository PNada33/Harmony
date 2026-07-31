package com.jagrosh.discordipc.entities;

/**
 * A encapsulation of a Discord User's data provided when a
 * {@link com.jagrosh.discordipc.IPCListener IPCListener} fires
 * {@link com.jagrosh.discordipc.IPCListener#onActivityJoinRequest(com.jagrosh.discordipc.IPCClient, String, User)
 * onActivityJoinRequest}.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class User {
    private final String name;
    private final String discriminator;
    private final long id;
    private final String avatar;

    /**
     * Constructs a new {@link User}.<br>
     * Only implemented internally.
     * @param name user's name
     * @param discriminator user's discrim (now always "0")
     * @param id user's id
     * @param avatar user's avatar hash, or {@code null} if they have no avatar
     */
    public User(String name, String discriminator, long id, String avatar) {
        this.name = name;
        this.discriminator = discriminator;
        this.id = id;
        this.avatar = avatar;
    }

    /**
     * Gets the Users account name.
     *
     * @return The Users account name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Users full Discord tag.
     *
     * @return The Users full tag (just name for modern Discord)
     */
    public String getDiscordTag() {
        return name;
    }

    /**
     * Gets the Users discriminator.
     *
     * @return The Users discriminator (always "0" now)
     */
    public String getDiscriminator() {
        return "0";
    }

    /**
     * Gets the Users Snowflake ID as a {@code long}.
     *
     * @return The Users Snowflake ID as a {@code long}.
     */
    public long getIdLong() {
        return id;
    }

    /**
     * Gets the Users Snowflake ID as a {@code String}.
     *
     * @return The Users Snowflake ID as a {@code String}.
     */
    public String getId() {
        return Long.toString(id);
    }

    /**
     * Gets the Users avatar ID.
     *
     * @return The Users avatar ID.
     */
    public String getAvatarId() {
        return avatar;
    }

    /**
     * Gets the Users avatar URL.
     *
     * @return The Users avatar URL.
     */
    public String getAvatarUrl() {
        return avatar == null ? null :
                "https://cdn.discordapp.com/avatars/" + getId() + "/" + getAvatarId() +
                        (getAvatarId().startsWith("a_") ? ".gif" : ".png");
    }

    /**
     * Gets the Users avatar URL, or their default avatar URL if they
     * do not have a custom avatar set on their account.
     *
     * @return The Users effective avatar URL.
     */
    public String getEffectiveAvatarUrl() {
        return getAvatarUrl() == null ? getDefaultAvatarUrl() : getAvatarUrl();
    }

    /**
     * Gets the Users default avatar URL.
     *
     * @return The Users default avatar URL.
     */
    public String getDefaultAvatarUrl() {
        return "https://cdn.discordapp.com/embed/avatars/" +
                (Math.abs(id >> 22) % 6) + ".png";
    }

    /**
     * Gets whether or not this User is a bot.
     *
     * @return False
     */
    public boolean isBot() {
        return false;
    }

    /**
     * Gets the User as a discord formatted mention.
     *
     * @return A discord formatted mention of this User.
     */
    public String getAsMention() {
        return "<@" + id + '>';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
            return false;
        User oUser = (User) o;
        return this == oUser || this.id == oUser.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}