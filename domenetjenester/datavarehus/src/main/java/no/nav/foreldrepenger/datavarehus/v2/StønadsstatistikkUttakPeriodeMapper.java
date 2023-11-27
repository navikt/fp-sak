package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

class StønadsstatistikkUttakPeriodeMapper {



    private StønadsstatistikkUttakPeriodeMapper() {
    }

    static List<StønadsstatistikkUttakPeriode> mapUttak(RelasjonsRolleType rolleType,
                                                        StønadsstatistikkVedtak.RettighetType rettighetType,
                                                        List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream()
            .filter(p -> !p.isOpphold())
            .map(p -> mapUttakPeriode(rolleType, rettighetType, p))
            .toList();
    }

    // OK: @NotNull LocalDate fom, @NotNull LocalDate tom,
    //                                     @NotNull StønadsstatistikkVedtak.StønadskontoType stønadskontoType, // hvilken konta man tar ut fra
    //                                     @NotNull StønadsstatistikkVedtak.RettighetType rettighetType,
    //                                     Forklaring forklaring,
    //                                     boolean erUtbetaling, // Skal utbetales for perioden
    //                                     int virkedager,
    //                                     @NotNull BigDecimal trekkdager,
    //                                     Gradering gradering, // Perioden er gradert
    //                                     BigDecimal samtidigUttakProsent ) {

    static StønadsstatistikkUttakPeriode mapUttakPeriode(RelasjonsRolleType rolleType,
                                                               StønadsstatistikkVedtak.RettighetType rettighetType,
                                                               ForeldrepengerUttakPeriode periode) {
        // Ser ikke på innvilget avslått men på kombinasjoner av harTrekkdager og erUtbetaling.
        // Søknad og PeriodeResultatÅrsak er supplerende informasjon men ikke endelig avgjørende
        // Ønsker ta med disse tilfellene:
        // - normalt uttak - harTrekkdater og erUtbetaling
        // - avslått uttak m/trekk -  harTrekkdager
        // - innvilget utsettelse (første 6 uker, bare far rett, sammenhengende uttak

        var harTrekkdager = periode.harTrekkdager();
        var erUtbetaling = periode.harUtbetaling();
        var foretrukketAktivitet = velgAktivitet(periode).orElseThrow();

        var gradering = periode.isGraderingInnvilget() ? periode.getAktiviteter().stream()
            .filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode)
            .filter(a -> a.getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getArbeidsprosent))
            .map(g -> new StønadsstatistikkUttakPeriode.Gradering(mapArbeidType(g), g.getArbeidsprosent())).orElseThrow() : null;


        var virkedager = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var trekkdager = foretrukketAktivitet.getTrekkdager();
        var rettighet = utledRettighet(rolleType, rettighetType, foretrukketAktivitet.getTrekkonto(), periode);

        /*
         * TODO: Trenger forklaring
         * - far/medmor tar ut fellesperiode eller foreldrepenger: aktivitetskrav, minsterett, flerbarnsdager, samtidig (<= 50%)
         * - mor tar ut fedrekvote eller far/medmor tar ut mødrekvote
         * - Utsettelse - mor ikke rett: Aktivitetskrav
         * - Utsettelse - Fritt uttak: utsettelser første 6 uker etter fødsel
         * - Utsettelse - Sammenhengende: alle utsettelser
         */


        return new StønadsstatistikkUttakPeriode(periode.getFom(), periode.getTom(), mapStønadskonto(foretrukketAktivitet.getTrekkonto()),
            rettighet, null, erUtbetaling, virkedager, trekkdager.decimalValue(), gradering, periode.getSamtidigUttaksprosent().decimalValue());
    }


    private static Optional<ForeldrepengerUttakPeriodeAktivitet> velgAktivitet(ForeldrepengerUttakPeriode periode) {
        if (periode.harTrekkdager()) {
            return periode.getAktiviteter().stream()
                .filter(a -> a.getTrekkonto().getKode().equals(periode.getSøktKonto().getKode()))
                .filter(a -> a.getTrekkdager().merEnn0())
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() ->  periode.getAktiviteter().stream()
                    .filter(a -> a.getTrekkdager().merEnn0())
                    .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
        } else if (periode.harUtbetaling()) {
            return periode.getAktiviteter().stream()
                .filter(a -> a.getTrekkonto().getKode().equals(periode.getSøktKonto().getKode()))
                .filter(a -> a.getUtbetalingsgrad().harUtbetaling())
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() ->  periode.getAktiviteter().stream()
                    .filter(a -> a.getUtbetalingsgrad().harUtbetaling())
                    .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
        } else {
            return periode.getAktiviteter().stream()
                .filter(a -> a.getTrekkonto().getKode().equals(periode.getSøktKonto().getKode()))
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() ->  periode.getAktiviteter().stream()
                    .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
        }
    }

    private static StønadsstatistikkUttakPeriode.AktivitetType mapArbeidType(ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        return switch (aktivitet.getUttakArbeidType()) {
            case ORDINÆRT_ARBEID -> StønadsstatistikkUttakPeriode.AktivitetType.ARBEIDSTAKER;
            case FRILANS -> StønadsstatistikkUttakPeriode.AktivitetType.FRILANS;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> StønadsstatistikkUttakPeriode.AktivitetType.NÆRING;
            case ANNET -> throw new IllegalStateException("Skal ikke kunne gradere andre aktiviteter");
        };
    }

    private static StønadsstatistikkVedtak.RettighetType utledRettighet(RelasjonsRolleType rolleType,
                                                                        StønadsstatistikkVedtak.RettighetType rettighetType,
                                                                        StønadskontoType stønadskontoType,
                                                                        ForeldrepengerUttakPeriode periode) {
        if (periode.isInnvilget() && periode.harTrekkdager() &&
            (RelasjonsRolleType.erMor(rolleType) && StønadskontoType.FEDREKVOTE.equals(stønadskontoType)) ||
             (RelasjonsRolleType.erFarEllerMedmor(rolleType) && StønadskontoType.MØDREKVOTE.equals(stønadskontoType))) {
            if (OverføringÅrsak.ALENEOMSORG.equals(periode.getOverføringÅrsak()) && periode.isInnvilget()) {
                return StønadsstatistikkVedtak.RettighetType.ALENEOMSORG;
            } else if (OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.BARE_SØKER_RETT;
            } else if (PeriodeResultatÅrsak.OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET.equals(periode.getResultatÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.ALENEOMSORG;
            } else if (PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER.equals(periode.getResultatÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.BARE_SØKER_RETT;
            }
        }
        return rettighetType;
    }

    private static StønadsstatistikkVedtak.StønadskontoType mapStønadskonto(StønadskontoType stønadskontoType) {
        return switch (stønadskontoType) {
            case MØDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE;
            case FORELDREPENGER_FØR_FØDSEL -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FEDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE;
            case FELLESPERIODE -> StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE;
            case FORELDREPENGER -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER;
            case UDEFINERT, FLERBARNSDAGER -> null;
        };
    }


}
