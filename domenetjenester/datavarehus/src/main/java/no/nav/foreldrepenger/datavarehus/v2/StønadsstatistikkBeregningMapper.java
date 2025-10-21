package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

class StønadsstatistikkBeregningMapper {

    private static final Set<FaktaOmBeregningTilfelle> BESTEBEREGNING_FAKTA = Set.of(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING,
        FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);

    private StønadsstatistikkBeregningMapper() {
    }

    static StønadsstatistikkVedtak.Beregning mapBeregning(BehandlingReferanse ref,
                                                          Beregningsgrunnlag beregningsgrunnlag, InntektArbeidYtelseGrunnlag iaygrunnlag) {
        if (beregningsgrunnlag == null) {
            return null;
        }
        var skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();

        var periodePåStp = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(p -> p.getPeriode().inkluderer(skjæringstidspunkt))
            .findFirst()
            .orElseThrow();
        var grunnbeløp = beregningsgrunnlag.getGrunnbeløp().getVerdi();
        var avkortet = FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) ? periodePåStp.getAvkortetPrÅr() :
            utledAvkortetÅrsbeløp(periodePåStp, grunnbeløp);
        var redusert = FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) ? periodePåStp.getRedusertPrÅr() : avkortet;
        var dagsats = FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) ? periodePåStp.getDagsats() : utledDagsats(avkortet);

        var årsbeløp = new StønadsstatistikkVedtak.BeregningÅrsbeløp(periodePåStp.getBruttoPrÅr(), avkortet, redusert, dagsats);

        var andeler = periodePåStp.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .collect(Collectors.groupingBy(Gruppering::new))
            .entrySet().stream()
            .filter(e -> e.getValue().stream().anyMatch(b -> b.getBruttoPrÅr() != null || b.getAvkortetPrÅr() != null || b.getRedusertPrÅr() != null || b.getDagsats() != null))
            .map(e -> mapAndeler(e.getKey(), e.getValue(), ref.fagsakYtelseType()))
            .toList();

        var næringOrgNr = Optional.ofNullable(iaygrunnlag)
            .flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening)
            .map(OppgittOpptjening::getEgenNæring)
            .orElse(List.of())
            .stream()
            .map(OppgittEgenNæring::getOrgnr)
            .collect(Collectors.toSet());

        var avviksvudert = periodePåStp.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(a -> a.getOverstyrtPrÅr() != null && a.getOverstyrtPrÅr().compareTo(BigDecimal.ZERO) > 0);
        var fastsatt = avviksvudert ? StønadsstatistikkVedtak.BeregningFastsatt.SKJØNN : StønadsstatistikkVedtak.BeregningFastsatt.AUTOMATISK;

        var hjemmel = mapBeregningshjemmel(ref.fagsakYtelseType(), beregningsgrunnlag);

        return new StønadsstatistikkVedtak.Beregning(grunnbeløp, årsbeløp, andeler, næringOrgNr, hjemmel, fastsatt);
    }

    private static Long utledDagsats(BigDecimal avkortet) {
        if (avkortet == null) {
            return null;
        }
        return avkortet.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_EVEN).longValue();
    }

    private static BigDecimal utledAvkortetÅrsbeløp(BeregningsgrunnlagPeriode periodePåStp, BigDecimal grunnbeløp) {
        var bruttoInkludertBortfaltNaturalytelsePrAar = periodePåStp.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
        if (bruttoInkludertBortfaltNaturalytelsePrAar == null) {
            return null;
        }
        BigDecimal seksG = grunnbeløp.multiply(BigDecimal.valueOf(6));
        return bruttoInkludertBortfaltNaturalytelsePrAar.compareTo(seksG) > 0 ? seksG : bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    private record Gruppering(StønadsstatistikkVedtak.AndelType andelType, String arbeidsgiver) {
        Gruppering(BeregningsgrunnlagPrStatusOgAndel andel) {
            this(mapAktivitetStatus(andel.getAktivitetStatus()), andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null));
        }

    }

    private static StønadsstatistikkVedtak.BeregningAndel mapAndeler(Gruppering gruppering, List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                                                     FagsakYtelseType ytelseType) {
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            var bruttoÅr = andeler.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            var avkortetÅr = andeler.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getAvkortetPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            var redusertÅr = andeler.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getRedusertPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            var dagsats = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getDagsats).filter(Objects::nonNull).reduce(0L, Long::sum);
            var årsbeløp = new StønadsstatistikkVedtak.BeregningÅrsbeløp(bruttoÅr, avkortetÅr, redusertÅr, dagsats);
            return new StønadsstatistikkVedtak.BeregningAndel(gruppering.andelType(), gruppering.arbeidsgiver(), årsbeløp);
        } else {
            var bruttoÅr = andeler.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            var årsbeløp = new StønadsstatistikkVedtak.BeregningÅrsbeløp(bruttoÅr, null, null, null);
            return new StønadsstatistikkVedtak.BeregningAndel(gruppering.andelType(), gruppering.arbeidsgiver(), årsbeløp);
        }
    }

    private static StønadsstatistikkVedtak.AndelType mapAktivitetStatus(AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> StønadsstatistikkVedtak.AndelType.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> StønadsstatistikkVedtak.AndelType.ARBEIDSTAKER;
            case DAGPENGER -> StønadsstatistikkVedtak.AndelType.DAGPENGER;
            case FRILANSER -> StønadsstatistikkVedtak.AndelType.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> StønadsstatistikkVedtak.AndelType.MILITÆR_SIVILTJENESTE;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> StønadsstatistikkVedtak.AndelType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case BRUKERS_ANDEL -> StønadsstatistikkVedtak.AndelType.YTELSE;
            case KOMBINERT_AT_FL, KOMBINERT_AT_SN, KOMBINERT_FL_SN, KOMBINERT_AT_FL_SN -> throw new IllegalStateException("Skal ikke forekomme");
            case KUN_YTELSE, TTLSTØTENDE_YTELSE, VENTELØNN_VARTPENGER, UDEFINERT -> throw new IllegalStateException("Skal ikke forekomme");
        };
    }

    private static StønadsstatistikkVedtak.BeregningHjemmel mapBeregningshjemmel(FagsakYtelseType ytelseType, Beregningsgrunnlag beregningsgrunnlag) {
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            var hjemler = beregningsgrunnlag.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getHjemmel).collect(Collectors.toSet());
            var statuser = beregningsgrunnlag.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toSet());

            if (hjemler.contains(Hjemmel.F_14_7_8_49) || statuser.contains(AktivitetStatus.DAGPENGER)) {
                var besteberegnet = beregningsgrunnlag.getBesteberegningGrunnlag().isPresent() ||
                    beregningsgrunnlag.getFaktaOmBeregningTilfeller().stream().anyMatch(BESTEBEREGNING_FAKTA::contains);
                return besteberegnet ? StønadsstatistikkVedtak.BeregningHjemmel.BESTEBEREGNING : StønadsstatistikkVedtak.BeregningHjemmel.DAGPENGER;
            }
            if (statuser.contains(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
                return StønadsstatistikkVedtak.BeregningHjemmel.ARBEIDSAVKLARINGSPENGER;
            }
            if (statuser.contains(AktivitetStatus.MILITÆR_ELLER_SIVIL)) {
                return StønadsstatistikkVedtak.BeregningHjemmel.MILITÆR_SIVIL;
            }
        }
        return switch (beregningsgrunnlag.getHjemmel()) {
            case F_14_7 -> StønadsstatistikkVedtak.BeregningHjemmel.ANNEN; // Stort set beregning med kun dagytelse i grunnlaget
            case F_14_7_8_30, F_14_7_8_28_8_30 -> StønadsstatistikkVedtak.BeregningHjemmel.ARBEID;
            case F_14_7_8_35 -> StønadsstatistikkVedtak.BeregningHjemmel.NÆRING;
            case F_14_7_8_38 -> StønadsstatistikkVedtak.BeregningHjemmel.FRILANS;
            case F_14_7_8_40 -> StønadsstatistikkVedtak.BeregningHjemmel.ARBEID_FRILANS;
            case F_14_7_8_41 -> StønadsstatistikkVedtak.BeregningHjemmel.ARBEID_NÆRING;
            case F_14_7_8_42 -> StønadsstatistikkVedtak.BeregningHjemmel.NÆRING_FRILANS;
            case F_14_7_8_43 -> StønadsstatistikkVedtak.BeregningHjemmel.ARBEID_NÆRING_FRILANS;
            case F_14_7_8_47 -> throw new IllegalArgumentException("Midlertidig inaktiv ikke støttet");
            case F_14_7_8_49 -> StønadsstatistikkVedtak.BeregningHjemmel.DAGPENGER;
            case UDEFINERT -> StønadsstatistikkVedtak.BeregningHjemmel.ANNEN;
        };
    }


}
