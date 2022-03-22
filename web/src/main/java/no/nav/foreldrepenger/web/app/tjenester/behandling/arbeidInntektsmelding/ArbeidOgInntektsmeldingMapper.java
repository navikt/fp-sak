package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektspostDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.PermisjonOgMangelDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;

public class ArbeidOgInntektsmeldingMapper {
    private static final Set<AksjonspunktÅrsak> MANGEL_INNTEKTSMELDING = Set.of(AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);

    private ArbeidOgInntektsmeldingMapper() {
        // SKjuler default
    }

    public static InntektsmeldingDto mapInntektsmelding(Inntektsmelding im,
                                                        Collection<ArbeidsforholdReferanse> referanser,
                                                        Optional<KontaktinformasjonIM> kontaktinfo,
                                                        Optional<String> dokumentId,
                                                        List<ArbeidsforholdMangel> mangler,
                                                        List<ArbeidsforholdValg> saksbehandlersVurderinger) {
        var inntekstmeldingMangel = finnIdentifisertInntektsmeldingMangel(im.getArbeidsgiver(), im.getArbeidsforholdRef(), mangler);
        var vurderingPåInntektsmelding = finnSaksbehandlersVurderingPåInntektsmelding(im.getArbeidsgiver(), im.getArbeidsforholdRef(), saksbehandlersVurderinger);
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
                im.getInnsendingstidspunkt(),
                inntekstmeldingMangel.orElse(null),
                vurderingPåInntektsmelding.map(ArbeidsforholdValg::getBegrunnelse).orElse(null),
                vurderingPåInntektsmelding.map(ArbeidsforholdValg::getVurdering).orElse(null));
    }

    private static Optional<ArbeidsforholdValg> finnSaksbehandlersVurderingPåInntektsmelding(Arbeidsgiver arbeidsgiver,
                                                                                             InternArbeidsforholdRef arbeidsforholdRef,
                                                                                             List<ArbeidsforholdValg> saksbehandlersVurderinger) {
        return saksbehandlersVurderinger.stream()
            .filter(vurdering -> vurdering.getArbeidsgiver().equals(arbeidsgiver) && vurdering.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
            .findFirst();
    }

    private static Optional<AksjonspunktÅrsak> finnIdentifisertMangelPermisjon(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdMangel> mangler) {
        return mangler.stream()
                .filter(mangel -> mangel.arbeidsgiver().equals(arbeidsgiver) && mangel.ref().gjelderFor(arbeidsforholdRef))
                .filter(mangel -> AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO.equals(mangel.årsak()))
                .findFirst()
                .map(ArbeidsforholdMangel::årsak);
    }

    private static Optional<AksjonspunktÅrsak> finnIdentifisertInntektsmeldingMangel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, List<ArbeidsforholdMangel> mangler) {
        return mangler.stream()
                .filter(mangel -> mangel.arbeidsgiver().equals(arbeidsgiver) && mangel.ref().gjelderFor(arbeidsforholdRef))
                .filter(mangel -> MANGEL_INNTEKTSMELDING.contains(mangel.årsak()))
                .findFirst()
                .map(ArbeidsforholdMangel::årsak);
    }

    private static BigDecimal fraBeløp(Beløp beløp) {
        return beløp == null ? null : beløp.getVerdi();
    }

    public static List<ArbeidsforholdDto> mapArbeidsforholdUtenOverstyringer(YrkesaktivitetFilter filter,
                                                                             Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                                             LocalDate stp,
                                                                             List<ArbeidsforholdMangel> mangler,
                                                                             List<ArbeidsforholdValg> saksbehandlersVurderingAvMangler,
                                                                             List<ArbeidsforholdOverstyring> overstyringAvPermisjon) {
        List<ArbeidsforholdDto> dtoer = new ArrayList<>();
        filter.getYrkesaktiviteter().forEach(ya -> mapTilArbeidsforholdDto(arbeidsforholdReferanser, stp, ya, mangler, saksbehandlersVurderingAvMangler, overstyringAvPermisjon).ifPresent(dtoer::add));
        return dtoer;
    }

    static Optional<ArbeidsforholdDto> mapTilArbeidsforholdDto(Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                               LocalDate stp,
                                                               Yrkesaktivitet ya,
                                                               List<ArbeidsforholdMangel> alleIdentifiserteMangler,
                                                               List<ArbeidsforholdValg> saksbehandlersVurderingAvMangler,
                                                               List<ArbeidsforholdOverstyring> overstyringAvPermisjon) {
        var ansettelsesperiode = finnRelevantAnsettelsesperiode(ya, stp);
        var mangelInntektsmelding =  finnIdentifisertInntektsmeldingMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), alleIdentifiserteMangler);
        var mangelPermisjon = finnIdentifisertMangelPermisjon(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), alleIdentifiserteMangler);
        var vurdering = finnSaksbehandlersVurderingAvMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), saksbehandlersVurderingAvMangler);

        return ansettelsesperiode.map(datoIntervallEntitet -> new ArbeidsforholdDto(ya.getArbeidsgiver() == null ? null : ya.getArbeidsgiver().getIdentifikator(),
                ya.getArbeidsforholdRef().getReferanse(),
                finnEksternRef(ya.getArbeidsforholdRef(), arbeidsforholdReferanser).orElse(null),
                datoIntervallEntitet.getFomDato(),
                datoIntervallEntitet.getTomDato(),
                finnStillingsprosentForPeriode(ya, datoIntervallEntitet).orElse(null),
                mangelInntektsmelding.orElse(null),
                vurdering.map(ArbeidsforholdValg::getVurdering).orElse(null),
                mapPermisjonOgMangel(ya, stp, mangelPermisjon.orElse(null), overstyringAvPermisjon).orElse(null),
                vurdering.map(ArbeidsforholdValg::getBegrunnelse).orElse(null)));
    }

    private static Optional<PermisjonOgMangelDto> mapPermisjonOgMangel(Yrkesaktivitet ya, LocalDate stp, AksjonspunktÅrsak årsak,  List<ArbeidsforholdOverstyring> overstyringAvPermisjon) {
        return HåndterePermisjoner.hentPermisjonOgMangel(ya, stp, årsak, utledBekreftetStatus(ya.getArbeidsforholdRef(), overstyringAvPermisjon) );
    }

    private static BekreftetPermisjonStatus utledBekreftetStatus(InternArbeidsforholdRef internArbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringAvPermisjon) {
        return overstyringAvPermisjon.stream()
                .filter(os -> internArbeidsforholdRef.equals(os.getArbeidsforholdRef()))
                .findFirst()
                .flatMap(ArbeidsforholdOverstyring::getBekreftetPermisjon)
                .map(BekreftetPermisjon::getStatus)
                .orElse(null);
    }

    private static Optional<ArbeidsforholdValg> finnSaksbehandlersVurderingAvMangel(Arbeidsgiver arbeidsgiver,
                                                                                    InternArbeidsforholdRef arbeidsforholdRef,
                                                                                    List<ArbeidsforholdValg> saksbehandlersVurderingAvMangler) {
        return saksbehandlersVurderingAvMangler.stream()
            .filter(vurdering -> vurdering.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef)
                && vurdering.getArbeidsgiver().equals(arbeidsgiver))
            .findFirst();

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
            .filter(post -> post.getPeriode().overlapper(sisteÅr))
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

    public static List<ArbeidsforholdDto> mapOverstyrteArbeidsforhold(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                      Collection<ArbeidsforholdReferanse> referanser,
                                                                      List<ArbeidsforholdMangel> mangler ) {
        // Trenger kun arbeidsforhold opprettet av handlinger som  kan oppstå i 5085
        return arbeidsforholdOverstyringer.stream()
            .filter(os -> ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING.equals(os.getHandling())
                || ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER.equals(os.getHandling()))
            .map(overstyring -> mapManueltArbeidsforhold(overstyring, referanser, mangler))
            .collect(Collectors.toList());
    }

    private static ArbeidsforholdDto mapManueltArbeidsforhold(ArbeidsforholdOverstyring overstyring,
                                                              Collection<ArbeidsforholdReferanse> referanser,
                                                              List<ArbeidsforholdMangel> mangler) {
        var eksternRef = finnEksternRef(overstyring.getArbeidsforholdRef(), referanser);
        var mangel = finnIdentifisertInntektsmeldingMangel(overstyring.getArbeidsgiver(), overstyring.getArbeidsforholdRef(), mangler);
        var relevantPeriode = overstyring.getArbeidsforholdOverstyrtePerioder().stream()
            .map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode)
            .findFirst();
        return new ArbeidsforholdDto(overstyring.getArbeidsgiver().getIdentifikator(),
            overstyring.getArbeidsforholdRef().getReferanse(),
            eksternRef.orElse(null),
            relevantPeriode.map(DatoIntervallEntitet::getFomDato).orElse(null),
            relevantPeriode.map(DatoIntervallEntitet::getTomDato).orElse(null),
            overstyring.getStillingsprosent() == null ? null : overstyring.getStillingsprosent().getVerdi(),
            null,
            utledSaksbehandlerVurderingOmManueltArbeidsforhold(mangel),
            null,
            overstyring.getBegrunnelse());
    }

    private static ArbeidsforholdKomplettVurderingType utledSaksbehandlerVurderingOmManueltArbeidsforhold(Optional<AksjonspunktÅrsak> mangel) {
        var erOpprettetFraInntektsmelding = mangel
            .map(m -> m.equals(AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD))
            .orElse(false);
        return erOpprettetFraInntektsmelding
            ? ArbeidsforholdKomplettVurderingType.OPPRETT_BASERT_PÅ_INNTEKTSMELDING
            : ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER;
    }
}
