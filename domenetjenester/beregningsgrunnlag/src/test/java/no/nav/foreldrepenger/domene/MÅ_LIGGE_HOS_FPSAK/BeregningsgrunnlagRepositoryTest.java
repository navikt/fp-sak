package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BeregningsgrunnlagRepositoryTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    private static final String ORGNR = "55";
    private static final BeregningsgrunnlagTilstand STEG_OPPRETTET = BeregningsgrunnlagTilstand.OPPRETTET;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingBuilder = new FagsakBehandlingBuilder(entityManager);
    }

    @Test
    public void skal_kopiere_med_register_aktiviteter_fra_original_behandling() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagAktivitetAggregat();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.FASTSATT);
        Behandling revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                                .medManueltOpprettet(false)
                                .medOriginalBehandlingId(behandling.getId()))
                .build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        // Act
        beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId(),
                BeregningsgrunnlagTilstand.FASTSATT);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(revurdering.getId(),
                        BeregningsgrunnlagTilstand.FASTSATT);
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertBeregningAktivitetAggregat(entitet.getRegisterAktiviteter(), beregningAktivitetAggregat);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT);
        });
    }

    @Test
    public void lagreRegisterBeregningAktiviteterOgBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagAktivitetAggregat();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
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
        for (int i = 0; i < expectedAggregat.getBeregningAktiviteter().size(); i++) {
            BeregningAktivitetEntitet expected = expectedAggregat.getBeregningAktiviteter().get(i);
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
    public void lagreSaksbehandletBeregningAktiviteterOgHentBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagAktivitetAggregat();

        // Act
        beregningsgrunnlagRepository.lagreSaksbehandledeAktiviteter(behandling.getId(), beregningAktivitetAggregat, STEG_OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertBeregningAktivitetAggregat(entitet.getSaksbehandletAktiviteter().get(), beregningAktivitetAggregat);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(STEG_OPPRETTET);
        });
    }

    @Test
    public void lagreOverstyring() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagAktivitetAggregat();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRegisterAktiviteter(beregningAktivitetAggregat);
        // Act: Lagre BG
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Arrange: Overstyring
        BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringer = lagOverstyringer();

        // Act: Lagre overstyring
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningAktivitetOverstyringer);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getBeregningsgrunnlag()).isPresent();
            assertOverstyringer(entitet.getOverstyring().get(), beregningAktivitetOverstyringer);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        });
    }

    @Test
    public void skal_lagre_refusjon_overstrying() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningRefusjonOverstyringEntitet overstyring = BeregningRefusjonOverstyringEntitet.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet("test123")).medFørsteMuligeRefusjonFom(LocalDate.now()).build();
        BeregningRefusjonOverstyringerEntitet refusjon = BeregningRefusjonOverstyringerEntitet.builder().leggTilOverstyring(
                overstyring).build();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(beregningsgrunnlag)
                .medRefusjonOverstyring(refusjon);

        // Act: Lagre BG
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getRefusjonOverstyringer().get().getRefusjonOverstyringer().get(0)).isEqualTo(overstyring);
            assertThat(entitet.getBeregningsgrunnlag()).isPresent();
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
        });
    }

    private void assertOverstyringer(BeregningAktivitetOverstyringerEntitet actual, BeregningAktivitetOverstyringerEntitet expected) {
        List<BeregningAktivitetOverstyringEntitet> actualOverstyringer = actual.getOverstyringer();
        List<BeregningAktivitetOverstyringEntitet> expectedOverstyringer = expected.getOverstyringer();
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
        BeregningAktivitetEntitet a1 = BeregningAktivitetEntitet.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(8), LocalDate.now()))
                .build();
        BeregningAktivitetEntitet a2 = BeregningAktivitetEntitet.builder()
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
    public void lagreBeregningsgrunnlagOgHentBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isTrue();
            assertThat(entitet.getBeregningsgrunnlag().get()).isSameAs(beregningsgrunnlag);
            assertThat(entitet.getBeregningsgrunnlagTilstand()).isEqualTo(STEG_OPPRETTET);
        });
    }

    @Test
    public void skalHentSisteBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag1 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag1, STEG_OPPRETTET);

        BeregningsgrunnlagEntitet beregningsgrunnlag2 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        BeregningsgrunnlagEntitet beregningsgrunnlag3 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag3, BeregningsgrunnlagTilstand.FORESLÅTT);

        // Act
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), STEG_OPPRETTET);

        // Assert
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).isFalse();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).isEqualTo(beregningsgrunnlag2.getId());
        });
    }

    @Test
    public void skalReaktiverBeregningsgrunnlagGrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag1 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag1, STEG_OPPRETTET);

        BeregningsgrunnlagEntitet beregningsgrunnlag2 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        BeregningsgrunnlagEntitet beregningsgrunnlag3 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag3, BeregningsgrunnlagTilstand.FORESLÅTT);

        // Act
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), STEG_OPPRETTET);
        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());

        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).as("bg.aktiv").isTrue();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).isNotNull();
            assertThat(entitet.getBeregningsgrunnlag().get().getId()).as("bg.id").isEqualTo(beregningsgrunnlag2.getId());
        });
    }

    @Test
    public void lagreOgHenteBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        Long id = beregningsgrunnlag.getId();
        assertThat(id).isNotNull();

        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagLest = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());

        assertThat(beregningsgrunnlagLest).isEqualTo(Optional.of(beregningsgrunnlag));
    }

    @Test
    public void lagreOgHenteBeregningsgrunnlagMedPrivatpersonSomArbgiver() {
        // Arrange
        var behandling = opprettBehandling();
        AktørId aktørId = AktørId.dummy();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlagPrivatpersonArbgiver(aktørId);

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        Long id = beregningsgrunnlag.getId();
        assertThat(id).isNotNull();
        BGAndelArbeidsforhold arbFor = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBgAndelArbeidsforhold().get();
        assertThat(arbFor.getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());

        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagLest = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());

        assertThat(beregningsgrunnlagLest).isEqualTo(Optional.of(beregningsgrunnlag));
        arbFor = beregningsgrunnlagLest.get().getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBgAndelArbeidsforhold().get();
        assertThat(arbFor.getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());

    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlagPrivatpersonArbgiver(AktørId aktørId) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
                .get(0);
        BGAndelArbeidsforhold bgArbFor = andel.getBgAndelArbeidsforhold().get();
        BGAndelArbeidsforhold.Builder bgBuilder = BGAndelArbeidsforhold
                .builder(bgArbFor)
                .medArbeidsgiver(Arbeidsgiver.person(aktørId));
        BeregningsgrunnlagPrStatusOgAndel.builder(andel).medBGAndelArbeidsforhold(bgBuilder)
                .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));
        return beregningsgrunnlag;
    }

    @Test
    public void lagreBeregningsgrunnlagOgUnderliggendeTabeller(EntityManager em) {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        Long bgId = beregningsgrunnlag.getId();
        assertThat(bgId).isNotNull();
        Long bgAktivitetStatusId = beregningsgrunnlag.getAktivitetStatuser().get(0).getId();
        assertThat(bgAktivitetStatusId).isNotNull();
        Long sammenlingningsgrId = beregningsgrunnlag.getSammenligningsgrunnlag().getId();
        assertThat(sammenlingningsgrId).isNotNull();
        BeregningsgrunnlagPeriode bgPeriodeLagret = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        Long bgPeriodeId = bgPeriodeLagret.getId();
        assertThat(bgPeriodeId).isNotNull();
        Long bgPrStatusOgAndelId = bgPeriodeLagret.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getId();
        assertThat(bgPrStatusOgAndelId).isNotNull();
        Long bgPeriodeÅrsakId = bgPeriodeLagret.getBeregningsgrunnlagPeriodeÅrsaker().get(0).getId();
        assertThat(bgPeriodeÅrsakId).isNotNull();

        var repository = new Repository(em);
        repository.flushAndClear();
        BeregningsgrunnlagEntitet beregningsgrunnlagLest = repository.hent(BeregningsgrunnlagEntitet.class, bgId);
        BeregningsgrunnlagAktivitetStatus bgAktivitetStatusLest = repository.hent(BeregningsgrunnlagAktivitetStatus.class, bgAktivitetStatusId);
        Sammenligningsgrunnlag sammenligningsgrunnlagLest = repository.hent(Sammenligningsgrunnlag.class, sammenlingningsgrId);
        BeregningsgrunnlagPeriode bgPeriodeLest = repository.hent(BeregningsgrunnlagPeriode.class, bgPeriodeId);
        BeregningsgrunnlagPrStatusOgAndel bgPrStatusOgAndelLest = repository.hent(BeregningsgrunnlagPrStatusOgAndel.class, bgPrStatusOgAndelId);
        BeregningsgrunnlagPeriodeÅrsak bgPeriodeÅrsakLest = repository.hent(BeregningsgrunnlagPeriodeÅrsak.class, bgPeriodeÅrsakId);

        assertThat(beregningsgrunnlag.getId()).isNotNull();
        assertThat(beregningsgrunnlagLest.getAktivitetStatuser()).hasSize(1);
        assertThat(bgAktivitetStatusLest).isEqualTo(beregningsgrunnlag.getAktivitetStatuser().get(0));
        assertThat(sammenligningsgrunnlagLest).isEqualTo(beregningsgrunnlag.getSammenligningsgrunnlag());
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
    public void toBehandlingerKanHaSammeBeregningsgrunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        Behandling behandling2 = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt1 = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling.getId());
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt2 = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForBehandling(behandling2.getId());
        assertThat(beregningsgrunnlagOpt1).hasValueSatisfying(beregningsgrunnlag1 -> assertThat(beregningsgrunnlagOpt2)
                .hasValueSatisfying(beregningsgrunnlag2 -> assertThat(beregningsgrunnlag1).isSameAs(beregningsgrunnlag2)));
    }

    private Behandling opprettBehandling() {
        return behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void skalHenteRiktigBeregningsgrunnlagBasertPåId() {
        // Arrange
        var behandling = opprettBehandling();
        Behandling behandling2 = opprettBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();
        BeregningsgrunnlagEntitet beregningsgrunnlag2 = BeregningsgrunnlagEntitet.builder(Kopimaskin.deepCopy(beregningsgrunnlag))
                .medSkjæringstidspunkt(beregningsgrunnlag.getSkjæringstidspunkt().plusDays(1))
                .build();

        // Act
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag, STEG_OPPRETTET);
        beregningsgrunnlagRepository.lagre(behandling2.getId(), beregningsgrunnlag2, STEG_OPPRETTET);

        // Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository
                .hentBeregningsgrunnlagForId(beregningsgrunnlag2.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(bg -> assertThat(bg).isSameAs(beregningsgrunnlag2));
    }

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
                .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
                .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
                .medRedusertPrÅr(BigDecimal.valueOf(52335))
                .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
                .medBruttoPrÅr(BigDecimal.valueOf(534343.55))
                .medAvkortetPrÅr(BigDecimal.valueOf(223421.33))
                .medRedusertPrÅr(BigDecimal.valueOf(23412.32))
                .medRegelEvalueringForeslå("input1", "clob1")
                .medRegelEvalueringFastsett("input2", "clob2")
                .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
                .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medGrunnbeløp(BigDecimal.valueOf(91425))
                .medRegelloggSkjæringstidspunkt("input1", "clob1")
                .medRegelloggBrukersStatus("input2", "clob2")
                .medRegelinputPeriodisering("input3")
                .build();
        buildSammenligningsgrunnlag(beregningsgrunnlag);
        buildBgAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode);
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .build(beregningsgrunnlag);
    }

    private Sammenligningsgrunnlag buildSammenligningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return Sammenligningsgrunnlag.builder()
                .medSammenligningsperiode(LocalDate.now().minusDays(12), LocalDate.now().minusDays(6))
                .medRapportertPrÅr(BigDecimal.valueOf(323212.12))
                .medAvvikPromille(BigDecimal.valueOf(120))
                .build(beregningsgrunnlag);
    }
}
