package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.InntektsmeldingStatusMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingRegisterTjenesteFpTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;
    @Mock
    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;

    private static BehandlingReferanse behandlingReferanse;
    private static final AktørId aktørId = AktørId.dummy();
    private final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
    private final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
    private final InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder ytelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(aktørId);
    private final InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

    @BeforeEach
    void setUp() {
        var foreldrepengerFilter = new InntektsmeldingFilterYtelseImpl();
        inntektsmeldingRegisterTjeneste = new InntektsmeldingRegisterTjeneste(inntektArbeidYtelseTjeneste, inntektsmeldingTjeneste, new UnitTestLookupInstanceImpl<>(foreldrepengerFilter));
        behandlingReferanse = new BehandlingReferanse(new Saksnummer("123"), 321L, FagsakYtelseType.FORELDREPENGER, 123L,
            UUID.randomUUID(), BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, null, aktørId, RelasjonsRolleType.MORA);
    }

    @Test
    void utledManglendeInntektsmeldingerFraGrunnlag_enkel_case() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());

        lagArbeid(List.of(yrkesaktivitet1));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, Collections.emptyList(), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            Collections.emptyList());

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        var listeAvArbeidsforholdsider = manglendeInntektsmeldinger.values().stream().toList();

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));
        assertThat(listeAvArbeidsforholdsider.getFirst()).isEqualTo(Set.of(ref));

        assertThat(statusPerArbeidsgiver).hasSize(1);
        assertThat(statusPerArbeidsgiver.stream().toList().getFirst().inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
        assertThat(statusPerArbeidsgiver.stream().toList().getFirst().arbeidsgiver()).isEqualTo(arbeidsgiver);
    }
    @Test
    void krever_ikkeinntektsmelding_for_en_av_arbeidsforholdene_fordi_det_er_100_prosent_permisjon() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var permisjon1 = byggPermisjon(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusMonths(2), PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET, BigDecimal.valueOf(100));
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(), arbeidsgiver, ref2, List.of(permisjon1));

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, Collections.emptyList(), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            Collections.emptyList());

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        var listeAvArbeidsforholdsider = manglendeInntektsmeldinger.values().stream().toList();

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));
        assertThat(listeAvArbeidsforholdsider.getFirst()).isEqualTo(Set.of(ref));

        assertThat(statusPerArbeidsgiver).hasSize(1);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
    }

    @Test
    void ett_arbeidsforhold_har_100_prosent_permisjon_vi_ikke_bryr_oss_om_og_inntektsmelding_kreves() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var permisjon1 = byggPermisjon(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusMonths(2), PermisjonsbeskrivelseType.UTDANNINGSPERMISJON, BigDecimal.valueOf(100));
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref2, List.of(permisjon1));

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, Collections.emptyList(), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            Collections.emptyList());

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        List<InternArbeidsforholdRef> internrefs = manglendeInntektsmeldinger.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));
        assertThat(internrefs).containsAll(List.of(ref, ref2));

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
    }

    @Test
    void ett_arbeidsforhold_har_50_prosent_permisjon_og_inntektsmelding_kreves() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var permisjon1 = byggPermisjon(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusMonths(2), PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET, BigDecimal.valueOf(50));
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref2, List.of(permisjon1));

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, Collections.emptyList(), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            Collections.emptyList());

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        List<InternArbeidsforholdRef> internrefs = manglendeInntektsmeldinger.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));
        assertThat(internrefs).containsAll(List.of(ref2, ref));

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
    }

    @Test
    void har_mottatt_inntektsmelding_uten_arbeidsforholdsid_for_arbeidsforhold() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref2, List.of());

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var inntektsmeldingUtenArbId = lagInntektsmelding( arbeidsgiver, BigDecimal.valueOf(55000), null, null);
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, List.of(inntektsmeldingUtenArbId), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            List.of(inntektsmeldingUtenArbId));

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).isEmpty();

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
    }

    @Test
    void har_mottatt_inntektsmelding_med_arbeidsforholdsid_for_ett_av_arbeidsforholdene_en_im_mangler() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, ref2, List.of());

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var inntektsmeldingMedArbId = lagInntektsmelding(arbeidsgiver, BigDecimal.valueOf(55000), EksternArbeidsforholdRef.ref("1"), ref);
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, List.of(inntektsmeldingMedArbId), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            List.of(inntektsmeldingMedArbId));

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        List<InternArbeidsforholdRef> internrefs = manglendeInntektsmeldinger.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));
        assertThat(internrefs).containsAll(List.of(ref2));

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.ref().equals(ref2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
    }

    @Test
    void ett_arbeidsforhold_har_ikke_inntekt_og_det_kreves_ikke_inntektsmelding() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");

        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var permisjon1 = byggPermisjon(SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusMonths(2), PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET, BigDecimal.valueOf(50));
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, null, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver2, null, List.of(permisjon1));

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, Collections.emptyList(), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            Collections.emptyList());

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));

        assertThat(statusPerArbeidsgiver).hasSize(1);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.arbeidsgiver().equals(arbeidsgiver))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
    }

    @Test
    void ett_arbeidsforhold_har_mottatt_inntektsmelding() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");

        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, null, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver2, null, List.of());

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        lagInntekt(arbeidsgiver2, SKJÆRINGSTIDSPUNKT.minusMonths(12), 7 );
        var inntektsmeldingMottatt = lagInntektsmelding(arbeidsgiver2, BigDecimal.valueOf(55000), null, null);
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, List.of(inntektsmeldingMottatt), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            List.of(inntektsmeldingMottatt));

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).hasSize(1);
        assertThat(manglendeInntektsmeldinger.keySet().stream().findFirst()).isEqualTo(Optional.of(arbeidsgiver));

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.arbeidsgiver().equals(arbeidsgiver))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.arbeidsgiver().equals(arbeidsgiver2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
    }

    @Test
    void ingen_manglende_inntektsmeldinger() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("987654321");

        var aktivitetsAvtaleBuilder = lagAktivitetsAvtaleBuilder(SKJÆRINGSTIDSPUNKT.minusYears(1), null);
        var yrkesaktivitet1 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver, null, List.of());
        var yrkesaktivitet2 = lagYrkesaktivitetBuilder(List.of(aktivitetsAvtaleBuilder), arbeidsgiver2, null, List.of());

        lagArbeid(List.of(yrkesaktivitet1, yrkesaktivitet2));
        lagInntekt(arbeidsgiver, SKJÆRINGSTIDSPUNKT.minusMonths(12), 12 );
        lagInntekt(arbeidsgiver2, SKJÆRINGSTIDSPUNKT.minusMonths(12), 7 );
        var inntektsmeldingArbeidsgiver1 = lagInntektsmelding(arbeidsgiver, BigDecimal.valueOf(55000), null, null);
        var inntektsmeldingArbeidsgiver2 = lagInntektsmelding(arbeidsgiver2, BigDecimal.valueOf(55000), null, null);
        var grunnlag = byggIAY(inntektArbeidYtelseAggregatBuilder, List.of(inntektsmeldingArbeidsgiver1, inntektsmeldingArbeidsgiver2), arbeidBuilder, inntektBuilder, ytelseBuilder);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())).thenReturn(Optional.of(grunnlag));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, skjæringstidspunkt.getUtledetSkjæringstidspunkt())).thenReturn(
            List.of(inntektsmeldingArbeidsgiver1, inntektsmeldingArbeidsgiver2));

        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt);
        var statusPerArbeidsgiver = finnStatusForInntektsmeldingArbeidsforhold(behandlingReferanse, skjæringstidspunkt);

        assertThat(manglendeInntektsmeldinger).isEmpty();

        assertThat(statusPerArbeidsgiver).hasSize(2);
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.arbeidsgiver().equals(arbeidsgiver))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
        assertThat(statusPerArbeidsgiver.stream().filter(status -> status.arbeidsgiver().equals(arbeidsgiver2))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)).containsAll(
            Collections.singleton(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT));
    }

    public List<ArbeidsforholdInntektsmeldingStatus> finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse referanse, Skjæringstidspunkt skjæringstidspunkt) {
        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(referanse, skjæringstidspunkt);
        var allePåkrevdeInntektsmeldinger = inntektsmeldingRegisterTjeneste.hentAllePåkrevdeInntektsmeldinger(referanse, skjæringstidspunkt);

        var saksbehandlersValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(referanse.behandlingId());
        return InntektsmeldingStatusMapper.mapInntektsmeldingStatus(allePåkrevdeInntektsmeldinger, manglendeInntektsmeldinger, saksbehandlersValg);
    }

    private Inntektsmelding lagInntektsmelding(Arbeidsgiver arbeidsgiver, BigDecimal beløp, EksternArbeidsforholdRef arbeidsforholdId, InternArbeidsforholdRef arbeidsforholdIdIntern ) {
        return InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(SKJÆRINGSTIDSPUNKT)
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(beløp)
            .medNærRelasjon(false)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(arbeidsforholdIdIntern)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .build();
    }


    private YrkesaktivitetBuilder lagYrkesaktivitetBuilder(List<AktivitetsAvtaleBuilder> aktivitetsAvtaler,
                                                    Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<Permisjon> permisjoner) {
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        aktivitetsAvtaler.forEach(yaBuilder::leggTilAktivitetsAvtale);
        permisjoner.forEach(yaBuilder::leggTilPermisjon);
        return yaBuilder;
    }

    private AktivitetsAvtaleBuilder lagAktivitetsAvtaleBuilder(LocalDate fom, LocalDate tom) {
        var builder = AktivitetsAvtaleBuilder.ny();
            if (tom == null) {
                builder.medPeriode(DatoIntervallEntitet.fraOgMed(fom));
            } else {
                builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
            }
            return builder;
    }

    private void lagInntekt(Arbeidsgiver ag, LocalDate fom, int måneder) {
        var intBuilder = InntektBuilder.oppdatere(Optional.empty());
        intBuilder.medArbeidsgiver(ag).medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING);
        for (var i = 0; i<måneder; i++) {
            var start = fom.plusMonths(i);
            var postBuilder = intBuilder.getInntektspostBuilder();
            postBuilder.medPeriode(start.withDayOfMonth(1), start.with(TemporalAdjusters.lastDayOfMonth()))
                .medBeløp(BigDecimal.valueOf(100))
                .medInntektspostType(InntektspostType.LØNN);
            intBuilder.leggTilInntektspost(postBuilder);
        }
        inntektBuilder.leggTilInntekt(intBuilder);
    }

    private void lagArbeid(List<YrkesaktivitetBuilder> yrkesaktivitetBuilderList) {
        yrkesaktivitetBuilderList.forEach(arbeidBuilder::leggTilYrkesaktivitet);
    }

    private InntektArbeidYtelseGrunnlag byggIAY(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, List<Inntektsmelding> inntektsmeldinger, InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder, InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder,
                                                InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder ytelseBuilder) {
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(arbeidBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(inntektBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(ytelseBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medData(inntektArbeidYtelseAggregatBuilder).medInntektsmeldinger(inntektsmeldinger).build();
    }

    private Permisjon byggPermisjon(LocalDate fom, LocalDate tom, PermisjonsbeskrivelseType permisjonType, BigDecimal prosent) {
        return YrkesaktivitetBuilder.nyPermisjonBuilder()
            .medProsentsats(prosent)
            .medPeriode(fom, tom)
            .medPermisjonsbeskrivelseType(permisjonType)
            .build();
    }
}
