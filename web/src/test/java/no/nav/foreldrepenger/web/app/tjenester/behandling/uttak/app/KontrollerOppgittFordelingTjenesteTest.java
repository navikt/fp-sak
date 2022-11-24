package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil.getBekreftetUttakPeriodeDto;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjenesteImpl;

public class KontrollerOppgittFordelingTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
    }

    @Test
    public void skal_lagre_overstyrt_perioder_bekreft_aksjonspunkt() {

        //Scenario med avklar fakta uttak
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);
        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());

        var dto = AvklarFaktaTestUtil.opprettDtoAvklarFaktaUttakDto();
        tjeneste().bekreftOppgittePerioder(dto.getBekreftedePerioder(), behandling);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat).isNotNull();
        var gjeldendeFordeling = ytelseFordelingAggregat
            .getGjeldendeFordeling()
            .getPerioder();

        assertThat(gjeldendeFordeling).isNotEmpty();
        assertThat(gjeldendeFordeling).hasSize(3);
    }

    @Test
    public void skal_lagre_overstyrt_perioder_overstyrings_aksjonspunkt() {

        //Scenario med avklar fakta uttak
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING,
            BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);
        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());

        var dto = AvklarFaktaTestUtil.opprettDtoManuellAvklarFaktaUttakDto();
        tjeneste().bekreftOppgittePerioder(dto.getBekreftedePerioder(), behandling);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat).isNotNull();
        var gjeldendeFordeling = ytelseFordelingAggregat
            .getGjeldendeFordeling()
            .getPerioder();

        assertThat(gjeldendeFordeling).isNotEmpty();
        assertThat(gjeldendeFordeling).hasSize(3);
    }

    @Test
    public void skal_lagre_ny_endringsdato_hvis_første_dato_bekreftet_perioder_er_før_første_uttaksdato_opprinnelige_perioder() {
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            BehandlingStegType.VURDER_UTTAK);
        var opprinneligDato = LocalDate.of(2019, 3, 25);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(opprinneligDato)
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        var opprinneligPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(opprinneligDato, opprinneligDato.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var gjeldendePerioder = List.of(opprinneligPeriode);
        scenario.medFordeling(new OppgittFordelingEntitet(gjeldendePerioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());

        var nyDato = LocalDate.of(2019, 2, 10);
        var bekreftetOppgittPeriodeDto = getBekreftetUttakPeriodeDto(nyDato, nyDato.plusDays(1),
            true);
        bekreftetOppgittPeriodeDto.setOrginalFom(opprinneligPeriode.getFom());
        bekreftetOppgittPeriodeDto.setOrginalTom(opprinneligPeriode.getTom());
        bekreftetOppgittPeriodeDto.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste = behandling1 -> Optional.of(LocalDate.of(2019, 1, 1));
        tjeneste(førsteUttaksdatoTjeneste).bekreftOppgittePerioder(List.of(bekreftetOppgittPeriodeDto), behandling);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());

        assertThat(ytelseFordelingAggregat.getAvklarteDatoer().get().getGjeldendeEndringsdato()).isEqualTo(nyDato);
        assertThat(ytelseFordelingAggregat.getAvklarteDatoer().get().getJustertEndringsdato()).isEqualTo(nyDato);
        assertThat(ytelseFordelingAggregat.getAvklarteDatoer().get().getOpprinneligEndringsdato()).isEqualTo(
            opprinneligDato);
    }

    private KontrollerOppgittFordelingTjeneste tjeneste() {
        return tjeneste(new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository())));
    }

    private KontrollerOppgittFordelingTjeneste tjeneste(FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste) {
        return new KontrollerOppgittFordelingTjeneste(ytelseFordelingTjeneste, repositoryProvider,
            førsteUttaksdatoTjeneste);
    }

}
