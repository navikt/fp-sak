package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UDEFINERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaInngangsVilkårUtleder;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

class EndringskontrollerTest {

    private KontrollerFaktaInngangsVilkårUtleder kontrollerFaktaTjenesteMock;
    private Instance<KontrollerFaktaInngangsVilkårUtleder> kontrollerFaktaTjenesterMock;
    private BehandlingskontrollTjeneste behandlingskontrollTjenesteMock;
    private Instance<StartpunktTjeneste> startpunktTjenesteProviderMock;
    private StartpunktTjeneste startpunktTjenesteMock;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjenesteMock;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

    @BeforeEach
    public void before() {
        kontrollerFaktaTjenesteMock = mock(KontrollerFaktaInngangsVilkårUtleder.class);
        when(kontrollerFaktaTjenesteMock.utledAksjonspunkterTilHøyreForStartpunkt(any(), any(StartpunktType.class))).thenReturn(new ArrayList<>());
        kontrollerFaktaTjenesterMock = new UnitTestLookupInstanceImpl<>(kontrollerFaktaTjenesteMock);
        historikkinnslagTjenesteMock = mock(RegisterinnhentingHistorikkinnslagTjeneste.class);

        behandlingskontrollTjenesteMock = mock(BehandlingskontrollTjeneste.class);
        when(behandlingskontrollTjenesteMock.finnAksjonspunktDefinisjonerFraOgMed(any(), any(BehandlingStegType.class), anyBoolean())).thenReturn(new HashSet<>());

        startpunktTjenesteMock = mock(StartpunktTjeneste.class);
        startpunktTjenesteProviderMock = new UnitTestLookupInstanceImpl<>(startpunktTjenesteMock);
    }

    @Test
    void skal_oppdatere_startpunkt_ved_tilbakespoling_til_punkt_før_nåværende_startpunkt() {
        // Arrange
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.VURDER_UTTAK, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        var startpunktUttak = StartpunktType.UTTAKSVILKÅR;
        revurdering.setStartpunkt(startpunktUttak);

        var startpunktBeregning = StartpunktType.BEREGNING;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktBeregning);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktBeregning);
    }

    @Test
    void skal_håndtere_koarb_utgang_til_inngang() {
        // Arrange
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTGANG);
        var startpunktKoarb = StartpunktType.KONTROLLER_ARBEIDSFORHOLD;

        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktKoarb);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0); // Samme steg

        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(UDEFINERT); // Ikke satt
        verify(behandlingskontrollTjenesteMock, times(1)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
        verifyNoInteractions(kontrollerFaktaTjenesteMock);
    }

    @Test
    void skal_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_til_venstre_for_aktivt_steg() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .lagMocked();
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.VURDER_UTTAK, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktBeregning = StartpunktType.BEREGNING;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktBeregning);
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(startpunktBeregning.getBehandlingSteg()));
    }

    @Test
    void skal_ikke_spole_til_start_når_førstegangsbehandling_får_utledet_startpunkt_udefinert() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktUdefinert = StartpunktType.UDEFINERT;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUdefinert);

        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(behandling.getStartpunkt()).isEqualTo(startpunktUdefinert);
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }

    @Test
    void skal_ikke_oppdatere_startpunkt_ved_tilbakespoling_til_punkt_etter_nåværende_startpunkt() {
        // Arrange
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        var startpunktBeregning = StartpunktType.BEREGNING;
        revurdering.setStartpunkt(startpunktBeregning);

        var startpunktUttak = StartpunktType.UTTAKSVILKÅR;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUttak);

        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktBeregning);
    }

    @Test
    void skal_ikke_oppdatere_startpunkt_hvis_vi_ikke_har_grunnlag_for_å_si_at_nytt_startpunkt_er_tidligere() {
        // Arrange
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        var startpunktTypePåBehandling = StartpunktType.UDEFINERT;
        revurdering.setStartpunkt(startpunktTypePåBehandling);

        var startpunktUtledetFraEndringssjekk = StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktUtledetFraEndringssjekk);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, null, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktTypePåBehandling);
    }

    @Test
    void skal_spole_til_start_når_manglende_fødsel_gir_startpunkt_til_venstre_for_aktivt_steg() {
        // Arrange
        var fdato = LocalDate.now().minusWeeks(3);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel().medFødselAdopsjonsdato(List.of(fdato))
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);
        var startpunktTypePåBehandling = StartpunktType.OPPTJENING;
        revurdering.setStartpunkt(startpunktTypePåBehandling);

        var startpunktSRB = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSRB);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(1);

        // Act
        endringskontroller.spolTilStartpunkt(revurdering, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(revurdering.getStartpunkt()).isEqualTo(startpunktSRB);
    }

    @Test
    void skal_avbryte_avklar_termin_når_det_finnes_fødsel() {
        // Arrange
        var fdato = LocalDate.now().minusWeeks(3);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fdato))
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medSøknadHendelse().medAntallBarn(1).medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().minusDays(5))
            .medUtstedtDato(LocalDate.now().minusMonths(1))
            .medNavnPå("Legen min"));
        scenario.medBekreftetHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now().minusDays(2))
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusDays(5))
                .medUtstedtDato(LocalDate.now().minusMonths(1))
                .medNavnPå("Legen min"));

        var behandling = scenario.lagMocked();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE).erÅpentAksjonspunkt()).isTrue();

        var startpunktSRB = StartpunktType.OPPTJENING;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSRB);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(true);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(-1);
        when(behandlingskontrollTjenesteMock.skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE)).thenReturn(true);

        // Blir ikke reutledet
        when(kontrollerFaktaTjenesteMock.utledAksjonspunkterFomSteg(any(), any())).thenReturn(Collections.emptyList());

        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);


        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        verify(behandlingskontrollTjenesteMock).lagreAksjonspunkterAvbrutt(any(), any(), any());
    }

    @Test
    void skal_spole_til_INNGANG_når_behandlingen_står_i_UTGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(0);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(BehandlingStegType.SØKERS_RELASJON_TIL_BARN));
    }

    @Test
    void skal_spole_til_INNGANG_når_behandlingen_står_i_UTGANG_med_op_og_startpunkt_er_senere_steg_som_behandlingen_står_i() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
        var behandling = scenario.lagMocked();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, BehandlingStegStatus.UTGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), eq(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT), eq(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT))).thenReturn(0);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), eq(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT), eq(StartpunktType.SØKERS_RELASJON_TIL_BARNET.getBehandlingSteg()))).thenReturn(-1);
        when(behandlingskontrollTjenesteMock.skalAksjonspunktLøsesIEllerEtterSteg(any(), any(), eq(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT), eq(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU))).thenReturn(true);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        verify(behandlingskontrollTjenesteMock).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(),eq(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT));
    }

    @Test
    void skal_ikke_spole_til_INNGANG_når_behandlingen_står_i_INNGANG_og_startpunkt_er_samme_steg_som_behandlingen_står_i() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, BehandlingStegStatus.INNGANG, BehandlingStegStatus.UTFØRT);

        var startpunktSrb = StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        when(startpunktTjenesteMock.utledStartpunktForDiffBehandlingsgrunnlag(any(), any(EndringsresultatDiff.class))).thenReturn(startpunktSrb);
        var endringskontroller = new Endringskontroller(behandlingskontrollTjenesteMock, startpunktTjenesteProviderMock, historikkinnslagTjenesteMock, kontrollerFaktaTjenesterMock, skjæringstidspunktTjeneste);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Long.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.erStegPassert(any(Behandling.class), any())).thenReturn(false);
        when(behandlingskontrollTjenesteMock.sammenlignRekkefølge(any(), any(), any(), any())).thenReturn(-1);

        // Act
        endringskontroller.spolTilStartpunkt(behandling, EndringsresultatDiff.medDiff(Inntektsmelding.class, 1L, 2L), StartpunktType.UDEFINERT);

        // Assert
        verify(behandlingskontrollTjenesteMock, times(0)).behandlingTilbakeføringHvisTidligereBehandlingSteg(any(), any());
    }
}
