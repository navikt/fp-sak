package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;

public class AvklarFaktaTestUtil {

    private AvklarFaktaTestUtil() {
    }

    public static FaktaUttakDto opprettDtoAvklarFaktaUttakDto() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var bekreftetOppgittPeriodeDto1 = getBekreftetUttakPeriodeDto(
            LocalDate.now().minusDays(20), LocalDate.now().minusDays(11), true);
        bekreftetOppgittPeriodeDto1.setOrginalFom(LocalDate.now().minusDays(20));
        bekreftetOppgittPeriodeDto1.setOrginalTom(LocalDate.now().minusDays(11));
        bekreftetOppgittPeriodeDto1.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        var bekreftetOppgittPeriodeDto2 = getBekreftetUttakPeriodeDto(
            LocalDate.now().minusDays(10), LocalDate.now(), true);
        bekreftetOppgittPeriodeDto2.setOrginalFom(LocalDate.now().minusDays(10));
        bekreftetOppgittPeriodeDto2.setOrginalTom(LocalDate.now());
        bekreftetOppgittPeriodeDto2.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        var bekreftetOppgittPeriodeDto3 = getBekreftetUttakPeriodeDto(
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), false);
        dto.setBekreftedePerioder(
            List.of(bekreftetOppgittPeriodeDto1, bekreftetOppgittPeriodeDto2, bekreftetOppgittPeriodeDto3));
        return dto;
    }

    public static OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto opprettDtoManuellAvklarFaktaUttakDto() {
        var dto = new OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto();
        var bekreftetOppgittPeriodeDto1 = getBekreftetUttakPeriodeDto(
            LocalDate.now().minusDays(20), LocalDate.now().minusDays(11), true);
        bekreftetOppgittPeriodeDto1.setOrginalFom(LocalDate.now().minusDays(20));
        bekreftetOppgittPeriodeDto1.setOrginalTom(LocalDate.now().minusDays(11));
        bekreftetOppgittPeriodeDto1.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        var bekreftetOppgittPeriodeDto2 = getBekreftetUttakPeriodeDto(
            LocalDate.now().minusDays(10), LocalDate.now(), true);
        bekreftetOppgittPeriodeDto2.setOrginalFom(LocalDate.now().minusDays(10));
        bekreftetOppgittPeriodeDto2.setOrginalTom(LocalDate.now());
        bekreftetOppgittPeriodeDto2.setOriginalResultat(UttakPeriodeVurderingType.PERIODE_OK);
        var bekreftetOppgittPeriodeDto3 = getBekreftetUttakPeriodeDto(
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(10), false);
        dto.setBekreftedePerioder(
            List.of(bekreftetOppgittPeriodeDto1, bekreftetOppgittPeriodeDto2, bekreftetOppgittPeriodeDto3));
        return dto;
    }

    public static AvklarAnnenforelderHarRettDto opprettDtoAvklarAnnenforelderharIkkeRett() {
        var dto = new AvklarAnnenforelderHarRettDto("Har rett");
        dto.setAnnenforelderHarRett(true);
        return dto;
    }

    public static void opprettBehandlingGrunnlag(EntityManager entityManager, Long behandlingId) {

        var periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var fordeling = new OppgittFordelingEntitet(List.of(periode_1, periode_2), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittFordeling(fordeling);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        var uttaksperiodegrense = new Uttaksperiodegrense(LocalDate.now().minusMonths(1));
        new UttaksperiodegrenseRepository(entityManager).lagre(behandlingId, uttaksperiodegrense);
    }

    public static ScenarioMorSøkerForeldrepenger opprettScenarioMorSøkerForeldrepenger() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var rettighet = new OppgittRettighetEntitet(false, true, false, false);
        scenario.medOppgittRettighet(rettighet);
        return scenario;
    }

    static BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom, boolean bekreftet) {
        var bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        var bekreftetperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        if (bekreftet) {
            var bekreftetFaktaPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(
                KontrollerFaktaPeriode.automatiskBekreftet(bekreftetperiode, UttakPeriodeVurderingType.PERIODE_OK),
                null).build();
            bekreftetOppgittPeriodeDto.setBekreftetPeriode(bekreftetFaktaPeriodeDto);
        } else {
            var ubekreftetFaktaPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(
                KontrollerFaktaPeriode.ubekreftet(bekreftetperiode), null).build();
            bekreftetOppgittPeriodeDto.setBekreftetPeriode(ubekreftetFaktaPeriodeDto);
        }
        return bekreftetOppgittPeriodeDto;
    }

}
