package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.BekreftAleneomsorgOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

public class YtelseFordelingDtoTjenesteTest extends EntityManagerAwareTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;
    private UføretrygdRepository uføretrygdRepository = mock(UføretrygdRepository.class);

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        førsteUttaksdatoTjeneste = new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    public void teste_lag_ytelsefordeling_dto() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(true);
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        // Act
        new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), mock(UføretrygdRepository.class)), mock(PersonopplysningRepository.class)) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).hasSize(1);
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    public void teste_lag_ytelsefordeling_dto_med_annenforelder_har_rett_perioder() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var uforeRepoMock = mock(UføretrygdRepository.class);
        when(uforeRepoMock.hentGrunnlag(anyLong())).thenReturn(Optional.empty());
        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), uforeRepoMock)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRett()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRettPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRettPerioder()).hasSize(1);
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderMottarUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().avklarAnnenforelderMottarUføretrygd()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    public void teste_lag_register_uføretrygd_ytelsefordeling_dto() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var uforeRepoMock = mock(UføretrygdRepository.class);
        when(uforeRepoMock.hentGrunnlag(anyLong())).thenReturn(Optional.empty());
        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), uforeRepoMock)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(true, LocalDate.now(), LocalDate.now()).build()));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderMottarUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().avklarAnnenforelderMottarUføretrygd()).isFalse();
    }

    @Test
    public void teste_lag_uavklart_register_uføretrygd_ytelsefordeling_dto() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var uforeRepoMock = mock(UføretrygdRepository.class);
        when(uforeRepoMock.hentGrunnlag(anyLong())).thenReturn(Optional.empty());
        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), uforeRepoMock)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null).build()));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderMottarUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().avklarAnnenforelderMottarUføretrygd()).isTrue();
    }

    @Test
    public void teste_lag_uavklart_register_uføretrygd_ytelsefordeling_tidligere_avklart_dto() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var uforeRepoMock = mock(UføretrygdRepository.class);
        when(uforeRepoMock.hentGrunnlag(anyLong())).thenReturn(Optional.empty());
        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), uforeRepoMock)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null).medOverstyrtUføretrygd(true).build()));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().annenforelderMottarUføretrygd()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().avklarAnnenforelderMottarUføretrygd()).isFalse();
    }

    private YtelseFordelingDtoTjeneste tjeneste() {
        return new YtelseFordelingDtoTjeneste(ytelseFordelingTjeneste, repositoryProvider.getFagsakRelasjonRepository(),
            uføretrygdRepository, førsteUttaksdatoTjeneste);
    }

    private Behandling opprettBehandling(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        // Arrange
        var termindato = LocalDate.now().plusWeeks(16);
        var rettighet = new OppgittRettighetEntitet(false, true, false, false);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            LocalDate.now().minusDays(20)).medOpprinneligEndringsdato(LocalDate.now().minusDays(20)).build();
        var periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(rettighet)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100())
            .medFordeling(new OppgittFordelingEntitet(List.of(periode_1, periode_2), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));

        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        return behandling;
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
