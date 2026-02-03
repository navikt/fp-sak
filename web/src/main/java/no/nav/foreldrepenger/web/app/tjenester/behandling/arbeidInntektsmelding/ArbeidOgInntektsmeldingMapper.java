package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektspostDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.NaturalYtelseDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.PermisjonOgMangelDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.RefusjonDto;
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
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
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
import no.nav.vedtak.konfig.Tid;

public class ArbeidOgInntektsmeldingMapper {
    private static final Set<AksjonspunktÅrsak> MANGEL_INNTEKTSMELDING = Set.of(AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
    private static final Set<ArbeidsforholdHandlingType> MANUELT_ARBEIDSFOROHOLD_HANDLING = Set.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING,
        ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);

    private ArbeidOgInntektsmeldingMapper() {
        // SKjuler default
    }

    public static InntektsmeldingDto mapInntektsmelding(Inntektsmelding im,
                                                        Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                        Optional<KontaktinformasjonIM> kontaktinfo,
                                                        Optional<String> dokumentId,
                                                        List<ArbeidsforholdMangel> mangler,
                                                        List<ArbeidsforholdValg> saksbehandlersVurderinger,
                                                        List<UUID> tilknyttedeBehandlingIder) {
        var inntekstmeldingMangel = finnIdentifisertInntektsmeldingMangel(im.getArbeidsgiver(), im.getArbeidsforholdRef(), mangler);
        var vurderingPåInntektsmelding = finnSaksbehandlersVurderingPåInntektsmelding(im.getArbeidsgiver(), im.getArbeidsforholdRef(), saksbehandlersVurderinger);

        var refusjonsEndringer = im.getEndringerRefusjon();

        // Representer opphøring av refusjon som en periode med 0 som refusjon
        if (im.getRefusjonOpphører() != null && !Tid.TIDENES_ENDE.equals(im.getRefusjonOpphører() )) {
            refusjonsEndringer = new ArrayList<>(refusjonsEndringer);
            refusjonsEndringer.add(new Refusjon(new BigDecimal(0), im.getRefusjonOpphører().plusDays(1)));
        }

        return new InntektsmeldingDto(
                fraBeløp(im.getInntektBeløp()),
                fraBeløp(im.getRefusjonBeløpPerMnd()),
                im.getArbeidsgiver().getIdentifikator(),
                finnEksternRef(im.getArbeidsforholdRef(), arbeidsforholdReferanser).orElse(null),
                im.getArbeidsforholdRef().getReferanse(),
                kontaktinfo.map(KontaktinformasjonIM::kontaktPerson).orElse(null),
                kontaktinfo.map(KontaktinformasjonIM::kontaktTelefonNummer).orElse(null),
                im.getJournalpostId() != null ? im.getJournalpostId().getVerdi() : null,
                dokumentId.orElse(null),
                im.getMottattDato(),
                im.getInnsendingstidspunkt(),
                inntekstmeldingMangel.orElse(null),
                vurderingPåInntektsmelding.map(ArbeidsforholdValg::getBegrunnelse).orElse(null),
                vurderingPåInntektsmelding.map(ArbeidsforholdValg::getVurdering).orElse(null),
                im.getKildesystem() == null ? "" : im.getKildesystem(), // Enkelte eldre IM har ikke kildesystem, men frontend forventer NotNull
                im.getStartDatoPermisjon().orElse(null),
                mapNaturalYtelser(im.getNaturalYtelser()),
                mapRefusjonendringer(refusjonsEndringer),
                im.getInntektsmeldingInnsendingsårsak(),
            tilknyttedeBehandlingIder != null ? tilknyttedeBehandlingIder : List.of()
            );
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

    public static List<ArbeidsforholdDto> mapArbeidsforhold(YrkesaktivitetFilter filter,
                                                            Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                            LocalDate stp,
                                                            List<ArbeidsforholdMangel> mangler,
                                                            List<ArbeidsforholdValg> saksbehandlersVurderingAvMangler,
                                                            List<ArbeidsforholdOverstyring> overstyringer) {
        List<ArbeidsforholdDto> dtoer = new ArrayList<>();
        filter.getYrkesaktiviteter().forEach(ya -> mapTilArbeidsforholdDto(arbeidsforholdReferanser, stp, ya, mangler, saksbehandlersVurderingAvMangler, overstyringer).ifPresent(dtoer::add));
        return dtoer;
    }

    static Optional<ArbeidsforholdDto> mapTilArbeidsforholdDto(Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                               LocalDate stp,
                                                               Yrkesaktivitet ya,
                                                               List<ArbeidsforholdMangel> alleIdentifiserteMangler,
                                                               List<ArbeidsforholdValg> saksbehandlersVurderingAvMangler,
                                                               List<ArbeidsforholdOverstyring> overstyringer) {
        var ansettelsesperiode = finnRelevantAnsettelsesperiode(ya, stp, finnOverstyring(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), overstyringer));
        var mangelInntektsmelding =  finnIdentifisertInntektsmeldingMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), alleIdentifiserteMangler);
        var mangelPermisjon = finnIdentifisertMangelPermisjon(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), alleIdentifiserteMangler);
        var vurdering = finnSaksbehandlersVurderingAvMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), saksbehandlersVurderingAvMangler);

        // Vurdering og begrunnelse for gammelt aksjonspunkt (5080) der saksbehandler hadde flere valgmuligheter vi må kunne støtte readonly visning for
        var legacyVurdering = finnOverstyring(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), overstyringer)
            .map(os -> mapHandlingTilVurdering(os.getHandling()));
        var legacyBegrunnelse = finnOverstyring(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), overstyringer)
            .map(ArbeidsforholdOverstyring::getBegrunnelse);

        return ansettelsesperiode.map(datoIntervallEntitet -> new ArbeidsforholdDto(ya.getArbeidsgiver() == null ? null : ya.getArbeidsgiver().getIdentifikator(),
                ya.getArbeidsforholdRef().getReferanse(),
                finnEksternRef(ya.getArbeidsforholdRef(), arbeidsforholdReferanser).orElse(null),
                datoIntervallEntitet.getFomDato(),
                datoIntervallEntitet.getTomDato(),
                finnStillingsprosent(ya, datoIntervallEntitet).orElse(null),
                mangelInntektsmelding.orElse(null),
                vurdering.map(ArbeidsforholdValg::getVurdering).orElse(legacyVurdering.orElse(null)),
                mapPermisjonOgMangel(ya, stp, mangelPermisjon.orElse(null), overstyringer).orElse(null),
                vurdering.map(ArbeidsforholdValg::getBegrunnelse).orElse(legacyBegrunnelse.orElse(null))));
    }

    private static Optional<ArbeidsforholdOverstyring> finnOverstyring(Arbeidsgiver arbeidsgiver,
                                                                       InternArbeidsforholdRef arbeidsforholdRef,
                                                                       List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(os -> Objects.equals(os.getArbeidsgiver(), arbeidsgiver) && os.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
            .findFirst();
    }

    private static ArbeidsforholdKomplettVurderingType mapHandlingTilVurdering(ArbeidsforholdHandlingType handling) {
        return switch(handling) {
            case UDEFINERT -> ArbeidsforholdKomplettVurderingType.UDEFINERT;
            case LAGT_TIL_AV_SAKSBEHANDLER -> ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER;
            case BASERT_PÅ_INNTEKTSMELDING -> ArbeidsforholdKomplettVurderingType.OPPRETT_BASERT_PÅ_INNTEKTSMELDING;
            case BRUK_UTEN_INNTEKTSMELDING -> ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING;
            case IKKE_BRUK -> ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING;
            case BRUK -> ArbeidsforholdKomplettVurderingType.BRUK;
            case NYTT_ARBEIDSFORHOLD -> ArbeidsforholdKomplettVurderingType.NYTT_ARBEIDSFORHOLD;
            case INNTEKT_IKKE_MED_I_BG -> ArbeidsforholdKomplettVurderingType.INNTEKT_IKKE_MED_I_BG;
            case BRUK_MED_OVERSTYRT_PERIODE -> ArbeidsforholdKomplettVurderingType.BRUK_MED_OVERSTYRT_PERIODE;
            case SLÅTT_SAMMEN_MED_ANNET -> ArbeidsforholdKomplettVurderingType.SLÅTT_SAMMEN_MED_ANNET;
        };
    }

    private static Optional<PermisjonOgMangelDto> mapPermisjonOgMangel(Yrkesaktivitet ya, LocalDate stp, AksjonspunktÅrsak årsak,  List<ArbeidsforholdOverstyring> overstyringAvPermisjon) {
        return HåndterePermisjoner.hentPermisjonOgMangel(ya, stp, årsak, utledBekreftetStatus(ya.getArbeidsforholdRef(), overstyringAvPermisjon) );
    }

    private static BekreftetPermisjonStatus utledBekreftetStatus(InternArbeidsforholdRef internArbeidsforholdRef, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
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

    private static Optional<BigDecimal> finnStillingsprosent(Yrkesaktivitet ya, DatoIntervallEntitet ansettelsesperiode) {
        var stillingsprosentForAnsettelsesperiode = finnSisteStillingsprosentForPeriode(ya, ansettelsesperiode);
        if (stillingsprosentForAnsettelsesperiode.isPresent()) {
            return stillingsprosentForAnsettelsesperiode;
        }
        // Av og til overlapper ikke ansettelsesperioden med periode for stillingsprosent, henter da den siste som finnes
        return finnSisteStillingsprosentForPeriode(ya, DatoIntervallEntitet.fraOgMedTilOgMed(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE));
    }

    private static Optional<BigDecimal> finnSisteStillingsprosentForPeriode(Yrkesaktivitet ya, DatoIntervallEntitet periode) {
        return ya.getAlleAktivitetsAvtaler()
            .stream()
            .filter(aa -> !aa.erAnsettelsesPeriode() && aa.getPeriode().overlapper(periode))
            .filter(aa -> aa.getProsentsats() != null && aa.getProsentsats().getVerdi() != null)
            .max(Comparator.comparing(AktivitetsAvtale::getPeriode))
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi);
    }

    private static Optional<String> finnEksternRef(InternArbeidsforholdRef arbeidsforholdRef, Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser) {
        return arbeidsforholdReferanser.stream()
            .filter(ref -> Objects.equals(ref.getInternReferanse().getReferanse(), arbeidsforholdRef.getReferanse()))
            .findFirst()
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .map(EksternArbeidsforholdRef::getReferanse);
    }

    private static Optional<DatoIntervallEntitet> finnRelevantAnsettelsesperiode(Yrkesaktivitet ya,
                                                                                 LocalDate stp,
                                                                                 Optional<ArbeidsforholdOverstyring> arbeidsforholdOverstyring) {
        var oversyrtePerioder = arbeidsforholdOverstyring
            .filter(os -> os.getHandling().equals(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE))
            .map(ArbeidsforholdOverstyring::getArbeidsforholdOverstyrtePerioder)
            .orElse(Collections.emptyList());
        if (!oversyrtePerioder.isEmpty()) {
            return oversyrtePerioder.stream().map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode)
                .max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        }
        return ya.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .filter(aa -> aa.getPeriode().inkluderer(stp.minusDays(1)) || aa.getPeriode().getFomDato().isAfter(stp.minusDays(1)))
            .map(AktivitetsAvtale::getPeriode)
            .max(DatoIntervallEntitet::compareTo);
    }

    public static List<InntektDto> mapInntekter(InntektFilter filter, LocalDate utledetSkjæringstidspunkt) {
        return filter.getAlleInntektBeregningsgrunnlag().stream()
            .filter(inntekt -> inntekt.getArbeidsgiver() != null)
            .map(inntekt -> mapInntekt(inntekt, utledetSkjæringstidspunkt))
            .toList();
    }

    private static InntektDto mapInntekt(Inntekt inntekt, LocalDate utledetSkjæringstidspunkt) {
        var sisteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(utledetSkjæringstidspunkt.minusMonths(12).withDayOfMonth(1), utledetSkjæringstidspunkt);
        var poster = inntekt.getAlleInntektsposter().stream()
            .filter(post -> post.getPeriode().overlapper(sisteÅr))
            .map(ArbeidOgInntektsmeldingMapper::mapInntektspost)
            .toList();
        return new InntektDto(inntekt.getArbeidsgiver().getIdentifikator(), poster);
    }

    private static InntektspostDto mapInntektspost(Inntektspost post) {
        return new InntektspostDto(fraBeløp(post.getBeløp()),
            post.getPeriode().getFomDato(),
            post.getPeriode().getTomDato(),
            post.getInntektspostType());
    }

    public static List<ArbeidsforholdDto> mapManueltOpprettedeArbeidsforhold(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                             Collection<ArbeidsforholdReferanse> referanser,
                                                                             List<ArbeidsforholdMangel> mangler ) {
        // Trenger kun arbeidsforhold opprettet av handlinger som  kan oppstå i 5085
        return arbeidsforholdOverstyringer.stream()
            .filter(os -> MANUELT_ARBEIDSFOROHOLD_HANDLING.contains(os.getHandling()))
            .map(overstyring -> mapManueltArbeidsforhold(overstyring, referanser, mangler))
            .toList();
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

    private static List<NaturalYtelseDto> mapNaturalYtelser(List<NaturalYtelse> naturalYtelser) {
        return Optional.ofNullable(naturalYtelser).orElseGet(List::of).stream()
            .map(ArbeidOgInntektsmeldingMapper::mapNaturalYtelse)
            .toList();
    }

    private static NaturalYtelseDto mapNaturalYtelse(NaturalYtelse naturalYtelse) {
        var periode = new NaturalYtelseDto.Periode(naturalYtelse.getPeriode().getFomDato(), naturalYtelse.getPeriode().getTomDato());
        var beløp = new NaturalYtelseDto.Beløp(naturalYtelse.getBeloepPerMnd().getVerdi());
        return new NaturalYtelseDto(periode, beløp, naturalYtelse.getType(), naturalYtelse.getIndexKey());
    }

    private static List<RefusjonDto> mapRefusjonendringer(List<Refusjon> refusjon) {
        return Optional.ofNullable(refusjon).orElseGet(List::of).stream()
            .map(ArbeidOgInntektsmeldingMapper::mapRefusjonendring)
            .toList();
    }

    private static RefusjonDto mapRefusjonendring(Refusjon refusjon) {
        var beløp = new RefusjonDto.Beløp(refusjon.getRefusjonsbeløp().getVerdi());
        return new RefusjonDto(refusjon.getFom(), beløp, beløp, refusjon.getIndexKey());
    }
}
