package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningAksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp.BeregningsgrunnlagInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

public class ForeslåBeregningsgrunnlagStegTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Mock
    private BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagRegelResultat;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private BehandlingskontrollKontekst kontekst;
    @Mock
    private Behandling behandling;
    private ForeslåBeregningsgrunnlagSteg steg;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    @Mock
    BeregningsgrunnlagInputProvider inputProvider;

    @Before
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        var ref = BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now()).medSkjæringstidspunktOpptjening(LocalDate.now()).build());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = new ForeldrepengerGrunnlag(100, false);
        var input = new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(ref), null, null, AktivitetGradering.INGEN_GRADERING, List.of(), foreldrepengerGrunnlag);
        var inputTjeneste = mock(BeregningsgrunnlagInputTjeneste.class);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(inputTjeneste.lagInput(behandling.getId())).thenReturn(input);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(any())).thenReturn(beregningsgrunnlagRegelResultat);

        FamilieHendelseEntitet mockFamilieHendelseEntitet = mock(FamilieHendelseEntitet.class);
        when(mockFamilieHendelseEntitet.getBarna()).thenReturn(List.of());

        FamilieHendelseGrunnlagEntitet mockFamilieHendelseGrunnlagEntitet = mock(FamilieHendelseGrunnlagEntitet.class);
        when(mockFamilieHendelseGrunnlagEntitet.getGjeldendeVersjon()).thenReturn(mockFamilieHendelseEntitet);

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(mockFamilieHendelseGrunnlagEntitet));

        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new ForeslåBeregningsgrunnlagSteg(behandlingRepository, familieHendelseRepository, beregningsgrunnlagKopierOgLagreTjeneste, inputProvider);

        iayTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of());
    }

    @Test
    public void stegUtførtUtenAksjonspunkter() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void stegUtførtNårRegelResultatInneholderAutopunkt() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);
        BeregningAksjonspunktResultat aksjonspunktResultat = BeregningAksjonspunktResultat.opprettFor(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        when(beregningsgrunnlagRegelResultat.getAksjonspunkter()).thenReturn(Collections.singletonList(aksjonspunktResultat));

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).hasSize(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

    private void opprettVilkårResultatForBehandling(VilkårResultatType resultatType) {
        VilkårResultat vilkårResultat = VilkårResultat.builder().medVilkårResultatType(resultatType)
            .buildFor(behandling);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
    }
}
