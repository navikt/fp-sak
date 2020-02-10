package no.nav.foreldrepenger.domene.vedtak.es;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.domene.vedtak.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.es.v2.BeregningsgrunnlagEngangsstoenad;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsgrunnlag;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class BeregningsgrunnlagXmlTjenesteImpl implements BeregningsgrunnlagXmlTjeneste {

    private ObjectFactory beregningObjectFactory;
    private LegacyESBeregningRepository beregningRepository;

    public BeregningsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public BeregningsgrunnlagXmlTjenesteImpl(LegacyESBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
        this.beregningObjectFactory = new ObjectFactory();
    }

    @Override
    public void setBeregningsgrunnlag(Beregningsresultat beregningsresultat, Behandling behandling) {
        BeregningsgrunnlagEngangsstoenad beregningsgrunnlag = beregningObjectFactory.createBeregningsgrunnlagEngangsstoenad();
        Optional<LegacyESBeregning> sisteBeregning = beregningRepository.getSisteBeregning(behandling.getId());
        if (sisteBeregning.isPresent()) {
            beregningsgrunnlag.setAntallBarn(VedtakXmlUtil.lagIntOpplysning((int) sisteBeregning.get().getAntallBarn()));
            beregningsgrunnlag.setSats(VedtakXmlUtil.lagLongOpplysning(sisteBeregning.get().getSatsVerdi()));
        }
        Beregningsgrunnlag beregningsgrunnlag1 = new Beregningsgrunnlag();
        beregningsgrunnlag1.getAny().add(beregningObjectFactory.createBeregningsgrunnlagEngangsstoenad(beregningsgrunnlag));
        beregningsresultat.setBeregningsgrunnlag(beregningsgrunnlag1);
    }
}
