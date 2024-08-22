package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.erAllePerioderMedRegionDekketAvOppholdstillatelser;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmBosattPersonstatus;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmManglendeBosted;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmUtenlandsadresser;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.BOSATT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLD;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.PersonstatusPeriode.Type;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

//TODO reimplementeres i fp-inngangsvilkår
final class MedlemInngangsvilkårRegel {

    private MedlemInngangsvilkårRegel() {
    }

    static Set<MedlemskapAksjonspunktÅrsak> kjørRegler(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        var resultat = new HashSet<MedlemskapAksjonspunktÅrsak>();
        utledMedlemskapPerioderÅrsak(grunnlag).ifPresent(resultat::add);
        // BOSATT
        utledBosattÅrsak(grunnlag).ifPresent(resultat::add);

        // LOVLIG OPPHOLD
        utledOppholdÅrsak(grunnlag).ifPresent(resultat::add);
        utledOppholdsrettÅrsak(grunnlag).ifPresent(resultat::add);
        return resultat;
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledOppholdsrettÅrsak(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmAllePerioderMedEøsErDekketAvPerioderMedOppholdstillatelser(grunnlag)) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLDSRETT);
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledOppholdÅrsak(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmAllePerioderMedTredjelandRegionErDekketAvPerioderMedOppholdstillatelser(grunnlag)) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLD);
    }

    private static boolean sjekkOmAllePerioderMedEøsErDekketAvPerioderMedOppholdstillatelser(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        return erAllePerioderMedRegionDekketAvOppholdstillatelser(grunnlag.vurderingsperiodeLovligOpphold(), Personopplysninger.Region.EØS,
            grunnlag.personopplysninger());
    }

    private static boolean sjekkOmAllePerioderMedTredjelandRegionErDekketAvPerioderMedOppholdstillatelser(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        return erAllePerioderMedRegionDekketAvOppholdstillatelser(grunnlag.vurderingsperiodeLovligOpphold(), Personopplysninger.Region.TREDJELAND,
            grunnlag.personopplysninger());
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledBosattÅrsak(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmOppgittUtenlandsopphold(grunnlag.søknad())) {
            return Optional.of(BOSATT);
        }
        var gyldigeStatuser = Set.of(Type.BOSATT_ETTER_FOLKEREGISTERLOVEN, Type.DØD);
        if (!sjekkOmBosattPersonstatus(gyldigeStatuser, grunnlag.personopplysninger(), grunnlag.vurderingsperiodeLovligOpphold())) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmUtenlandsadresser(grunnlag.personopplysninger(), grunnlag.vurderingsperiodeBosatt())) {
            return Optional.of(BOSATT);
        }
        if (sjekkOmManglendeBosted(grunnlag.personopplysninger(), grunnlag.vurderingsperiodeBosatt())) {
            return Optional.of(BOSATT);
        }
        return Optional.empty();
    }

    private static boolean sjekkOmOppgittUtenlandsopphold(MedlemInngangsvilkårRegelGrunnlag.Søknad søknad) {
        return !søknad.utenlandsopphold().isEmpty();
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledMedlemskapPerioderÅrsak(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        return sjekkOmPerioderMedMedlemskapsBeslutninger(grunnlag) ? Optional.of(MEDLEMSKAPSPERIODER_FRA_REGISTER) : Optional.empty();
    }

    private static boolean sjekkOmPerioderMedMedlemskapsBeslutninger(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        var vurderingsperiode = grunnlag.vurderingsperiodeBosatt();
        var registerMedlemskapBeslutninger = grunnlag.registrertMedlemskapBeslutning();

        return registerMedlemskapBeslutninger.stream().anyMatch(mp -> mp.interval().overlaps(vurderingsperiode));
    }
}
