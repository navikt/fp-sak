package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public abstract class AbstractOverstyringshåndterer<T extends OverstyringAksjonspunkt> implements Overstyringshåndterer<T> {

    private AksjonspunktDefinisjon aksjonspunktDefinisjon;

    protected AbstractOverstyringshåndterer() {
        // for CDI proxy
    }

    protected AbstractOverstyringshåndterer(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
    }

    @Override
    public void håndterAksjonspunktForOverstyringPrecondition(T dto, Behandling behandling) {
        precondition(behandling, dto);
    }

    @Override
    public void håndterAksjonspunktForOverstyringHistorikk(T dto, Behandling behandling) {
        lagHistorikkInnslag(behandling, dto);
    }

    @Override
    public AksjonspunktDefinisjon aksjonspunktForInstans() {
        return aksjonspunktDefinisjon;
    }

    /**
     * Valider om precondition for overstyring er møtt. Kaster exception hvis ikke.
     *
     * @param behandling behandling
     * @param dto
     */
    protected void precondition(Behandling behandling, T dto) {
        // all good, do NOTHING.
    }

    protected abstract void lagHistorikkInnslag(Behandling behandling, T dto);

}
