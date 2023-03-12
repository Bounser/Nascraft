package me.bounser.nascraft.market.managers.resources;

public enum Trend {

    BULL1,
    BULL2,
    BULL3,
    BULLRUN,

    BEAR1,
    BEAR2,
    BEAR3,
    CRASH,

    FLAT;

    public static float[] extents(Trend tendency) {

        float[] trend = new float[2];

        switch (tendency) {

            case BULL1:
                trend[0] = 1.05f;
                trend[1] = 1f;
                break;
            case BULL2:
                trend[0] = 1.1f;
                trend[1] = 1f;
                break;
            case BULL3:
                trend[0] = 1.3f;
                trend[1] = 1f;
                break;
            case BULLRUN:
                trend[0] = 3f;
                trend[1] = 1f;
                break;

            case BEAR1:
                trend[0] = 1f;
                trend[1] = 1.05f;
                break;
            case BEAR2:
                trend[0] = 1f;
                trend[1] = 1.1f;
                break;
            case BEAR3:
                trend[0] = 1f;
                trend[1] = 1.3f;
                break;
            case CRASH:
                trend[0] = 1f;
                trend[1] = 3f;
                break;

            default:
                trend[0] = 1f;
                trend[1] = 1f;
        }
        return trend;
    }

}
