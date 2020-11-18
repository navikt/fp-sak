package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class RisikoklassifiseringEvent implements BehandlingEvent {

    private final BehandlingReferanse behandlingReferanse;

    public RisikoklassifiseringEvent(BehandlingReferanse behandlingReferanse) {
        this.behandlingReferanse = behandlingReferanse;
    }

    @Override
    public Long getFagsakId() {
        return behandlingReferanse.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return behandlingReferanse.getId();
    }

    public FagsakYtelseType getFagsakYtelseType(){
        return behandlingReferanse.getFagsakYtelseType();
    }

    public BehandlingReferanse getBehandlingRef(){
        return this.behandlingReferanse;
    }
}
