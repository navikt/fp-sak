package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.domene.typer.AktørId;

/** Input data til AksjonspunktOppdaterere. */
public final class AksjonspunktOppdaterParameter {
    private final Behandling behandling;
    private final Builder vilkårBuilder;
    private final Optional<Aksjonspunkt> aksjonspunkt;
    private Skjæringstidspunkt skjæringstidspunkt;
    private BehandlingReferanse ref;
    private final boolean erBegrunnelseEndret;

    private AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, Skjæringstidspunkt skjæringstidspunkt, Builder vilkårBuilder, String begrunnelse) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunkt, "Optional<Aksjonspunkt> kan ikke selv være null");
        Objects.requireNonNull(vilkårBuilder, "vilkårBuilder");
        this.behandling = behandling;
        this.vilkårBuilder = vilkårBuilder;
        this.aksjonspunkt = aksjonspunkt;
        this.skjæringstidspunkt = skjæringstidspunkt; // tillater null foreløpig pga tester som ikke har satt
        this.ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        this.erBegrunnelseEndret = begrunnelse != null ? aksjonspunkt.map(ap -> !Objects.equals(ap.getBegrunnelse(), begrunnelse)).orElse(Boolean.FALSE) : Boolean.FALSE;
    }

    public AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, Skjæringstidspunkt skjæringstidspunkt, Builder vilkårBuilder, BekreftetAksjonspunktDto dto) {
        this(behandling, aksjonspunkt, skjæringstidspunkt, vilkårBuilder, dto.getBegrunnelse());
    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Aksjonspunkt aksjonspunkt, BekreftetAksjonspunktDto dto) {
        this(behandling, Optional.ofNullable(aksjonspunkt), null, VilkårResultat.builder(), dto.getBegrunnelse());

    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, BekreftetAksjonspunktDto dto) {
        this(behandling, aksjonspunkt, null, VilkårResultat.builder(), dto.getBegrunnelse());
    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Aksjonspunkt aksjonspunkt, Skjæringstidspunkt skjæringstidspunkt, String begrunnelse) {
        this(behandling, Optional.ofNullable(aksjonspunkt), skjæringstidspunkt, VilkårResultat.builder(), begrunnelse);
    }

    /**
     * @deprecated Bruk {@link #getRef()} i stedet.
     */
    @Deprecated
    public Behandling getBehandling() {
        return behandling;
    }

    public Long getBehandlingId() {
        return ref.getBehandlingId();
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        Objects.requireNonNull(skjæringstidspunkt, "Utviker-feil: this.skjæringstidspunkt er ikke initialisert");
        return skjæringstidspunkt;
    }

    public Optional<Aksjonspunkt> getAksjonspunkt() {
        return aksjonspunkt;
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    /** Returnerer builder som brukes til å bygge opp vilkår. Er mutable. */
    public VilkårResultat.Builder getVilkårResultatBuilder() {
        Objects.requireNonNull(vilkårBuilder, "this.vilkårBuilder");
        return vilkårBuilder;
    }

    public AktørId getAktørId() {
        return ref.getAktørId();
    }

    public boolean erBegrunnelseEndret() {
        return erBegrunnelseEndret;
    }
}
