package com.xinyu.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @Size(max = 80, message = "nickname must be at most 80 characters")
    @Pattern(regexp = ".*\\S.*", message = "nickname must not be blank")
    private String nickname;

    @Size(max = 512, message = "avatarUrl must be at most 512 characters")
    private String avatarUrl;

    @Size(max = 1000, message = "bio must be at most 1000 characters")
    private String bio;

    private boolean nicknamePresent;
    private boolean avatarUrlPresent;
    private boolean bioPresent;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nicknamePresent = true;
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrlPresent = true;
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bioPresent = true;
        this.bio = bio;
    }

    public boolean hasNickname() {
        return nicknamePresent;
    }

    public boolean hasAvatarUrl() {
        return avatarUrlPresent;
    }

    public boolean hasBio() {
        return bioPresent;
    }

    public boolean hasChanges() {
        return nicknamePresent || avatarUrlPresent || bioPresent;
    }
}
