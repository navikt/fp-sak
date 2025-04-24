package no.nav.foreldrepenger.domene.prosess;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriodeÅrsak;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(JpaExtension.class)
class BeregningsgrunnlagRepositoryTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    private static final String ORGNR = "55";
    private static final BeregningsgrunnlagTilstand STEG_OPPRETTET = BeregningsgrunnlagTilstand.OPPRETTET;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider behandlingRepositoryProvider;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_kopiere_med_register_aktiviteter_fra_original_behandling() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningAktivitetAggregat = lagAktivitetAggregat();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.FASTSATT);
        var revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                                .medManueltOpprettet(false)
                                .medOriginalBehandlingId(behandling.getId()))
                .build();
        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        // Act
        beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId(),
                BeregningsgrunnlagTilstand.FASTSATT);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(revurdering.getId(),
                        BeregningsgrunnlagTilstand.FASTSATT);
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertBeregningAktivitetAggregat(entitet.getRegisterAktiviteter(), beregningAktivitetAggregat);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT);
        });
    }

    @Test
    void lagreRegisterBeregningAktiviteterOgBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningAktivitetAggregat = lagAktivitetAggregat();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertBeregningAktivitetAggregat(entitet.getRegisterAktiviteter(), beregningAktivitetAggregat);
            assertThat(entitet.getBeregningsgrunnlag()).hasValueSatisfying(bg -> assertThat(bg).isSameAs(beregningsgrunnlag));
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
        });
    }

    private void assertBeregningAktivitetAggregat(BeregningAktivitetAggregatEntitet actualAggregat,
            BeregningAktivitetAggregatEntitet expectedAggregat) {
        assertThat(actualAggregat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(actualAggregat.getBeregningAktiviteter()).hasSize(expectedAggregat.getBeregningAktiviteter().size());
        for (var i = 0; i < expectedAggregat.getBeregningAktiviteter().size(); i++) {
            var expected = expectedAggregat.getBeregningAktiviteter().get(i);
            assertThat(actualAggregat.getBeregningAktiviteter())
                    .as("expected.getBeregningAktiviteter().get(" + i + ")")
                    .anySatisfy(actual -> assertBeregningAktivitet(actual, expected));
        }
    }

    private void assertBeregningAktivitet(BeregningAktivitetEntitet actual, BeregningAktivitetEntitet expected) {
        assertThat(actual.getArbeidsgiver()).as("arbeidsgiver").isEqualTo(expected.getArbeidsgiver());
        assertThat(actual.getArbeidsforholdRef()).as("arbeidsforholdRef").isEqualTo(expected.getArbeidsforholdRef());
        assertThat(actual.getOpptjeningAktivitetType()).as("opptjeningAktivitetType").isEqualTo(expected.getOpptjeningAktivitetType());
        assertThat(actual.getPeriode()).as("periode").isEqualTo(expected.getPeriode());
    }

    @Test
    void lagreSaksbehandletBeregningAktiviteterOgHentBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningAktivitetAggregat = lagAktivitetAggregat();

        // Act
        beregningsgrunnlagRepository.lagreSaksbehandledeAktiviteter(behandling.getId(), beregningAktivitetAggregat, STEG_OPPRETTET);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertBeregningAktivitetAggregat(entitet.getSaksbehandletAktiviteter().get(), beregningAktivitetAggregat);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(STEG_OPPRETTET);
        });
    }

    @Test
    void lagreOverstyring() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningAktivitetAggregat = lagAktivitetAggregat();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        // Act: Lagre BG
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Arrange: Overstyring
        var beregningAktivitetOverstyringer = lagOverstyringer();

        // Act: Lagre overstyring
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningAktivitetOverstyringer);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getBeregningsgrunnlag()).isPresent();
            assertOverstyringer(entitet.getOverstyring().get(), beregningAktivitetOverstyringer);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        });
    }

    @Test
    void skal_lagre_refusjon_overstrying() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var overstyring = BeregningRefusjonOverstyringEntitet.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet("test123")).medFørsteMuligeRefusjonFom(LocalDate.now()).build();
        var refusjon = BeregningRefusjonOverstyringerEntitet.builder().leggTilOverstyring(
                overstyring).build();
        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRefusjonOverstyring(refusjon);

        // Act: Lagre BG
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getRefusjonOverstyringer().get().getRefusjonOverstyringer().get(0)).isEqualTo(overstyring);
            assertThat(entitet.getBeregningsgrunnlag()).isPresent();
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
        });
    }

    private void assertOverstyringer(BeregningAktivitetOverstyringerEntitet actual, BeregningAktivitetOverstyringerEntitet expected) {
        var actualOverstyringer = actual.getOverstyringer();
        var expectedOverstyringer = expected.getOverstyringer();
        assertThat(actualOverstyringer).hasSize(expectedOverstyringer.size());
        assertThat(actualOverstyringer.get(0).getHandling()).isEqualTo(BeregningAktivitetHandlingType.IKKE_BENYTT);
        assertThat(actualOverstyringer.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(actualOverstyringer.get(0).getArbeidsgiver().orElseThrow().getOrgnr()).isEqualTo("55");
        assertThat(actualOverstyringer.get(0).getPeriode().getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.minusMonths(12));
        assertThat(actualOverstyringer.get(0).getPeriode().getTomDato()).isEqualTo(TIDENES_ENDE);
        assertThat(actualOverstyringer.get(0).getArbeidsforholdRef()).isEqualTo(ARBEIDSFORHOLD_ID);
    }

    private BeregningAktivitetOverstyringerEntitet lagOverstyringer() {

        return BeregningAktivitetOverstyringerEntitet.builder()
                .leggTilOverstyring(BeregningAktivitetOverstyringEntitet.builder()
                        .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
                        .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                        .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                        .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(12), TIDENES_ENDE))
                        .medArbeidsforholdRef(ARBEIDSFORHOLD_ID)
                        .build())
                .build();
    }

    private BeregningAktivitetAggregatEntitet lagAktivitetAggregat() {
        var a1 = BeregningAktivitetEntitet.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(8), LocalDate.now()))
                .build();
        var a2 = BeregningAktivitetEntitet.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLD_ID)
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(8), LocalDate.now()))
                .build();
        return BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(a1)
                .leggTilAktivitet(a2)
                .build();
    }

    @Test
    void lagreBeregningsgrunnlagOgHentBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getBeregningsgrunnlag().get()).isSameAs(beregningsgrunnlag);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(STEG_OPPRETTET);
        });
    }

    @Test
    void skalHentSisteBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag1 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag1, STEG_OPPRETTET);

        var beregningsgrunnlag2 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        var beregningsgrunnlag3 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag3, BeregningsgrunnlagTilstand.FORESLÅTT);

        // Act
        var entitetOpt = beregningsgrunnlagRepository
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), STEG_OPPRETTET);

        // Assert
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isFalse();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).isEqualTo(beregningsgrunnlag2.getId());
        });
    }

    @Test
    void skalReaktiverBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag1 = buildBeregningsgrunnlag();
        var grBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag1)
                .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                        .medSkjæringstidspunktOpptjening(LocalDate.now())
                        .build());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grBuilder, STEG_OPPRETTET);

        var beregningsgrunnlag2 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        var beregningsgrunnlag3 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag3, BeregningsgrunnlagTilstand.FORESLÅTT);

        // Act
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), STEG_OPPRETTET);
        var entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());

        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).as("bg.aktiv").isTrue();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).isNotNull();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).as("bg.id").isEqualTo(beregningsgrunnlag2.getId());
        });
    }

    @Test
    void lagreOgHenteBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        var id = beregningsgrunnlag.getId();
        assertThat(id).isNotNull();

        var beregningsgrunnlagLest = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());

        assertThat(beregningsgrunnlagLest).isEqualTo(Optional.of(beregningsgrunnlag));
    }

    @Test
    void lagreOgHenteBeregningsgrunnlagMedPrivatpersonSomArbgiver() {
        // Arrange
        var behandling = opprettBehandling();
        var aktørId = AktørId.dummy();
        var beregningsgrunnlag = buildBeregningsgrunnlagPrivatpersonArbgiver(aktørId);

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        var id = beregningsgrunnlag.getId();
        assertThat(id).isNotNull();
        var arbFor = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBgAndelArbeidsforhold().get();
        assertThat(arbFor.getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());

        var beregningsgrunnlagLest = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());

        assertThat(beregningsgrunnlagLest).isEqualTo(Optional.of(beregningsgrunnlag));
        arbFor = beregningsgrunnlagLest.get().getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBgAndelArbeidsforhold().get();
        assertThat(arbFor.getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());

    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlagPrivatpersonArbgiver(AktørId aktørId) {
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
                .get(0);
        var bgArbFor = andel.getBgAndelArbeidsforhold().get();
        var bgBuilder = BGAndelArbeidsforhold
                .builder(bgArbFor)
                .medArbeidsgiver(Arbeidsgiver.person(aktørId));
        BeregningsgrunnlagPrStatusOgAndel.builder(andel).medBGAndelArbeidsforhold(bgBuilder)
                .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));
        return beregningsgrunnlag;
    }

    @Test
    void lagreBeregningsgrunnlagOgUnderliggendeTabeller(EntityManager em) {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        var bgId = beregningsgrunnlag.getId();
        assertThat(bgId).isNotNull();
        var bgAktivitetStatusId = beregningsgrunnlag.getAktivitetStatuser().get(0).getId();
        assertThat(bgAktivitetStatusId).isNotNull();
        var sammenlingningsgrId = beregningsgrunnlag.getSammenligningsgrunnlag().get().getId();
        assertThat(sammenlingningsgrId).isNotNull();
        var bgPeriodeLagret = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var bgPeriodeId = bgPeriodeLagret.getId();
        assertThat(bgPeriodeId).isNotNull();
        var bgPrStatusOgAndelId = bgPeriodeLagret.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getId();
        assertThat(bgPrStatusOgAndelId).isNotNull();
        var bgPeriodeÅrsakId = bgPeriodeLagret.getBeregningsgrunnlagPeriodeÅrsaker().get(0).getId();
        assertThat(bgPeriodeÅrsakId).isNotNull();

        em.flush();
        em.clear();
        var beregningsgrunnlagLest = em.find(BeregningsgrunnlagEntitet.class, bgId);
        var bgAktivitetStatusLest = em.find(BeregningsgrunnlagAktivitetStatus.class, bgAktivitetStatusId);
        var sammenligningsgrunnlagLest = em.find(Sammenligningsgrunnlag.class, sammenlingningsgrId);
        var bgPeriodeLest = em.find(BeregningsgrunnlagPeriode.class, bgPeriodeId);
        var bgPrStatusOgAndelLest = em.find(BeregningsgrunnlagPrStatusOgAndel.class, bgPrStatusOgAndelId);
        var bgPeriodeÅrsakLest = em.find(BeregningsgrunnlagPeriodeÅrsak.class, bgPeriodeÅrsakId);

        assertThat(beregningsgrunnlag.getId()).isNotNull();
        assertThat(beregningsgrunnlagLest.getAktivitetStatuser()).hasSize(1);
        assertThat(bgAktivitetStatusLest).isEqualTo(beregningsgrunnlag.getAktivitetStatuser().get(0));
        assertThat(sammenligningsgrunnlagLest).isEqualTo(beregningsgrunnlag.getSammenligningsgrunnlag().get());
        assertThat(beregningsgrunnlagLest.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlagLest.getRegelloggSkjæringstidspunkt()).isEqualTo(beregningsgrunnlag.getRegelloggSkjæringstidspunkt());
        assertThat(beregningsgrunnlagLest.getRegelloggBrukersStatus()).isEqualTo(beregningsgrunnlag.getRegelloggBrukersStatus());
        assertThat(bgPeriodeLest).isEqualTo(bgPeriodeLagret);
        assertThat(bgPeriodeLest.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertThat(bgPrStatusOgAndelLest).isEqualTo(bgPeriodeLagret.getBeregningsgrunnlagPrStatusOgAndelList().get(0));
        assertThat(bgPeriodeÅrsakLest).isEqualTo(bgPeriodeLagret.getBeregningsgrunnlagPeriodeÅrsaker().get(0));
        assertThat(bgPeriodeLest.getRegelEvalueringForeslå()).isEqualTo(bgPeriodeLagret.getRegelEvalueringForeslå());
    }

    @Test
    void toBehandlingerKanHaSammeBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var behandling2 = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        var beregningsgrunnlagOpt1 = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());
        var beregningsgrunnlagOpt2 = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling2.getId());
        assertThat(beregningsgrunnlagOpt1).hasValueSatisfying(beregningsgrunnlag1 -> assertThat(beregningsgrunnlagOpt2)
                .hasValueSatisfying(beregningsgrunnlag2 -> assertThat(beregningsgrunnlag1).isSameAs(beregningsgrunnlag2)));
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(behandlingRepositoryProvider);
    }

    @Test
    void skalHenteRiktigBeregningsgrunnlagBasertPåId() {
        // Arrange
        var behandling = opprettBehandling();
        var behandling2 = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var beregningsgrunnlag2 = BeregningsgrunnlagEntitet.builder(beregningsgrunnlag)
                .medSkjæringstidspunkt(beregningsgrunnlag.getSkjæringstidspunkt().plusDays(1))
                .build();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        // Assert
        var beregningsgrunnlagOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForId(beregningsgrunnlag2.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(bg -> assertThat(bg).isSameAs(beregningsgrunnlag2));
    }

    @Test
    void skal_hente_ut_aap_saker() {
        // Arrange
        var behandling = opprettBehandling();
        var behandling2 = opprettBehandling();
        var beregningsgrunnlag = buildBeregningsgrunnlag();
        var beregningsgrunnlagAap = buildBeregningsgrunnlag();
        buildBgAktivitetStatus(beregningsgrunnlagAap, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlagAap, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        var aapGrunnlag = beregningsgrunnlagRepository.hentFagsakerMedAAPIGrunnlag();

        // Assert
        assertThat(aapGrunnlag).hasSize(1);
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
            .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
                .medBruttoPrÅr(BigDecimal.valueOf(534343.55))
                .medAvkortetPrÅr(BigDecimal.valueOf(223421.33))
                .medRedusertPrÅr(BigDecimal.valueOf(23412.32))
                .medRegelEvaluering("input1", "clob1", BeregningsgrunnlagPeriodeRegelType.FORESLÅ, "versjon")
                .medRegelEvaluering("input2", "clob2", BeregningsgrunnlagPeriodeRegelType.FASTSETT, "versjon")
                .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
                .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medGrunnbeløp(BigDecimal.valueOf(91425))
                .medRegelSporing("input1", "clob1", BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, "versjon")
                .medRegelSporing("input1", "clob1", BeregningsgrunnlagRegelType.BRUKERS_STATUS, "versjon")
                .build();
        buildSammenligningsgrunnlag(beregningsgrunnlag);
        buildBgAktivitetStatus(beregningsgrunnlag);
        var bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode);
        return beregningsgrunnlag;
    }

    private void buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        buildBgAktivitetStatus(beregningsgrunnlag, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    private void buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetStatus aktivitetStatus) {
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(aktivitetStatus)
            .build(beregningsgrunnlag);
    }

    private void buildSammenligningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(LocalDate.now().minusDays(12), LocalDate.now().minusDays(6))
            .medRapportertPrÅr(BigDecimal.valueOf(323212.12))
            .medAvvikPromille(BigDecimal.valueOf(120))
            .build(beregningsgrunnlag);
    }
}
