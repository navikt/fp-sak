package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.util.Objects;

public class AksjonspunktUtlederInput {

    private BehandlingReferanse ref;

    public AksjonspunktUtlederInput(BehandlingReferanse ref) {
        Objects.requireNonNull(ref, "ref");
        this.ref = ref;
    }

    public Long getBehandlingId() {
        return ref.behandlingId();
    }

    public AktørId getAktørId() {
        return ref.aktørId();
    }

    public FagsakYtelseType getYtelseType() {
        return ref.fagsakYtelseType();
    }

    public BehandlingType getBehandlingType() {
        return ref.behandlingType();
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return ref.relasjonRolle();
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return ref.getSkjæringstidspunkt();
    }

    public Saksnummer getSaksnummer() {
        return ref.saksnummer();
    }
}
