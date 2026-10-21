package com.darkona.feathersoffatigue.compatibility;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.core.FeathersData;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import net.minecraft.world.entity.player.Player;

public final class ThirstRegen {

    public static final int FULL_THIRST = 20;

    private ThirstRegen() {}

    public static double factor(int thirst, double hydration, double reductionPerPoint, double bonusPerPoint) {
        return -(FULL_THIRST - thirst) * reductionPerPoint + hydration * bonusPerPoint;
    }

    public static int owedSinceLastCall(Player player, FeathersView feathers, Keys key, double costPerFeather) {
        FeathersData data = FeathersServiceImpl.data(player);
        double since = regeneratedSinceLastCall(data, feathers, key);
        if (costPerFeather <= 0 || since <= 0) return 0;

        double owed = data.getCounter(key.owed()) + since / Stamina.PER_FEATHER * costPerFeather;
        int whole = (int) owed;
        data.setCounter(key.owed(), owed - whole);
        return whole;
    }

    public static float exhaustionSinceLastCall(Player player, FeathersView feathers, Keys key, double exhaustionPerFeather) {
        double since = regeneratedSinceLastCall(FeathersServiceImpl.data(player), feathers, key);
        if (exhaustionPerFeather <= 0 || since <= 0) return 0f;
        return (float) (since / Stamina.PER_FEATHER * exhaustionPerFeather);
    }

    private static double regeneratedSinceLastCall(FeathersData data, FeathersView feathers, Keys key) {
        double regenerated = feathers.totalRegenerated();
        double since = regenerated - data.getCounter(key.lastRegenerated());
        data.setCounter(key.lastRegenerated(), regenerated);
        return since;
    }

    public record Keys(String lastRegenerated, String owed) {
        public static Keys of(String compat) {
            return new Keys(compat + ".last_regenerated", compat + ".owed");
        }
    }
}
