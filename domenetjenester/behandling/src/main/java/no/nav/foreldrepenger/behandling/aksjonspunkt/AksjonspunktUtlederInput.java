package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AksjonspunktUtlederInput {

    private BehandlingReferanse ref;

    /**
     * @deprecated Ikke bruk i prod kode - kun mens vi endrer kodebasen
     */
    @Deprecated(forRemoval = true)
    public AksjonspunktUtlederInput(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        this.ref = BehandlingReferanse.fra(behandling);
    }

    /**
     * @deprecated Ikke bruk i prod kode - kun mens vi endrer kodebasen
     */
    @Deprecated(forRemoval = true)
    public AksjonspunktUtlederInput(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
        this.ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    public AksjonspunktUtlederInput(BehandlingReferanse ref) {
        Objects.requireNonNull(ref, "ref");
        this.ref = ref;
    }

    public Long getBehandlingId( ) {
        return ref.getBehandlingId();
    }

    public AktørId getAktørId() {
        return ref.getAktørId();
    }

    public FagsakYtelseType getYtelseType() {
        return ref.getFagsakYtelseType();
    }

    public BehandlingType getBehandlingType() {
        return ref.getBehandlingType();
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return ref.getRelasjonsRolleType();
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return ref.getSkjæringstidspunkt();
    }

    public Saksnummer getSaksnummer() {
        return ref.getSaksnummer();
    }
}
