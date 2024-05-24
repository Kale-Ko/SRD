package io.github.kale_ko.srd.manager.protocol;

public enum MessageArgumentType {
    BOOLEAN(1),
    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8),
    UUID(16),

    BOOLEAN_ARRAY(4, BOOLEAN.size),
    BYTE_ARRAY(4, BYTE.size),
    SHORT_ARRAY(4, SHORT.size),
    INT_ARRAY(4, INT.size),
    LONG_ARRAY(4, LONG.size),
    UUID_ARRAY(4, UUID.size);

    private final int size;
    private final int variableSize;

    private MessageArgumentType(int size) {
        this.size = (short) size;
        this.variableSize = -1;
    }

    private MessageArgumentType(int size, int variableSize) {
        this.size = (short) size;
        this.variableSize = variableSize;
    }

    public int getSize() {
        return this.size;
    }

    public boolean isVariable() {
        return this.variableSize >= 0;
    }

    public int getVariableSize() {
        return this.variableSize;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "#" + this.name() + "{size=" + this.size + ", variableSize=" + this.variableSize + '}';
    }
}