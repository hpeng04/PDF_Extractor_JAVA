package utils;

public enum ConversionType {
    TEMPERATURE {
        @Override
        public float convert(float value) {
            return (value - 32f) / 1.8f;
        }
    },
    LENGTH {
        @Override
        public float convert(float value) {
            return value * 0.3048f;
        }
    },
    AREA {
        @Override
        public float convert(float value) {
            return value * 0.092903f;
        }
    },
    VOLUME_M3 {
        @Override
        public float convert(float value) {
            return value * 0.0283168466f;
        }
    },
    THERMAL_RESISTANCE {
        @Override
        public float convert(float value) {
            return value / 5.67826f;
        }
    },
    ENERGY {
        @Override
        public float convert(float value) {
            return value / 0.000948f;
        }
    },
    VOLUME_FLOW_RATE {
        @Override
        public float convert(float value) {
            return value * 0.47194745f;
        }
    },
    VOLUME_LITRE {
        @Override
        public float convert(float value) {
            return value * 4.54609f;
        }
    },
    POWER {
        @Override
        public float convert(float value) {
            return value / 3.412141633f;
        }
    };

    public abstract float convert(float value);
}