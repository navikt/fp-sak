package no.nav.foreldrepenger.mottak.kompletthettjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

@ExtendWith(MockitoExtension.class)
class KompletthetssjekkerInntektsmeldingTest {
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @BeforeEach
    public void setUp() {
        kompletthetssjekkerInntektsmelding = new KompletthetssjekkerInntektsmelding(inntektsmeldingRegisterTjeneste, svangerskapspengerRepository);
    }

    @Test
    void en_inntektsmelding_mangler_for_ett_arbeidsforhold_fp() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverSomManglerIm = Arbeidsgiver.virksomhet("123456789");
        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> arbeidsgivereSomManglerIm = Map.of(arbeidsgiverSomManglerIm, Collections.emptySet());

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(arbeidsgivereSomManglerIm);
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(1);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiverSomManglerIm.getOrgnr());
    }

    @Test
    void en_inntektsmelding_mangler_for_ett_arbeidsforhold_Svp() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverSomManglerIm = Arbeidsgiver.virksomhet("123456789");
        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> arbeidsgivereSomManglerIm = Map.of(arbeidsgiverSomManglerIm, Collections.emptySet());
        var tilrettelegging = lagTilrettelegging(List.of(lagFraDatoTilr(BigDecimal.valueOf(100))), arbeidsgiverSomManglerIm, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var svpGrunnlag = lagSvpGrunnlag(List.of(tilrettelegging));

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(arbeidsgivereSomManglerIm);
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(svpGrunnlag));

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(1);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiverSomManglerIm.getOrgnr());
    }

    @Test
    void to_inntektsmelding_mangler_selv_om_ett_har_permisjon_fordi_det_er_søkt_på_svp() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverSomManglerIm = Arbeidsgiver.virksomhet("123456789");
        var arbeidsgiverMedPermisjon = Arbeidsgiver.virksomhet("987654321");

        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> arbeidsgivereSomManglerIm = Map.of(arbeidsgiverSomManglerIm, Collections.emptySet());

        var tilrFom = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var tilrettelegging = lagTilrettelegging(tilrFom, arbeidsgiverSomManglerIm, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var tilrettelggingMedPermisjon = lagTilrettelegging(tilrFom, arbeidsgiverMedPermisjon, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var svpGrunnlag = lagSvpGrunnlag(List.of(tilrettelegging, tilrettelggingMedPermisjon));

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(arbeidsgivereSomManglerIm);
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(svpGrunnlag));

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(2);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiverSomManglerIm.getOrgnr());
        assertThat(manglerInntektsmelding.get(1).getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.get(1).getArbeidsgiver()).isEqualTo(arbeidsgiverMedPermisjon.getOrgnr());
    }

    @Test
    void to_arbeidsforhold_under_samme_arbeidsgiver_mangler_inntektsmelding_skal_bare_bli_en() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("987654321");

        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> arbeidsgivereSomManglerIm = Map.of(arbeidsgiver, Set.of(EksternArbeidsforholdRef.ref("1"), EksternArbeidsforholdRef.ref("2")));

        var tilrFom = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var tilrettelegging = lagTilrettelegging(tilrFom, arbeidsgiver, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var svpGrunnlag = lagSvpGrunnlag(List.of(tilrettelegging));

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(arbeidsgivereSomManglerIm);
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(svpGrunnlag));

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(1);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiver.getOrgnr());
    }

    @Test
    void to_arbeidsforhold_under_samme_arbeidsgiver_begge_i_permisjon() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverMedPermisjon = Arbeidsgiver.virksomhet("987654321");

        var tilrFom = List.of(lagFraDatoTilr(BigDecimal.valueOf(100)));
        var tilrettelegging = lagTilrettelegging(tilrFom, arbeidsgiverMedPermisjon, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var svpGrunnlag = lagSvpGrunnlag(List.of(tilrettelegging));

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(Map.of());
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(svpGrunnlag));

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(1);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiverMedPermisjon.getOrgnr());
    }

    @Test
    void en_inntektsmelding_mangler_fordi_ett_arbeidsforhold_har_relevant_permisjon_og_ikke_søkt_svp() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var arbeidsgiverSomManglerIm = Arbeidsgiver.virksomhet("123456789");

        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> arbeidsgivereSomManglerIm = Map.of(arbeidsgiverSomManglerIm, Collections.emptySet());

        var tilrFom = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var tilrettelegging = lagTilrettelegging(tilrFom, arbeidsgiverSomManglerIm, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var svpGrunnlag = lagSvpGrunnlag(List.of(tilrettelegging));

        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraAAregVurderPermisjon(ref, skjæringstidspunkt)).thenReturn(arbeidsgivereSomManglerIm);
        when(svangerskapspengerRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(svpGrunnlag));

        var manglerInntektsmelding = kompletthetssjekkerInntektsmelding.finnManglendeInntektsmeldingerUtenRelevantPermisjon(ref, skjæringstidspunkt);

        // Act
        assertThat(manglerInntektsmelding).hasSize(1);
        assertThat(manglerInntektsmelding.getFirst().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(manglerInntektsmelding.getFirst().getArbeidsgiver()).isEqualTo(arbeidsgiverSomManglerIm.getOrgnr());
    }

    SvpGrunnlagEntitet lagSvpGrunnlag(List<SvpTilretteleggingEntitet> tilrettelegginger) {
        return new SvpGrunnlagEntitet.Builder().medBehandlingId(123456L)
            .medOverstyrteTilrettelegginger(tilrettelegginger)
            .build();
    }

    private TilretteleggingFOM lagFraDatoTilr(BigDecimal stillingsprosent) {
        return new TilretteleggingFOM.Builder()
            .medFomDato(SKJÆRINGSTIDSPUNKT)
            .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
            .medStillingsprosent(stillingsprosent)
            .medTidligstMottattDato(SKJÆRINGSTIDSPUNKT)
            .build();
    }
    private SvpTilretteleggingEntitet lagTilrettelegging(List<TilretteleggingFOM> fraDatoer, Arbeidsgiver arbeidsgiver, ArbeidType arbeidType){
        var builder = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(SKJÆRINGSTIDSPUNKT)
            .medTilretteleggingFraDatoer(fraDatoer)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver)
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false);
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
        }
        return builder.build();
    }
}
