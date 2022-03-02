package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderPermisjonTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private IAYRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_ingen_bekreftet_permisjon_eksisterer() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        var permisjon_2 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(1), SKJÆRINGSTIDSPUNKT);
        var permisjon_3 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.plusDays(1), TIDENES_ENDE);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1, permisjon_2, permisjon_3));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver);
        assertThat(result.get(arbeidsgiver).iterator().next().getRef()).isEqualTo(ref);
        assertThat(result.get(arbeidsgiver).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_det_ikke_finnes_yrkesaktivteter_før_stp() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var permisjon = byggPermisjon(yaBuilder, fom, tom);
        var ya = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(ya));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_det_ikke_finnes_yrkesaktivteter_med_permisjon() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var ya = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of());

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(ya));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_bekreftet_permisjon_inneholder_UGYLDIGE_PERIODER_og_man_utledere_flere_overlappende_permisjoner() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        var permisjon_2 = byggPermisjon(yaBuilder, fom.minusDays(1), tom.plusDays(1));
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1, permisjon_2));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_bekreftet_permisjon_inneholder_UGYLDIGE_PERIODER_og_man_utledere_kun_en_permisjon() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        var permisjon_2 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(1), SKJÆRINGSTIDSPUNKT);
        var permisjon_3 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.plusDays(1), TIDENES_ENDE);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1, permisjon_2, permisjon_3));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver);
        assertThat(result.get(arbeidsgiver).iterator().next().getRef()).isEqualTo(ref);
        assertThat(result.get(arbeidsgiver).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_bekreftet_permisjon_inneholder_samme_fom_og_tom_som_utledet_permisjon() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_bekreftet_permisjon_ikke_inneholder_samme_fom_som_utledet_permisjon() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom.minusDays(1), tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver);
        assertThat(result.get(arbeidsgiver).iterator().next().getRef()).isEqualTo(ref);
        assertThat(result.get(arbeidsgiver).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_bekreftet_permisjon_ikke_inneholder_samme_tom_som_utledet_permisjon() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();
        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom.plusDays(1), BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver);
        assertThat(result.get(arbeidsgiver).iterator().next().getRef()).isEqualTo(ref);
        assertThat(result.get(arbeidsgiver).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_legge_til_to_arbeidsforhold_når_begge_har_relevant_permisjon_og_hvor_den_ene_har_bekreftet_permisjon_med_ulik_tom() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var arbeidsgiver_1 = Arbeidsgiver.virksomhet("1");
        var ref_1 = InternArbeidsforholdRef.nyRef();
        var yaBuilder_1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa_1 = lagAktivitetsAvtaleBuilder(yaBuilder_1, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder_1, fom, tom);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder_1, aa_1,
                arbeidsgiver_1, ref_1, List.of(permisjon_1));

        var arbeidsgiver_2 = Arbeidsgiver.virksomhet("2");
        var ref_2 = InternArbeidsforholdRef.nyRef();
        var yaBuilder_2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa_2 = lagAktivitetsAvtaleBuilder(yaBuilder_2, fom, tom);
        var permisjon_2 = byggPermisjon(yaBuilder_2, fom, tom);
        var yrkesaktivitet_2 = lagYrkesaktivitetBuilder(yaBuilder_2, aa_2,
                arbeidsgiver_2, ref_2, List.of(permisjon_2));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1, yrkesaktivitet_2));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver_2, ref_2)
                .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom.plusDays(1), BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsKey(arbeidsgiver_1);
        assertThat(result).containsKey(arbeidsgiver_2);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getRef()).isEqualTo(ref_1);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);
        assertThat(result.get(arbeidsgiver_2).iterator().next().getRef()).isEqualTo(ref_2);
        assertThat(result.get(arbeidsgiver_2).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_legge_til_en_av_to_arbeidsforhold_hvor_det_ene_fortsatt_har_ugyldige_perioder() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        var tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        var arbeidsgiver_1 = Arbeidsgiver.virksomhet("1");
        var ref_1 = InternArbeidsforholdRef.nyRef();
        var yaBuilder_1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa_1 = lagAktivitetsAvtaleBuilder(yaBuilder_1, fom, tom);
        var permisjon_1 = byggPermisjon(yaBuilder_1, fom, tom);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder_1, aa_1,
                arbeidsgiver_1, ref_1, List.of(permisjon_1));

        var arbeidsgiver_2 = Arbeidsgiver.virksomhet("2");
        var ref_2 = InternArbeidsforholdRef.nyRef();
        var yaBuilder_2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa_2 = lagAktivitetsAvtaleBuilder(yaBuilder_2, fom, tom);
        var permisjon_2 = byggPermisjon(yaBuilder_2, fom, tom);
        var permisjon_3 = byggPermisjon(yaBuilder_2, fom.minusDays(1), tom.plusDays(1));
        var yrkesaktivitet_2 = lagYrkesaktivitetBuilder(yaBuilder_2, aa_2,
                arbeidsgiver_2, ref_2, List.of(permisjon_2, permisjon_3));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1, yrkesaktivitet_2));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver_2, ref_2)
                .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver_1);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getRef()).isEqualTo(ref_1);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    @Test
    public void skal_returnere_ingen_arbeidsforhold_når_permisjonen_har_sluttdato() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));

        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = VurderPermisjonTjeneste.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).isEmpty();
    }

    @Test
    public void skal_finne_arbeidsforhold_når_permisjon_uten_sluttdato_ikke_er_bekreftet() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_ok = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        var permisjon_uten_sluttdato = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), TIDENES_ENDE);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_ok, permisjon_uten_sluttdato));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        informasjonBuilder.leggTil(overstyringBuilder);

        // Act
        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Assert
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = VurderPermisjonTjeneste.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).ref()).isEqualTo(ref);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).årsak()).isEqualTo(AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO);
    }


    @Test
    public void skal_ikke_finne_arbeidsforhold_når_permisjon_uten_sluttdato_er_bekreftet() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_ok = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        var permisjon_uten_sluttdato = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), TIDENES_ENDE);
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_ok, permisjon_uten_sluttdato));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        informasjonBuilder.leggTil(overstyringBuilder);

        // Act
        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Assert
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = VurderPermisjonTjeneste.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).ref()).isEqualTo(ref);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).årsak()).isEqualTo(AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO);
    }

    @Test
    public void skal_returnere_arbeidsforhold_når_permisjonen_har_sluttdato() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), TIDENES_ENDE);

        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
                arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
                List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = VurderPermisjonTjeneste.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder lagAktørArbeidBuilder(Behandling behandling,
            List<YrkesaktivitetBuilder> yrkesaktiviteter) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty()).medAktørId(behandling.getAktørId());
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        return aktørArbeidBuilder;
    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        var inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørArbeid(aktørArbeidBuilder);
        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt()
                .medData(inntektArbeidYtelseAggregatBuilder);
        arbeidsforholdInformasjonOpt.ifPresent(inntektArbeidYtelseGrunnlagBuilder::medInformasjon);
        return inntektArbeidYtelseGrunnlagBuilder.build();
    }

    private YrkesaktivitetBuilder lagYrkesaktivitetBuilder(YrkesaktivitetBuilder yrkesaktivitetBuilder, AktivitetsAvtaleBuilder aktivitetsAvtale,
            Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<Permisjon> permisjoner) {
        yrkesaktivitetBuilder
                .medArbeidsforholdId(ref)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsAvtale);
        permisjoner.forEach(yrkesaktivitetBuilder::leggTilPermisjon);
        return yrkesaktivitetBuilder;
    }

    private Permisjon byggPermisjon(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getPermisjonBuilder()
                .medProsentsats(BigDecimal.valueOf(100))
                .medPeriode(fom, tom)
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                .build();
    }

    private AktivitetsAvtaleBuilder lagAktivitetsAvtaleBuilder(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom, LocalDate tom) {
        return yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling,
                Skjæringstidspunkt.builder()
                        .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                        .build());
    }

}
