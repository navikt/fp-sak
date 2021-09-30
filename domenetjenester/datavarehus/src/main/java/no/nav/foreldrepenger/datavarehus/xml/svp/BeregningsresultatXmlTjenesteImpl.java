package no.nav.foreldrepenger.datavarehus.xml.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class BeregningsresultatXmlTjenesteImpl extends BeregningsresultatXmlTjeneste {

    private UttakXmlTjenesteImpl uttakXmlTjeneste;

    public BeregningsresultatXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public BeregningsresultatXmlTjenesteImpl(@FagsakYtelseTypeRef("SVP") BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste,
                                                           @FagsakYtelseTypeRef("SVP") YtelseXmlTjeneste ytelseXmlTjeneste,
                                                           @FagsakYtelseTypeRef("SVP") UttakXmlTjenesteImpl uttakXmlTjeneste) {
        //TODO PFP-7642: Her må det undersøkes om det trengs en egen UttakXmlTjenesteForeldrepenger eller om samme skal gjenbrukes og annotasjon fjernes
        super(beregningsgrunnlagXmlTjeneste, ytelseXmlTjeneste);
        this.uttakXmlTjeneste = uttakXmlTjeneste;
    }

    @Override
    public void setEkstraInformasjonPåBeregningsresultat(Beregningsresultat beregningsresultat, Behandling behandling) {
        uttakXmlTjeneste.setUttak(beregningsresultat, behandling);
    }
}
