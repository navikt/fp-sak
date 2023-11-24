package no.nav.foreldrepenger.datavarehus.v2;

import java.util.Collection;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.KlassekodeUtleder;

class StønadsstatistikkUtbetalingPeriodeMapper {
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
        return perioder.stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> mapTilkjentPeriode(kombinert, p))
            .flatMap(Collection::stream)
            .toList();
    }

    static List<StønadsstatistikkUtbetalingPeriode> mapTilkjentPeriode(FamilieYtelseType familieYtelseType, BeregningsresultatPeriode periode) {
        return periode.getBeregningsresultatAndelList().stream()
            .filter(a -> a.getDagsats() > 0)
            .map(a -> mapTilkjentAndel(familieYtelseType, periode, a))
            .toList();
    }

    static StønadsstatistikkUtbetalingPeriode mapTilkjentAndel(FamilieYtelseType familieYtelseType, BeregningsresultatPeriode periode, BeregningsresultatAndel andel) {
        // TODO gamle svangerskapspenger: her er utbetalingsgrad 100 og dagsats = dagsatsFraBG. Dette er senere fikset i beregning-ytelse
        // Kan løses ved å simulere Tilkjent for dissse tilfellene og så plukke utbetalingsgrad fra MapBeregningsresultatFraRegelTilVL - bruke MapUttakResultatFraVLTilRegel

        return new StønadsstatistikkUtbetalingPeriode(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(),
            KlassekodeUtleder.utled(andel, familieYtelseType).getKode(),
            andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null),
            andel.getDagsats(),
            andel.getStillingsprosent(),
            andel.getUtbetalingsgrad());
    }

}
