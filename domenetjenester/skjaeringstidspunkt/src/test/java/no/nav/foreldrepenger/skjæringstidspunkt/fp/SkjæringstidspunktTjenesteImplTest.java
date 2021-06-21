package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;

public class SkjæringstidspunktTjenesteImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SkjæringstidspunktUtils stputil = new SkjæringstidspunktUtils(Period.parse("P4M"), Period.parse("P1Y"));
    private UtsettelseCore2021 utsettelseCore2021 = new UtsettelseCore2021(LocalDate.now().minusMonths(1));

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, null, stputil, utsettelseCore2021);
    }

    @Test
    public void skal_finne_fud_søkt_uttak_periode_mor() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(3).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt);
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    @Test
    public void skal_finne_fud_grunnbeløp_søkt_uttak_periode_mor() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.plusDays(5), skjæringstidspunkt.plusWeeks(3).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt.plusDays(5));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    @Test
    public void skal_finne_fud_grunnbeløp_tidlig_fødsel() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(3).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        scenario.medBekreftetHendelse()
            .medFødselsDato(skjæringstidspunkt.minusWeeks(1), 1)
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(skjæringstidspunkt.plusWeeks(3))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor Dankel"));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt);
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusWeeks(1)));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt.minusWeeks(1));
    }

    @Test
    public void skal_finne_fud_søkt_uttak_periode_far_overføring() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(3).minusDays(1))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt, 1);
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunkt, 1);
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt);
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    @Test
    public void skal_finne_fud_søkt_uttak_periode_far_utsettelse() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder1 = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.plusWeeks(31), skjæringstidspunkt.plusWeeks(35).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.FERIE);
        var oppgittPeriodeBuilder2 = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.plusWeeks(36), skjæringstidspunkt.plusWeeks(46).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder1.build(), oppgittPeriodeBuilder2.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt, 1);
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunkt, 1);
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt.plusWeeks(36));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.plusWeeks(36)));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt.plusWeeks(36));
    }

    @Test
    public void skal_finne_fud_søkt_uttak_periode_far_før_fødsel() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.minusWeeks(2), skjæringstidspunkt.plusWeeks(8).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt, 1);
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunkt, 1);
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt.minusWeeks(2));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }


}
