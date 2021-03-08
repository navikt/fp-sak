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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
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
        Optional<YtelseFordelingAggregat> yfAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.getBehandlingId());
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
        return yfAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .map(OppgittPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    public AktivitetGradering finnAktivitetGraderinger(BehandlingReferanse ref) {
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.getBehandlingId());
        return new AktivitetGradering(utled(ref, ytelseFordelingAggregat));
    }

    private List<AndelGradering> utled(BehandlingReferanse ref, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        if (!ytelseFordelingAggregat.isPresent()) {
            return List.of();
        }
        OppgittFordelingEntitet søknadsperioder = ytelseFordelingAggregat.get().getGjeldendeSøknadsperioder();
        List<PeriodeMedGradering> perioderMedGradering = fraSøknad(søknadsperioder.getOppgittePerioder());

        if (ref.erRevurdering()) {
            Long originalBehandling = ref.getOriginalBehandlingId()
                    .orElseThrow(() -> new IllegalStateException("Forventer original behandling i revurdering"));
            // Første periode i søknad kan starte midt i periode i vedtak
            Optional<LocalDate> førsteDatoSøknad = førsteDatoSøknad(søknadsperioder.getOppgittePerioder());
            List<PeriodeMedGradering> perioderMedGraderingFraVedtak = fraVedtak(førsteDatoSøknad, originalBehandling);
            perioderMedGradering.addAll(perioderMedGraderingFraVedtak);
        }

        return map(perioderMedGradering);
    }

    private List<AndelGradering> map(List<PeriodeMedGradering> perioderMedGradering) {
        Map<AndelGradering, AndelGradering.Builder> map = new HashMap<>();
        perioderMedGradering.forEach(periodeMedGradering -> {
            AktivitetStatus aktivitetStatus = periodeMedGradering.aktivitetStatus;
            AndelGradering.Builder nyBuilder = AndelGradering.builder()
                    .medStatus(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.fraKode(aktivitetStatus.getKode()));
            if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus)) {
                Arbeidsgiver arbeidsgiver = periodeMedGradering.arbeidsgiver;
                Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
                nyBuilder.medArbeidsgiver(arbeidsgiver);
            }
            AndelGradering andelGradering = nyBuilder.build();

            AndelGradering.Builder builder = map.get(andelGradering);
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
                .filter(oppgittPeriode -> oppgittPeriode.isGradert())
                .map(this::map)
                .collect(Collectors.toList());
    }

    private PeriodeMedGradering map(OppgittPeriodeEntitet gradertPeriode) {
        return new PeriodeMedGradering(gradertPeriode.getFom(), gradertPeriode.getTom(), gradertPeriode.getArbeidsprosent(),
                mapAktivitetStatus(gradertPeriode),
                gradertPeriode.getArbeidsgiver() == null ? null : mapArbeidsgiver(gradertPeriode.getArbeidsgiver()));
    }

    private List<PeriodeMedGradering> fraVedtak(Optional<LocalDate> førsteDatoSøknad, Long originalBehandling) {
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(originalBehandling);
        if (uttak.isEmpty()) {
            return List.of();
        }

        return uttak.get().getGjeldendePerioder().stream()
                .filter(periode -> førsteDatoSøknad.isEmpty() || periode.getFom().isBefore(førsteDatoSøknad.get()))
                .filter(periode -> gradertAktivitet(periode).isPresent())
                .map(periode -> map(periode, gradertAktivitet(periode).orElseThrow(), førsteDatoSøknad))
                .collect(Collectors.toList());

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
                .map((no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver a) -> mapArbeidsgiver(a));
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
        if (oppgittPeriode.isArbeidstaker()) {
            return AktivitetStatus.ARBEIDSTAKER;
        } else if (oppgittPeriode.isFrilanser()) {
            return AktivitetStatus.FRILANSER;
        } else if (oppgittPeriode.isSelvstendig()) {
            return AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
        } else {
            throw new IllegalStateException("Mangelfull søknad: Mangler informasjon om det er FL eller SN som graderes");
        }
    }

    public static class PeriodeMedGradering {

        private final LocalDate fom;
        private final LocalDate tom;
        private final BigDecimal arbeidsprosent;
        private final AktivitetStatus aktivitetStatus;
        private final Arbeidsgiver arbeidsgiver;

        PeriodeMedGradering(LocalDate fom,
                LocalDate tom,
                BigDecimal arbeidsprosent,
                AktivitetStatus aktivitetStatus,
                Arbeidsgiver arbeidsgiver) {

            this.fom = fom;
            this.tom = tom;
            this.arbeidsprosent = arbeidsprosent;
            this.aktivitetStatus = aktivitetStatus;
            this.arbeidsgiver = arbeidsgiver;
        }
    }
}
