package scorched.weapons;

import java.util.ArrayList;
import java.util.List;

public class WeaponRegistry {
	private static final List<AmmoType> ALL_WEAPONS = new ArrayList<>();

    static {
    	ALL_WEAPONS.add(new HERound());
        ALL_WEAPONS.add(new APRound());
        ALL_WEAPONS.add(new MiniNuke());
    }

    public static List<AmmoType> getAllWeapons() {
        return ALL_WEAPONS;
    }
}
