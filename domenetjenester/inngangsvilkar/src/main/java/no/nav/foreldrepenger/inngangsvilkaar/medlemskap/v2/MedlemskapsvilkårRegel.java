package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.BOSATT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLD;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.PersonstatusPeriode.*;
import static no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

//TODO reimplementeres i fp-inngangsvilkår
final class MedlemskapsvilkårRegel {

    private MedlemskapsvilkårRegel() {
    }

    static Set<MedlemskapAksjonspunktÅrsak> kjørRegler(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var resultat = new HashSet<MedlemskapAksjonspunktÅrsak>();
        utledMedlemskapPerioderÅrsak(grunnlag).ifPresent(resultat::add);
        utledBosattÅrsak(grunnlag).ifPresent(resultat::add);
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
        //TODO skal vi telle udefinert region som tredjeland?
        return erAllePerioderMedRegionDekketAvOppholdstillatelser(grunnlag, Personopplysninger.Region.TREDJELAND);
    }

    private static boolean erAllePerioderMedRegionDekketAvOppholdstillatelser(MedlemskapsvilkårRegelGrunnlag grunnlag,
                                                                              Personopplysninger.Region region) {
        var statsborgerskapSegmenter = grunnlag.personopplysninger()
            .regioner()
            .stream()
            .filter(srp -> srp.region().equals(region))
            .map(rp -> new LocalDateSegment<>(rp.interval(), rp.region()))
            .toList();

        var regionTimeline = new LocalDateTimeline<>(statsborgerskapSegmenter, (datoInterval, datoSegment, datoSegment2) -> {
            var prio = datoSegment.getValue().getPrioritet() < datoSegment2.getValue().getPrioritet() ? datoSegment : datoSegment2;
            return new LocalDateSegment<>(datoInterval, prio.getValue());
        }).intersection(grunnlag.vurderingsperiode());

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
        return Optional.empty();
    }

    private static boolean sjekkOmBosattPersonstatus(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var personstatusPerioder = grunnlag.personopplysninger().personstatus();
        var gyldigeStatuser = Set.of(Type.BOSATT, Type.DØD);
        var personstatusTimeline = new LocalDateTimeline<>(
            personstatusPerioder.stream().map(p -> new LocalDateSegment<>(p.interval(), p.type())).collect(Collectors.toSet()));
        return personstatusTimeline.combine(new LocalDateTimeline<>(grunnlag.vurderingsperiode(), Boolean.TRUE),
            (datoInterval, datoSegment, datoSegment2) -> {
                if (datoSegment == null) {
                    return new LocalDateSegment<>(datoInterval, Boolean.FALSE);
                }
                return new LocalDateSegment<>(datoInterval, gyldigeStatuser.contains(datoSegment.getValue()) ? Boolean.TRUE : Boolean.FALSE);
            }, JoinStyle.RIGHT_JOIN).stream().allMatch(s -> s.getValue() == Boolean.TRUE);
    }

    private static boolean sjekkOmOppgittUtenlandsopphold(MedlemskapsvilkårRegelGrunnlag.Søknad søknad) {
        //TODO Bare sjekke i vurderingsperioden?
        return !søknad.utenlandsopphold().isEmpty();
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledMedlemskapPerioderÅrsak(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        return sjekkOmIngenPerioderMedMedlemskapsperioder(grunnlag) ? Optional.empty() : Optional.of(MEDLEMSKAPSPERIODER_FRA_REGISTER);
    }

    private static boolean sjekkOmIngenPerioderMedMedlemskapsperioder(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        var vurderingsperiode = grunnlag.vurderingsperiode();
        var registerMedlemskapPerioder = grunnlag.registrertMedlemskapPerioder();

        return registerMedlemskapPerioder.stream().noneMatch(mp -> mp.overlaps(vurderingsperiode));
    }
}
