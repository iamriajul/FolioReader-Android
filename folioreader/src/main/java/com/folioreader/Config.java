package com.folioreader;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

/**
 * Configuration class for FolioReader.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class Config implements Parcelable {

    private static final String LOG_TAG = Config.class.getSimpleName();
    public static final String INTENT_CONFIG = "config";
    public static final String EXTRA_OVERRIDE_CONFIG = "com.folioreader.extra.OVERRIDE_CONFIG";
    public static final String CONFIG_FONT = "font";
    public static final String CONFIG_FONT_SIZE = "font_size";
    public static final String CONFIG_IS_NIGHT_MODE = "is_night_mode";
    public static final String CONFIG_THEME_COLOR_INT = "theme_color_int";
    public static final String CONFIG_IS_TTS = "is_tts";
    public static final String CONFIG_ALLOWED_DIRECTION = "allowed_direction";
    public static final String CONFIG_DIRECTION = "direction";
    //Settings
    public static final String CONFIG_ENABLE_AUTO_SCROLL = "auto_scroll";
    public static final String CONFIG_INTERVAL_AUTO_SCROLL = "auto_scroll_interval";
    public static final String CONFIG_CONTINUOUS_AUTO_SCROLL = "continuous_auto_scroll";
    public static final String CONFIG_INTERVAL_CONTINUOUS_AUTO_SCROLL = "continuous_auto_scroll_interval";
    public static final String CONFIG_WHOLE_PAGE_AT_A_TIME_SCROLL = "whole_page_at_a_time";
    public static final String CONFIG_USE_VOLUME_TO_CONTROL_NAVIGATION = "volume_control_navigation";
    public static final String CONFIG_SWIPE_TO_CONTROL_BRIGHTNESS = "swipe_control_brightness";
    public static final String CONFIG_DIM_ON_INACTIVE = "dim_on_inactive";
    public static final String CONFIG_DIM_ON_INACTIVE_TIME = "dim_on_inactive_time";
    public static final String CONFIG_SHAKE_TO_TAKE_SCREEN_SHORT = "shake_to_take_ss";
    public static final String CONFIG_JUSTIFIED_ALIGNMENT = "justified_alignment";
    public static final String CONFIG_LINE_HEIGHT = "line_height";
    public static final String CONFIG_HYPHENATION = "hyphenation";
    private static final AllowedDirection DEFAULT_ALLOWED_DIRECTION = AllowedDirection.ONLY_VERTICAL;
    private static final Direction DEFAULT_DIRECTION = Direction.VERTICAL;
    private static final int DEFAULT_THEME_COLOR_INT = ContextCompat.getColor(
            AppContext.get(), R.color.default_theme_accent_color
    );

    private int font = 3;
    private int fontSize = 2;
    private boolean nightMode;
    @ColorInt
    private int themeColor = DEFAULT_THEME_COLOR_INT;
    private boolean showTts = true;
    private AllowedDirection allowedDirection = DEFAULT_ALLOWED_DIRECTION;
    private Direction direction = DEFAULT_DIRECTION;
    private boolean autoScroll = false;
    private long intervalAutoScroll = 0;
    private boolean continuousAutoScroll = false;
    private long intervalContinuousAutoScroll = 0;
    private boolean wholePageAtATime = false;
    private boolean useVolumeForControlNavigation = false;
    private boolean swipeForControlBrightness = false;
    private boolean dimOnInactive = false;
    private long dimOnInactiveTime = 0;
    private boolean shakeToTakeScreenShort = false;
    private boolean justifiedAlignment = false;
    private int lineHeight = 0;
    private boolean hyphenation = false;

    /**
     * Reading modes available
     */
    public enum AllowedDirection {
        ONLY_VERTICAL, ONLY_HORIZONTAL, VERTICAL_AND_HORIZONTAL
    }

    /**
     * Reading directions
     */
    public enum Direction {
        VERTICAL, HORIZONTAL
    }

    public Config() {
    }

    public Config(JSONObject jsonObject) {
        font = jsonObject.optInt(CONFIG_FONT);
        fontSize = jsonObject.optInt(CONFIG_FONT_SIZE);
        nightMode = jsonObject.optBoolean(CONFIG_IS_NIGHT_MODE);
        themeColor = getValidColorInt(jsonObject.optInt(CONFIG_THEME_COLOR_INT));
        showTts = jsonObject.optBoolean(CONFIG_IS_TTS);
        allowedDirection = getAllowedDirectionFromString(LOG_TAG,
                jsonObject.optString(CONFIG_ALLOWED_DIRECTION));
        direction = getDirectionFromString(LOG_TAG, jsonObject.optString(CONFIG_DIRECTION));
        autoScroll = jsonObject.optBoolean(CONFIG_ENABLE_AUTO_SCROLL);
        intervalAutoScroll = jsonObject.optLong(CONFIG_INTERVAL_AUTO_SCROLL);
        continuousAutoScroll = jsonObject.optBoolean(CONFIG_CONTINUOUS_AUTO_SCROLL);
        intervalContinuousAutoScroll = jsonObject.optLong(CONFIG_INTERVAL_CONTINUOUS_AUTO_SCROLL);
        wholePageAtATime = jsonObject.optBoolean(CONFIG_WHOLE_PAGE_AT_A_TIME_SCROLL);
        useVolumeForControlNavigation = jsonObject.optBoolean(CONFIG_USE_VOLUME_TO_CONTROL_NAVIGATION);
        swipeForControlBrightness = jsonObject.optBoolean(CONFIG_SWIPE_TO_CONTROL_BRIGHTNESS);
        dimOnInactive = jsonObject.optBoolean(CONFIG_DIM_ON_INACTIVE);
        dimOnInactiveTime = jsonObject.optLong(CONFIG_DIM_ON_INACTIVE_TIME);
        shakeToTakeScreenShort = jsonObject.optBoolean(CONFIG_SHAKE_TO_TAKE_SCREEN_SHORT);
        justifiedAlignment = jsonObject.optBoolean(CONFIG_JUSTIFIED_ALIGNMENT);
        lineHeight = jsonObject.optInt(CONFIG_LINE_HEIGHT);
        hyphenation = jsonObject.optBoolean(CONFIG_HYPHENATION);

    }

    protected Config(Parcel in) {
        font = in.readInt();
        fontSize = in.readInt();
        nightMode = in.readByte() != 0;
        themeColor = in.readInt();
        showTts = in.readByte() != 0;
        autoScroll = in.readByte() != 0;
        intervalAutoScroll = in.readLong();
        continuousAutoScroll = in.readByte() != 0;
        intervalContinuousAutoScroll = in.readLong();
        wholePageAtATime = in.readByte() != 0;
        useVolumeForControlNavigation = in.readByte() != 0;
        swipeForControlBrightness = in.readByte() != 0;
        dimOnInactive = in.readByte() != 0;
        dimOnInactiveTime = in.readLong();
        shakeToTakeScreenShort = in.readByte() != 0;
        justifiedAlignment = in.readByte() != 0;
        lineHeight = in.readInt();
        hyphenation = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(font);
        dest.writeInt(fontSize);
        dest.writeByte((byte) (nightMode ? 1 : 0));
        dest.writeInt(themeColor);
        dest.writeByte((byte) (showTts ? 1 : 0));
        dest.writeByte((byte) (autoScroll ? 1 : 0));
        dest.writeLong(intervalAutoScroll);
        dest.writeByte((byte) (continuousAutoScroll ? 1 : 0));
        dest.writeLong(intervalContinuousAutoScroll);
        dest.writeByte((byte) (wholePageAtATime ? 1 : 0));
        dest.writeByte((byte) (useVolumeForControlNavigation ? 1 : 0));
        dest.writeByte((byte) (swipeForControlBrightness ? 1 : 0));
        dest.writeByte((byte) (dimOnInactive ? 1 : 0));
        dest.writeLong(dimOnInactiveTime);
        dest.writeByte((byte) (shakeToTakeScreenShort ? 1 : 0));
        dest.writeByte((byte) (justifiedAlignment ? 1 : 0));
        dest.writeInt(lineHeight);
        dest.writeByte((byte) (hyphenation ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Config> CREATOR = new Creator<Config>() {
        @Override
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        @Override
        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public static Direction getDirectionFromString(final String LOG_TAG, String directionString) {

        switch (directionString) {
            case "VERTICAL":
                return Direction.VERTICAL;
            case "HORIZONTAL":
                return Direction.HORIZONTAL;
            default:
                Log.w(LOG_TAG, "-> Illegal argument directionString = `" + directionString
                        + "`, defaulting direction to " + DEFAULT_DIRECTION);
                return DEFAULT_DIRECTION;
        }
    }

    public static AllowedDirection getAllowedDirectionFromString(final String LOG_TAG,
                                                                 String allowedDirectionString) {

        switch (allowedDirectionString) {
            case "ONLY_VERTICAL":
                return AllowedDirection.ONLY_VERTICAL;
            case "ONLY_HORIZONTAL":
                return AllowedDirection.ONLY_HORIZONTAL;
            case "VERTICAL_AND_HORIZONTAL":
                return AllowedDirection.VERTICAL_AND_HORIZONTAL;
            default:
                Log.w(LOG_TAG, "-> Illegal argument allowedDirectionString = `"
                        + allowedDirectionString + "`, defaulting allowedDirection to "
                        + DEFAULT_ALLOWED_DIRECTION);
                return DEFAULT_ALLOWED_DIRECTION;
        }
    }

    public int getFont() {
        return font;
    }

    public Config setFont(int font) {
        this.font = font;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Config setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public Config setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
        return this;
    }

    @ColorInt
    private int getValidColorInt(@ColorInt int colorInt) {
        if (colorInt >= 0) {
            Log.w(LOG_TAG, "-> getValidColorInt -> Invalid argument colorInt = " + colorInt +
                    ", Returning DEFAULT_THEME_COLOR_INT = " + DEFAULT_THEME_COLOR_INT);
            return DEFAULT_THEME_COLOR_INT;
        }
        return colorInt;
    }

    @ColorInt
    public int getThemeColor() {
        return themeColor;
    }

    public Config setThemeColorRes(@ColorRes int colorResId) {
        try {
            this.themeColor = ContextCompat.getColor(AppContext.get(), colorResId);
        } catch (Resources.NotFoundException e) {
            Log.w(LOG_TAG, "-> setThemeColorRes -> " + e);
            Log.w(LOG_TAG, "-> setThemeColorRes -> Defaulting themeColor to " +
                    DEFAULT_THEME_COLOR_INT);
            this.themeColor = DEFAULT_THEME_COLOR_INT;
        }
        return this;
    }

    public Config setThemeColorInt(@ColorInt int colorInt) {
        this.themeColor = getValidColorInt(colorInt);
        return this;
    }

    public boolean isShowTts() {
        return showTts;
    }

    public Config setShowTts(boolean showTts) {
        this.showTts = showTts;
        return this;
    }

    public AllowedDirection getAllowedDirection() {
        return allowedDirection;
    }

    /**
     * Set reading direction mode options for users. This method should be called before
     * {@link #setDirection(Direction)} as it has higher preference.
     *
     * @param allowedDirection reading direction mode options for users. Setting to
     *                         {@link AllowedDirection#VERTICAL_AND_HORIZONTAL} users will have
     *                         choice to select the reading direction at runtime.
     */
    public Config setAllowedDirection(AllowedDirection allowedDirection) {

        this.allowedDirection = allowedDirection;

        if (allowedDirection == null) {
            this.allowedDirection = DEFAULT_ALLOWED_DIRECTION;
            direction = DEFAULT_DIRECTION;
            Log.w(LOG_TAG, "-> allowedDirection cannot be null, defaulting " +
                    "allowedDirection to " + DEFAULT_ALLOWED_DIRECTION + " and direction to " +
                    DEFAULT_DIRECTION);

        } else if (allowedDirection == AllowedDirection.ONLY_VERTICAL &&
                direction != Direction.VERTICAL) {
            direction = Direction.VERTICAL;
            Log.w(LOG_TAG, "-> allowedDirection is " + allowedDirection +
                    ", defaulting direction to " + direction);

        } else if (allowedDirection == AllowedDirection.ONLY_HORIZONTAL &&
                direction != Direction.HORIZONTAL) {
            direction = Direction.HORIZONTAL;
            Log.w(LOG_TAG, "-> allowedDirection is " + allowedDirection
                    + ", defaulting direction to " + direction);
        }

        return this;
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * Set reading direction. This method should be called after
     * {@link #setAllowedDirection(AllowedDirection)} as it has lower preference.
     *
     * @param direction reading direction.
     */
    public Config setDirection(Direction direction) {

        if (allowedDirection == AllowedDirection.VERTICAL_AND_HORIZONTAL && direction == null) {
            this.direction = DEFAULT_DIRECTION;
            Log.w(LOG_TAG, "-> direction cannot be `null` when allowedDirection is " +
                    allowedDirection + ", defaulting direction to " + this.direction);

        } else if (allowedDirection == AllowedDirection.ONLY_VERTICAL &&
                direction != Direction.VERTICAL) {
            this.direction = Direction.VERTICAL;
            Log.w(LOG_TAG, "-> direction cannot be `" + direction + "` when allowedDirection is " +
                    allowedDirection + ", defaulting direction to " + this.direction);

        } else if (allowedDirection == AllowedDirection.ONLY_HORIZONTAL &&
                direction != Direction.HORIZONTAL) {
            this.direction = Direction.HORIZONTAL;
            Log.w(LOG_TAG, "-> direction cannot be `" + direction + "` when allowedDirection is " +
                    allowedDirection + ", defaulting direction to " + this.direction);

        } else {
            this.direction = direction;
        }

        return this;
    }

    public Config setThemeColor(int themeColor) {
        this.themeColor = themeColor;
        return this;
    }

    public Config setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        return this;
    }

    public Config setIntervalAutoScroll(long intervalAutoScroll) {
        this.intervalAutoScroll = intervalAutoScroll;
        return this;
    }

    public Config setContinuousAutoScroll(boolean continuousAutoScroll) {
        this.continuousAutoScroll = continuousAutoScroll;
        return this;
    }

    public Config setIntervalContinuousAutoScroll(long intervalContinuousAutoScroll) {
        this.intervalContinuousAutoScroll = intervalContinuousAutoScroll;
        return this;
    }

    public Config setWholePageAtATime(boolean wholePageAtATime) {
        this.wholePageAtATime = wholePageAtATime;
        return this;
    }

    public Config setUseVolumeForControlNavigation(boolean useVolumeForControlNavigation) {
        this.useVolumeForControlNavigation = useVolumeForControlNavigation;
        return this;
    }

    public Config setSwipeForControlBrightness(boolean swipeForControlBrightness) {
        this.swipeForControlBrightness = swipeForControlBrightness;
        return this;
    }

    public Config setDimOnInactive(boolean dimOnInactive) {
        this.dimOnInactive = dimOnInactive;
        return this;
    }

    public Config setDimOnInactiveTime(long dimOnInactiveTime) {
        this.dimOnInactiveTime = dimOnInactiveTime;
        return this;
    }

    public Config setShakeToTakeScreenShort(boolean shakeToTakeScreenShort) {
        this.shakeToTakeScreenShort = shakeToTakeScreenShort;
        return this;
    }

    public Config setJustifiedAlignment(boolean justifiedAlignment) {
        this.justifiedAlignment = justifiedAlignment;
        return this;
    }

    public Config setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
        return this;
    }

    public Config setHyphenation(boolean hyphenation) {
        this.hyphenation = hyphenation;
        return this;
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }

    public long getIntervalAutoScroll() {
        return intervalAutoScroll;
    }

    public boolean isContinuousAutoScroll() {
        return continuousAutoScroll;
    }

    public long getIntervalContinuousAutoScroll() {
        return intervalContinuousAutoScroll;
    }

    public boolean isWholePageAtATime() {
        return wholePageAtATime;
    }

    public boolean isUseVolumeForControlNavigation() {
        return useVolumeForControlNavigation;
    }

    public boolean isSwipeForControlBrightness() {
        return swipeForControlBrightness;
    }

    public boolean isDimOnInactive() {
        return dimOnInactive;
    }

    public long getDimOnInactiveTime() {
        return dimOnInactiveTime;
    }

    public boolean isShakeToTakeScreenShort() {
        return shakeToTakeScreenShort;
    }

    public boolean isJustifiedAlignment() {
        return justifiedAlignment;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public boolean isHyphenation() {
        return hyphenation;
    }

    @NonNull
    @Override
    public String toString() {
        return "Config{" +
                "font=" + font +
                ", fontSize=" + fontSize +
                ", nightMode=" + nightMode +
                ", themeColor=" + themeColor +
                ", showTts=" + showTts +
                ", allowedDirection=" + allowedDirection +
                ", direction=" + direction +
                '}';
    }
}


