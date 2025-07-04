package no.nav.foreldrepenger.domene.arbeidInntektsmelding;


import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(JpaExtension.class)
class HåndterePermisjonerTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private IAYRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
    }

    @Test
    void skal_ikke_finne_mangel_når_permisjonen_har_sluttdato() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1));
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.minusDays(1));

        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        var arbForholdMedPermUtenSluttdato = HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, SKJÆRINGSTIDSPUNKT, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).isEmpty();
    }

    @Test
    void skal_finne_mangel_når_permisjonen_ikke_har_sluttdato() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1));
        var permisjon_1 = byggPermisjon(yaBuilder, SKJÆRINGSTIDSPUNKT.minusDays(2), TIDENES_ENDE);

        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjon_1));

        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling,
            List.of(yrkesaktivitet_1));

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        // Act
        var arbForholdMedPermUtenSluttdato = HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, SKJÆRINGSTIDSPUNKT, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
    }

    @Test
    void skal_finne_mangel_når_permisjon_uten_sluttdato_ikke_er_bekreftet() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1));
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
        var arbForholdMedPermUtenSluttdato = HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, SKJÆRINGSTIDSPUNKT, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).ref()).isEqualTo(ref);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).årsak()).isEqualTo(AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO);
    }

    @Test
    void skal_ikke_finne_mangel_når_permisjon_uten_sluttdato_er_bekreftet() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingReferanse = lagReferanse(behandling);

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, SKJÆRINGSTIDSPUNKT.minusYears(1));
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
        var arbForholdMedPermUtenSluttdato = HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(behandlingReferanse, SKJÆRINGSTIDSPUNKT, grunnlag);

        assertThat(arbForholdMedPermUtenSluttdato).hasSize(1);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).ref()).isEqualTo(ref);
        assertThat(arbForholdMedPermUtenSluttdato.get(0).årsak()).isEqualTo(AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO);
    }
    @Test
    void har_relevant_permisjon_for_tilretteleggingFom() {

        // Arrange
        var tilretteleggingFom = LocalDate.now();

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, tilretteleggingFom.minusYears(1));

        var permisjonInnenfor = byggPermisjon(yaBuilder, tilretteleggingFom, tilretteleggingFom.plusDays(1));
        var permisjonUtenfor = byggPermisjon(yaBuilder, tilretteleggingFom.minusYears(1), tilretteleggingFom.minusMonths(1));
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjonInnenfor, permisjonUtenfor)).build();

        // Assert
        var erPermisjonInnenfor = HåndterePermisjoner.finnRelevantePermisjonSomOverlapperTilretteleggingFom(yrkesaktivitet_1, tilretteleggingFom);

        assertThat(erPermisjonInnenfor).hasSize(1);
    }
    @Test
    void har_ingen_relevant_permisjoner_for_tilretteleggingFom() {
        // Arrange
        var tilretteleggingFom = LocalDate.now();

        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var ref = InternArbeidsforholdRef.nyRef();

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aa = lagAktivitetsAvtaleBuilder(yaBuilder, tilretteleggingFom.minusYears(1));

        var permisjonUtenfor1 = byggPermisjon(yaBuilder, tilretteleggingFom.minusMonths(5), tilretteleggingFom.minusMonths(1));
        var permisjonUtenfor2 = byggPermisjon(yaBuilder, tilretteleggingFom.minusYears(1), tilretteleggingFom.minusMonths(1));
        var yrkesaktivitet_1 = lagYrkesaktivitetBuilder(yaBuilder, aa,
            arbeidsgiver, ref, List.of(permisjonUtenfor1, permisjonUtenfor2)).build();

        // Assert
        var erPermisjonInnenfor = HåndterePermisjoner.finnRelevantePermisjonSomOverlapperTilretteleggingFom(yrkesaktivitet_1, tilretteleggingFom);

        assertThat(erPermisjonInnenfor).isEmpty();
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
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
            .build();
    }
    private AktivitetsAvtaleBuilder lagAktivitetsAvtaleBuilder(YrkesaktivitetBuilder yrkesaktivitetBuilder, LocalDate fom) {
        return yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, TIDENES_ENDE));
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }
}
