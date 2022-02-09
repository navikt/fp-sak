package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp.BeregningsgrunnlagInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
public class ForeslåBeregningsgrunnlagStegTest {

    @Mock
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Mock
    private KalkulusTjeneste kalkulusTjeneste;
    @Mock
    private FagsakRelasjonRepository fagsakRelasjonRepository;
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

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        var stp = Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(LocalDate.now())
            .medFørsteUttaksdatoGrunnbeløp(LocalDate.now())
            .medSkjæringstidspunktOpptjening(LocalDate.now());

        var ref = BehandlingReferanse.fra(behandling, stp.build());
        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag(100, false, AktivitetGradering.INGEN_GRADERING);
        var input = new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(ref), null, null, List.of(), foreldrepengerGrunnlag);
        var inputTjeneste = mock(BeregningsgrunnlagInputTjeneste.class);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(stp.build());
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(inputTjeneste.lagInput(behandling.getId())).thenReturn(input);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(any())).thenReturn(beregningsgrunnlagRegelResultat);
        when(fagsakRelasjonRepository.finnRelasjonHvisEksisterer(any())).thenReturn(
            Optional.of(new FagsakRelasjon(behandling.getFagsak(), null, null, null, Dekningsgrad._100, null, null)));
        var mockFamilieHendelseEntitet = mock(FamilieHendelseEntitet.class);
        when(mockFamilieHendelseEntitet.getBarna()).thenReturn(List.of());

        var mockFamilieHendelseGrunnlagEntitet = mock(FamilieHendelseGrunnlagEntitet.class);
        when(mockFamilieHendelseGrunnlagEntitet.getGjeldendeVersjon()).thenReturn(mockFamilieHendelseEntitet);

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(mockFamilieHendelseGrunnlagEntitet));

        var beregningTjeneste = new BeregningFPSAK(beregningsgrunnlagKopierOgLagreTjeneste, behandlingRepository,
            new UnitTestLookupInstanceImpl<>(skjæringstidspunktTjeneste), null,
            null, inputProvider, null, iayTjeneste);
        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new ForeslåBeregningsgrunnlagSteg(behandlingRepository, familieHendelseRepository, new BeregningTjeneste(null, beregningTjeneste), fagsakRelasjonRepository);

        iayTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of());
    }

    @Test
    public void stegUtførtUtenAksjonspunkter() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void stegUtførtNårRegelResultatInneholderAutopunkt() {
        // Arrange
        opprettVilkårResultatForBehandling(VilkårResultatType.INNVILGET);
        var aksjonspunktResultat = BeregningAvklaringsbehovResultat.opprettFor(
            AvklaringsbehovDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        when(beregningsgrunnlagRegelResultat.getAksjonspunkter()).thenReturn(Collections.singletonList(aksjonspunktResultat));

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).hasSize(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(
            no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

    private void opprettVilkårResultatForBehandling(VilkårResultatType resultatType) {
        var vilkårResultat = VilkårResultat.builder().medVilkårResultatType(resultatType).buildFor(behandling);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
    }
}
