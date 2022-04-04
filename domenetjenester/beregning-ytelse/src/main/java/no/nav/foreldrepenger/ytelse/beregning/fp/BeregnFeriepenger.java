package no.nav.foreldrepenger.ytelse.beregning.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BeregnFeriepenger extends BeregnFeriepengerTjeneste {

    BeregnFeriepenger() {
        //NOSONAR
    }

    /**
     * @param antallDagerFeriepenger - Antall dager i feriepengerperioden for foreldrepenger ved 100% dekningsgrad
     */
    @Inject
    public BeregnFeriepenger(BehandlingRepositoryProvider repositoryProvider,
                                       @KonfigVerdi(value = "fp.antall.dager.feriepenger", defaultVerdi = "60") int antallDagerFeriepenger) {
        super(repositoryProvider, antallDagerFeriepenger);
    }
}
