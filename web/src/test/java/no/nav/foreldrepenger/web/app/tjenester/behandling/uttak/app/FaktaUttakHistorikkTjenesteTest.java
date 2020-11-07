package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumerImpl;

public class FaktaUttakHistorikkTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);


    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        DokumentArkivTjeneste dokumentArkivTjeneste = new DokumentArkivTjeneste(mock(JournalConsumerImpl.class),
            repositoryProvider.getFagsakRepository());
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        historikkApplikasjonTjeneste = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),
            dokumentArkivTjeneste);
    }

    @Test
    public void skal_generere_historikkinnslag_ved_ny_søknadsperiode_avklar_fakta() {

        //Scenario med avklar fakta uttak
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(repositoryProvider);
        // Behandling
        Behandling behandling = AvklarFaktaTestUtil.opprettBehandling(scenario, getEntityManager());

        // dto
        FaktaUttakDto dto = AvklarFaktaTestUtil.opprettDtoAvklarFaktaUttakDto();

        tjeneste().byggHistorikkinnslag(dto.getBekreftedePerioder(), dto.getSlettedePerioder(), behandling, false);

        Historikkinnslag historikkinnslag = repositoryProvider.getHistorikkRepository()
            .hentHistorikk(behandling.getId())
            .get(0);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.UTTAK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(
                skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getAvklartSoeknadsperiode()).as("soeknadsperiode")
            .hasValueSatisfying(soeknadsperiode -> assertThat(soeknadsperiode.getNavn()).as("navn")
                .isEqualTo(HistorikkAvklartSoeknadsperiodeType.NY_SOEKNADSPERIODE.getKode()));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_ny_søknadsperiode_saksbehandler_overstyring() {

        //Scenario med manuell avklar fakta uttak
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING,
            BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(repositoryProvider);
        // Behandling
        Behandling behandling = AvklarFaktaTestUtil.opprettBehandling(scenario, getEntityManager());

        // dto
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = AvklarFaktaTestUtil.opprettDtoManuellAvklarFaktaUttakDto();

        tjeneste().byggHistorikkinnslag(dto.getBekreftedePerioder(), dto.getSlettedePerioder(), behandling, false);

        Historikkinnslag historikkinnslag = repositoryProvider.getHistorikkRepository()
            .hentHistorikk(behandling.getId())
            .get(0);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.UTTAK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(
                skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getAvklartSoeknadsperiode()).as("soeknadsperiode")
            .hasValueSatisfying(soeknadsperiode -> assertThat(soeknadsperiode.getNavn()).as("navn")
                .isEqualTo(HistorikkAvklartSoeknadsperiodeType.NY_SOEKNADSPERIODE.getKode()));
    }

    private FaktaUttakHistorikkTjeneste tjeneste() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        return new FaktaUttakHistorikkTjeneste(historikkApplikasjonTjeneste, null, ytelseFordelingTjeneste,
            inntektArbeidYtelseTjeneste);
    }
}
