package no.nav.foreldrepenger.datavarehus.xml.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class BeregningsresultatXmlTjenesteImpl extends BeregningsresultatXmlTjeneste {

    public BeregningsresultatXmlTjenesteImpl() {
    }

    @Inject
    public BeregningsresultatXmlTjenesteImpl(@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste,
                                                     @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) YtelseXmlTjeneste ytelseXmlTjeneste) {
        super(beregningsgrunnlagXmlTjeneste, ytelseXmlTjeneste);
    }

    @Override
    public void setEkstraInformasjonPåBeregningsresultat(Beregningsresultat beregningsresultat, Behandling behandling) {
        //ES trenger ikke noe ekstra iforhold til det som lagres i superklassen.
    }


}
