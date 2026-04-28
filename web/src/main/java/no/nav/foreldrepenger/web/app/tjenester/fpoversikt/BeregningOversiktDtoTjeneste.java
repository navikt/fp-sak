package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
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
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BeregningOversiktDtoTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningOversiktDtoTjeneste.class);
    private static final Set<AktivitetStatus> IKKE_STØTTEDE_AKTIVITET_STATUSER = Set.of(AktivitetStatus.VENTELØNN_VARTPENGER,
        AktivitetStatus.TTLSTØTENDE_YTELSE, AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatus.DAGPENGER, AktivitetStatus.MILITÆR_ELLER_SIVIL,
        AktivitetStatus.BRUKERS_ANDEL);
    private static final Set<PeriodeÅrsak> IKKE_STØTTEDE_PERIODEÅRSAKER = Set.of(PeriodeÅrsak.NATURALYTELSE_BORTFALT,
        PeriodeÅrsak.NATURALYTELSE_TILKOMMER, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public BeregningOversiktDtoTjeneste(BeregningTjeneste beregningTjeneste,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                        ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    protected BeregningOversiktDtoTjeneste() {
        // CDI
    }

    public Optional<OversiktBeregningsgrunnlag> lagDtoForBehandling(BehandlingReferanse ref) {
        try {
            var grBeregningsgrunnlag = beregningTjeneste.hent(ref);
            var inntektsmeldinger = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId())
                .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
                .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
                .orElse(List.of());
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
            return grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).flatMap(bg -> mapBeregning(bg, inntektsmeldinger, skjæringstidspunkter.getFørsteUttaksdato()));
        } catch (Exception e) {
            LOG.info("Feil ved henting av beregningsgrunnlag for behandling {}", ref.behandlingId(), e);
            return Optional.empty();
        }
    }

    private Optional<OversiktBeregningsgrunnlag> mapBeregning(Beregningsgrunnlag beregningsgrunnlag,
                                                            List<Inntektsmelding> inntektsmeldinger,
                                                            LocalDate førsteUttaksdato) {
        if (gjelderSakInnsynIkkeStøtter(beregningsgrunnlag, førsteUttaksdato)) {
            // TODO -- implementeres senere
            return Optional.empty();
        }
        var aktivitetStatuser = beregningsgrunnlag.getAktivitetStatuser().stream().map(this::mapAktivitetStatusMedHjemmel).toList();
        var beregningsAndeler = førsteBeregningsperiode(beregningsgrunnlag, førsteUttaksdato).map(
            førstePeriode -> mapAndeler(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList(), inntektsmeldinger)).orElse(List.of());
        var grunnbeløp = beregningsgrunnlag.getGrunnbeløp() == null ? null : beregningsgrunnlag.getGrunnbeløp().getVerdi();

        return Optional.of(
            new OversiktBeregningsgrunnlag(beregningsgrunnlag.getSkjæringstidspunkt(), beregningsAndeler, aktivitetStatuser, grunnbeløp));
    }


    private static Optional<BeregningsgrunnlagPeriode> førsteBeregningsperiode(Beregningsgrunnlag beregningsgrunnlag, LocalDate førsteUttaksdato) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(bgp -> bgp.getPeriode().inkluderer(førsteUttaksdato))
            .findFirst();
    }

    private boolean gjelderSakInnsynIkkeStøtter(Beregningsgrunnlag beregningsgrunnlag, LocalDate førsteUttaksdato) {
        // Besteberegning er ikke støttet i innsyn enda, og det må implementeres støtte for det uavhengig av statusen dagpenger
        var erBesteberegnet = førsteBeregningsperiode(beregningsgrunnlag, førsteUttaksdato).map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(List.of())
            .stream()
            .anyMatch(andel -> andel.getBesteberegnetPrÅr() != null);

        // Saker med statuser som det ikke er laget støtte for å vise i innsyn enda
        var harIkkeStøttetStatus = beregningsgrunnlag.getAktivitetStatuser()
            .stream()
            .anyMatch(as -> IKKE_STØTTEDE_AKTIVITET_STATUSER.contains(as.getAktivitetStatus()));

        // Kan skje i et fåtall tilfeller, f.eks etterlønn / sluttpakke
        var harArbeidsandelUtenArbeidsgiver = førsteBeregningsperiode(beregningsgrunnlag, førsteUttaksdato).map(
                BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(List.of())
            .stream()
            .anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER) && a.getArbeidsgiver().isEmpty());

        // Disse har varierende dagsats fra grunnlaget, det er ikke støtte for å vise slike saker i innsyn enda
        var harTidsbegrensetEllerNaturalytelse = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .anyMatch(p -> harIkkeStøttetPeriodeårsak(p.getPeriodeÅrsaker()));

        return harIkkeStøttetStatus || erBesteberegnet || harArbeidsandelUtenArbeidsgiver || harTidsbegrensetEllerNaturalytelse;
    }

    private boolean harIkkeStøttetPeriodeårsak(List<PeriodeÅrsak> periodeÅrsaker) {
        return periodeÅrsaker.stream().anyMatch(IKKE_STØTTEDE_PERIODEÅRSAKER::contains);
    }

    private List<OversiktBeregningsgrunnlag.BeregningsAndel> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList,
                                                                      List<Inntektsmelding> inntektsmeldinger) {
        var andelerFraStart = beregningsgrunnlagPrStatusOgAndelList.stream().filter(a -> a.getKilde().equals(AndelKilde.PROSESS_START)).toList();
        var andelerUtenArbeidsforhold = andelerFraStart.stream()
            .filter(a -> !a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .map(this::mapAndelUtenArbeidsforhold)
            .toList();
        var arbeidsandeler = andelerFraStart.stream().filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).toList();
        var andelerMedArbeidsforhold = mapAndelerMedArbeidsforhold(arbeidsandeler, inntektsmeldinger);

        return Stream.of(andelerMedArbeidsforhold, andelerUtenArbeidsforhold).flatMap(List::stream).toList();
    }

    private List<OversiktBeregningsgrunnlag.BeregningsAndel> mapAndelerMedArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> arbeidsandeler,
                                                                                       List<Inntektsmelding> inntektsmeldinger) {

        Map<Arbeidsgiver, List<BeregningsgrunnlagPrStatusOgAndel>> andelerPrArbeidsgiver = arbeidsandeler.stream()
            .collect(Collectors.groupingBy(andel -> andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).orElseThrow()));
        return andelerPrArbeidsgiver.entrySet().stream().map(entry -> {
            var erSkjønnsfastsatt = entry.getValue().stream().anyMatch(a -> a.getOverstyrtPrÅr() != null);
            var finnesIM = inntektsmeldinger.stream().anyMatch(im -> im.getArbeidsgiver().equals(entry.getKey()));
            var fastsattPerÅr = entry.getValue()
                .stream()
                .map(erSkjønnsfastsatt ? BeregningsgrunnlagPrStatusOgAndel::getOverstyrtPrÅr : BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            var inntektsKilde = erSkjønnsfastsatt ? OversiktBeregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : finnesIM ? OversiktBeregningsgrunnlag.InntektsKilde.INNTEKTSMELDING : OversiktBeregningsgrunnlag.InntektsKilde.A_INNTEKT;

            var refusjonPerÅr = entry.getValue()
                .stream()
                .map(a -> a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
            var refusjonPerMnd = refusjonPerÅr.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

            var arbeidsgivernavn = arbeidsgiverTjeneste.hent(entry.getKey()).getNavn();

            var arbeidsforhold = new OversiktBeregningsgrunnlag.Arbeidsforhold(entry.getKey().getIdentifikator(), arbeidsgivernavn, refusjonPerMnd);

            var dagsatsArbeidsgiver = entry.getValue()
                .stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getDagsatsArbeidsgiver)
                .filter(Objects::nonNull)
                .reduce(Long::sum)
                .orElse(0L);
            var dagsatsSøker = entry.getValue()
                .stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getDagsatsBruker)
                .filter(Objects::nonNull)
                .reduce(Long::sum)
                .orElse(0L);

            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.ARBEIDSTAKER, fastsattPerÅr, inntektsKilde, arbeidsforhold,
                BigDecimal.valueOf(dagsatsArbeidsgiver), BigDecimal.valueOf(dagsatsSøker));
        }).toList();
    }

    private OversiktBeregningsgrunnlag.BeregningsAndel mapAndelUtenArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        var erSkjønsfastsatt = andel.getOverstyrtPrÅr() != null;
        var fastsattPrÅr = erSkjønsfastsatt ? andel.getOverstyrtPrÅr() : andel.getBeregnetPrÅr();
        if (andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            var inntektsKilde = erSkjønsfastsatt ? OversiktBeregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : OversiktBeregningsgrunnlag.InntektsKilde.PENSJONSGIVENDE_INNTEKT;
            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()),
                fastsattPrÅr, inntektsKilde, null, BigDecimal.ZERO, mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER)) {
            var inntektsKilde = erSkjønsfastsatt ? OversiktBeregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT : OversiktBeregningsgrunnlag.InntektsKilde.A_INNTEKT;
            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()),
                fastsattPrÅr, inntektsKilde, null, BigDecimal.ZERO, mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()),
                fastsattPrÅr, OversiktBeregningsgrunnlag.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, BigDecimal.ZERO, mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)) {
            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()),
                fastsattPrÅr, OversiktBeregningsgrunnlag.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, BigDecimal.ZERO, mapDagsats(andel));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.BRUKERS_ANDEL)) {
            return new OversiktBeregningsgrunnlag.BeregningsAndel(OversiktAktivitetStatus.fraBehandlingslagerStatus(andel.getAktivitetStatus()),
                fastsattPrÅr, OversiktBeregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT, null, BigDecimal.ZERO, mapDagsats(andel));
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

    private OversiktBeregningsgrunnlag.BeregningAktivitetStatus mapAktivitetStatusMedHjemmel(BeregningsgrunnlagAktivitetStatus aks) {
        return new OversiktBeregningsgrunnlag.BeregningAktivitetStatus(OversiktAktivitetStatus.fraBehandlingslagerStatus(aks.getAktivitetStatus()),
            aks.getHjemmel());
    }
}
