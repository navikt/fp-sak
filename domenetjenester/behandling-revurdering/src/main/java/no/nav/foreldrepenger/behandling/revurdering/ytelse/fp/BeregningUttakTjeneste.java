package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsgiver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

/**
 * Tjeneste som brukes av beregning for å populere beregningsgrunnlagInput med uttaksdata
 */
@ApplicationScoped
public class BeregningUttakTjeneste {

    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    BeregningUttakTjeneste() {
        // CDI
    }

    @Inject
    public BeregningUttakTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste,
                                  YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.uttakTjeneste = uttakTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    /**
     * Siden beregning kjøres før uttak må vi gjøre en estimering for når siste uttaksdag er.
     * Vi ser på ytelsesfordelingen om vi har det, eller ser vi på forrige behandlings uttaksresultat.
     * @param ref referanse til behandlingen
     * @return
     */
    public Optional<LocalDate> finnSisteTilnærmedeUttaksdato(BehandlingReferanse ref) {
        var yfAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId());
        return yfAggregat.map(this::finnSisteSøkteUttaksdag)
            .orElseGet(() -> ref.getOriginalBehandlingId()
            .flatMap(oid -> uttakTjeneste.hentUttakHvisEksisterer(oid))
            .flatMap(this::finnSisteInnvilgedeUttak));
    }

    private Optional<LocalDate> finnSisteInnvilgedeUttak(ForeldrepengerUttak uttak) {
        return uttak.getGjeldendePerioder()
            .stream()
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getTom)
            .max(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnSisteSøkteUttaksdag(YtelseFordelingAggregat yfAggregat) {
        return yfAggregat.getGjeldendeFordeling().getPerioder()
            .stream()
            .map(OppgittPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    public AktivitetGradering finnAktivitetGraderinger(BehandlingReferanse ref) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId());
        return new AktivitetGradering(utled(ref, ytelseFordelingAggregat));
    }

    private List<AndelGradering> utled(BehandlingReferanse ref, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        if (ytelseFordelingAggregat.isEmpty()) {
            return List.of();
        }
        var søknadsperioder = ytelseFordelingAggregat.get().getGjeldendeFordeling();
        var perioderMedGradering = new ArrayList<>(fraSøknad(søknadsperioder.getPerioder()));

        if (ref.erRevurdering()) {
            var originalBehandling = ref.getOriginalBehandlingId()
                    .orElseThrow(() -> new IllegalStateException("Forventer original behandling i revurdering"));
            // Første periode i søknad kan starte midt i periode i vedtak
            var førsteDatoSøknad = førsteDatoSøknad(søknadsperioder.getPerioder());
            var perioderMedGraderingFraVedtak = fraVedtak(førsteDatoSøknad, originalBehandling);
            perioderMedGradering.addAll(perioderMedGraderingFraVedtak);
        }

        return map(perioderMedGradering);
    }

    private List<AndelGradering> map(List<PeriodeMedGradering> perioderMedGradering) {
        Map<AndelGradering, AndelGradering.Builder> map = new HashMap<>();
        perioderMedGradering.forEach(periodeMedGradering -> {
            var aktivitetStatus = periodeMedGradering.aktivitetStatus;
            var nyBuilder = AndelGradering.builder()
                    .medStatus(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.fraKode(aktivitetStatus.getKode()));
            if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus)) {
                var arbeidsgiver = periodeMedGradering.arbeidsgiver;
                Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
                nyBuilder.medArbeidsgiver(arbeidsgiver);
            }
            var andelGradering = nyBuilder.build();

            var builder = map.get(andelGradering);
            if (builder == null) {
                builder = nyBuilder;
                map.put(andelGradering, nyBuilder);
            }

            builder.leggTilGradering(mapGradering(periodeMedGradering));
        });
        return new ArrayList<>(map.keySet());
    }

    private AndelGradering.Gradering mapGradering(PeriodeMedGradering periodeMedGradering) {
        return new AndelGradering.Gradering(periodeMedGradering.fom, periodeMedGradering.tom, periodeMedGradering.arbeidsprosent);
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
                gradertPeriode.getArbeidsgiver() == null ? null : mapArbeidsgiver(gradertPeriode.getArbeidsgiver()));
    }

    private List<PeriodeMedGradering> fraVedtak(Optional<LocalDate> førsteDatoSøknad, Long originalBehandling) {
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(originalBehandling);
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
        var arbeidsgiver = gradertAktivitet.getArbeidsgiver()
                .map(IAYMapperTilKalkulus::mapArbeidsgiver);
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

    public record PeriodeMedGradering(LocalDate fom, LocalDate tom, BigDecimal arbeidsprosent,
                                      AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver) {
    }
}
