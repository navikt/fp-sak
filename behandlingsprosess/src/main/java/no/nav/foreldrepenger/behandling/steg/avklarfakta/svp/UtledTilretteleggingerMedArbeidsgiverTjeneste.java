package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
class UtledTilretteleggingerMedArbeidsgiverTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    UtledTilretteleggingerMedArbeidsgiverTjeneste() {
        // CDI
    }

    @Inject
    UtledTilretteleggingerMedArbeidsgiverTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public ArrayList<SvpTilretteleggingEntitet> utled(Behandling behandling,
            Skjæringstidspunkt skjæringstidspunkt,
            List<SvpTilretteleggingEntitet> opprinneligeTilrettelegginger) {

        var nyeTilrettelegginger = new ArrayList<SvpTilretteleggingEntitet>();
        var tilretteleggingerMedArbeidsgiver = opprinneligeTilrettelegginger.stream()
            .filter(tilrettelegging -> tilrettelegging.getArbeidsgiver().isPresent())
            .toList();

        if (tilretteleggingerMedArbeidsgiver.isEmpty()) {
            return nyeTilrettelegginger;
        }

        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling, skjæringstidspunkt), stp);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var arbeidsforholdInformasjonOpt = iayGrunnlag.getArbeidsforholdInformasjon();
        var aktørArbeidFraRegisterOpt = iayGrunnlag.getAktørArbeidFraRegister(behandling.getAktørId());

        var relevanteYrkesaktiviteter = new YrkesaktivitetFilter(arbeidsforholdInformasjonOpt, aktørArbeidFraRegisterOpt)
                .getYrkesaktiviteter()
                .stream()
                .filter(yrkesaktivitet -> AA_REGISTER_TYPER.contains(yrkesaktivitet.getArbeidType()))
                .filter(yrkesaktivitet -> harAnsettelsesperiodeSomInkludererEllerTilkommerEtterStp(stp, yrkesaktivitet))
                .toList();

        var tilretteleggingerMedArbeidsforholdId = tilretteleggingerMedArbeidsgiver.stream()
            .filter(t -> t.getInternArbeidsforholdRef().isPresent())
            .toList();

        nyeTilrettelegginger.addAll(tilretteleggingerMedArbeidsforholdId);

        var tilretteleggingerUtenArbeidsforholdId = tilretteleggingerMedArbeidsgiver.stream()
            .filter(t -> t.getInternArbeidsforholdRef().isEmpty())
            .toList();

        for (var tilrettelegging : tilretteleggingerUtenArbeidsforholdId) {

            var arbeidsgiver = tilrettelegging.getArbeidsgiver().orElseThrow(() -> new IllegalStateException(
                    "Utviklerfeil: Skal ikke kunne være her med en tilrettelegging uten arbeidsgiver for tilrettelegging: "
                            + tilrettelegging.getId()));

            var inntektsmeldingerForArbeidsgiver = inntektsmeldinger.stream()
                    .filter(im -> arbeidsgiver.equals(im.getArbeidsgiver()))
                    .toList();

            if (skalKunOppretteEnTilretteleggingForArbeidsgiver(inntektsmeldingerForArbeidsgiver)) {
                nyeTilrettelegginger.add(new SvpTilretteleggingEntitet.Builder(tilrettelegging).build());
            } else {
                var tilrettelegginger = opprettTilretteleggingForHverYrkesaktivitet(relevanteYrkesaktiviteter, tilrettelegging, arbeidsgiver);
                nyeTilrettelegginger.addAll(tilrettelegginger);
            }

        }
        return nyeTilrettelegginger;

    }

    private boolean skalKunOppretteEnTilretteleggingForArbeidsgiver(List<Inntektsmelding> inntektsmeldingerForArbeidsgiver) {
        if (inntektsmeldingerForArbeidsgiver.isEmpty()) {
            return false;
        }
        return inntektsmeldingerForArbeidsgiver.stream().allMatch(im -> InternArbeidsforholdRef.nullRef().equals(im.getArbeidsforholdRef()));
    }

    private List<SvpTilretteleggingEntitet> opprettTilretteleggingForHverYrkesaktivitet(List<Yrkesaktivitet> yrkesaktiviteter,
            SvpTilretteleggingEntitet tilrettelegging,
            Arbeidsgiver arbeidsgiver) {
        return yrkesaktiviteter.stream()
                .filter(yrkesaktivitet -> arbeidsgiver.equals(yrkesaktivitet.getArbeidsgiver()))
                .map(yrkesaktivitet -> new SvpTilretteleggingEntitet.Builder(tilrettelegging)
                        .medInternArbeidsforholdRef(yrkesaktivitet.getArbeidsforholdRef())
                        .build())
                .toList();
    }

    private boolean harAnsettelsesperiodeSomInkludererEllerTilkommerEtterStp(LocalDate stp, Yrkesaktivitet yrkesaktivitet) {
        return new YrkesaktivitetFilter(yrkesaktivitet)
                .getAnsettelsesPerioder(yrkesaktivitet)
                .stream()
                .anyMatch(aktivitetsavtale -> aktivitetsavtale.getPeriode().inkluderer(stp) ||
                        aktivitetsavtale.getPeriode().getFomDato().isAfter(stp));
    }

}
