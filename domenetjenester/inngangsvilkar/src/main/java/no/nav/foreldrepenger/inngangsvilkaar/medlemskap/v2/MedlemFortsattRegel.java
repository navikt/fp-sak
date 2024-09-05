package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.harRegionIPeriode;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmBosattPersonstatus;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmManglendeBosted;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmOppholdstillatelserIHelePeriodenMedTredjelandsRegion;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmUtenlandsadresser;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.BOSATT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLD;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.PersonstatusPeriode.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//TODO reimplementeres i fp-inngangsvilkår
final class MedlemFortsattRegel {

    private MedlemFortsattRegel() {
    }

    static Set<MedlemskapAksjonspunktÅrsak> kjørRegler(MedlemFortsattRegelGrunnlag grunnlag) {
        var resultat = new HashSet<MedlemskapAksjonspunktÅrsak>();
        utledMedlemskapPerioderÅrsak(grunnlag).ifPresent(resultat::add);
        // BOSATT
        utledBosattÅrsak(grunnlag).ifPresent(resultat::add);

        // LOVLIG OPPHOLD
        utledLovligOppholdÅrsak(grunnlag).ifPresent(resultat::add);
        // Oppholdsrett vurdert som kke relevant for fortsatt medlemskap
        return resultat;
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledLovligOppholdÅrsak(MedlemFortsattRegelGrunnlag grunnlag) {
        if (!harRegionIPeriode(grunnlag.personopplysninger(), Personopplysninger.Region.TREDJELAND, grunnlag.vurderingsperiode())) {
            return Optional.empty();
        }
        if (sjekkOmOppholdstillatelserIHelePeriodenMedTredjelandsRegion(grunnlag.vurderingsperiode(), grunnlag.personopplysninger())) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLD);
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledBosattÅrsak(MedlemFortsattRegelGrunnlag grunnlag) {
        var gyldigePersonStatus = List.of(Type.BOSATT_ETTER_FOLKEREGISTERLOVEN, Type.DØD, startStatus(grunnlag));
        if (!sjekkOmBosattPersonstatus(gyldigePersonStatus, grunnlag.personopplysninger(), grunnlag.vurderingsperiode())) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmUtenlandsadresser(grunnlag.personopplysninger(), grunnlag.vurderingsperiode())) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmManglendeBosted(grunnlag.personopplysninger(), grunnlag.vurderingsperiode())) {
            return Optional.of(BOSATT);
        }
        return Optional.empty();
    }

    private static Type startStatus(MedlemFortsattRegelGrunnlag grunnlag) {
        var dagenFørVurderingsperiode = grunnlag.vurderingsperiode().getFomDato().minusDays(1);
        return grunnlag.personopplysninger()
            .personstatus()
            .stream()
            .filter(s -> s.interval().contains(dagenFørVurderingsperiode))
            .findFirst()
            .orElseThrow() //Greit med throw her?
            .type();
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledMedlemskapPerioderÅrsak(MedlemFortsattRegelGrunnlag grunnlag) {
        return sjekkOmPerioderMedMedlemskapsBeslutninger(grunnlag) ? Optional.of(MEDLEMSKAPSPERIODER_FRA_REGISTER) : Optional.empty();
    }

    private static boolean sjekkOmPerioderMedMedlemskapsBeslutninger(MedlemFortsattRegelGrunnlag grunnlag) {
        var vurderingsperiode = grunnlag.vurderingsperiode();
        var registerMedlemskapBeslutninger = grunnlag.registrertMedlemskapBeslutning();

        return registerMedlemskapBeslutninger.stream()
            .filter(b -> b.beslutningsdato() == null || vurderingsperiode.contains(b.beslutningsdato()))
            .anyMatch(mp -> mp.interval().overlaps(vurderingsperiode));
    }
}
