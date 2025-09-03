package ee.bigbank.task.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ee.bigbank.task.api.GameClient;
import ee.bigbank.task.api.dto.BuyResponse;
import ee.bigbank.task.api.dto.ShopItem;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock GameClient client;

    @BeforeEach
    void setup() {
        // no-op
    }

    @Test
    void buysHealthPotion_whenLivesLow_andEnoughGold() {
        // Shop has HP and one extra
        when(client.getShop("g1")).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50),
                new ShopItem("cs", "Claw Sharpening", 100)
        ));
        // buy returns something (we don't assert contents)
        when(client.buyItem("g1", "hpot")).thenReturn(new BuyResponse("OK", 50, 2, 1, 2));

        ShopService shop = new ShopService(client, "g1", 400);

        boolean bought = shop.maybeBuyItem(/*gold*/100, /*lives*/1);

        assertThat(bought).isTrue();
        verify(client).buyItem("g1", "hpot");
    }

    @Test
    void buysNextNonHpItem_ifAffordableWithReserve() {
        // Reserve=100, Gold=250, Cost=120 -> 250 - 100 >= 120 => true
        when(client.getShop("g2")).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50),
                new ShopItem("cs", "Claw Sharpening", 120),
                new ShopItem("wax", "Copper Plating", 100)
        ));
        when(client.buyItem("g2", "cs")).thenReturn(new BuyResponse("OK", 130, 2, 1, 2));

        ShopService shop = new ShopService(client, "g2", 300);

        boolean bought = shop.maybeBuyItem(/*gold*/450, /*lives*/2);

        assertThat(bought).isTrue();
        verify(client).buyItem("g2", "cs");
    }

    @Test
    void doesNotBuyNonHp_ifNotAffordableWithReserve() {
        // Reserve=100, Gold=180, Cost=120 -> 180 - 100 >= 120 => false
        when(client.getShop("g3")).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50),
                new ShopItem("cs", "Claw Sharpening", 120)
        ));

        ShopService shop = new ShopService(client, "g3", 300);

        boolean bought = shop.maybeBuyItem(/*gold*/180, /*lives*/3);

        assertThat(bought).isFalse();
        verify(client, never()).buyItem(anyString(), anyString());
    }

    @Test
    void buysHealthPotion_whenAllNonHpAlreadyPurchased_andEnoughGold() {
        // We will buy extras first across calls, then HP afterwards.
        when(client.getShop("g4")).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50),
                new ShopItem("cs", "Claw Sharpening", 100),
                new ShopItem("wax", "Copper Plating", 100)
        ));
        when(client.buyItem("g4", "cs")).thenReturn(new BuyResponse("OK", 200, 3, 1, 2));
        when(client.buyItem("g4", "wax")).thenReturn(new BuyResponse("OK", 200, 3, 1, 3));
        when(client.buyItem("g4", "hpot")).thenReturn(new BuyResponse("OK", 150, 4, 1, 4));

        ShopService shop = new ShopService(client, "g4", 300);

        // 1st call: buys cs (affordable with reserve)
        boolean b1 = shop.maybeBuyItem(/*gold*/500, /*lives*/3);
        // 2nd call: buys wax (affordable with reserve)
        boolean b2 = shop.maybeBuyItem(/*gold*/500, /*lives*/3);
        // 3rd call: all non-HP purchased -> buys hpot (gold>=50)
        boolean b3 = shop.maybeBuyItem(/*gold*/500, /*lives*/3);

        assertThat(b1).isTrue();
        assertThat(b2).isTrue();
        assertThat(b3).isTrue();

        InOrder inOrder = inOrder(client);
        inOrder.verify(client).getShop("g4"); // ctor
        inOrder.verify(client).buyItem("g4", "cs");
        inOrder.verify(client).buyItem("g4", "wax");
        inOrder.verify(client).buyItem("g4", "hpot");
    }

    @Test
    void buyItem_handlesExceptions_andReturnsFalse() {
        when(client.getShop("g5")).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50)
        ));
        when(client.buyItem("g5", "hpot")).thenThrow(new RuntimeException("Bad Request"));

        ShopService shop = new ShopService(client, "g5", 300);

        boolean bought = shop.maybeBuyItem(/*gold*/200, /*lives*/1);

        assertThat(bought).isFalse();
        verify(client).buyItem("g5", "hpot");
    }
}
