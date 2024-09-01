package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    public List<SvpTilretteleggingEntitet> utled(Behandling behandling,
            Skjæringstidspunkt skjæringstidspunkt,
            List<SvpTilretteleggingEntitet> gjeldendeTilrettelegginger) {

        var nyeTilrettelegginger = new ArrayList<SvpTilretteleggingEntitet>();
        var tilretteleggingerMedArbeidsgiver = gjeldendeTilrettelegginger.stream()
            .filter(tilrettelegging -> tilrettelegging.getArbeidsgiver().isPresent())
            .toList();

        if (tilretteleggingerMedArbeidsgiver.isEmpty()) {
            return nyeTilrettelegginger;
        }

        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling), skjæringstidspunkt, stp);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var arbeidsforholdInformasjonOpt = iayGrunnlag.getArbeidsforholdInformasjon();
        var aktørArbeidFraRegisterOpt = iayGrunnlag.getAktørArbeidFraRegister(behandling.getAktørId());

        var relevanteYrkesaktiviteter = new YrkesaktivitetFilter(arbeidsforholdInformasjonOpt, aktørArbeidFraRegisterOpt)
                .getYrkesaktiviteter()
                .stream()
                .filter(yrkesaktivitet -> AA_REGISTER_TYPER.contains(yrkesaktivitet.getArbeidType()))
                .filter(yrkesaktivitet -> harAnsettelsesperiodeSomInkludererEllerTilkommerEtterStp(stp, yrkesaktivitet))
                .toList();

        List<SvpTilretteleggingEntitet> tilretteleggingerMedArbeidsforholdId = new ArrayList<>(tilretteleggingerMedArbeidsgiver.stream()
            .filter(t -> t.getInternArbeidsforholdRef().isPresent())
            .toList());

        List<SvpTilretteleggingEntitet> tilretteleggingerUtenArbeidsforholdId = new ArrayList<>(tilretteleggingerMedArbeidsgiver.stream()
            .filter(t -> t.getInternArbeidsforholdRef().isEmpty())
            .toList());

        //Vi må sjekke om tilretteleggingene i listen tilretteleggingerMedArbeidsforholdId fortsatt har en matchende inntektsmelding
        //Dersom det ikke finnes må det opprettes nye tilrettelegginger for nye yrkesaktiviteter (feks pga endringer i aa-Reg og det er sendt inn ny IM)
        //Eller om det er endring på om arbeidsforholdsId er satt på IM eller ikke
        if (!tilretteleggingerMedArbeidsforholdId.isEmpty()) {
            var arbeidsforholdIdgruppertPerArbeidsgiver = tilretteleggingerMedArbeidsforholdId.stream().collect(Collectors.groupingBy(this::tilretteleggingNøkkel));
            List<SvpTilretteleggingEntitet> måVurderesPåNytt = finnTilretteleggingerSomMåVurderesPåNytt(inntektsmeldinger, arbeidsforholdIdgruppertPerArbeidsgiver);

            tilretteleggingerMedArbeidsforholdId.removeAll(måVurderesPåNytt);
            //Dersom det er allerede finnes tilrettelegging for samme arbeidsgiver skal den ikke legges til listen. Vi vet ikke hvilken tilrettelegging som hører til en eventuell ny arbeidsforholdsId. Vi oppretter en kopi av den første tilr vi finner for hver nye yrkesaktivitet for arbeidsgiveren.
            måVurderesPåNytt.forEach(tilr -> {
                if (tilretteleggingerUtenArbeidsforholdId.stream().map(SvpTilretteleggingEntitet::getArbeidsgiver).noneMatch(a -> a.equals(tilr.getArbeidsgiver()))) {
                    tilretteleggingerUtenArbeidsforholdId.add(tilr);
                }
            });
        }

        nyeTilrettelegginger.addAll(tilretteleggingerMedArbeidsforholdId);

        for (var tilrettelegging : tilretteleggingerUtenArbeidsforholdId) {

            var arbeidsgiver = tilrettelegging.getArbeidsgiver().orElseThrow(() -> new IllegalStateException(
                    "Utviklerfeil: Skal ikke kunne være her med en tilrettelegging uten arbeidsgiver for tilrettelegging: "
                            + tilrettelegging.getId()));

            var inntektsmeldingerForArbeidsgiver = inntektsmeldinger.stream()
                    .filter(im -> arbeidsgiver.equals(im.getArbeidsgiver()))
                    .toList();

            if (skalKunOppretteEnTilretteleggingForArbeidsgiver(inntektsmeldingerForArbeidsgiver)) {
                nyeTilrettelegginger.add(new SvpTilretteleggingEntitet.Builder(tilrettelegging).medInternArbeidsforholdRef(null).build());
            } else {
                var tilrettelegginger = opprettTilretteleggingForHverYrkesaktivitet(relevanteYrkesaktiviteter, tilrettelegging, arbeidsgiver);
                nyeTilrettelegginger.addAll(tilrettelegginger);
            }

        }
        return nyeTilrettelegginger;

    }

    private String tilretteleggingNøkkel(SvpTilretteleggingEntitet tilrettelegging) {
        return tilrettelegging.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseGet(() -> tilrettelegging.getArbeidType().getKode());
    }

    List<SvpTilretteleggingEntitet> finnTilretteleggingerSomMåVurderesPåNytt(List<Inntektsmelding> kobledeInntektsmeldinger, Map<String, List<SvpTilretteleggingEntitet>> tilrMedArbeidsforholdsIdPerArbeidsgiverMap) {
        List<SvpTilretteleggingEntitet> tilrSomMåvurderesPåNytt = new ArrayList<>();

        tilrMedArbeidsforholdsIdPerArbeidsgiverMap.forEach( (key, value) -> {
            var arbeidsgiver = Arbeidsgiver.virksomhet(key);
            var alleIderHarMatchendeIm = value.stream().map(SvpTilretteleggingEntitet::getInternArbeidsforholdRef)
                .allMatch(internArbeidsforholdRef -> finnesIdIListenAvInntektsmeldingerForArbeidsgiver(arbeidsgiver, kobledeInntektsmeldinger, internArbeidsforholdRef.orElse(null)));
            if (!alleIderHarMatchendeIm) {
                tilrSomMåvurderesPåNytt.addAll(value);
            }
        });
        return tilrSomMåvurderesPåNytt;
    }

    private boolean finnesIdIListenAvInntektsmeldingerForArbeidsgiver(Arbeidsgiver arbeidsgvier, List<Inntektsmelding> inntektsmeldinger, InternArbeidsforholdRef internArbeidsforholdRef) {
        if (internArbeidsforholdRef == null) {
            return false;
        }
        return inntektsmeldinger.stream()
            .filter(inntektsmelding -> inntektsmelding.getArbeidsgiver().equals(arbeidsgvier)).map(Inntektsmelding::getArbeidsforholdRef)
            .anyMatch( imInternArbeidsforholdRef -> imInternArbeidsforholdRef.equals(internArbeidsforholdRef));
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
