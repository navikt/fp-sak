package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class FørsteUttaksdatoTjenesteImpl implements FørsteUttaksdatoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    FørsteUttaksdatoTjenesteImpl() {
        // CDI
    }

    @Inject
    public FørsteUttaksdatoTjenesteImpl(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public Optional<LocalDate> finnFørsteUttaksdato(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var førsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer()
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
        if (førsteUttaksdato.isPresent()) {
            return førsteUttaksdato;
        }
        return behandling.erRevurdering() ? finnFørsteUttaksdatoRevurdering(
            behandling) : finnFørsteUttaksdatoFørstegangsbehandling(behandling);
    }

    private Optional<LocalDate> finnFørsteUttaksdatoFørstegangsbehandling(Behandling behandling) {
        var oppgittePerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId())
            .getGjeldendeFordeling()
            .getPerioder();
        if (oppgittePerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sortert(oppgittePerioder).get(0).getFom());
    }

    private Optional<LocalDate> finnFørsteUttaksdatoRevurdering(Behandling behandling) {
        var revurderingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(revurderingId);
        if (uttak.isEmpty()) {
            return finnFørsteUttaksdatoFørstegangsbehandling(behandling);
        }
        return Optional.of(uttak.get().getGjeldendePerioder().get(0).getFom());
    }

    private List<OppgittPeriodeEntitet> sortert(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .toList();
    }
}
