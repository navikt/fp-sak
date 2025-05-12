package no.nav.foreldrepenger.domene.migrering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetAggregatDto;

import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import org.jboss.weld.exceptions.IllegalStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.folketrygdloven.kalkulus.migrering.MigrerBeregningsgrunnlagResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.prosess.KalkulusKlient;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class BeregningMigreringTjenesteTest {

    @Mock
    private KalkulusKlient klient;
    @Mock
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Mock
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private RegelsporingMigreringTjeneste regelsporingMigreringTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BeregningMigreringTjeneste beregningMigreringTjeneste;

    @BeforeEach
    void setup() {
        beregningMigreringTjeneste = new BeregningMigreringTjeneste(klient, beregningsgrunnlagRepository, koblingRepository, behandlingRepository, regelsporingMigreringTjeneste, skjæringstidspunktTjeneste);
    }

    @Test
    void happycase_migrering() {
        // Arrange
        var saksnummer = new Saksnummer("123");
        var behandling = lagBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(any())).thenReturn(Optional.of(lagGrunnlagEntitet()));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)).thenReturn(List.of(behandling));
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(koblingRepository.hentKobling(any())).thenReturn(Optional.empty());
        when(koblingRepository.opprettKobling(any())).thenReturn(new BeregningsgrunnlagKobling(ref.behandlingId(), ref.behandlingUuid()));
        when(klient.migrerGrunnlag(any())).thenReturn(new MigrerBeregningsgrunnlagResponse(lagGrunnlagDto(), null, List.of(), List.of(), List.of()));

        // Act
        beregningMigreringTjeneste.migrerSak(saksnummer);

        // Assert
        verify(klient).migrerGrunnlag(any());
    }

    @Test
    void skal_kaste_feil_ved_feilende_sammenligning() {
        // Arrange
        var saksnummer = new Saksnummer("123");
        var behandling = lagBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(any())).thenReturn(Optional.of(lagGrunnlagEntitet()));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)).thenReturn(List.of(behandling));
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(koblingRepository.hentKobling(any())).thenReturn(Optional.empty());
        when(koblingRepository.opprettKobling(any())).thenReturn(new BeregningsgrunnlagKobling(ref.behandlingId(), ref.behandlingUuid()));
        when(klient.migrerGrunnlag(any())).thenReturn(new MigrerBeregningsgrunnlagResponse(lagGrunnlagDto(), null, List.of(new MigrerBeregningsgrunnlagResponse.RegelsporingPeriode(
            BeregningsgrunnlagPeriodeRegelType.FASTSETT, "eval", "input", "1.0", new Periode(LocalDate.now(), LocalDate.now()))), List.of(), List.of()));

        // Act
        // Assert
        assertThrows(IllegalStateException.class, () -> beregningMigreringTjeneste.migrerSak(saksnummer));
    }

    @Test
    void skal_kunne_migrere_regelsporing_uten_evalueringer() {
        // Arrange
        var saksnummer = new Saksnummer("123");
        var behandling = lagBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        var grunnlag = lagGrunnlagEntitet(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.PERIODISERING, null, "input", BigDecimal.valueOf(100000));
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(any())).thenReturn(Optional.of(grunnlag));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)).thenReturn(List.of(behandling));
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(koblingRepository.hentKobling(any())).thenReturn(Optional.empty());
        when(regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(any(), any())).thenReturn(grunnlag.getBeregningsgrunnlag().get().getRegelSporinger());
        var kobling = new BeregningsgrunnlagKobling(ref.behandlingId(), ref.behandlingUuid());
        when(koblingRepository.opprettKobling(any())).thenReturn(kobling);
        when(klient.migrerGrunnlag(any())).thenReturn(new MigrerBeregningsgrunnlagResponse(lagGrunnlagDto(), null, List.of(), List.of(new MigrerBeregningsgrunnlagResponse.RegelsporingGrunnlag(
            BeregningsgrunnlagRegelType.PERIODISERING, null, "input", null)), List.of()));

        // Act
        beregningMigreringTjeneste.migrerSak(saksnummer);

        // Assert
        verify(koblingRepository, times(1)).oppdaterKoblingMedStpOgGrunnbeløp(kobling, grunnlag.getBeregningsgrunnlag().get().getGrunnbeløp(), grunnlag.getBeregningsgrunnlag().get().getSkjæringstidspunkt());
    }

    @Test
    void skal_migrere_uten_grunnbeløp() {
        // Arrange
        var saksnummer = new Saksnummer("123");
        var behandling = lagBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        var grunnlag = lagGrunnlagEntitet(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.PERIODISERING, null, "input", null);
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(any())).thenReturn(Optional.of(grunnlag));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)).thenReturn(List.of(behandling));
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(koblingRepository.hentKobling(any())).thenReturn(Optional.empty());
        when(regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(any(), any())).thenReturn(grunnlag.getBeregningsgrunnlag().get().getRegelSporinger());
        var kobling = new BeregningsgrunnlagKobling(ref.behandlingId(), ref.behandlingUuid());
        when(koblingRepository.opprettKobling(any())).thenReturn(kobling);
        when(klient.migrerGrunnlag(any())).thenReturn(new MigrerBeregningsgrunnlagResponse(lagGrunnlagDto(null), null, List.of(), List.of(new MigrerBeregningsgrunnlagResponse.RegelsporingGrunnlag(
            BeregningsgrunnlagRegelType.PERIODISERING, null, "input", null)), List.of()));

        // Act
        beregningMigreringTjeneste.migrerSak(saksnummer);

        // Assert
        verify(koblingRepository, times(1)).oppdaterKoblingMedStpOgGrunnbeløp(kobling, null, grunnlag.getBeregningsgrunnlag().get().getSkjæringstidspunkt());
    }

    @Test
    void skal_sortere_behandlinger_i_klassisk_rekkefølge() {
        // Arrange
        var behandling1 = mock(Behandling.class);
        var behandling2 = mock(Behandling.class);
        var behandling3 = mock(Behandling.class);
        when(behandling1.getId()).thenReturn(1L);
        when(behandling2.getId()).thenReturn(2L);
        when(behandling3.getId()).thenReturn(3L);

        when(behandling1.getOriginalBehandlingId()).thenReturn(Optional.empty());
        when(behandling2.getOriginalBehandlingId()).thenReturn(Optional.of(1L));
        when(behandling3.getOriginalBehandlingId()).thenReturn(Optional.of(2L));

        // Act
        var sortertListe = beregningMigreringTjeneste.sorterBehandlinger(List.of(behandling2, behandling1, behandling3));

        // Assert
        var iterator = sortertListe.iterator();
        var b1 = iterator.next();
        assertThat(b1.getId()).isEqualTo(1L);

        var b2 = iterator.next();
        assertThat(b2.getId()).isEqualTo(2L);

        var b3 = iterator.next();
        assertThat(b3.getId()).isEqualTo(3L);
    }

    @Test
    void skal_sortere_behandlinger_ingen_revurderinger_rekkefølge_ubetydelig() {
        // Arrange
        var behandling1 = mock(Behandling.class);
        var behandling2 = mock(Behandling.class);

        when(behandling1.getOriginalBehandlingId()).thenReturn(Optional.empty());
        when(behandling2.getOriginalBehandlingId()).thenReturn(Optional.empty());

        // Act
        var sortertListe = beregningMigreringTjeneste.sorterBehandlinger(List.of(behandling2, behandling1));

        // Assert
        assertThat(sortertListe).containsExactlyInAnyOrder(behandling2, behandling1);
    }

    @Test
    void skal_sortere_behandlinger_i_sprikende_tre() {
        // Arrange
        var behandling1 = mock(Behandling.class);
        var behandling2 = mock(Behandling.class);
        var behandling3 = mock(Behandling.class);
        var behandling4 = mock(Behandling.class);
        var behandling5 = mock(Behandling.class);

        when(behandling1.getId()).thenReturn(1L);
        when(behandling2.getId()).thenReturn(2L);
        when(behandling3.getId()).thenReturn(3L);
        when(behandling4.getId()).thenReturn(4L);
        when(behandling5.getId()).thenReturn(5L);

        when(behandling1.getOriginalBehandlingId()).thenReturn(Optional.empty());
        when(behandling2.getOriginalBehandlingId()).thenReturn(Optional.of(1L));
        when(behandling3.getOriginalBehandlingId()).thenReturn(Optional.of(2L));
        when(behandling4.getOriginalBehandlingId()).thenReturn(Optional.of(2L));
        when(behandling5.getOriginalBehandlingId()).thenReturn(Optional.of(4L));

        // Act
        var sortertListe = beregningMigreringTjeneste.sorterBehandlinger(List.of(behandling4, behandling2,behandling5, behandling1, behandling3));

        // Assert
        var iterator = sortertListe.iterator();
        var b1 = iterator.next();
        assertThat(b1.getId()).isEqualTo(1L);

        var b2 = iterator.next();
        assertThat(b2.getId()).isEqualTo(2L);

        var b3 = iterator.next();
        var akseptertId3 = b3.getId().equals(3L) || b3.getId().equals(4L);
        assertThat(akseptertId3).isTrue();

        var b4 = iterator.next();
        var akseptertId4 = b4.getId().equals(3L) || b4.getId().equals(4L) ||b4.getId().equals(5L);
        assertThat(akseptertId4).isTrue();

        var b5 = iterator.next();
        var akseptertId5 = b5.getId().equals(3L) ||b5.getId().equals(5L);
        assertThat(akseptertId5).isTrue();

    }
    private Behandling lagBehandling() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        behandling.avsluttBehandling();
        return behandling;
    }

    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlagEntitet() {
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE).medBruttoPrÅr(
            BigDecimal.valueOf(100_000)).medAvkortetPrÅr(BigDecimal.valueOf(100_000)).medRedusertPrÅr(BigDecimal.valueOf(100_000));
        var grunnbeløp = Beløp.av(100000);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(grunnbeløp)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .leggTilAktivitetStatus(new BeregningsgrunnlagAktivitetStatus.Builder().medAktivitetStatus(AktivitetStatus.KOMBINERT_AT_FL).medHjemmel(
                Hjemmel.F_14_7_8_40))
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);
        return gr;
    }

    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlagEntitet(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType regelType, String evaluering, String input, BigDecimal grunnbeløp) {
        var beregningsgrunnlagPeriode = new BeregningsgrunnlagPeriode.Builder().medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE).medBruttoPrÅr(
            BigDecimal.valueOf(100_000)).medAvkortetPrÅr(BigDecimal.valueOf(100_000)).medRedusertPrÅr(BigDecimal.valueOf(100_000));
        var beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medRegelSporing(input, evaluering, regelType, null)
            .leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)
            .leggTilAktivitetStatus(new BeregningsgrunnlagAktivitetStatus.Builder().medAktivitetStatus(AktivitetStatus.KOMBINERT_AT_FL).medHjemmel(
                Hjemmel.F_14_7_8_40));
        if (grunnbeløp != null) {
            beregningsgrunnlagBuilder.medGrunnbeløp(grunnbeløp);
        }
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlagBuilder.build()).build(1L, BeregningsgrunnlagTilstand.FASTSATT);
        return gr;
    }

    private BeregningsgrunnlagGrunnlagDto lagGrunnlagDto() {
        return lagGrunnlagDto(BigDecimal.valueOf(100000));
    }

    private BeregningsgrunnlagGrunnlagDto lagGrunnlagDto(BigDecimal grunnbeløp) {
        var beregningsgrunnlagPeriodeDto = new BeregningsgrunnlagPeriodeDto(List.of(), new Periode(LocalDate.now(), Tid.TIDENES_ENDE),
            new no.nav.folketrygdloven.kalkulus.felles.v1.Beløp(BigDecimal.valueOf(100_000)),
            new no.nav.folketrygdloven.kalkulus.felles.v1.Beløp(BigDecimal.valueOf(100_000)),
            new no.nav.folketrygdloven.kalkulus.felles.v1.Beløp(BigDecimal.valueOf(100_000)), null, List.of(), null, null, null);
        var status = new BeregningsgrunnlagAktivitetStatusDto(
            no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.KOMBINERT_AT_FL, no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_40);
        var bgDto = new BeregningsgrunnlagDto(LocalDate.now(), List.of(), List.of(beregningsgrunnlagPeriodeDto), List.of(), List.of(),
            false, grunnbeløp == null ? null : new no.nav.folketrygdloven.kalkulus.felles.v1.Beløp(grunnbeløp), List.of(status));
        var grDto = new BeregningsgrunnlagGrunnlagDto(bgDto, null, new BeregningAktivitetAggregatDto(List.of(), LocalDate.now()), null, null, null,
            no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);
        return grDto;
    }

}
