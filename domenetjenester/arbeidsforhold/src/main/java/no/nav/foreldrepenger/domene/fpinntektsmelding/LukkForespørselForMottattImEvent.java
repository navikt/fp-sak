package no.nav.foreldrepenger.domene.fpinntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record LukkForespørselForMottattImEvent(Behandling behandling, OrgNummer orgNummer) implements BehandlingEvent {
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

    public Saksnummer getSaksnummer() {return behandling.getFagsak().getSaksnummer();}

    public OrgNummer getOrgNummer() {return orgNummer;}
}
