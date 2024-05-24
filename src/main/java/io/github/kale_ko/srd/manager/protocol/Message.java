package io.github.kale_ko.srd.manager.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class Message {
    public static final byte PROTOCOL_VERSION_MAJOR = 0x01;
    public static final byte PROTOCOL_VERSION_MINOR = 0x00;
    public static final short PROTOCOL_VERSION = ((PROTOCOL_VERSION_MAJOR & 0xFF) << 8) | (PROTOCOL_VERSION_MINOR & 0xFF);

    protected final @NotNull MessageType type;

    protected final @NotNull @Unmodifiable List<MessageArgument> arguments;

    public Message(@NotNull MessageType type, @NotNull List<MessageArgument> arguments) {
        this.type = type;

        this.arguments = List.copyOf(arguments);
    }

    public @NotNull MessageType getType() {
        return this.type;
    }

    public @NotNull @Unmodifiable List<MessageArgument> getArguments() {
        return this.arguments;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{type=" + this.type + ", arguments=" + this.arguments + "}";
    }

    public static @Nullable Message read(@Nullable ByteBuf data) {
        data.retain();

        try {
            if (data.readableBytes() < 2) {
                return null;
            }
            short typeS = data.readShort();

            MessageType type = MessageType.valueOf(typeS);
            if (type == null) {
                return null;
            }

            List<MessageArgument> arguments = new ArrayList<>();

            for (MessageArgumentType argumentType : type.getArguments()) {
                if (data.readableBytes() < argumentType.getSize() || (argumentType.isVariable() && data.readableBytes() < argumentType.getSize() + 4) || (argumentType.isVariable() && data.readableBytes() < (argumentType.getSize() + 4 + (argumentType.getVariableSize() * data.getInt(data.readerIndex()))))) {
                    return null;
                }

                switch (argumentType) {
                    case BOOLEAN -> {
                        boolean value = data.readBoolean();
                        arguments.add(new MessageArgument(value));
                    }
                    case BYTE -> {
                        byte value = data.readByte();
                        arguments.add(new MessageArgument(value));
                    }
                    case SHORT -> {
                        short value = data.readShort();
                        arguments.add(new MessageArgument(value));
                    }
                    case INT -> {
                        int value = data.readInt();
                        arguments.add(new MessageArgument(value));
                    }
                    case LONG -> {
                        long value = data.readLong();
                        arguments.add(new MessageArgument(value));
                    }
                    case UUID -> {
                        UUID value = new UUID(data.readLong(), data.readLong());
                        arguments.add(new MessageArgument(value));
                    }
                    case BOOLEAN_ARRAY -> {
                        int size = data.readInt();
                        boolean[] array = new boolean[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = data.readBoolean();
                        }
                        arguments.add(new MessageArgument(array));
                    }
                    case BYTE_ARRAY -> {
                        int size = data.readInt();
                        byte[] array = new byte[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = data.readByte();
                        }
                        arguments.add(new MessageArgument(array));
                    }
                    case SHORT_ARRAY -> {
                        int size = data.readInt();
                        short[] array = new short[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = data.readShort();
                        }
                        arguments.add(new MessageArgument(array));
                    }
                    case INT_ARRAY -> {
                        int size = data.readInt();
                        int[] array = new int[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = data.readInt();
                        }
                        arguments.add(new MessageArgument(array));
                    }
                    case LONG_ARRAY -> {
                        int size = data.readInt();
                        long[] array = new long[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = data.readLong();
                        }
                        arguments.add(new MessageArgument(array));
                    }
                    case UUID_ARRAY -> {
                        int size = data.readInt();
                        UUID[] array = new UUID[size];
                        for (int i = 0; i < size; i++) {
                            array[i] = new UUID(data.readLong(), data.readLong());
                        }
                        arguments.add(new MessageArgument(array));
                    }
                }
            }

            return new Message(type, arguments);
        } finally {
            data.release();
        }
    }

    public static @Nullable ByteBuf write(@NotNull Message message) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            DataOutputStream os = new DataOutputStream(bos);

            os.writeShort(message.getType().getCode());

            for (MessageArgument argument : message.getArguments()) {
                switch (argument.getType()) {
                    case BOOLEAN -> {
                        os.writeBoolean(argument.getBooleanValue());
                    }
                    case BYTE -> {
                        os.writeByte(argument.getByteValue());
                    }
                    case SHORT -> {
                        os.writeShort(argument.getShortValue());
                    }
                    case INT -> {
                        os.writeInt(argument.getIntValue());
                    }
                    case LONG -> {
                        os.writeLong(argument.getLongValue());
                    }
                    case UUID -> {
                        os.writeLong(argument.getUuidValue().getMostSignificantBits());
                        os.writeLong(argument.getUuidValue().getLeastSignificantBits());
                    }
                    case BOOLEAN_ARRAY -> {
                        os.writeInt(argument.getBooleanArrayValue().length);
                        for (int i = 0; i < argument.getBooleanArrayValue().length; i++) {
                            os.writeBoolean(argument.getBooleanArrayValue()[i]);
                        }
                    }
                    case BYTE_ARRAY -> {
                        os.writeInt(argument.getByteArrayValue().length);
                        for (int i = 0; i < argument.getByteArrayValue().length; i++) {
                            os.writeByte(argument.getByteArrayValue()[i]);
                        }
                    }
                    case SHORT_ARRAY -> {
                        os.writeInt(argument.getShortArrayValue().length);
                        for (int i = 0; i < argument.getShortArrayValue().length; i++) {
                            os.writeShort(argument.getShortArrayValue()[i]);
                        }
                    }
                    case INT_ARRAY -> {
                        os.writeInt(argument.getIntArrayValue().length);
                        for (int i = 0; i < argument.getIntArrayValue().length; i++) {
                            os.writeInt(argument.getIntArrayValue()[i]);
                        }
                    }
                    case LONG_ARRAY -> {
                        os.writeInt(argument.getLongArrayValue().length);
                        for (int i = 0; i < argument.getLongArrayValue().length; i++) {
                            os.writeLong(argument.getLongArrayValue()[i]);
                        }
                    }
                    case UUID_ARRAY -> {
                        os.writeInt(argument.getUuidArrayValue().length);
                        for (int i = 0; i < argument.getUuidArrayValue().length; i++) {
                            os.writeLong(argument.getUuidArrayValue()[i].getMostSignificantBits());
                            os.writeLong(argument.getUuidArrayValue()[i].getLeastSignificantBits());
                        }
                    }
                }
            }

            return Unpooled.wrappedBuffer(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}