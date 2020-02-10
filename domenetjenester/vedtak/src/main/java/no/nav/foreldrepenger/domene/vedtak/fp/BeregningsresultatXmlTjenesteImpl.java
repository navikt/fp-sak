package no.nav.foreldrepenger.domene.vedtak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class BeregningsresultatXmlTjenesteImpl extends BeregningsresultatXmlTjeneste {

    private UttakXmlTjenesteImpl uttakXmlTjeneste;

    public BeregningsresultatXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public BeregningsresultatXmlTjenesteImpl(@FagsakYtelseTypeRef("FP") BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste,
                                                       @FagsakYtelseTypeRef("FP") YtelseXmlTjeneste ytelseXmlTjeneste,
                                                       @FagsakYtelseTypeRef("FP") UttakXmlTjenesteImpl uttakXmlTjeneste) {
        super(beregningsgrunnlagXmlTjeneste, ytelseXmlTjeneste);
        this.uttakXmlTjeneste = uttakXmlTjeneste;
    }

    @Override
    public void setEkstraInformasjonPåBeregningsresultat(Beregningsresultat beregningsresultat, Behandling behandling) {
        uttakXmlTjeneste.setUttak(beregningsresultat, behandling);

    }

}
