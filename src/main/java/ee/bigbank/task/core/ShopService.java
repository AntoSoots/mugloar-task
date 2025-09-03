package ee.bigbank.task.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.bigbank.task.api.GameClient;
import ee.bigbank.task.api.dto.ShopItem;

public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private static final String HEALTH_POT = "hpot";

    private GameClient client;
    private String gameId;
    private List<ShopItem> shopItems;
    private final List<String> purchasedItems = new ArrayList<>();
    /** Keep this much gold unspent for emergency healing problems */
    private final int goldReserveForHealing;

    public ShopService(GameClient client, String gameId) {
        this(client, gameId, 300);
    }

    public ShopService(GameClient client, String gameId, int goldReserveForHealing) {
        this.client = Objects.requireNonNull(client);
        this.gameId = Objects.requireNonNull(gameId);
        this.shopItems = client.getShop(gameId);
        this.goldReserveForHealing = goldReserveForHealing;
    }

    /**
     * Attempts to buy an item based on current state:
     * - If lives <= 1 (or all non-healing items are already purchased) and gold >= 50, buy a healing potion.
     * - Otherwise buy the next unpurchased non-healing item if affordable while keeping gold reserve intact.
     *
     * @return true if something was bought
     */
    public boolean maybeBuyItem(int currentGold, int currentLives) {
        // Are all non-HP items already purchased?
        boolean allNonHPBought = shopItems.stream()
            .filter(item -> !HEALTH_POT.equalsIgnoreCase(item.id()))
            .allMatch(item -> purchasedItems.contains(item.id()));

        // Buy HP if low on lives OR we have bought all other items and have at least 50 gold
        if ((currentLives <= 1 || allNonHPBought) && currentGold >= 50) {
            log.debug("Purchased health pot");
            return buyItem(HEALTH_POT);
        }

        // Find next unpurchased non-HP item
        ShopItem nextUnpurchasedItem = shopItems.stream()
            .filter(item -> !HEALTH_POT.equalsIgnoreCase(item.id()))
            .filter(item -> !purchasedItems.contains(item.id()))
            .findFirst()
            .orElse(null);

        if (nextUnpurchasedItem != null) {
            boolean affordableWithReserve = (currentGold - goldReserveForHealing) >= nextUnpurchasedItem.cost();
            if (affordableWithReserve) {
                boolean bought = buyItem(nextUnpurchasedItem.id());
                if (bought) {
                    log.debug("Purchased {}", nextUnpurchasedItem.name());
                    purchasedItems.add(nextUnpurchasedItem.id());
                }
                return bought;
            }
        }
        return false;
    }

    private boolean buyItem(String itemName) {
        return shopItems.stream()
            .filter(item -> itemName.equalsIgnoreCase(item.id()))
            .findFirst()
            .map(item -> {
                try {
                    client.buyItem(gameId, item.id());
                    return true;
                } catch (RuntimeException e) {
                    log.warn("Buying item '{}' failed: {}", item.id(), e.getMessage());
                    return false;
                }
            })
            .orElse(false);
    }
}
