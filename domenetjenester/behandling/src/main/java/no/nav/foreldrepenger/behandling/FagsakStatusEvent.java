package no.nav.foreldrepenger.behandling;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Event publiseres når Fagsak endrer status
 */
public class FagsakStatusEvent implements FagsakEvent {

    private Long fagsakId;
    private Long behandlingId;
    private FagsakStatus forrigeStatus;
    private FagsakStatus nyStatus;
    private AktørId aktørId;

    public FagsakStatusEvent(Long fagsakId, Long behandlingId, AktørId aktørId, FagsakStatus forrigeStatus, FagsakStatus nyStatus) {
        super();
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.aktørId = aktørId;
        this.forrigeStatus = forrigeStatus;
        this.nyStatus = nyStatus;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public FagsakStatus getForrigeStatus() {
        return forrigeStatus;
    }

    public FagsakStatus getNyStatus() {
        return nyStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + fagsakId +
                ", forrigeStatus=" + forrigeStatus +
                ", nyStatus=" + nyStatus +
                ">";
    }
}
