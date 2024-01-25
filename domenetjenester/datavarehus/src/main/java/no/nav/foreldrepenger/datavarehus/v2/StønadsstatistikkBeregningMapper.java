package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

class StønadsstatistikkBeregningMapper {

    private StønadsstatistikkBeregningMapper() {
    }

    static StønadsstatistikkVedtak.Beregning mapBeregning(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iaygrunnlag) {
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

        var årsbeløp = new StønadsstatistikkVedtak.BeregningÅrsbeløp(periodePåStp.getBruttoPrÅr(), periodePåStp.getAvkortetPrÅr(),
            periodePåStp.getRedusertPrÅr(), periodePåStp.getDagsats());

        var andeler = periodePåStp.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .collect(Collectors.groupingBy(Gruppering::new))
            .entrySet().stream()
            .filter(e -> e.getValue().stream().anyMatch(b -> b.getBruttoPrÅr() != null || b.getAvkortetPrÅr() != null || b.getRedusertPrÅr() != null || b.getDagsats() != null))
            .map(e -> mapAndeler(e.getKey(), e.getValue()))
            .toList();

        var næringOrgNr = Optional.ofNullable(iaygrunnlag)
            .flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening)
            .map(OppgittOpptjening::getEgenNæring)
            .orElse(List.of())
            .stream()
            .map(OppgittEgenNæring::getOrgnr)
            .collect(Collectors.toSet());

        return new StønadsstatistikkVedtak.Beregning(grunnbeløp, årsbeløp, andeler, næringOrgNr);
    }

    private record Gruppering(StønadsstatistikkVedtak.AndelType andelType, String arbeidsgiver) {
        Gruppering(BeregningsgrunnlagPrStatusOgAndel andel) {
            this(mapAktivitetStatus(andel.getAktivitetStatus()), andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null));
        }

    }

    private static StønadsstatistikkVedtak.BeregningAndel mapAndeler(Gruppering gruppering, List<BeregningsgrunnlagPrStatusOgAndel> andeler) {
        var bruttoÅr = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        var avkortetÅr = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getAvkortetPrÅr).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        var redusertÅr = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getRedusertPrÅr).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        var dagsats = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getDagsats).filter(Objects::nonNull).reduce(0L, Long::sum);
        var årsbeløp = new StønadsstatistikkVedtak.BeregningÅrsbeløp(bruttoÅr, avkortetÅr, redusertÅr, dagsats);
        return new StønadsstatistikkVedtak.BeregningAndel(gruppering.andelType(), gruppering.arbeidsgiver(), årsbeløp);
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

}
