package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_INNLAGT;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_SYKDOM_SKADE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriode.AktivitetType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriode.Forklaring;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriode.Gradering;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriode.PeriodeType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
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
            .filter(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()) > 0)
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

        var erUtbetaling = periode.harUtbetaling();
        var foretrukketAktivitet = velgAktivitet(periode).orElseThrow();

        var gradering = utledGradering(periode);

        var virkedager = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var trekkdager = foretrukketAktivitet.getTrekkdager();
        var rettighet = utledRettighet(rolleType, rettighetType, foretrukketAktivitet.getTrekkonto(), periode);

        var periodeType = utledPeriodeType(periode);
        var forklaring = periodeType == PeriodeType.AVSLAG ? utledForklaringAvslag(periode)
            : utledForklaring(periode, foretrukketAktivitet.getTrekkonto(), rolleType);

        var samtidigUttakProsent = periode.getSamtidigUttaksprosent() != null ? periode.getSamtidigUttaksprosent().decimalValue() : null;
        var stønadskontoType = !trekkdager.merEnn0() ? null : mapStønadskonto(foretrukketAktivitet.getTrekkonto());
        return new StønadsstatistikkUttakPeriode(periode.getFom(), periode.getTom(), periodeType, stønadskontoType, rettighet,
            forklaring, erUtbetaling, virkedager, new StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager(trekkdager.decimalValue()),
            gradering, samtidigUttakProsent);
    }

    private static Forklaring utledForklaringAvslag(ForeldrepengerUttakPeriode periode) {
        if (periode.getResultatÅrsak().getUtfallType() != PeriodeResultatÅrsak.UtfallType.AVSLÅTT) {
            throw new IllegalArgumentException("Forventer periode med avslågsårsak. Fikk " + periode.getResultatÅrsak()
                + " for periode " + periode.getTidsperiode());
        }

        return switch (periode.getResultatÅrsak()) {
            case AKTIVITETSKRAVET_ARBEID_I_KOMB_UTDANNING_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_ARBEID_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_ARBEID_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_INNLEGGELSE_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_INTROPROGRAM_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_KVP_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_UTDANNING_IKKE_DOKUMENTERT,
                AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_INTRODUKSJONSPROGRAM_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_KVALIFISERINGSPROGRAM_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_MORS_INNLEGGELSE_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_MORS_SYKDOM_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_I_KOMBINASJON_MED_ARBEID_IKKE_OPPFYLT,
                AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_IKKE_OPPFYLT,
                MORS_MOTTAK_AV_UFØRETRYGD_IKKE_OPPFYLT,
                BARE_FAR_RETT_MOR_FYLLES_IKKE_AKTIVITETSKRAVET,
                BARE_FAR_RETT_MANGLER_MORS_AKTIVITET -> Forklaring.AVSLAG_AKTIVITETSKRAV;
            case SØKNADSFRIST -> Forklaring.AVSLAG_SØKNADSFRIST;
            case HULL_MELLOM_FORELDRENES_PERIODER,
                MOR_TAR_IKKE_ALLE_UKENE,
                BARE_FAR_RETT_IKKE_SØKT,
                MOR_FØRSTE_SEKS_UKER_IKKE_SØKT -> Forklaring.AVSLAG_IKKE_SØKT;
            case UTSETTELSE_ARBEID_IKKE_DOKUMENTERT,
                UTSETTELSE_FERIE_IKKE_DOKUMENTERT,
                UTSETTELSE_BARNETS_INNLEGGELSE_IKKE_DOKUMENTERT,
                UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT,
                UTSETTELSE_SØKERS_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT,
                IKKE_HELTIDSARBEID,
                SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT,
                SØKERS_INNLEGGELSE_IKKE_OPPFYLT,
                BARNETS_INNLEGGELSE_IKKE_OPPFYLT -> Forklaring.AVSLAG_UTSETTELSE;
            case AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID,
                AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID -> Forklaring.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
            case FRATREKK_PLEIEPENGER -> Forklaring.AVSLAG_PLEIEPENGER;
            case STØNADSPERIODE_NYTT_BARN -> Forklaring.AVSLAG_STARTET_NY_STØNADSPERIODE;
            case BARN_OVER_3_ÅR -> Forklaring.AVSLAG_BARNETS_ALDER;
            case OPPHØR_MEDLEMSKAP,
                OPPTJENINGSVILKÅRET_IKKE_OPPFYLT,
                ADOPSJONSVILKÅRET_IKKE_OPPFYLT,
                FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT,
                FØDSELSVILKÅRET_IKKE_OPPFYLT -> Forklaring.AVSLAG_VILKÅR;
            default -> Forklaring.AVSLAG_ANNET;
        };
    }

    private static PeriodeType utledPeriodeType(ForeldrepengerUttakPeriode periode) {
        if (periode.harTrekkdager()) {
            return periode.harUtbetaling() ? PeriodeType.UTTAK : PeriodeType.AVSLAG;
        }
        return PeriodeType.UTSETTELSE;
    }

    private static Gradering utledGradering(ForeldrepengerUttakPeriode periode) {
        return periode.isGraderingInnvilget() ? periode.getAktiviteter()
            .stream()
            .filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode)
            .filter(a -> a.getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getArbeidsprosent))
            .map(g -> new Gradering(mapArbeidType(g), g.getArbeidsprosent()))
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
    private static Forklaring utledForklaring(ForeldrepengerUttakPeriode periode,
                                              StønadskontoType stønadskontoType,
                                              RelasjonsRolleType rolleType) {
        var forklaringForMorsAktivitet = utledForklaringAktivitetskrav(rolleType, periode.getMorsAktivitet(), stønadskontoType);
        if (periode.isInnvilgetUtsettelse()) { //Se bort i fra innvilget, se på trekk og utbetaling
            return utledForklaringVedUtsettelse(periode, forklaringForMorsAktivitet);
        }

        if (overførerAnnenPartsKvote(rolleType, stønadskontoType)) {
            if (OVERFØRING_ANNEN_PART_INNLAGT.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return Forklaring.OVERFØRING_ANNEN_PART_INNLAGT;
            }
            if (OVERFØRING_ANNEN_PART_SYKDOM_SKADE.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.SYKDOM_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return Forklaring.OVERFØRING_ANNEN_PART_SYKDOM;
            }
            if (OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.ALENEOMSORG.equals(periode.getOverføringÅrsak())) {
                return Forklaring.OVERFØRING_ALENEOMSORG;
            }
            if (OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER.equals(periode.getResultatÅrsak())
                || OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER.equals(periode.getOverføringÅrsak())) {
                return Forklaring.OVERFØRING_BARE_SØKER_RETT;
            }
        }

        if (stønadskontoType != null && stønadskontoType.harAktivitetskrav() && RelasjonsRolleType.erFarEllerMedmor(rolleType)) {
            if (Set.of(FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR, GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR).contains(periode.getResultatÅrsak())) {
                return Forklaring.MINSTERETT;
            }
            if (periode.isFlerbarnsdager()) {
                return Forklaring.FLERBARNSDAGER;
            }
            if (forklaringForMorsAktivitet == null
                && StønadskontoType.FELLESPERIODE.equals(stønadskontoType)
                && periode.isSamtidigUttak()
                && periode.getSamtidigUttaksprosent().mindreEnnEllerLik50()) {
                return Forklaring.SAMTIDIG_MØDREKVOTE;
            }
            return forklaringForMorsAktivitet;
        }
        return null;
    }

    private static Forklaring utledForklaringVedUtsettelse(ForeldrepengerUttakPeriode periode,
                                                                          Forklaring forklaringForMorsAktivitet) {
        return switch (periode.getUtsettelseType()) { //Se på årsak??
            case ARBEID -> Forklaring.AKTIVITETSKRAV_ARBEID;
            case FERIE -> Forklaring.UTSETTELSE_FERIE;
            case SYKDOM_SKADE -> Forklaring.UTSETTELSE_SYKDOM;
            case SØKER_INNLAGT -> Forklaring.UTSETTELSE_INNLEGGELSE;
            case BARN_INNLAGT -> Forklaring.UTSETTELSE_BARNINNLAGT;
            case HV_OVELSE -> Forklaring.UTSETTELSE_HVOVELSE;
            case NAV_TILTAK -> Forklaring.UTSETTELSE_NAVTILTAK;
            case FRI -> forklaringForMorsAktivitet;
            case UDEFINERT -> null;
        };
    }

    private static Forklaring utledForklaringAktivitetskrav(RelasjonsRolleType rolleType,
                                                                                          MorsAktivitet morsAktivitet,
                                                                                          StønadskontoType stønadskontoType) {
        if (RelasjonsRolleType.erMor(rolleType) || !stønadskontoType.harAktivitetskrav()) {
            return null;
        }
        return switch (morsAktivitet) {
            case UDEFINERT, UFØRE, IKKE_OPPGITT -> StønadskontoType.FORELDREPENGER.equals(stønadskontoType) ? Forklaring.MINSTERETT : null;
            case ARBEID -> Forklaring.AKTIVITETSKRAV_ARBEID;
            case UTDANNING -> Forklaring.AKTIVITETSKRAV_UTDANNING;
            case KVALPROG -> Forklaring.AKTIVITETSKRAV_KVALIFISERINGSPROGRAM;
            case INTROPROG -> Forklaring.AKTIVITETSKRAV_INTRODUKSJONSPROGRAM;
            case TRENGER_HJELP -> Forklaring.AKTIVITETSKRAV_SYKDOM;
            case INNLAGT -> Forklaring.AKTIVITETSKRAV_INNLEGGELSE;
            case ARBEID_OG_UTDANNING -> Forklaring.AKTIVITETSKRAV_ARBEIDUTDANNING;
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

    private static AktivitetType mapArbeidType(ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        return switch (aktivitet.getUttakArbeidType()) {
            case ORDINÆRT_ARBEID -> AktivitetType.ARBEIDSTAKER;
            case FRILANS -> AktivitetType.FRILANS;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetType.NÆRING;
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
