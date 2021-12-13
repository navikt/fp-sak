package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektspostDto;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArbeidOgInntektsmeldingMapper {

    private ArbeidOgInntektsmeldingMapper() {
        // SKjuler default
    }

    public static InntektsmeldingDto mapInntektsmelding(Inntektsmelding im,
                                                        Collection<ArbeidsforholdReferanse> referanser,
                                                        Optional<KontaktinformasjonIM> kontaktinfo,
                                                        Optional<String> dokumentId) {
        return new InntektsmeldingDto(
            fraBeløp(im.getInntektBeløp()),
            fraBeløp(im.getRefusjonBeløpPerMnd()),
            im.getArbeidsgiver().getIdentifikator(),
            finnEksternRef(im.getArbeidsforholdRef(), referanser).orElse(null),
            im.getArbeidsforholdRef().getReferanse(),
            kontaktinfo.map(KontaktinformasjonIM::kontaktPerson).orElse(null),
            kontaktinfo.map(KontaktinformasjonIM::kontaktTelefonNummer).orElse(null),
            im.getJournalpostId() != null ? im.getJournalpostId().getVerdi() : null,
            dokumentId.orElse(null),
            im.getMottattDato(),
            im.getInnsendingstidspunkt());
    }

    private static BigDecimal fraBeløp(Beløp beløp) {
        return beløp == null ? null : beløp.getVerdi();
    }

    public static List<ArbeidsforholdDto> mapArbeidsforholdUtenOverstyringer(YrkesaktivitetFilter filter,
                                                                             Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                                             LocalDate stp) {
        List<ArbeidsforholdDto> dtoer = new ArrayList<>();
        filter.getYrkesaktiviteter().forEach(ya -> mapTilDto(arbeidsforholdReferanser, stp, ya).ifPresent(dtoer::add));
        return dtoer;
    }

    private static Optional<ArbeidsforholdDto> mapTilDto(Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser, LocalDate stp, Yrkesaktivitet ya) {
        var ansettelsesperiode = finnRelevantAnsettelsesperiode(ya, stp);
        return ansettelsesperiode.map(datoIntervallEntitet -> new ArbeidsforholdDto(ya.getArbeidsgiver() == null ? null : ya.getArbeidsgiver().getIdentifikator(),
            ya.getArbeidsforholdRef().getReferanse(),
            finnEksternRef(ya.getArbeidsforholdRef(), arbeidsforholdReferanser).orElse(null),
            datoIntervallEntitet.getFomDato(),
            datoIntervallEntitet.getTomDato(),
            finnStillingsprosentForPeriode(ya, datoIntervallEntitet).orElse(null)));
    }

    private static Optional<BigDecimal> finnStillingsprosentForPeriode(Yrkesaktivitet ya, DatoIntervallEntitet datoIntervallEntitet) {
        return ya.getStillingsprosentFor(datoIntervallEntitet.getFomDato()).map(Stillingsprosent::getVerdi);
    }

    private static Optional<String> finnEksternRef(InternArbeidsforholdRef arbeidsforholdRef, Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        return arbeidsforholdReferanser.stream()
            .filter(ref -> Objects.equals(ref.getInternReferanse().getReferanse(), arbeidsforholdRef.getReferanse()))
            .findFirst()
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .map(EksternArbeidsforholdRef::getReferanse);
    }

    private static Optional<DatoIntervallEntitet> finnRelevantAnsettelsesperiode(Yrkesaktivitet ya, LocalDate stp) {
        return ya.getAlleAktivitetsAvtaler().stream()
            .filter(aa -> aa.getPeriode().inkluderer(stp) || aa.getPeriode().getFomDato().isAfter(stp))
            .map(AktivitetsAvtale::getPeriode)
            .max(DatoIntervallEntitet::compareTo);
    }

    public static List<InntektDto> mapInntekter(InntektFilter filter, LocalDate utledetSkjæringstidspunkt) {
        return filter.getAlleInntektBeregningsgrunnlag().stream()
            .filter(inntekt -> inntekt.getArbeidsgiver() != null)
            .map(inntekt -> mapInntekt(inntekt, utledetSkjæringstidspunkt))
            .collect(Collectors.toList());
    }

    private static InntektDto mapInntekt(Inntekt inntekt, LocalDate utledetSkjæringstidspunkt) {
        var sisteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(utledetSkjæringstidspunkt.minusMonths(12).withDayOfMonth(1), utledetSkjæringstidspunkt);
        var poster = inntekt.getAlleInntektsposter().stream()
            .filter(post -> !post.getPeriode().overlapper(sisteÅr))
            .map(ArbeidOgInntektsmeldingMapper::mapInntektspost)
            .collect(Collectors.toList());
        return new InntektDto(inntekt.getArbeidsgiver().getIdentifikator(), poster);
    }

    private static InntektspostDto mapInntektspost(Inntektspost post) {
        return new InntektspostDto(fraBeløp(post.getBeløp()),
            post.getPeriode().getFomDato(),
            post.getPeriode().getTomDato(),
            post.getInntektspostType());
    }
}
