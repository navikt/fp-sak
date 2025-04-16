package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record AksjonspunktUtlederInput(BehandlingReferanse ref, Skjæringstidspunkt stp) {

    public AksjonspunktUtlederInput {
        Objects.requireNonNull(ref, "ref");
    }

    public Long getBehandlingId() {
        return ref().behandlingId();
    }

    public AktørId getAktørId() {
        return ref().aktørId();
    }

    public FagsakYtelseType getYtelseType() {
        return ref().fagsakYtelseType();
    }

    public BehandlingType getBehandlingType() {
        return ref().behandlingType();
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return ref().relasjonRolle();
    }

    public BehandlingReferanse getRef() {
        return ref();
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return stp();
    }

    public Saksnummer getSaksnummer() {
        return ref().saksnummer();
    }
}
