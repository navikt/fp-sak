package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmAnsettelseIHelePeriodenMedEøsRegion;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmBosattPersonstatus;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmManglendeBosted;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmOppholdstillatelserIHelePeriodenMedTredjelandsRegion;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemFellesRegler.sjekkOmUtenlandsadresser;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.BOSATT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLD;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.PersonstatusPeriode.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemInngangsvilkårRegelGrunnlag.Beløp;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

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
        if (sjekkOmAnsettelseIHelePeriodenMedEøsRegion(grunnlag.arbeid().ansettelsePerioder(), grunnlag.personopplysninger(),
            grunnlag.vurderingsperiodeLovligOpphold())) {
            if (sjekkOmInntektSiste3mndFørStp(grunnlag)) {
                return Optional.empty();
            }
        }
        return Optional.of(OPPHOLDSRETT);
    }

    private static boolean sjekkOmInntektSiste3mndFørStp(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        var inntekter = grunnlag.arbeid().inntekter();
        Set<LocalDateSegment<Beløp>> inntektSegmenter = inntekter.stream().map(i -> new LocalDateSegment<>(i.interval(), i.beløp())).collect(Collectors.toSet());
        var stpInntekt = grunnlag.skjæringstidspunkt().isAfter(grunnlag.behandlingsdato()) ? grunnlag.behandlingsdato() : grunnlag.skjæringstidspunkt();
        var relevantInntektsInterval = new LocalDateInterval(stpInntekt.minusMonths(3).minusDays(1), stpInntekt.minusDays(1));

        var inntektTimeline = new LocalDateTimeline<>(inntektSegmenter,
            (datoInterval, datoSegment, datoSegment2) -> new LocalDateSegment<>(datoInterval, datoSegment.getValue().add(datoSegment2.getValue())))
            .intersection(relevantInntektsInterval);

        var godkjentSamletInntekt = grunnlag.grunnbeløp().divide(new Beløp(BigDecimal.valueOf(4)), RoundingMode.DOWN);
        return inntektTimeline.stream().map(LocalDateSegment::getValue).reduce(Beløp.ZERO, Beløp::add).erMerEnn(godkjentSamletInntekt);
    }

    private static Optional<MedlemskapAksjonspunktÅrsak> utledOppholdÅrsak(MedlemInngangsvilkårRegelGrunnlag grunnlag) {
        if (sjekkOmOppholdstillatelserIHelePeriodenMedTredjelandsRegion(grunnlag.vurderingsperiodeLovligOpphold(), grunnlag.personopplysninger())) {
            return Optional.empty();
        }
        return Optional.of(OPPHOLD);
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
        var registerMedlemskapBeslutninger = grunnlag.registrertMedlemskapPerioder();

        return registerMedlemskapBeslutninger.stream().anyMatch(mp -> mp.overlaps(vurderingsperiode));
    }
}
