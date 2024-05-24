package io.github.kale_ko.srd.manager.protocol;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public enum MessageType {
    HELLO(0x00, List.of(MessageArgumentType.SHORT, MessageArgumentType.BYTE_ARRAY)),
    GOODBYE(0x01, List.of(MessageArgumentType.SHORT));

    private final short code;

    private final @NotNull @Unmodifiable List<MessageArgumentType> arguments;

    private MessageType(short code, @NotNull List<MessageArgumentType> arguments) {
        this.code = code;

        this.arguments = List.copyOf(arguments);
    }

    private MessageType(int code, @NotNull List<MessageArgumentType> arguments) {
        if (code > Short.MAX_VALUE || code < Short.MIN_VALUE) {
            throw new IllegalArgumentException();
        }
        this.code = (short) code;

        this.arguments = List.copyOf(arguments);
    }

    public short getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "#" + this.name() + "{code=" + this.code + ", arguments=" + this.arguments + "}";
    }

    public @NotNull @Unmodifiable List<MessageArgumentType> getArguments() {
        return this.arguments;
    }

    public static @Nullable MessageType valueOf(short code) {
        for (MessageType value : values()) {
            if (value.code == code) {
                return value;
            }
        }

        return null;
    }

    public static @Nullable MessageType valueOf(int code) {
        if (code > Short.MAX_VALUE || code < Short.MIN_VALUE) {
            throw new IllegalArgumentException();
        }
        return valueOf((short) code);
    }
}