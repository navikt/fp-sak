package no.nav.foreldrepenger.ytelse.beregning.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class BeregnFeriepenger extends BeregnFeriepengerTjeneste {

    BeregnFeriepenger() {
        //NOSONAR
    }

    /**
     *
     * @param antallDagerFeriepenger - Antall dager i feriepengerperioden for svangerskapspenger
     */
    @Inject
    public BeregnFeriepenger(BehandlingRepositoryProvider repositoryProvider,
                                        @KonfigVerdi(value = "svp.antall.dager.feriepenger", defaultVerdi = "64") int antallDagerFeriepenger) {
        super(repositoryProvider, antallDagerFeriepenger);
    }
}
