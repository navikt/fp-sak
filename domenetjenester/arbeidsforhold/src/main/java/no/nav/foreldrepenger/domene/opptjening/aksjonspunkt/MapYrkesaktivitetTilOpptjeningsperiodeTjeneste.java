package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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

public final class MapYrkesaktivitetTilOpptjeningsperiodeTjeneste {

    private MapYrkesaktivitetTilOpptjeningsperiodeTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            Yrkesaktivitet overstyrtAktivitet) {
        var type = utledOpptjeningType(mapArbeidOpptjening, registerAktivitet.getArbeidType());
        return new ArrayList<>(mapAktivitetsavtaler(behandlingReferanse, registerAktivitet, grunnlag,
                vurderForSaksbehandling, type, overstyrtAktivitet));
    }

    private static OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
                .stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapAktivitetsavtaler(BehandlingReferanse behandlingReferanse,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            OpptjeningAktivitetType type,
            Yrkesaktivitet overstyrtAktivitet) {
        List<OpptjeningsperiodeForSaksbehandling> perioderForAktivitetsavtaler = new ArrayList<>();
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        for (var avtale : gjeldendeAvtaler(grunnlag, skjæringstidspunkt, registerAktivitet, overstyrtAktivitet)) {
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                    .medOpptjeningAktivitetType(type)
                    .medPeriode(avtale.getPeriode())
                    .medBegrunnelse(avtale.getBeskrivelse())
                    .medStillingsandel(finnStillingsprosent(registerAktivitet));
            harSaksbehandlerVurdert(builder, type, behandlingReferanse, registerAktivitet, vurderForSaksbehandling, grunnlag);
            settArbeidsgiverInformasjon(gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet), builder);
            builder.medVurderingsStatus(
                    vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, overstyrtAktivitet, grunnlag,
                            grunnlag.harBlittSaksbehandlet()));
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

    private static Stillingsprosent finnStillingsprosent(Yrkesaktivitet registerAktivitet) {
        var defaultStillingsprosent = new Stillingsprosent(0);
        if (registerAktivitet.erArbeidsforhold() || ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(registerAktivitet.getArbeidType())) {
            var filter = new YrkesaktivitetFilter(null, List.of(registerAktivitet));
            return filter.getAktivitetsAvtalerForArbeid()
                    .stream()
                    .map(AktivitetsAvtale::getProsentsats)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Stillingsprosent::getVerdi))
                    .orElse(defaultStillingsprosent);
        }
        return defaultStillingsprosent;
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
            LocalDate skjæringstidspunktForOpptjening,
            Yrkesaktivitet registerAktivitet,
            Yrkesaktivitet overstyrtAktivitet) {
        var gjeldendeAktivitet = gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet);
        if (registerAktivitet.erArbeidsforhold()) {
            return MapAnsettelsesPeriodeOgPermisjon.beregn(grunnlag, gjeldendeAktivitet);
        }
        if (ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(registerAktivitet.getArbeidType())) {
            // Inntil videre antar vi at man ikke velger bruk permisjon på frilans-permisjoner. Stortinget har en spesiell praksis
            return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), gjeldendeAktivitet).før(skjæringstidspunktForOpptjening)
                .getAnsettelsesPerioderFrilans(gjeldendeAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), gjeldendeAktivitet).før(skjæringstidspunktForOpptjening)
                .getAktivitetsAvtalerForArbeid();
    }

    private static Yrkesaktivitet gjeldendeAktivitet(Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet) {
        return overstyrtAktivitet == null ? registerAktivitet : overstyrtAktivitet;
    }

    private static void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
            BehandlingReferanse behandlingReferanse, Yrkesaktivitet registerAktivitet,
            OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, grunnlag, false)
                .equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

}
