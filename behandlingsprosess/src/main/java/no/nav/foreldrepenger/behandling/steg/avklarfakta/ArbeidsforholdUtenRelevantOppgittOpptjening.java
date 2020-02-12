package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;


public final class ArbeidsforholdUtenRelevantOppgittOpptjening {

    private ArbeidsforholdUtenRelevantOppgittOpptjening() {
        // Skjuler default
    }

    public static boolean erUtenRelevantOppgittOpptjening(AksjonspunktUtlederInput param, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOpt){
        if (inntektArbeidYtelseGrunnlagOpt.isPresent()) {
            InntektArbeidYtelseGrunnlag grunnlag = inntektArbeidYtelseGrunnlagOpt.get();
            if (finnesIkkeArbeidsforhold(grunnlag, param.getAktørId(), param.getSkjæringstidspunkt()) && !finnesYtelseAAPEllerDP(grunnlag, param.getAktørId())) {
                return sjekkForUtenOppgittOpptjening(grunnlag);
            }
            return false;
        }
        return true;
    }

    private static boolean finnesYtelseAAPEllerDP(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        YtelseFilter ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        return ytelseFilter.getAlleYtelser().stream().anyMatch(yt -> erDPEllerAAP(yt.getRelatertYtelseType()));
    }

    private static boolean erDPEllerAAP(RelatertYtelseType relatertYtelseType) {
        return RelatertYtelseType.ARBEIDSAVKLARINGSPENGER.equals(relatertYtelseType) ||  RelatertYtelseType.DAGPENGER.equals(relatertYtelseType);
    }

    private static boolean sjekkForUtenOppgittOpptjening(InntektArbeidYtelseGrunnlag grunnlag) {
        Optional<OppgittOpptjening> oppgittOpptjeningOpt = grunnlag.getOppgittOpptjening();
        if (oppgittOpptjeningOpt.isPresent()) {
            OppgittOpptjening oppgittOpptjening = oppgittOpptjeningOpt.get();
            // Militær- og siviltjeneste, Vartpenger/Ventelønn, og Etterlønn/Sluttpakke
            List<OppgittAnnenAktivitet> annenAktiviteter = finnRelevanteAnnenAktiviteter(oppgittOpptjening);
            if (!annenAktiviteter.isEmpty()) {
                return false;
            }
            // Frilansvirksomhet
            Optional<OppgittFrilans> frilansOpt = oppgittOpptjening.getFrilans();
            if (frilansOpt.isPresent()) {
                return false;
            }
            // Selvstending næringsdrivende
            List<OppgittEgenNæring> egenNæring = oppgittOpptjening.getEgenNæring();
            return egenNæring.isEmpty();
        }
        return true;
    }

    private static boolean finnesIkkeArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId, Skjæringstidspunkt skjæringstidspunkt) {
        var yaFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));
        return harIkkeRelevantArbeidsforhold(yaFilter, skjæringstidspunkt);
    }

    private static boolean harIkkeRelevantArbeidsforhold(YrkesaktivitetFilter yaFilter, Skjæringstidspunkt skjæringstidspunkt) {
        return yaFilter.getYrkesaktiviteter().stream()
            .noneMatch(akt -> erRelevant(akt, skjæringstidspunkt));
    }

    private static boolean erRelevant(Yrkesaktivitet yrkesaktivitet, Skjæringstidspunkt skjæringstidspunkt) {
        return yrkesaktivitet.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .anyMatch(a -> a.getPeriode().inkluderer(skjæringstidspunkt.getUtledetSkjæringstidspunkt()));
    }

    private static List<OppgittAnnenAktivitet> finnRelevanteAnnenAktiviteter(OppgittOpptjening oppgittOpptjening) {
        return oppgittOpptjening.getAnnenAktivitet().stream()
            .filter(annenAktivitet ->
                annenAktivitet.getArbeidType().equals(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE) ||
                annenAktivitet.getArbeidType().equals(ArbeidType.VENTELØNN_VARTPENGER) ||
                annenAktivitet.getArbeidType().equals(ArbeidType.ETTERLØNN_SLUTTPAKKE))
            .collect(Collectors.toList());
    }

}
