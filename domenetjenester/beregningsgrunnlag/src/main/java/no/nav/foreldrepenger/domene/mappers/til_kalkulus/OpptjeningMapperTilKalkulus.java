package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class OpptjeningMapperTilKalkulus {

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter,
                                                                    List<Inntektsmelding> inntektsmeldinger) {
        return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.getOpptjeningPerioder().stream()
                .filter(opp -> finnesInntektsmeldingForEllerKanBeregnesUten(opp, inntektsmeldinger))
                .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                    OpptjeningAktivitetType.fraKode(opptjeningPeriode.opptjeningAktivitetType().getKode()),
                    mapPeriode(opptjeningPeriode),
                    opptjeningPeriode.arbeidsgiverOrgNummer(),
                    opptjeningPeriode.arbeidsgiverAktørId(),
                    opptjeningPeriode.arbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(opptjeningPeriode.arbeidsforholdId()))).collect(Collectors.toList()));
    }

    private static boolean finnesInntektsmeldingForEllerKanBeregnesUten(OpptjeningAktiviteter.OpptjeningPeriode opp,
                                                                        List<Inntektsmelding> inntektsmeldinger) {
        if (opp.arbeidsgiverAktørId() == null && opp.arbeidsgiverOrgNummer() == null) {
            // Ikke et arbeidsforhold, trenger ikke ta stilling til IM
            return true;
        }
        var inntektsmeldingerForAktuellArbeidsgiver = inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(getArbeidsgiver(opp)))
            .collect(Collectors.toList());
        if (inntektsmeldingerForAktuellArbeidsgiver.isEmpty()) {
            return true;
        }
        return inntektsmeldingerForAktuellArbeidsgiver.stream()
            .anyMatch(im -> im.getArbeidsforholdRef().gjelderFor(opp.arbeidsforholdId()));
    }

    private static Arbeidsgiver getArbeidsgiver(OpptjeningAktiviteter.OpptjeningPeriode opp) {
        if (opp.arbeidsgiverAktørId() != null) {
            return Arbeidsgiver.person(new AktørId(opp.arbeidsgiverAktørId()));
        }
        return Arbeidsgiver.virksomhet(opp.arbeidsgiverOrgNummer());
    }

    private static Intervall mapPeriode(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        if (opptjeningPeriode.periode().getTom() == null) {
            return Intervall.fraOgMed(opptjeningPeriode.periode().getFom());
        }
        return Intervall.fraOgMedTilOgMed(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom());
    }
}
