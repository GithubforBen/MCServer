package com.benschnorr.communication.events.lotto;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.lotto.LottoDraw;
import com.benschnorr.types.lotto.LottoStatus;

import java.io.Serializable;

/**
 * Tells every server where the lotto stands - after every purchase, so the pot on the lobby stand grows
 * while people watch, and after a draw, which every server then announces to its own players.
 */
public class LottoUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4605L;

    private LottoStatus status;
    /** The draw that just happened, or {@code null} when only the pot or the settings changed. */
    private LottoDraw draw;

    public LottoUpdatedEvent(LottoStatus status, LottoDraw draw) {
        super(ListenerAdapter.ServerName.ALL);
        this.status = status;
        this.draw = draw;
    }

    public LottoUpdatedEvent() {
    }

    public LottoStatus getStatus() {
        return status;
    }

    public LottoDraw getDraw() {
        return draw;
    }
}
