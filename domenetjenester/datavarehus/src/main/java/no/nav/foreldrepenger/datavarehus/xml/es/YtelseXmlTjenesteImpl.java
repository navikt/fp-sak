package no.nav.foreldrepenger.datavarehus.xml.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.TilkjentYtelse;
import no.nav.vedtak.felles.xml.vedtak.ytelse.es.v2.ObjectFactory;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class YtelseXmlTjenesteImpl implements YtelseXmlTjeneste {

    private ObjectFactory ytelseObjectFactory;
    private EngangsstønadBeregningRepository beregningRepository;

    public YtelseXmlTjenesteImpl() {
        //For Cdi
    }

    @Inject
    public YtelseXmlTjenesteImpl(EngangsstønadBeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
        ytelseObjectFactory = new ObjectFactory();
    }

    @Override
    public void setYtelse(Beregningsresultat beregningsresultat, Behandling behandling) {
        var engangstoenadYtelse = ytelseObjectFactory.createYtelseEngangsstoenad();
        var sisteBeregning = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        sisteBeregning.ifPresent(beregning -> engangstoenadYtelse.setBeloep(beregning.getBeregnetTilkjentYtelse()));
        var tilkjentYtelse = new TilkjentYtelse();
        tilkjentYtelse.getAny().add(ytelseObjectFactory.createYtelseEngangsstoenad(engangstoenadYtelse));
        beregningsresultat.setTilkjentYtelse(tilkjentYtelse);

    }
}
