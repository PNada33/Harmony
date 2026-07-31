package org.spongepowered.asm.mixin.injection.callback;

public class CallbackInfoReturnable<T> extends CallbackInfo {
    private T returnValue;

    public T getReturnValue() {
        return this.returnValue;
    }

    public void setReturnValue(T returnValue) {
        this.returnValue = returnValue;
        this.cancel();
    }
}
