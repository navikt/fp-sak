package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class BeregningOversiktDtoTjeneste {

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public BeregningOversiktDtoTjeneste(BeregningTjeneste beregningTjeneste,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public BeregningOversiktDtoTjeneste() {
        // CDI
    }

    public Optional<FpSak.Beregningsgrunnlag> lagDtoForBehandling(BehandlingReferanse ref) {
        // Ønsker ikke sende over saker i prod før vi er trygge på modellen
        if (Environment.current().isProd()) {
            return Optional.empty();
        }

        var grBeregningsgrunnlag = beregningTjeneste.hent(ref);
        var inntektsmeldinger = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(List.of());
        return grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).flatMap(bg -> mapBeregning(bg, inntektsmeldinger));
    }

    private Optional<FpSak.Beregningsgrunnlag> mapBeregning(Beregningsgrunnlag beregningsgrunnlag, List<Inntektsmelding> inntektsmeldinger) {
        if (gjelderBesteberegning(beregningsgrunnlag)) {
            // TODO -- implementeres senere
            return Optional.empty();
        }
        var aktivitetStatuser = beregningsgrunnlag.getAktivitetStatuser().stream().map(this::mapAktivitetStatusMedHjemmel).toList();
        var beregningsAndeler = førsteBeregningsperiode(beregningsgrunnlag).map(førstePeriode -> mapAndeler(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList(), inntektsmeldinger)).orElse(List.of());
        return Optional.of(new FpSak.Beregningsgrunnlag(beregningsgrunnlag.getSkjæringstidspunkt(), beregningsAndeler, aktivitetStatuser));
    }

    private static Optional<BeregningsgrunnlagPeriode> førsteBeregningsperiode(Beregningsgrunnlag beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(bgp -> bgp.getPeriode().getFomDato().equals(beregningsgrunnlag.getSkjæringstidspunkt()))
            .findFirst();
    }

    private boolean gjelderBesteberegning(Beregningsgrunnlag beregningsgrunnlag) {
        return førsteBeregningsperiode(beregningsgrunnlag).map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(List.of())
            .stream()
            .anyMatch(andel -> andel.getBesteberegnetPrÅr() != null);
    }

    private List<FpSak.Beregningsgrunnlag.BeregningsAndel> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList, List<Inntektsmelding> inntektsmeldinger) {
        var andelerFraStart = beregningsgrunnlagPrStatusOgAndelList.stream()
            .filter(a -> a.getKilde().equals(AndelKilde.PROSESS_START))
            .toList();
        var andelerUtenArbeidsforhold = andelerFraStart.stream()
            .filter(a -> !a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .map(this::mapAndelUtenArbeidsforhold)
            .toList();
        var arbeidsandeler = andelerFraStart.stream().filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).toList();
        var andelerMedArbeidsforhold = mapAndelerMedArbeidsforhold(arbeidsandeler, inntektsmeldinger);

        return Stream.of(andelerMedArbeidsforhold, andelerUtenArbeidsforhold)
                    .flatMap(List::stream)
                    .toList();
    }

    private List<FpSak.Beregningsgrunnlag.BeregningsAndel> mapAndelerMedArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> arbeidsandeler, List<Inntektsmelding> inntektsmeldinger) {
        var finnesArbeidsandelUtenArbeidstaker = arbeidsandeler.stream().anyMatch(a -> a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).isEmpty());
        if (finnesArbeidsandelUtenArbeidstaker) {
            // TODO Implementer
            return List.of();
        }
        Map<Arbeidsgiver, List<BeregningsgrunnlagPrStatusOgAndel>> andelerPrArbeidsgiver = arbeidsandeler.stream()
            .collect(Collectors.groupingBy(
                andel -> andel.getBgAndelArbeidsforhold()
                    .map(BGAndelArbeidsforhold::getArbeidsgiver)
                    .orElseThrow()
            ));
        return andelerPrArbeidsgiver.entrySet().stream().map(entry -> {
            var erSkjønnsfastsatt = entry.getValue().stream().anyMatch(a -> a.getOverstyrtPrÅr() != null);
            var finnesIM = inntektsmeldinger.stream().anyMatch(im -> im.getArbeidsgiver().equals(entry.getKey()));
            var fastsattPerÅr = entry.getValue().stream()
                .map(erSkjønnsfastsatt ? BeregningsgrunnlagPrStatusOgAndel::getOverstyrtPrÅr : BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            var inntektsKilde = erSkjønnsfastsatt ? FpSak.Beregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : finnesIM ? FpSak.Beregningsgrunnlag.InntektsKilde.INNTEKTSMELDING : FpSak.Beregningsgrunnlag.InntektsKilde.A_INNTEKT;

            var refusjonPerÅr = entry.getValue().stream()
                .map(a -> a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            var refusjonPerMnd = refusjonPerÅr.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

            var arbeidsforhold = new FpSak.Beregningsgrunnlag.Arbeidsforhold(entry.getKey().getIdentifikator(), refusjonPerMnd);

            var dagsatsArbeidsgiver = entry.getValue().stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getDagsatsArbeidsgiver)
                .filter(Objects::nonNull)
                .reduce(Long::sum)
                .orElse(0L);
            var dagsatsSøker = entry.getValue().stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getDagsatsBruker)
                .filter(Objects::nonNull)
                .reduce(Long::sum)
                .orElse(0L);

            return new FpSak.Beregningsgrunnlag.BeregningsAndel(FpSak.Beregningsgrunnlag.AktivitetStatus.ARBEIDSTAKER, fastsattPerÅr, inntektsKilde, arbeidsforhold, BigDecimal.valueOf(dagsatsArbeidsgiver), BigDecimal.valueOf(dagsatsSøker));
        }).toList();
    }

    private FpSak.Beregningsgrunnlag.BeregningsAndel mapAndelUtenArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        var erSkjønsfastsatt = andel.getOverstyrtPrÅr() != null;
        var fastsattPrÅr = erSkjønsfastsatt ? andel.getOverstyrtPrÅr() : andel.getBeregnetPrÅr();
        if (andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            var inntektsKilde = erSkjønsfastsatt ? FpSak.Beregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : FpSak.Beregningsgrunnlag.InntektsKilde.PENSJONSGIVENDE_INNTEKT;
            return new FpSak.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.getAktivitetStatus()), fastsattPrÅr, inntektsKilde, null, null,
                mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER)) {
            var inntektsKilde = erSkjønsfastsatt ? FpSak.Beregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : FpSak.Beregningsgrunnlag.InntektsKilde.A_INNTEKT;
            return new FpSak.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.getAktivitetStatus()), fastsattPrÅr, inntektsKilde, null, null,
                mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            return new FpSak.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.getAktivitetStatus()), fastsattPrÅr, FpSak.Beregningsgrunnlag.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, null,
                mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)) {
            return new FpSak.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.getAktivitetStatus()), fastsattPrÅr, FpSak.Beregningsgrunnlag.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, null,
                mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.BRUKERS_ANDEL)) {
            return new FpSak.Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andel.getAktivitetStatus()), fastsattPrÅr, FpSak.Beregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT, null, null,
                mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL)) {
            throw new IllegalStateException("Støttes ikke ennå");
        }
        throw new IllegalStateException("Ukjent aktivitetstatus uten arbeidsforhold: " + andel.getAktivitetStatus());
    }

    private static BigDecimal mapDagsats(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (andel.getDagsats() == null) {
            return null;
        }
        return BigDecimal.valueOf(andel.getDagsats());
    }

    private FpSak.Beregningsgrunnlag.AktivitetStatus mapAktivitetstatus(AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> FpSak.Beregningsgrunnlag.AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> FpSak.Beregningsgrunnlag.AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> FpSak.Beregningsgrunnlag.AktivitetStatus.DAGPENGER;
            case FRILANSER -> FpSak.Beregningsgrunnlag.AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> FpSak.Beregningsgrunnlag.AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> FpSak.Beregningsgrunnlag.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> FpSak.Beregningsgrunnlag.AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> FpSak.Beregningsgrunnlag.AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> FpSak.Beregningsgrunnlag.AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> FpSak.Beregningsgrunnlag.AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> FpSak.Beregningsgrunnlag.AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> FpSak.Beregningsgrunnlag.AktivitetStatus.KUN_YTELSE;
            case VENTELØNN_VARTPENGER, TTLSTØTENDE_YTELSE, UDEFINERT -> null;
        };
    }

    private FpSak.Beregningsgrunnlag.BeregningAktivitetStatus mapAktivitetStatusMedHjemmel(BeregningsgrunnlagAktivitetStatus aks) {
        return new FpSak.Beregningsgrunnlag.BeregningAktivitetStatus(mapAktivitetstatus(aks.getAktivitetStatus()), aks.getHjemmel());
    }
}
