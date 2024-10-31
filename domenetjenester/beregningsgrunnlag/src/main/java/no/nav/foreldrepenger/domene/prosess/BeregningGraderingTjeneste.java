package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
public class BeregningGraderingTjeneste {
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    BeregningGraderingTjeneste() {
        // CDI
    }

    @Inject
    public BeregningGraderingTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste, YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.uttakTjeneste = uttakTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public List<PeriodeMedGradering> finnPerioderMedGradering(BehandlingReferanse referanse) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(referanse.behandlingId());
        return ytelseFordelingAggregat.map(fordelingAggregat -> finnPerioderMedGradering(referanse, fordelingAggregat)).orElseGet(List::of);
    }

    private List<PeriodeMedGradering> finnPerioderMedGradering(BehandlingReferanse ref,
                                                                                           YtelseFordelingAggregat ytelseFordelingAggregat) {
        var søknadsperioder = ytelseFordelingAggregat.getGjeldendeFordeling();
        var perioderMedGradering = new ArrayList<>(fraSøknad(søknadsperioder.getPerioder()));

        if (ref.erRevurdering()) {
            var originalBehandling = ref.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Forventer original behandling i revurdering"));
            // Første periode i søknad kan starte midt i periode i vedtak
            var førsteDatoSøknad = førsteDatoSøknad(søknadsperioder.getPerioder());
            var perioderMedGraderingFraVedtak = fraVedtak(førsteDatoSøknad, originalBehandling);
            perioderMedGradering.addAll(perioderMedGraderingFraVedtak);
        }
        return perioderMedGradering;
    }
    private List<PeriodeMedGradering> fraSøknad(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .filter(OppgittPeriodeEntitet::isGradert)
            .map(this::map)
            .toList();
    }

    private PeriodeMedGradering map(OppgittPeriodeEntitet gradertPeriode) {
        return new PeriodeMedGradering(gradertPeriode.getFom(), gradertPeriode.getTom(), gradertPeriode.getArbeidsprosent(),
            mapAktivitetStatus(gradertPeriode),
            gradertPeriode.getArbeidsgiver());
    }

    private List<PeriodeMedGradering> fraVedtak(Optional<LocalDate> førsteDatoSøknad, Long originalBehandling) {
        var uttak = uttakTjeneste.hentHvisEksisterer(originalBehandling);
        return uttak.map(foreldrepengerUttak -> foreldrepengerUttak.getGjeldendePerioder()
            .stream()
            .filter(periode -> førsteDatoSøknad.isEmpty() || periode.getFom().isBefore(førsteDatoSøknad.get()))
            .filter(periode -> gradertAktivitet(periode).isPresent())
            .map(periode -> map(periode, gradertAktivitet(periode).orElseThrow(), førsteDatoSøknad))
            .toList()).orElseGet(List::of);

    }

    private Optional<ForeldrepengerUttakPeriodeAktivitet> gradertAktivitet(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter().stream().filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode).findFirst();
    }
    private PeriodeMedGradering map(ForeldrepengerUttakPeriode gradertPeriode,
                                                                ForeldrepengerUttakPeriodeAktivitet gradertAktivitet,
                                                                Optional<LocalDate> førsteDatoSøknad) {
        final LocalDate tom;
        if (førsteDatoSøknad.isPresent()) {
            tom = førstAv(førsteDatoSøknad.get().minusDays(1), gradertPeriode.getTom());
        } else {
            tom = gradertPeriode.getTom();
        }
        var arbeidsgiver = gradertAktivitet.getArbeidsgiver();
        return new PeriodeMedGradering(gradertPeriode.getFom(), tom, gradertAktivitet.getArbeidsprosent(),
            mapAktivitetStatus(gradertAktivitet.getUttakArbeidType()),
            arbeidsgiver.orElse(null));
    }

    private LocalDate førstAv(LocalDate dato1, LocalDate dato2) {
        return dato1.isBefore(dato2) ? dato1 : dato2;
    }

    private Optional<LocalDate> førsteDatoSøknad(List<OppgittPeriodeEntitet> søknadsperioder) {
        return søknadsperioder.stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .map(OppgittPeriodeEntitet::getFom)
            .findFirst();
    }

    private static AktivitetStatus mapAktivitetStatus(UttakArbeidType uttakArbeidType) {
        if (UttakArbeidType.ORDINÆRT_ARBEID.equals(uttakArbeidType)) {
            return AktivitetStatus.ARBEIDSTAKER;
        }
        if (UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(uttakArbeidType)) {
            return AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        if (UttakArbeidType.FRILANS.equals(uttakArbeidType)) {
            return AktivitetStatus.FRILANSER;
        }
        if (UttakArbeidType.ANNET.equals(uttakArbeidType)) {
            throw new IllegalArgumentException("Kan ikke gradere " + UttakArbeidType.ANNET);
        }
        throw new IllegalArgumentException("Ukjent type " + uttakArbeidType);
    }

    private static AktivitetStatus mapAktivitetStatus(OppgittPeriodeEntitet oppgittPeriode) {
        if (oppgittPeriode.getGraderingAktivitetType() == null ) {
            throw new IllegalStateException("Mangelfull søknad: Mangler informasjon om det er FL eller SN som graderes");
        }

        return switch (oppgittPeriode.getGraderingAktivitetType()) {
            case ARBEID -> AktivitetStatus.ARBEIDSTAKER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> AktivitetStatus.FRILANSER;
        };
    }
}
