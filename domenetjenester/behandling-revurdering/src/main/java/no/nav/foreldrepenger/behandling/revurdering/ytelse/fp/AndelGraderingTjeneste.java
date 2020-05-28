package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsgiver;

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

import no.nav.folketrygdloven.kalkulator.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
public class AndelGraderingTjeneste {

    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    AndelGraderingTjeneste() {
        //CDI
    }

    @Inject
    public AndelGraderingTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste,
                                  YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.uttakTjeneste = uttakTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public AktivitetGradering utled(BehandlingReferanse ref) {
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
            Long originalBehandling = ref.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Forventer original behandling i revurdering"));
            //Første periode i søknad kan starte midt i periode i vedtak
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
                .medStatus(no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.AktivitetStatus.fraKode(aktivitetStatus.getKode()));
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
            .filter(oppgittPeriode -> oppgittPeriode.erGradert())
            .map(this::map)
            .collect(Collectors.toList());
    }

    private PeriodeMedGradering map(OppgittPeriodeEntitet gradertPeriode) {
        return new PeriodeMedGradering(gradertPeriode.getFom(), gradertPeriode.getTom(), gradertPeriode.getArbeidsprosent(), mapAktivitetStatus(gradertPeriode),
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
        if (oppgittPeriode.getErArbeidstaker()) {
            return AktivitetStatus.ARBEIDSTAKER;
        } else if (oppgittPeriode.getErFrilanser()) {
            return AktivitetStatus.FRILANSER;
        } else if (oppgittPeriode.getErSelvstendig()) {
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
