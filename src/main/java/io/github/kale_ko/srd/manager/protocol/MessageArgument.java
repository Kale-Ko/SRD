package io.github.kale_ko.srd.manager.protocol;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class MessageArgument {
    protected final @NotNull MessageArgumentType type;

    protected boolean booleanValue;
    protected byte byteValue;
    protected short shortValue;
    protected int intValue;
    protected long longValue;
    protected UUID uuidValue;

    protected boolean[] booleanArrayValue;
    protected byte[] byteArrayValue;
    protected short[] shortArrayValue;
    protected int[] intArrayValue;
    protected long[] longArrayValue;
    protected UUID[] uuidArrayValue;

    public MessageArgument(boolean value) {
        this.type = MessageArgumentType.BOOLEAN;
        this.booleanValue = value;
    }

    public MessageArgument(byte value) {
        this.type = MessageArgumentType.BYTE;
        this.byteValue = value;
    }

    public MessageArgument(short value) {
        this.type = MessageArgumentType.SHORT;
        this.shortValue = value;
    }

    public MessageArgument(int value) {
        this.type = MessageArgumentType.INT;
        this.intValue = value;
    }

    public MessageArgument(long value) {
        this.type = MessageArgumentType.LONG;
        this.longValue = value;
    }

    public MessageArgument(@NotNull UUID value) {
        this.type = MessageArgumentType.UUID;
        this.uuidValue = value;
    }

    public MessageArgument(boolean[] value) {
        this.type = MessageArgumentType.BOOLEAN_ARRAY;
        this.booleanArrayValue = value;
    }

    public MessageArgument(byte[] value) {
        this.type = MessageArgumentType.BYTE_ARRAY;
        this.byteArrayValue = value;
    }

    public MessageArgument(short[] value) {
        this.type = MessageArgumentType.SHORT_ARRAY;
        this.shortArrayValue = value;
    }

    public MessageArgument(int[] value) {
        this.type = MessageArgumentType.INT_ARRAY;
        this.intArrayValue = value;
    }

    public MessageArgument(long[] value) {
        this.type = MessageArgumentType.LONG_ARRAY;
        this.longArrayValue = value;
    }

    public MessageArgument(UUID[] value) {
        this.type = MessageArgumentType.UUID_ARRAY;
        this.uuidArrayValue = value;
    }

    public @NotNull MessageArgumentType getType() {
        return this.type;
    }

    public boolean getBooleanValue() {
        return this.booleanValue;
    }

    public byte getByteValue() {
        return this.byteValue;
    }

    public short getShortValue() {
        return this.shortValue;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public long getLongValue() {
        return this.longValue;
    }

    public UUID getUuidValue() {
        return this.uuidValue;
    }

    public boolean[] getBooleanArrayValue() {
        return this.booleanArrayValue;
    }

    public byte[] getByteArrayValue() {
        return this.byteArrayValue;
    }

    public short[] getShortArrayValue() {
        return this.shortArrayValue;
    }

    public int[] getIntArrayValue() {
        return this.intArrayValue;
    }

    public long[] getLongArrayValue() {
        return this.longArrayValue;
    }

    public UUID[] getUuidArrayValue() {
        return this.uuidArrayValue;
    }
}