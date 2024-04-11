package utils;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicDoubleBackedByAtomicLong {
    private final AtomicLong bits;

    public AtomicDoubleBackedByAtomicLong(double initialValue) {
        this.bits = new AtomicLong(Double.doubleToRawLongBits(initialValue));
    }

    public double get() {
        return Double.longBitsToDouble(bits.get());
    }

    public void set(double newValue) {
        bits.set(Double.doubleToRawLongBits(newValue));
    }

    public double addAndGet(double delta) {
        long currentBits;
        double newValue;
        do {
            currentBits = bits.get();
            newValue = Double.longBitsToDouble(currentBits) + delta;
        } while (!bits.compareAndSet(currentBits, Double.doubleToRawLongBits(newValue)));
        return newValue;
    }
}

