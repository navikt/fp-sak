package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.domene.typer.AktørId;

/** Input data til AksjonspunktOppdaterere. */
public final class AksjonspunktOppdaterParameter {
    private final BehandlingReferanse ref;
    private final boolean erBegrunnelseEndret;

    private AksjonspunktOppdaterParameter(BehandlingReferanse ref, String begrunnelse, Aksjonspunkt aksjonspunkt) {
        this.ref = ref;
        this.erBegrunnelseEndret = begrunnelse != null
                ? Optional.ofNullable(aksjonspunkt).map(ap -> !Objects.equals(aksjonspunkt.getBegrunnelse(), begrunnelse)).orElse(Boolean.FALSE)
                : Boolean.FALSE;
    }

    public AksjonspunktOppdaterParameter(BehandlingReferanse ref, BekreftetAksjonspunktDto dto, Aksjonspunkt aksjonspunkt) {
        this(ref, dto.getBegrunnelse(), aksjonspunkt);
    }

    /**
     * Test-only
     */
    public AksjonspunktOppdaterParameter(BehandlingReferanse ref, BekreftetAksjonspunktDto dto) {
        this(ref, dto, null);}

    public Long getBehandlingId() {
        return ref.behandlingId();
    }

    public Long getFagsakId() {
        return ref.fagsakId();
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    public AktørId getAktørId() {
        return ref.aktørId();
    }

    public boolean erBegrunnelseEndret() {
        return erBegrunnelseEndret;
    }
}
