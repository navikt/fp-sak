package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record BehandlingVedtakEvent(BehandlingVedtak vedtak, Behandling behandling) implements BehandlingEvent {

    @Override
    public Long getFagsakId() {
        return behandling.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return behandling.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return behandling.getId();
    }

    public boolean iverksattVedtak() {
        return IverksettingStatus.IVERKSATT.equals(vedtak().getIverksettingStatus());
    }

    public boolean iverksattYtelsesVedtak() {
        return iverksattVedtak() && behandling.erYtelseBehandling();
    }
}
