package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.FaktaUttakOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;

@CdiDbAwareTest
public class FaktaUttakOppdatererTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING;

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;

    private KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste = mock(KontrollerFaktaUttakTjeneste.class);
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste = mock(HistorikkTjenesteAdapter.class);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FaktaUttakOppdaterer faktaUttakOppdaterer;

    @BeforeEach
    public void setup() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        var faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(historikkApplikasjonTjeneste,
                arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste);
        this.faktaUttakOppdaterer = new FaktaUttakOppdaterer(uttakInputTjeneste,
                kontrollerFaktaUttakTjeneste,
                kontrollerOppgittFordelingTjeneste,
                faktaUttakHistorikkTjeneste);
    }

    @Test
    public void skal_generere_historikkinnslag_ved_slettet_søknadsperiode(EntityManager entityManager) {

        // Scenario med avklar fakta uttak
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(behandlingRepositoryProvider);
        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(entityManager, behandling.getId());
        // dto
        var dto = opprettDtoAvklarFaktaUttakDto();
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());

        faktaUttakOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkApplikasjonTjeneste).lagInnslag(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.UTTAK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
                .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getResultat()).isEmpty();
        assertThat(del.getAvklartSoeknadsperiode()).as("soeknadsperiode")
                .hasValueSatisfying(soeknadsperiode -> assertThat(soeknadsperiode.getNavn()).as("navn")
                        .isEqualTo(HistorikkAvklartSoeknadsperiodeType.SLETTET_SOEKNASPERIODE.getKode()));
    }

    @Test
    public void skal_fjerne_saksbehandling_overstyrings_aksjonspunkt_hvis_finnes(EntityManager entityManager) {

        // Scenario med både avklar fakta uttak og manuell avklar fakta uttak
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(behandlingRepositoryProvider);
        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(entityManager, behandling.getId());
        // dto
        var dto = opprettDtoAvklarFaktaUttakDto();
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());

        var resultat = faktaUttakOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        var ikkeFinnes = resultat.getEkstraAksjonspunktResultat().stream()
                .noneMatch(aer -> aer.aksjonspunktResultat().equals(AKSJONSPUNKT_DEF) && !AksjonspunktStatus.AVBRUTT.equals(aer.aksjonspunktStatus()));

        assertThat(ikkeFinnes).isTrue();
    }

    @Test
    public void skal_avbryte_overstyrings_aksjonspunkt_hvis_finnes(EntityManager entityManager) {

        // Scenario med både avklar fakta uttak og manuell avklar fakta uttak
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
                BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK,
                BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(behandlingRepositoryProvider);
        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(entityManager, behandling.getId());

        // dto
        var dto = opprettDtoAvklarFaktaUttakDto();

        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        var resultat = faktaUttakOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        var finnesAvbrutt = resultat.getEkstraAksjonspunktResultat().stream()
                .anyMatch(aer -> aer.aksjonspunktResultat().getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK)
                        && AksjonspunktStatus.AVBRUTT.equals(aer.aksjonspunktStatus()));

        assertThat(finnesAvbrutt).isTrue();
    }

    @Test
    public void skalLagreOppgittPeriodeMedPrivatpersonSomArbeidsgiver() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(LocalDate.now(), LocalDate.now())
                .build()), false))
                .medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(behandlingRepositoryProvider);

        FaktaUttakDto avklarFaktaDto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var bekreftetDto = new BekreftetOppgittPeriodeDto();
        var arbeidsgiverDto = new ArbeidsgiverLagreDto(behandling.getAktørId());
        bekreftetDto.setBekreftetPeriode(new KontrollerFaktaPeriodeLagreDto.Builder()
                .medPeriode(LocalDate.now(), LocalDate.now())
                .medUttakPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medArbeidstidsprosent(BigDecimal.TEN)
                .medArbeidsgiver(arbeidsgiverDto)
                .medMottattDato(LocalDate.now())
                .build());
        avklarFaktaDto.setBekreftedePerioder(List.of(bekreftetDto));
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(avklarFaktaDto.getAksjonspunktDefinisjon());

        faktaUttakOppdaterer.oppdater(avklarFaktaDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, avklarFaktaDto));

        var lagretPerioder = behandlingRepositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId())
                .getGjeldendeSøknadsperioder().getOppgittePerioder();
        assertThat(lagretPerioder).hasSize(1);
        assertThat(lagretPerioder.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(arbeidsgiverDto.getAktørId().getId());
        assertThat(lagretPerioder.get(0).getArbeidsgiver().getAktørId().getId()).isEqualTo(arbeidsgiverDto.getAktørId().getId());
        assertThat(lagretPerioder.get(0).getArbeidsgiver().getErVirksomhet()).isFalse();
    }

    private FaktaUttakDto opprettDtoAvklarFaktaUttakDto() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var bekreftetOppgittPeriodeDto = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(11));
        var slettetPeriodeDto = new SlettetUttakPeriodeDto();
        slettetPeriodeDto.setBegrunnelse("ugyldig søknadsperiode");
        slettetPeriodeDto.setUttakPeriodeType(UttakPeriodeType.FORELDREPENGER);
        slettetPeriodeDto.setFom(LocalDate.now().minusDays(10));
        slettetPeriodeDto.setTom(LocalDate.now());
        dto.setSlettedePerioder(List.of(slettetPeriodeDto));
        dto.setBekreftedePerioder(List.of(bekreftetOppgittPeriodeDto));
        return dto;
    }

    private BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom) {
        var bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        var bekreftetperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var bekreftetPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(
                KontrollerFaktaPeriode.ubekreftet(bekreftetperiode),
                null)
                        .build();
        bekreftetOppgittPeriodeDto.setBekreftetPeriode(bekreftetPeriodeDto);
        bekreftetOppgittPeriodeDto.setOrginalFom(fom);
        bekreftetOppgittPeriodeDto.setOrginalTom(tom);
        return bekreftetOppgittPeriodeDto;
    }
}
