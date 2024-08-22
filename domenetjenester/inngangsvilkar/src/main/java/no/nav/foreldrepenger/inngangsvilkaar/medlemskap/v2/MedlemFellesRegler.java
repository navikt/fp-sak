package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.Adresse.Type.BOSTEDSADRESSE;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

final class MedlemFellesRegler {

    private MedlemFellesRegler() {
    }


    static boolean erAllePerioderMedRegionDekketAvOppholdstillatelser(LocalDateInterval vurderingsperiode,
                                                                      Personopplysninger.Region region,
                                                                      Personopplysninger personopplysninger) {
        var regionSegmenter = personopplysninger.regioner()
            .stream()
            .filter(srp -> srp.region().equals(region))
            .map(rp -> new LocalDateSegment<>(rp.periode(), rp.region()))
            .toList();

        var regionTimeline = new LocalDateTimeline<>(regionSegmenter).intersection(vurderingsperiode);

        var oppholdstillatelseTimeline = new LocalDateTimeline<>(
            personopplysninger.oppholdstillatelser().stream().map(o -> new LocalDateSegment<>(o, Boolean.TRUE)).collect(Collectors.toSet()),
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, Boolean.TRUE));
        return regionTimeline.disjoint(oppholdstillatelseTimeline).isEmpty();
    }

    static boolean sjekkOmManglendeBosted(Personopplysninger personopplysninger, LocalDateInterval vurderingsperiode) {
        var bostedsadresserSegments = personopplysninger.adresser()
            .stream()
            .filter(a -> a.type() == BOSTEDSADRESSE)
            .map(a -> new LocalDateSegment<>(a.periode(), Boolean.TRUE))
            .collect(Collectors.toSet());
        var timeline = new LocalDateTimeline<>(bostedsadresserSegments, StandardCombinators::alwaysTrueForMatch);
        return !new LocalDateTimeline<>(vurderingsperiode, Boolean.TRUE).disjoint(timeline).isEmpty();
    }

    static boolean sjekkOmUtenlandsadresser(Personopplysninger personopplysninger, LocalDateInterval vurderingsperiode) {
        var utenlandsAdresserSegments = personopplysninger.adresser()
            .stream()
            .filter(Personopplysninger.Adresse::erUtenlandsk)
            .map(a -> new LocalDateSegment<>(a.periode(), Boolean.TRUE))
            .collect(Collectors.toSet());
        var timeline = new LocalDateTimeline<>(utenlandsAdresserSegments, StandardCombinators::alwaysTrueForMatch);
        return !timeline.intersection(vurderingsperiode).isEmpty();
    }

    static boolean sjekkOmBosattPersonstatus(Set<Personopplysninger.PersonstatusPeriode.Type> gyldigeStatuser,
                                             Personopplysninger personopplysninger,
                                             LocalDateInterval vurderingsperiode) {
        var personstatusPerioder = personopplysninger.personstatus();
        var personstatusTimeline = new LocalDateTimeline<>(
            personstatusPerioder.stream().map(p -> new LocalDateSegment<>(p.interval(), p.type())).collect(Collectors.toSet()));
        return personstatusTimeline.combine(new LocalDateTimeline<>(vurderingsperiode, Boolean.TRUE), (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null) {
                return new LocalDateSegment<>(datoInterval, Boolean.FALSE);
            }
            return new LocalDateSegment<>(datoInterval, gyldigeStatuser.contains(datoSegment.getValue()) ? Boolean.TRUE : Boolean.FALSE);
        }, LocalDateTimeline.JoinStyle.RIGHT_JOIN).stream().allMatch(s -> s.getValue() == Boolean.TRUE);
    }
}
