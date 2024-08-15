package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.BOSATT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLD;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Adresse.Type.*;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.PersonstatusPeriode.Type;
import static no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

//TODO reimplementeres i fp-inngangsvilkår
final class MedlemskapsvilkårRegel {

    private MedlemskapsvilkårRegel() {
    }

    static Set<MedlemskapAksjonspunktÅrsak> kjørRegler(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var resultat = new HashSet<MedlemskapAksjonspunktÅrsak>();
        utledMedlemskapPerioderÅrsak(grunnlag).ifPresent(resultat::add);
        // BOSATT
        utledBosattÅrsak(grunnlag).ifPresent(resultat::add);

        // LOVLIG OPPHOLD
        utledOppholdÅrsak(grunnlag).ifPresent(resultat::add);
        utledOppholdsrettÅrsak(grunnlag).ifPresent(resultat::add);
        return resultat;
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledOppholdsrettÅrsak(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmAllePerioderMedEøsErDekketAvPerioderMedOppholdstillatelser(grunnlag)) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLDSRETT);
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledOppholdÅrsak(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmAllePerioderMedTredjelandRegionErDekketAvPerioderMedOppholdstillatelser(grunnlag)) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLD);
    }

    private static boolean sjekkOmAllePerioderMedEøsErDekketAvPerioderMedOppholdstillatelser(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        return erAllePerioderMedRegionDekketAvOppholdstillatelser(grunnlag, Personopplysninger.Region.EØS);
    }

    private static boolean sjekkOmAllePerioderMedTredjelandRegionErDekketAvPerioderMedOppholdstillatelser(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        return erAllePerioderMedRegionDekketAvOppholdstillatelser(grunnlag, Personopplysninger.Region.TREDJELAND);
    }

    private static boolean erAllePerioderMedRegionDekketAvOppholdstillatelser(MedlemskapsvilkårRegelGrunnlag grunnlag,
                                                                              Personopplysninger.Region region) {
        var regionSegmenter = grunnlag.personopplysninger()
            .regioner()
            .stream()
            .filter(srp -> srp.region().equals(region))
            .map(rp -> new LocalDateSegment<>(rp.periode(), rp.region()))
            .toList();

        var regionTimeline = new LocalDateTimeline<>(regionSegmenter)
            .intersection(grunnlag.vurderingsperiodeLovligOpphold());

        var oppholdstillatelseTimeline = new LocalDateTimeline<>(grunnlag.personopplysninger()
            .oppholdstillatelser()
            .stream()
            .map(o -> new LocalDateSegment<>(o, Boolean.TRUE))
            .collect(Collectors.toSet()), (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, Boolean.TRUE));
        return regionTimeline.disjoint(oppholdstillatelseTimeline).isEmpty();
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledBosattÅrsak(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmOppgittUtenlandsopphold(grunnlag.søknad())) {
            return Optional.of(BOSATT);
        }
        if (!sjekkOmBosattPersonstatus(grunnlag)) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmUtenlandsadresser(grunnlag)) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmManglendeBosted(grunnlag)) {
            return Optional.of(BOSATT);
        }
        return Optional.empty();
    }

    private static boolean sjekkOmManglendeBosted(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var bostedsadresserSegments = grunnlag.personopplysninger()
            .adresser()
            .stream()
            .filter(a -> a.type() == BOSTEDSADRESSE)
            .map(a -> new LocalDateSegment<>(a.periode(), Boolean.TRUE))
            .collect(Collectors.toSet());
        var timeline = new LocalDateTimeline<>(bostedsadresserSegments, StandardCombinators::alwaysTrueForMatch);
        return !new LocalDateTimeline<>(grunnlag.vurderingsperiodeBosatt(), Boolean.TRUE).disjoint(timeline).isEmpty();
    }

    private static boolean sjekkOmUtenlandsadresser(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var utenlandsAdresserSegments = grunnlag.personopplysninger()
            .adresser()
            .stream()
            .filter(MedlemskapsvilkårRegelGrunnlag.Adresse::erUtenlandsk)
            .map(a -> new LocalDateSegment<>(a.periode(), Boolean.TRUE))
            .collect(Collectors.toSet());
        var timeline = new LocalDateTimeline<>(utenlandsAdresserSegments, StandardCombinators::alwaysTrueForMatch);
        return !timeline.intersection(grunnlag.vurderingsperiodeBosatt()).isEmpty();
    }

    private static boolean sjekkOmBosattPersonstatus(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var personstatusPerioder = grunnlag.personopplysninger().personstatus();
        var gyldigeStatuser = Set.of(Type.BOSATT_ETTER_FOLKEREGISTERLOVEN, Type.DØD);
        var personstatusTimeline = new LocalDateTimeline<>(
            personstatusPerioder.stream().map(p -> new LocalDateSegment<>(p.interval(), p.type())).collect(Collectors.toSet()));
        return personstatusTimeline.combine(new LocalDateTimeline<>(grunnlag.vurderingsperiodeLovligOpphold(), Boolean.TRUE),
            (datoInterval, datoSegment, datoSegment2) -> {
                if (datoSegment == null) {
                    return new LocalDateSegment<>(datoInterval, Boolean.FALSE);
                }
                return new LocalDateSegment<>(datoInterval, gyldigeStatuser.contains(datoSegment.getValue()) ? Boolean.TRUE : Boolean.FALSE);
            }, JoinStyle.RIGHT_JOIN).stream().allMatch(s -> s.getValue() == Boolean.TRUE);
    }

    private static boolean sjekkOmOppgittUtenlandsopphold(MedlemskapsvilkårRegelGrunnlag.Søknad søknad) {
        return !søknad.utenlandsopphold().isEmpty();
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledMedlemskapPerioderÅrsak(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        return sjekkOmPerioderMedMedlemskapsperioder(grunnlag) ? Optional.of(MEDLEMSKAPSPERIODER_FRA_REGISTER) : Optional.empty();
    }

    private static boolean sjekkOmPerioderMedMedlemskapsperioder(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var vurderingsperiode = grunnlag.vurderingsperiodeBosatt();
        var registerMedlemskapPerioder = grunnlag.registrertMedlemskapPerioder();

        return registerMedlemskapPerioder.stream().anyMatch(mp -> mp.overlaps(vurderingsperiode));
    }
}
