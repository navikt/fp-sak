package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.KlassekodeUtleder;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

class StønadsstatistikkUtbetalingPeriodeMapper {

    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private StønadsstatistikkUtbetalingPeriodeMapper() {
    }

    static List<StønadsstatistikkUtbetalingPeriode> mapTilkjent(StønadsstatistikkVedtak.YtelseType ytelseType,
                                                                StønadsstatistikkVedtak.HendelseType  familieHendelse,
                                                                List<BeregningsresultatPeriode> perioder) {
        var kombinert = switch (ytelseType) {
            case ENGANGSSTØNAD -> throw new IllegalStateException("Utviklerfeil skal ikke periodisere ES");
            case SVANGERSKAPSPENGER -> FamilieYtelseType.SVANGERSKAPSPENGER;
            case FORELDREPENGER -> switch (familieHendelse) {
                case FØDSEL -> FamilieYtelseType.FØDSEL;
                case ADOPSJON, OMSORGSOVERTAKELSE -> FamilieYtelseType.ADOPSJON;
            };
        };
        // Grupper på konto, arbeidsgiver og utbetaling - komprimer tidslinjer
        return perioder.stream()
            .filter(p -> p.getDagsats() > 0)
            .flatMap(p -> mapTilkjentPeriode(kombinert, p))
            .collect(Collectors.groupingBy(s -> new Gruppering(s.getValue())))
            .values().stream()
            .map(LocalDateTimeline::new)
            .map(ldt -> ldt.compress(StønadsstatistikkUtbetalingPeriodeMapper::likeNaboer, StandardCombinators::leftOnly))
            .flatMap(LocalDateTimeline::stream)
            .map(s -> new StønadsstatistikkUtbetalingPeriode(s.getFom(), s.getTom(), s.getValue().klassekode(), s.getValue().arbeidsgiver(),
                s.getValue().dagsats(), s.getValue().utbetalingsgrad()))
            .toList();
    }

    static Stream<LocalDateSegment<UtbetalingSammenlign>> mapTilkjentPeriode(FamilieYtelseType familieYtelseType, BeregningsresultatPeriode periode) {
        // Konverterer alle andeler, grupperer på konto, arbeidsgiver og utbetaling, summerer dagsats og returnerer segment
        return periode.getBeregningsresultatAndelList().stream()
            .filter(a -> a.getDagsats() > 0)
            .map(a -> mapTilkjentAndel(familieYtelseType, a))
            .collect(Collectors.groupingBy(Gruppering::new))
            .values().stream()
            .filter(l -> !l.isEmpty())
            .map(l -> mapAndelTilSegment(periode, l));
    }

    private static LocalDateSegment<UtbetalingSammenlign> mapAndelTilSegment(BeregningsresultatPeriode periode,
                                                                             List<UtbetalingSammenlign> utbetalinger) {
        var any = utbetalinger.stream().findFirst().orElseThrow();
        var sumDagsats = utbetalinger.stream().map(UtbetalingSammenlign::dagsats).reduce(0, Integer::sum);
        return new LocalDateSegment<>(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(),
            new UtbetalingSammenlign(any, sumDagsats));
    }

    private static UtbetalingSammenlign mapTilkjentAndel(FamilieYtelseType familieYtelseType, BeregningsresultatAndel andel) {
        // TODO gamle svangerskapspenger: her er utbetalingsgrad 100 og dagsats = dagsatsFraBG. Dette er senere fikset i beregning-ytelse
        // Kan løses ved å simulere Tilkjent for dissse tilfellene og så plukke utbetalingsgrad fra MapBeregningsresultatFraRegelTilVL - bruke MapUttakResultatFraVLTilRegel
        return new UtbetalingSammenlign(KlassekodeUtleder.utled(andel, familieYtelseType).getKode(),
            andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null),
            andel.getDagsats(), andel.getUtbetalingsgrad());
    }

    // Bruker int for å slippe hash-problematikk rundt BigDecimal
    private record Gruppering(String klassekode, String arbeidsgiver, int utbetalingsgrad) {
        Gruppering(UtbetalingSammenlign utbetaling) {
            this(utbetaling.klassekode(), utbetaling.arbeidsgiver(), utbetaling.utbetalingsgrad().multiply(HUNDRE).setScale(0, RoundingMode.HALF_EVEN).intValue());
        }
    }

    private record UtbetalingSammenlign(String klassekode, String arbeidsgiver, int dagsats, BigDecimal utbetalingsgrad) {
        UtbetalingSammenlign(UtbetalingSammenlign utbetaling, int dagsats) {
            this(utbetaling.klassekode(), utbetaling.arbeidsgiver(), dagsats, utbetaling.utbetalingsgrad());
        }
    }

    // For å få til compress - må ha equals som gjøre BigDecimal.compareTo
    private static boolean likeNaboer(UtbetalingSammenlign u1, UtbetalingSammenlign u2) {
        return u1.klassekode().equals(u2.klassekode()) && Objects.equals(u1.arbeidsgiver(), u2.arbeidsgiver())
            && u1.dagsats() == u2.dagsats() && u1.utbetalingsgrad().compareTo(u2.utbetalingsgrad()) == 0;
    }

}
