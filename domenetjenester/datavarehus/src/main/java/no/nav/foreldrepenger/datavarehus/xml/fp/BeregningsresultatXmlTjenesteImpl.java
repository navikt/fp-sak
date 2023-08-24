package no.nav.foreldrepenger.datavarehus.xml.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BeregningsresultatXmlTjenesteImpl extends BeregningsresultatXmlTjeneste {

    private UttakXmlTjeneste uttakXmlTjeneste;

    public BeregningsresultatXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public BeregningsresultatXmlTjenesteImpl(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) BeregningsgrunnlagXmlTjeneste beregningsgrunnlagXmlTjeneste,
                                                       @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) YtelseXmlTjeneste ytelseXmlTjeneste,
                                                       @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) UttakXmlTjeneste uttakXmlTjeneste) {
        super(beregningsgrunnlagXmlTjeneste, ytelseXmlTjeneste);
        this.uttakXmlTjeneste = uttakXmlTjeneste;
    }

    @Override
    public void setEkstraInformasjonPåBeregningsresultat(Beregningsresultat beregningsresultat, Behandling behandling) {
        uttakXmlTjeneste.setUttak(beregningsresultat, behandling);

    }

}
