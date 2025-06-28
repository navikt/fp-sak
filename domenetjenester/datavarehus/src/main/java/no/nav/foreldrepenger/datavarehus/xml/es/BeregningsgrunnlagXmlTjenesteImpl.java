package no.nav.foreldrepenger.datavarehus.xml.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsgrunnlag;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class BeregningsgrunnlagXmlTjenesteImpl implements BeregningsgrunnlagXmlTjeneste {

    private ObjectFactory beregningObjectFactory;
    private EngangsstønadBeregningRepository beregningRepository;

    public BeregningsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public BeregningsgrunnlagXmlTjenesteImpl(EngangsstønadBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
        this.beregningObjectFactory = new ObjectFactory();
    }

    @Override
    public void setBeregningsgrunnlag(Beregningsresultat beregningsresultat, Behandling behandling) {
        var beregningsgrunnlag = beregningObjectFactory.createBeregningsgrunnlagEngangsstoenad();
        var sisteBeregning = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        if (sisteBeregning.isPresent()) {
            beregningsgrunnlag.setAntallBarn(VedtakXmlUtil.lagIntOpplysning((int) sisteBeregning.get().getAntallBarn()));
            beregningsgrunnlag.setSats(VedtakXmlUtil.lagLongOpplysning(sisteBeregning.get().getSatsVerdi()));
        }
        var beregningsgrunnlag1 = new Beregningsgrunnlag();
        beregningsgrunnlag1.getAny().add(beregningObjectFactory.createBeregningsgrunnlagEngangsstoenad(beregningsgrunnlag));
        beregningsresultat.setBeregningsgrunnlag(beregningsgrunnlag1);
    }
}
