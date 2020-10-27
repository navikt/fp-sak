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
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
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
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderPermisjonTjenesteTest extends EntityManagerAwareTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private IAYRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(){
        repositoryProvider = new IAYRepositoryProvider(getEntityManager());
    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_ingen_bekreftet_permisjon_eksisterer() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        Permisjon permisjon_2 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(1), SKJÆRINGSTIDSPUNKT);
        Permisjon permisjon_3 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.plusDays(1), TIDENES_ENDE);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1, permisjon_2, permisjon_3));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

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
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_det_ikke_finnes_yrkesaktivteter_med_permisjon() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusYears(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        YrkesaktivitetBuilder ya = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of());

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(ya));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_ikke_legge_til_arbeidsforhold_når_bekreftet_permisjon_inneholder_UGYLDIGE_PERIODER_og_man_utledere_flere_overlappende_permisjoner() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        Permisjon permisjon_2 = byggPermisjon(yaBuilder, fom.minusDays(1), tom.plusDays(1));
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1, permisjon_2));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_bekreftet_permisjon_inneholder_UGYLDIGE_PERIODER_og_man_utledere_kun_en_permisjon() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));
        Permisjon permisjon_2 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(1), SKJÆRINGSTIDSPUNKT);
        Permisjon permisjon_3 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.plusDays(1), TIDENES_ENDE);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1, permisjon_2, permisjon_3));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

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
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(0);

    }

    @Test
    public void skal_legge_til_arbeidsforhold_når_bekreftet_permisjon_ikke_inneholder_samme_fom_som_utledet_permisjon() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom.minusDays(1), tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

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
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa = lagAktivitetsAvtaleBuilder(yaBuilder, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom.plusDays(1), BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

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
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        Arbeidsgiver arbeidsgiver_1 = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref_1 = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yaBuilder_1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa_1 = lagAktivitetsAvtaleBuilder(yaBuilder_1, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder_1, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder_1, aa_1,
            arbeidsgiver_1, ref_1, List.of(permisjon_1));

        Arbeidsgiver arbeidsgiver_2 = Arbeidsgiver.virksomhet("2");
        InternArbeidsforholdRef ref_2 = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yaBuilder_2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa_2 = lagAktivitetsAvtaleBuilder(yaBuilder_2, fom, tom);
        Permisjon permisjon_2 = byggPermisjon(yaBuilder_2, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_2 = lagYrkesaktivitetBuilder(yaBuilder_2, aa_2,
            arbeidsgiver_2, ref_2, List.of(permisjon_2));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1, yrkesaktivitet_2));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver_2, ref_2)
            .medBekreftetPermisjon(new BekreftetPermisjon(fom, tom.plusDays(1), BekreftetPermisjonStatus.BRUK_PERMISJON));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

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
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusWeeks(1);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(1);

        Arbeidsgiver arbeidsgiver_1 = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref_1 = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yaBuilder_1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa_1 = lagAktivitetsAvtaleBuilder(yaBuilder_1, fom, tom);
        Permisjon permisjon_1 = byggPermisjon(yaBuilder_1, fom, tom);
        YrkesaktivitetBuilder yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder_1, aa_1,
            arbeidsgiver_1, ref_1, List.of(permisjon_1));

        Arbeidsgiver arbeidsgiver_2 = Arbeidsgiver.virksomhet("2");
        InternArbeidsforholdRef ref_2 = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yaBuilder_2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aa_2 = lagAktivitetsAvtaleBuilder(yaBuilder_2, fom, tom);
        Permisjon permisjon_2 = byggPermisjon(yaBuilder_2, fom, tom);
        Permisjon permisjon_3 = byggPermisjon(yaBuilder_2, fom.minusDays(1), tom.plusDays(1));
        YrkesaktivitetBuilder yrkesaktivitet_2 = lagYrkesaktivitetBuilder(yaBuilder_2, aa_2,
            arbeidsgiver_2, ref_2, List.of(permisjon_2, permisjon_3));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1, yrkesaktivitet_2));

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver_2, ref_2)
            .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(behandlingReferanse, result, grunnlag);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(arbeidsgiver_1);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getRef()).isEqualTo(ref_1);
        assertThat(result.get(arbeidsgiver_1).iterator().next().getÅrsaker()).containsExactly(AksjonspunktÅrsak.PERMISJON);

    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder lagAktørArbeidBuilder(Behandling behandling, List<YrkesaktivitetBuilder> yrkesaktiviteter) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
            .oppdatere(Optional.empty()).medAktørId(behandling.getAktørId());
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        return aktørArbeidBuilder;
    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                                    Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder);
        InntektArbeidYtelseGrunnlagBuilder inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt()
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
