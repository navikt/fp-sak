package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;


import javax.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG_2)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlag2Steg implements BeregningsgrunnlagSteg {

    protected ForeslåBeregningsgrunnlag2Steg() {
        // for CDI proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
