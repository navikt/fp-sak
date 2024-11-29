package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record MottattDokumentPersistertEvent(MottattDokument mottattDokument, Behandling behandling) implements BehandlingEvent {

    @Override
    public Long getFagsakId() {
        return behandling.getFagsakId();
    }

    @Override
    public Saksnummer getSaksnummer() {
        return behandling.getSaksnummer();
    }

    @Override
    public Long getBehandlingId() {
        return behandling.getId();
    }

    public MottattDokument getMottattDokument() {
        return mottattDokument;
    }
}
