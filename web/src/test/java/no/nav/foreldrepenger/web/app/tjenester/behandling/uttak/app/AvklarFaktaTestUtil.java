package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Rule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;

public class AvklarFaktaTestUtil {
    @Rule
    public static UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private static YtelsesFordelingRepository fordelingRepository = new YtelsesFordelingRepository(repositoryRule.getEntityManager());

    private AvklarFaktaTestUtil() {
    }

    public static FaktaUttakDto opprettDtoAvklarFaktaUttakDto() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto1 = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11), true);
        bekreftetOppgittPeriodeDto1.setOrginalFom(LocalDate.now().minusDays(20));
        bekreftetOppgittPeriodeDto1.setOrginalTom(LocalDate.now().minusDays(11));
        bekreftetOppgittPeriodeDto1.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto2 = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(10), LocalDate.now(), true);
        bekreftetOppgittPeriodeDto2.setOrginalFom(LocalDate.now().minusDays(10));
        bekreftetOppgittPeriodeDto2.setOrginalTom(LocalDate.now());
        bekreftetOppgittPeriodeDto2.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto3 = getBekreftetUttakPeriodeDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), false);
        dto.setBekreftedePerioder(Arrays.asList(bekreftetOppgittPeriodeDto1, bekreftetOppgittPeriodeDto2, bekreftetOppgittPeriodeDto3));
        return dto;
    }

    public static OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto opprettDtoManuellAvklarFaktaUttakDto() {
        OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto = new OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto();
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto1 = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11), true);
        bekreftetOppgittPeriodeDto1.setOrginalFom(LocalDate.now().minusDays(20));
        bekreftetOppgittPeriodeDto1.setOrginalTom(LocalDate.now().minusDays(11));
        bekreftetOppgittPeriodeDto1.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto2 = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(10), LocalDate.now(), true);
        bekreftetOppgittPeriodeDto2.setOrginalFom(LocalDate.now().minusDays(10));
        bekreftetOppgittPeriodeDto2.setOrginalTom(LocalDate.now());
        bekreftetOppgittPeriodeDto2.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto3 = getBekreftetUttakPeriodeDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), false);
        dto.setBekreftedePerioder(Arrays.asList(bekreftetOppgittPeriodeDto1, bekreftetOppgittPeriodeDto2, bekreftetOppgittPeriodeDto3));
        return dto;
    }

    public static AvklarAnnenforelderHarRettDto opprettDtoAvklarAnnenforelderharIkkeRett() {
        AvklarAnnenforelderHarRettDto dto = new AvklarAnnenforelderHarRettDto("Har rett");
        dto.setAnnenforelderHarRett(true);
        return dto;
    }

    public static Behandling opprettBehandling(ScenarioMorSøkerForeldrepenger scenario) {

        Behandling behandling = scenario.getBehandling();

        final OppgittPeriodeEntitet periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        final OppgittPeriodeEntitet periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        fordelingRepository.lagre(behandling.getId(), new OppgittFordelingEntitet(Arrays.asList(periode_1, periode_2), true));
        return behandling;
    }

    public static ScenarioMorSøkerForeldrepenger opprettScenarioMorSøkerForeldrepenger() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, false, true);
        scenario.medOppgittRettighet(rettighet);
        return scenario;
    }

    static BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom, boolean bekreftet) {
        BekreftetOppgittPeriodeDto bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        OppgittPeriodeEntitet bekreftetperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        if(bekreftet) {
            KontrollerFaktaPeriodeLagreDto bekreftetFaktaPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.automatiskBekreftet(bekreftetperiode, UttakPeriodeVurderingType.PERIODE_OK), null)
                .build();
            bekreftetOppgittPeriodeDto.setBekreftetPeriode(bekreftetFaktaPeriodeDto);
        } else {
            KontrollerFaktaPeriodeLagreDto ubekreftetFaktaPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.ubekreftet(bekreftetperiode), null)
                .build();
            bekreftetOppgittPeriodeDto.setBekreftetPeriode(ubekreftetFaktaPeriodeDto);
        }
        return bekreftetOppgittPeriodeDto;
    }

}
