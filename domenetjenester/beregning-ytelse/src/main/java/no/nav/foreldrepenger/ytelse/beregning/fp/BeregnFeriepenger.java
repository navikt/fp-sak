package no.nav.foreldrepenger.ytelse.beregning.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BeregnFeriepenger extends BeregnFeriepengerTjeneste {

    BeregnFeriepenger() {
        //CDI
    }

    /**
     * @param antallDagerFeriepenger - Antall dager i feriepengerperioden for foreldrepenger ved 100% dekningsgrad
     */
    @Inject
    public BeregnFeriepenger(BehandlingRepositoryProvider repositoryProvider,
                             MapInputFraVLTilRegelGrunnlag inputTjeneste,
                             FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                             DekningsgradTjeneste dekningsgradTjeneste,
                             @KonfigVerdi(value = "fp.antall.dager.feriepenger", defaultVerdi = "60") int antallDagerFeriepenger) {
        super(repositoryProvider, inputTjeneste, fagsakRelasjonTjeneste, dekningsgradTjeneste, antallDagerFeriepenger);
    }

    @Override
    protected int finnTigjengeligeFeriepengedager(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        return antallDagerFeriepenger;
    }
}
