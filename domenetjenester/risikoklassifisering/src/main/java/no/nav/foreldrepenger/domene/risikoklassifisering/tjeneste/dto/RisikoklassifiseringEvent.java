package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class RisikoklassifiseringEvent implements BehandlingEvent {
    private Behandling behandling;

    public RisikoklassifiseringEvent(Behandling behandling) {
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

    public FagsakYtelseType getFagsakYtelseType(){
        return behandling.getFagsakYtelseType();
    }

    public Fagsak getFagsak(){
        return behandling.getFagsak();
    }

    public Behandling getBehandling(){
        return this.behandling;
    }
}
