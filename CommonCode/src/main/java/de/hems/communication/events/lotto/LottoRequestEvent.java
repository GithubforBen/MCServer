package de.hems.communication.events.lotto;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Something a player or an admin does with the lotto. Answered with {@link RespondLottoEvent}. */
public class LottoRequestEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4603L;

    public enum Action {
        /** Where it stands: a {@code LottoStatus}. */
        STATUS,
        /** Buy the tips in {@link #getTips()}; answers with the player's tips of this round. */
        BUY,
        /** The player's tips of this round. */
        MINE,
        /** Admin: draw now. */
        DRAW_NOW,
        /** Admin: day and time of the draw, in {@link #getText()} like "SONNTAG 20:00". */
        SET_SCHEDULE,
        /** Admin: what a tip costs, in {@link #getAmount()}. */
        SET_PRICE
    }

    private Action action;
    private UUID player;
    private String playerName;
    private boolean staff;
    private ArrayList<int[]> tips = new ArrayList<>();
    private String text;
    private int amount;

    public LottoRequestEvent(Action action, UUID player, String playerName, boolean staff) {
        super(ListenerAdapter.ServerName.HOST);
        this.action = action;
        this.player = player;
        this.playerName = playerName;
        this.staff = staff;
    }

    public LottoRequestEvent() {
    }

    public LottoRequestEvent withTips(List<int[]> tips) {
        this.tips = new ArrayList<>(tips);
        return this;
    }

    public LottoRequestEvent withText(String text) {
        this.text = text;
        return this;
    }

    public LottoRequestEvent withAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isStaff() {
        return staff;
    }

    public List<int[]> getTips() {
        return tips;
    }

    public String getText() {
        return text;
    }

    public int getAmount() {
        return amount;
    }
}
