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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class StønadsstatistikkUttakPeriodeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkUttakPeriodeMapper.class);

    private StønadsstatistikkUttakPeriodeMapper() {
    }

    static List<StønadsstatistikkUttakPeriode> mapUttak(RelasjonsRolleType rolleType,
                                                        StønadsstatistikkVedtak.RettighetType rettighetType,
                                                        List<ForeldrepengerUttakPeriode> perioder, String logContext) {
        var statistikkPerioder = perioder.stream()
            .filter(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()) > 0)
            .filter(p -> p.harTrekkdager() || p.harUtbetaling() || p.isInnvilgetUtsettelse())
            .map(p -> mapUttakPeriode(rolleType, rettighetType, p, logContext)).toList();
        return komprimerTidslinje(statistikkPerioder);
    }

    static StønadsstatistikkUttakPeriode mapUttakPeriode(RelasjonsRolleType rolleType,
                                                         StønadsstatistikkVedtak.RettighetType rettighetType,
                                                         ForeldrepengerUttakPeriode periode, String logContext) {
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
        var forklaring = periodeType == PeriodeType.AVSLAG ? utledForklaringAvslag(periode, logContext)
            : utledForklaring(periode, foretrukketAktivitet.getTrekkonto(), rolleType);

        var mottattDato = Optional.ofNullable(periode.getTidligstMottatttDato()).orElseGet(periode::getMottattDato);

        var samtidigUttakProsent = periode.getSamtidigUttaksprosent() != null ? periode.getSamtidigUttaksprosent().decimalValue() : null;
        var stønadskontoType = !trekkdager.merEnn0() ? null : mapStønadskonto(foretrukketAktivitet.getTrekkonto());
        return new StønadsstatistikkUttakPeriode(periode.getFom(), periode.getTom(), periodeType, stønadskontoType, rettighet,
            forklaring, mottattDato, erUtbetaling, virkedager, new StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager(trekkdager.decimalValue()),
            gradering, samtidigUttakProsent);
    }

    private static Forklaring utledForklaringAvslag(ForeldrepengerUttakPeriode periode, String logContext) {
        if (periode.getResultatÅrsak().getUtfallType() != PeriodeResultatÅrsak.UtfallType.AVSLÅTT) {
            LOG.info("Stønadsstatistikk forventer periode med avslågsårsak {}. Fikk {} for periode {}", logContext, periode.getResultatÅrsak(), periode.getTidsperiode());
            return Forklaring.AVSLAG_ANNET;
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
                IKKE_LOVBESTEMT_FERIE,
                UTSETTELSE_FERIE_PÅ_BEVEGELIG_HELLIGDAG,
                FERIE_SELVSTENDIG_NÆRINGSDRIVENDSE_FRILANSER,
                SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT,
                SØKERS_INNLEGGELSE_IKKE_OPPFYLT,
                BARNETS_INNLEGGELSE_IKKE_OPPFYLT,
                SØKERS_SYKDOM_SKADE_SEKS_UKER_IKKE_OPPFYLT,
                SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT,
                BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT,
                SØKERS_SYKDOM_ELLER_SKADE_SEKS_UKER_IKKE_DOKUMENTERT,
                SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT,
                BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT -> Forklaring.AVSLAG_UTSETTELSE;
            case AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID,
                AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID -> Forklaring.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
            case FRATREKK_PLEIEPENGER -> Forklaring.AVSLAG_PLEIEPENGER;
            case BARN_OVER_3_ÅR, STØNADSPERIODE_NYTT_BARN -> Forklaring.AVSLAG_STØNADSPERIODE_UTLØPT;
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
        if (!periode.isGraderingInnvilget()) {
            return null;
        }
        return periode.getAktiviteter()
            .stream()
            .filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode)
            .filter(a -> a.getArbeidsprosent().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getArbeidsprosent))
            .map(g -> new Gradering(mapArbeidType(g), g.getArbeidsprosent()))
            .orElseGet(() -> periode.getAktiviteter()
                .stream()
                .filter(ForeldrepengerUttakPeriodeAktivitet::isSøktGraderingForAktivitetIPeriode)
                .min(Comparator.comparing(ForeldrepengerUttakPeriodeAktivitet::getUtbetalingsgrad))
                .map(g -> new Gradering(mapArbeidType(g), new BigDecimal(100).subtract(g.getUtbetalingsgrad().decimalValue())))
                .orElse(null));
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
                                              UttakPeriodeType stønadskontoType,
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
                && UttakPeriodeType.FELLESPERIODE.equals(stønadskontoType)
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
                                                            UttakPeriodeType stønadskontoType) {
        if (RelasjonsRolleType.erMor(rolleType) || !stønadskontoType.harAktivitetskrav()) {
            return null;
        }
        return switch (morsAktivitet) {
            case UDEFINERT, UFØRE, IKKE_OPPGITT -> UttakPeriodeType.FORELDREPENGER.equals(stønadskontoType) ? Forklaring.MINSTERETT : null;
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
            a.getTrekkonto().equals(periode.getSøktKonto());
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
                                                                        UttakPeriodeType stønadskontoType,
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

    private static boolean overførerAnnenPartsKvote(RelasjonsRolleType rolleType, UttakPeriodeType stønadskontoType) {
        return morFedrekvote(rolleType, stønadskontoType) || farMødrekvote(rolleType, stønadskontoType);
    }

    private static boolean farMødrekvote(RelasjonsRolleType rolleType, UttakPeriodeType stønadskontoType) {
        return RelasjonsRolleType.erFarEllerMedmor(rolleType) && UttakPeriodeType.MØDREKVOTE.equals(stønadskontoType);
    }

    private static boolean morFedrekvote(RelasjonsRolleType rolleType, UttakPeriodeType stønadskontoType) {
        return RelasjonsRolleType.erMor(rolleType) && UttakPeriodeType.FEDREKVOTE.equals(stønadskontoType);
    }

    private static StønadsstatistikkVedtak.StønadskontoType mapStønadskonto(UttakPeriodeType stønadskontoType) {
        return switch (stønadskontoType) {
            case MØDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE;
            case FORELDREPENGER_FØR_FØDSEL -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FEDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE;
            case FELLESPERIODE -> StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE;
            case FORELDREPENGER -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER;
            case UDEFINERT -> null;
        };
    }

    private static List<StønadsstatistikkUttakPeriode> komprimerTidslinje(List<StønadsstatistikkUttakPeriode> perioder) {
        var segmenter = perioder.stream()
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p))
            .toList();
        return new LocalDateTimeline<>(segmenter)
            .compress(LocalDateInterval::abutsWorkdays, StønadsstatistikkUttakPeriodeMapper::likeNaboer, StønadsstatistikkUttakPeriodeMapper::slåSammen)
            .stream().map(LocalDateSegment::getValue).toList();
    }

    // For å få til compress - må ha equals som gjør BigDecimal.compareTo
    private static boolean likeNaboer(StønadsstatistikkUttakPeriode u1, StønadsstatistikkUttakPeriode u2) {
        return u1.erUtbetaling() == u2.erUtbetaling() && Objects.equals(u1.type(), u2.type())
            && Objects.equals(u1.stønadskontoType(), u2.stønadskontoType())
            && Objects.equals(u1.rettighetType(), u2.rettighetType()) && Objects.equals(u1.forklaring(), u2.forklaring())
            && likGradering(u1.gradering(), u2.gradering())
            && likeBD(u1.samtidigUttakProsent(), u2.samtidigUttakProsent());
    }

    private static LocalDateSegment<StønadsstatistikkUttakPeriode> slåSammen(LocalDateInterval i,
                                                                             LocalDateSegment<StønadsstatistikkUttakPeriode> lhs,
                                                                             LocalDateSegment<StønadsstatistikkUttakPeriode> rhs) {
        var u1 = lhs.getValue();
        var u2 = rhs.getValue();
        var virkedager = u1.virkedager() + u2.virkedager();
        var trekkdager = u1.trekkdager().add(u2.trekkdager());
        var mottatt = u1.søknadsdato() != null && u2.søknadsdato() != null && u2.søknadsdato().isBefore(u1.søknadsdato()) ?
            u2.søknadsdato() : Optional.ofNullable(u1.søknadsdato()).orElseGet(u2::søknadsdato);
        var ny = new StønadsstatistikkUttakPeriode(i.getFomDato(), i.getTomDato(), u1.type(), u1.stønadskontoType(),
            u1.rettighetType(), u1.forklaring(), mottatt, u1.erUtbetaling(), virkedager, trekkdager, u1.gradering(), u1.samtidigUttakProsent());
        return new LocalDateSegment<>(i, ny);
    }

    private static boolean likGradering(Gradering g1, Gradering g2) {
        return Objects.equals(g1, g2)
            || (g1 != null && g2 != null && Objects.equals(g1.aktivitetType(), g2.aktivitetType()) && likeBD(g1.arbeidsprosent(), g2.arbeidsprosent()));
    }
    private static boolean likeBD(BigDecimal d1, BigDecimal d2) {
        return Objects.equals(d1, d2) || (d1 != null && d2 != null && d1.compareTo(d2) == 0);
    }


}
