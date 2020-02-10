package no.nav.foreldrepenger.behandlingslager.behandling;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class MottattDokumentPersistertEvent implements BehandlingEvent {
    private MottattDokument mottattDokument;
    private Behandling behandling;

    public MottattDokumentPersistertEvent(MottattDokument mottattDokument, Behandling behandling) {
        this.mottattDokument = mottattDokument;
        this.behandling = behandling;
    }

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

    public MottattDokument getMottattDokument() {
        return mottattDokument;
    }
}
