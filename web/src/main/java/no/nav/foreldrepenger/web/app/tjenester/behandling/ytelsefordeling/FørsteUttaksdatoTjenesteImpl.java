package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class FørsteUttaksdatoTjenesteImpl implements FørsteUttaksdatoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UttakRepository uttakRepository;

    FørsteUttaksdatoTjenesteImpl() {
        // CDI
    }

    @Inject
    public FørsteUttaksdatoTjenesteImpl(YtelseFordelingTjeneste ytelseFordelingTjeneste, UttakRepository uttakRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Optional<LocalDate> finnFørsteUttaksdato(Behandling behandling) {
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        Optional<LocalDate> førsteUttaksdato;
        if (førsteUttaksdatoErAvklart(ytelseFordelingAggregat)) {
            førsteUttaksdato = Optional.ofNullable(ytelseFordelingAggregat.getAvklarteDatoer().get().getFørsteUttaksdato());
        } else if (behandling.erRevurdering()) {
            førsteUttaksdato = finnFørsteUttaksdatoRevurdering(behandling);
        } else {
            førsteUttaksdato = finnFørsteUttaksdatoFørstegangsbehandling(behandling);
        }
        return førsteUttaksdato;
    }

    private Optional<LocalDate> finnFørsteUttaksdatoFørstegangsbehandling(Behandling behandling) {
        List<OppgittPeriodeEntitet> oppgittePerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeSøknadsperioder().getOppgittePerioder();
        if (oppgittePerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sortert(oppgittePerioder).get(0).getFom());
    }

    private boolean førsteUttaksdatoErAvklart(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getAvklarteDatoer().isPresent() && ytelseFordelingAggregat.getAvklarteDatoer().get().getFørsteUttaksdato() != null;
    }

    private Optional<LocalDate> finnFørsteUttaksdatoRevurdering(Behandling behandling) {
        Behandling revurdering = behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        Optional<UttakResultatEntitet> uttak = uttakRepository.hentUttakResultatHvisEksisterer(revurdering.getId());
        if (!uttak.isPresent()) {
            return finnFørsteUttaksdatoFørstegangsbehandling(behandling);
        }
        return Optional.of(uttak.get().getGjeldendePerioder().getPerioder().get(0).getFom());
    }

    private List<OppgittPeriodeEntitet> sortert(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).collect(Collectors.toList());
    }
}
