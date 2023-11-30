package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_INNLAGT;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_SYKDOM_SKADE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
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
            .filter(p -> p.harTrekkdager() || p.harUtbetaling() || p.isInnvilgetUtsettelse())
            .map(p -> mapUttakPeriode(rolleType, rettighetType, p)).toList();
    }

    static StønadsstatistikkUttakPeriode mapUttakPeriode(RelasjonsRolleType rolleType,
                                                         StønadsstatistikkVedtak.RettighetType rettighetType,
                                                         ForeldrepengerUttakPeriode periode) {
        // Ser ikke på innvilget avslått men på kombinasjoner av harTrekkdager og erUtbetaling.
        // Søknad og PeriodeResultatÅrsak er supplerende informasjon men ikke endelig avgjørende
        // Ønsker ta med disse tilfellene:
        // - normalt uttak - harTrekkdater og erUtbetaling
        // - avslått uttak m/trekk -  harTrekkdager
        // - innvilget utsettelse (første 6 uker, bare far rett, sammenhengende uttak

        //TODO: Trenger vi forklaring på avslag med trekkdager (søknadsfrist, msp, dok++)?
        var erUtbetaling = periode.harUtbetaling();
        var foretrukketAktivitet = velgAktivitet(periode).orElseThrow();

        var gradering = utledGradering(periode);


        var virkedager = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var trekkdager = foretrukketAktivitet.getTrekkdager();
        var rettighet = utledRettighet(rolleType, rettighetType, foretrukketAktivitet.getTrekkonto(), periode);

        var forklaring = utledForklaring(periode, foretrukketAktivitet.getTrekkonto(), rolleType);

        var samtidigUttakProsent = periode.getSamtidigUttaksprosent() != null ? periode.getSamtidigUttaksprosent().decimalValue() : null;
        return new StønadsstatistikkUttakPeriode(periode.getFom(), periode.getTom(), mapStønadskonto(foretrukketAktivitet.getTrekkonto()), rettighet,
            forklaring, erUtbetaling, virkedager, trekkdager.decimalValue(), gradering, samtidigUttakProsent);
    }

    private static StønadsstatistikkUttakPeriode.Gradering utledGradering(ForeldrepengerUttakPeriode periode) {
        return periode.isGraderingInnvilget() ? periode.getAktiviteter()
            .stream()
            .filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode)
            .filter(a -> a.getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getArbeidsprosent))
            .map(g -> new StønadsstatistikkUttakPeriode.Gradering(mapArbeidType(g), g.getArbeidsprosent()))
            .orElseThrow() : null;
    }

    /**
     * Trenger forklaring
     * - far/medmor tar ut fellesperiode eller foreldrepenger: aktivitetskrav, minsterett, flerbarnsdager, samtidig (<= 50%)
     * - mor tar ut fedrekvote eller far/medmor tar ut mødrekvote
     * - Utsettelse - mor ikke rett: Aktivitetskrav
     * - Utsettelse - Fritt uttak: utsettelser første 6 uker etter fødsel
     * - Utsettelse - Sammenhengende: alle utsettelser
     **/
    static StønadsstatistikkUttakPeriode.Forklaring utledForklaring(ForeldrepengerUttakPeriode periode,
                                                                    StønadskontoType stønadskontoType,
                                                                    RelasjonsRolleType rolleType) {
        var forklaringForMorsAktivitet = utledForklaringAktivitetskrav(rolleType, periode.getMorsAktivitet(), stønadskontoType);
        if (periode.isInnvilgetUtsettelse()) { //Se bort i fra innvilget, se på trekk og utbetaling
            return switch (periode.getUtsettelseType()) { //Se på årsak??
                case ARBEID -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_ARBEID;
                case FERIE -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_FERIE;
                case SYKDOM_SKADE -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_SYKDOM;
                case SØKER_INNLAGT -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_INNLEGGELSE;
                case BARN_INNLAGT -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_BARNINNLAGT;
                case HV_OVELSE -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_HVOVELSE;
                case NAV_TILTAK -> StønadsstatistikkUttakPeriode.Forklaring.UTSETTELSE_NAVTILTAK;
                case FRI -> forklaringForMorsAktivitet;
                case UDEFINERT -> null;
            };
        }

        if (overførerAnnenPartsKvote(rolleType, stønadskontoType)) { //Sjekke db
            if (OVERFØRING_ANNEN_PART_INNLAGT.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return StønadsstatistikkUttakPeriode.Forklaring.OVERFØRING_ANNEN_PART_INNLAGT;
            }
            if (OVERFØRING_ANNEN_PART_SYKDOM_SKADE.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.SYKDOM_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return StønadsstatistikkUttakPeriode.Forklaring.OVERFØRING_ANNEN_PART_SYKDOM;
            }
        }

        if (stønadskontoType != null && stønadskontoType.harAktivitetskrav() && RelasjonsRolleType.erFarEllerMedmor(rolleType)) {
            if (Set.of(FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR, GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR).contains(periode.getResultatÅrsak())) {
                return StønadsstatistikkUttakPeriode.Forklaring.MINSTERETT;
            }
            if (periode.isFlerbarnsdager()) {
                return StønadsstatistikkUttakPeriode.Forklaring.FLERBARNSDAGER;
            }
            if (forklaringForMorsAktivitet == null
                && StønadskontoType.FELLESPERIODE.equals(stønadskontoType)
                && periode.isSamtidigUttak()
                && periode.getSamtidigUttaksprosent().mindreEnnEllerLik50()) {
                return StønadsstatistikkUttakPeriode.Forklaring.SAMTIDIG_MØDREKVOTE;
            }
            return forklaringForMorsAktivitet;
        }
        return null;
    }

    private static StønadsstatistikkUttakPeriode.Forklaring utledForklaringAktivitetskrav(RelasjonsRolleType rolleType,
                                                                                          MorsAktivitet morsAktivitet,
                                                                                          StønadskontoType stønadskontoType) {
        if (RelasjonsRolleType.erMor(rolleType) || !stønadskontoType.harAktivitetskrav()) {
            return null;
        }
        return switch (morsAktivitet) {
            case UDEFINERT, UFØRE, IKKE_OPPGITT -> StønadskontoType.FORELDREPENGER.equals(stønadskontoType) ? StønadsstatistikkUttakPeriode.Forklaring.MINSTERETT : null;
            case ARBEID -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_ARBEID;
            case UTDANNING -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_UTDANNING;
            case KVALPROG -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_KVALIFISERINGSPROGRAM;
            case INTROPROG -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_INTRODUKSJONSPROGRAM;
            case TRENGER_HJELP -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_SYKDOM;
            case INNLAGT -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_INNLEGGELSE;
            case ARBEID_OG_UTDANNING -> StønadsstatistikkUttakPeriode.Forklaring.AKTIVITETSKRAV_ARBEIDUTDANNING;
        };
    }

    private static Optional<ForeldrepengerUttakPeriodeAktivitet> velgAktivitet(ForeldrepengerUttakPeriode periode) {
        Predicate<ForeldrepengerUttakPeriodeAktivitet> kompatibelKonto = a -> periode.getSøktKonto() == null ||
            a.getTrekkonto().getKode().equals(periode.getSøktKonto().getKode());
        if (periode.harTrekkdager()) {
            return periode.getAktiviteter()
                .stream()
                .filter(kompatibelKonto)
                .filter(a -> a.getTrekkdager().merEnn0())
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() -> periode.getAktiviteter()
                    .stream()
                    .filter(a -> a.getTrekkdager().merEnn0())
                    .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
        } else if (periode.harUtbetaling()) {
            return periode.getAktiviteter()
                .stream()
                .filter(kompatibelKonto)
                .filter(a -> a.getUtbetalingsgrad().harUtbetaling())
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() -> periode.getAktiviteter()
                    .stream()
                    .filter(a -> a.getUtbetalingsgrad().harUtbetaling())
                    .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
        } else {
            return periode.getAktiviteter()
                .stream()
                .filter(kompatibelKonto)
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager))
                .or(() -> periode.getAktiviteter().stream().min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getTrekkdager)));
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
        if (periode.isInnvilget() && periode.harTrekkdager() && overførerAnnenPartsKvote(rolleType, stønadskontoType)) {
            if (OverføringÅrsak.ALENEOMSORG.equals(periode.getOverføringÅrsak()) && periode.isInnvilget()) {
                return StønadsstatistikkVedtak.RettighetType.ALENEOMSORG;
            } else if (OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.BARE_SØKER_RETT;
            } else if (OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET.equals(periode.getResultatÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.ALENEOMSORG;
            } else if (OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER.equals(periode.getResultatÅrsak())) {
                return StønadsstatistikkVedtak.RettighetType.BARE_SØKER_RETT;
            }
        }
        return rettighetType;
    }

    private static boolean overførerAnnenPartsKvote(RelasjonsRolleType rolleType, StønadskontoType stønadskontoType) {
        return morFedrekvote(rolleType, stønadskontoType) || farMødrekvote(rolleType, stønadskontoType);
    }

    private static boolean farMødrekvote(RelasjonsRolleType rolleType, StønadskontoType stønadskontoType) {
        return RelasjonsRolleType.erFarEllerMedmor(rolleType) && StønadskontoType.MØDREKVOTE.equals(stønadskontoType);
    }

    private static boolean morFedrekvote(RelasjonsRolleType rolleType, StønadskontoType stønadskontoType) {
        return RelasjonsRolleType.erMor(rolleType) && StønadskontoType.FEDREKVOTE.equals(stønadskontoType);
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
