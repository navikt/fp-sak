package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

import java.time.LocalDate;
import java.util.List;

public class AvklarFaktaTestUtil {

    private AvklarFaktaTestUtil() {
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
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        return scenario;
    }

}
