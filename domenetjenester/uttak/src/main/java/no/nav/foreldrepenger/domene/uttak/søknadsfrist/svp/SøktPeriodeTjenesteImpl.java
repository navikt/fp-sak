package no.nav.foreldrepenger.domene.uttak.søknadsfrist.svp;

import java.util.ArrayList;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.svp.RegelmodellSøknaderMapper;
import no.nav.foreldrepenger.domene.uttak.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.svangerskapspenger.domene.søknad.Søknad;
import no.nav.svangerskapspenger.tjeneste.opprettperioder.UttaksperioderTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class SøktPeriodeTjenesteImpl implements SøktPeriodeTjeneste {

    private RegelmodellSøknaderMapper regelmodellSøknaderMapper;
    private UttaksperioderTjeneste uttaksperioderTjeneste = new UttaksperioderTjeneste();

    public SøktPeriodeTjenesteImpl() {
        // For CDI
    }

    @Inject
    public SøktPeriodeTjenesteImpl(RegelmodellSøknaderMapper regelmodellSøknaderMapper) {
        this.regelmodellSøknaderMapper = regelmodellSøknaderMapper;
    }

    @Override
    public Optional<LocalDateInterval> finnSøktPeriode(UttakInput input) {
        var alleSøknader = new ArrayList<Søknad>();
        alleSøknader.addAll(regelmodellSøknaderMapper.hentSøknader(input));
        var uttaksperioder = uttaksperioderTjeneste.opprett(alleSøknader);

        var førsteUttaksdato = uttaksperioder.finnFørsteUttaksdato();
        var sisteUttaksdato = uttaksperioder.finnSisteUttaksdato();

        if (førsteUttaksdato.isPresent() && sisteUttaksdato.isPresent()) {
            return Optional.of(new LocalDateInterval(førsteUttaksdato.get(), sisteUttaksdato.get()));
        }
        return Optional.empty();
    }

}
