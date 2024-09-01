package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public final class MapYrkesaktivitetTilOpptjeningsperiodeTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.class);

    private MapYrkesaktivitetTilOpptjeningsperiodeTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYrkesaktivitet(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            Yrkesaktivitet overstyrtAktivitet) {
        var type = utledOpptjeningType(mapArbeidOpptjening, registerAktivitet.getArbeidType());
        return new ArrayList<>(mapAktivitetsavtaler(behandlingReferanse, stp, registerAktivitet, grunnlag,
                vurderForSaksbehandling, type, overstyrtAktivitet));
    }

    private static OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
                .stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapAktivitetsavtaler(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            OpptjeningAktivitetType type,
            Yrkesaktivitet overstyrtAktivitet) {
        List<OpptjeningsperiodeForSaksbehandling> perioderForAktivitetsavtaler = new ArrayList<>();
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        // Avtale er i praksis en ansettelsesavtale
        for (var avtale : gjeldendeAvtaler(behandlingReferanse, grunnlag, skjæringstidspunkt, registerAktivitet, overstyrtAktivitet)) {
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                    .medOpptjeningAktivitetType(type)
                    .medPeriode(avtale.getPeriode())
                    .medBegrunnelse(avtale.getBeskrivelse())
                    .medStillingsandel(finnStillingsprosent(registerAktivitet, avtale));
            harSaksbehandlerVurdert(builder, type, behandlingReferanse, stp, registerAktivitet, vurderForSaksbehandling, grunnlag);
            settArbeidsgiverInformasjon(gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet), builder);
            var vurdering = vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, stp, overstyrtAktivitet, grunnlag,
                grunnlag.harBlittSaksbehandlet(), registerAktivitet, avtale);
            builder.medVurderingsStatus(vurdering);
            if (harEndretPåPeriode(avtale.getPeriode(), overstyrtAktivitet)) {
                builder.medErPeriodenEndret();
            }
            perioderForAktivitetsavtaler.add(builder.build());
        }
        return perioderForAktivitetsavtaler;
    }

    public static void settArbeidsgiverInformasjon(Yrkesaktivitet yrkesaktivitet, OpptjeningsperiodeForSaksbehandling.Builder builder) {
        var arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(yrkesaktivitet.getArbeidsforholdRef(), arbeidsgiver));
        }
        if (yrkesaktivitet.getNavnArbeidsgiverUtland() != null) {
            builder.medArbeidsgiverUtlandNavn(yrkesaktivitet.getNavnArbeidsgiverUtland());
        }
    }

    public static String lagReferanseForUtlandskOrganisasjon(String navn) {
        // Nøkkel som er passe unik med skop en behandling
        return ("9" + Math.abs(Objects.hash(navn)) + "999999").substring(0, 7);
    }

    private static boolean harEndretPåPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return false;
        }

        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode)
                .noneMatch(p -> p.equals(periode));
    }

    private static Stillingsprosent finnStillingsprosent(Yrkesaktivitet registerAktivitet, AktivitetsAvtale ansettelsesPeriode) {
        var defaultStillingsprosent = new Stillingsprosent(0);
        if (registerAktivitet.erArbeidsforhold() || ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(registerAktivitet.getArbeidType())) {
            var filter = new YrkesaktivitetFilter(null, List.of(registerAktivitet));
            return filter.getAktivitetsAvtalerForArbeid()
                    .stream()
                    .filter(p -> p.getPeriode().overlapper(ansettelsesPeriode.getPeriode()))
                    .map(AktivitetsAvtale::getProsentsats)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Stillingsprosent::getVerdi))
                    .orElse(defaultStillingsprosent);
        }
        return defaultStillingsprosent;
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            LocalDate skjæringstidspunktForOpptjening,
            Yrkesaktivitet registerAktivitet,
            Yrkesaktivitet overstyrtAktivitet) {
        var gjeldendeAktivitet = gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), gjeldendeAktivitet);
        if (registerAktivitet.erArbeidsforhold()) {
            return splitAnsettelsesPerioderVedNullProsent(filter, gjeldendeAktivitet, MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, gjeldendeAktivitet));
        }
        if (ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(registerAktivitet.getArbeidType())) {
            // Inntil videre antar vi at man ikke velger bruk permisjon på frilans-permisjoner. Stortinget har en spesiell praksis
            return splitAnsettelsesPerioderVedNullProsent(filter, gjeldendeAktivitet,
                new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), gjeldendeAktivitet).før(skjæringstidspunktForOpptjening)
                    .getAnsettelsesPerioderFrilans(gjeldendeAktivitet));
        }
        LOG.info("MapYrkesaktivitetTilOpptjeningsperiodeTjeneste hverken arbeid eller frilans behandling {} register {} gjeldende {}", behandlingReferanse.behandlingId(),
            registerAktivitet.getArbeidType(), gjeldendeAktivitet.getArbeidType());
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), gjeldendeAktivitet).før(skjæringstidspunktForOpptjening)
                .getAktivitetsAvtalerForArbeid();
    }

    private static Yrkesaktivitet gjeldendeAktivitet(Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet) {
        return overstyrtAktivitet == null ? registerAktivitet : overstyrtAktivitet;
    }

    private static void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp, Yrkesaktivitet registerAktivitet,
            OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, stp, registerAktivitet, grunnlag, false)
                .equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

    // Knekk opp ansettelsesperiodene mot evt arbeidsavtaler med avtalt stillingsprosent 0% slik at perioder matcher og kan vurderes separat.
    private static Collection<AktivitetsAvtale> splitAnsettelsesPerioderVedNullProsent(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet,
                                                                                       Collection<AktivitetsAvtale> ansettelsesPerioder) {
        // Arbeidsavtaler med stillingsprosent 0 som overlapper en av ansettelsesperiodene
        var nullprosentAvtaler = filter.getAktivitetsAvtalerForArbeid(yrkesaktivitet).stream()
            .filter(aa -> aa.getProsentsats() == null || aa.getProsentsats().erNulltall())
            .filter(aa -> ansettelsesPerioder.stream().anyMatch(ans -> ans.getPeriode().overlapper(aa.getPeriode())))
            .toList();
        if (nullprosentAvtaler.isEmpty()) {
            return ansettelsesPerioder;
        }
        // Antar at det kan være overlapp i arbeidsavtalene med 0 prosent
        var nullprosentTidslinje = new LocalDateTimeline<>(nullprosentAvtaler.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(),a))
            .collect(Collectors.toList()), StandardCombinators::coalesceLeftHandSide);

        // Kan vurdere å lage en tidslinje av ansettelsesperioder og blir da kvitt overlappende tilfelle (bruk coalesce). Se MapAnsettelses....
        var nyListe = ansettelsesPerioder.stream()
            .flatMap(ap -> knekkVedNullProsent(ap, nullprosentTidslinje))
            .sorted(Comparator.comparing(p -> p.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        return nyListe;
    }

    // Skal beholde alle ansettelsesperioder men knekker dem ved å gjøre snitt og differanse - så konkatenere uten forsøk på compress.
    private static Stream<AktivitetsAvtale> knekkVedNullProsent(AktivitetsAvtale ansettelsesPeriode, LocalDateTimeline<AktivitetsAvtale> nullprosent) {
        var ansettelsesSegment = new LocalDateSegment<>(ansettelsesPeriode.getPeriode().getFomDato(), ansettelsesPeriode.getPeriode().getTomDato(), ansettelsesPeriode);
        var ansettelsesTidslinje = new LocalDateTimeline<>(List.of(ansettelsesSegment));
        return Stream.concat(ansettelsesTidslinje.intersection(nullprosent).toSegments().stream(),
                ansettelsesTidslinje.disjoint(nullprosent).toSegments().stream())
            .map(MapAnsettelsesPeriodeOgPermisjon::aktivitetsAvtaleFraSegment);
    }

}
