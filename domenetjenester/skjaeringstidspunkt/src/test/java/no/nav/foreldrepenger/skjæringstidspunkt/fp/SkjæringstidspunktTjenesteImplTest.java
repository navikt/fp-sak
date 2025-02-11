package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;

class SkjæringstidspunktTjenesteImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MinsterettBehandling2022 minsterett2022;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        minsterett2022 = new MinsterettBehandling2022(repositoryProvider, fagsakRelasjonTjeneste);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, null, minsterett2022);
    }

    @Test
    void skal_finne_fud_søkt_uttak_periode_mor() {
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
        assertThat(stp.getFamilieHendelseDato().map(FamilieHendelseDato::familieHendelseDato).orElse(null)).isEqualTo(skjæringstidspunkt.plusWeeks(3));
    }

    @Test
    void skal_finne_fud_grunnbeløp_søkt_uttak_periode_mor() {
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
    void skal_finne_fud_grunnbeløp_tidlig_fødsel() {
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
    void skal_finne_fud_søkt_uttak_periode_far_overføring() {
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
    void skal_finne_fud_søkt_uttak_periode_far_utsettelse() {
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
    void skal_finne_fud_søkt_uttak_periode_far_før_fødsel_uten_termin() {
        var fødselsdato = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var førsteSøkteDato = VirkedagUtil.fomVirkedag(fødselsdato.minusWeeks(1));
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteSøkteDato, fødselsdato.plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1);
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1);
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(førsteSøkteDato);
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(førsteSøkteDato);
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(førsteSøkteDato);
    }

    @Test
    void skal_finne_fud_søkt_uttak_periode_far_før_termin() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.minusWeeks(1), skjæringstidspunkt.plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt, 1)
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(skjæringstidspunkt));
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunkt, 1)
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder().medTermindato(skjæringstidspunkt));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt.minusWeeks(1));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusWeeks(1)));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt.minusWeeks(1));
        assertThat(stp.getFamilieHendelseDato().map(FamilieHendelseDato::familieHendelseDato).orElse(null)).isEqualTo(skjæringstidspunkt);
        assertThat(stp.uttakSkalJusteresTilFødselsdato()).isFalse();
    }

    @Test
    void skal_finne_fud_søkt_uttak_periode_far_før_termin_juster_fødsel() {
        var termindato = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato, termindato.plusWeeks(2).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true, true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        scenario.medBekreftetHendelse().medFødselsDato(termindato.plusDays(1), 1)
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var forventetStp = VirkedagUtil.fomVirkedag(termindato.plusDays(1));
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(forventetStp);
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(forventetStp);
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(forventetStp);
        assertThat(stp.getFamilieHendelseDato().map(FamilieHendelseDato::familieHendelseDato).orElse(null)).isEqualTo(termindato.plusDays(1));
        assertThat(stp.uttakSkalJusteresTilFødselsdato()).isTrue();
    }

    @Test
    void skal_finne_fud_søkt_uttak_periode_far_før_fødsel_termin_avkorter_fud_beregning() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(1L).minusDays(1L);
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunkt.minusWeeks(3), skjæringstidspunkt.plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder.build()), true));
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt, 1)
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(skjæringstidspunkt));
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunkt, 1)
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder().medTermindato(skjæringstidspunkt));
        var behandling = scenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunkt.minusWeeks(3));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusWeeks(2)));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt.minusWeeks(2));
    }

    @Test
    void skal_finne_fud_søkt_utsettelse_fra_start() {
        // Sikre fritt uttak
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, null, minsterett2022);


        var skjæringstidspunktOriginal = VirkedagUtil.fomVirkedag(YearMonth.from(LocalDate.now()).atEndOfMonth().plusDays(1));
        var skjæringstidspunktEndring = skjæringstidspunktOriginal.plusWeeks(2);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var uttakFar = new UttakResultatPeriodeEntitet.Builder(skjæringstidspunktOriginal,
            VirkedagUtil.tomVirkedag(skjæringstidspunktOriginal.plusWeeks(6).minusDays(1)))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakFar, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN)).build();
        perioder.leggTilPeriode(uttakFar);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.medUttak(perioder);
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunktOriginal.minusMonths(6), 1);
        scenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunktOriginal.minusMonths(6), 1);
        var behandling = scenario.lagre(repositoryProvider);

        var oppgittPeriodeBuilder1 = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunktOriginal, VirkedagUtil.tomVirkedag(skjæringstidspunktOriginal.plusWeeks(6).minusDays(1)))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.FRI);
        var oppgittPeriodeBuilder2 = OppgittPeriodeBuilder.ny()
            .medPeriode(skjæringstidspunktEndring, VirkedagUtil.tomVirkedag(skjæringstidspunktEndring.plusWeeks(6).minusDays(1)))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE);
        var revurderingScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeBuilder1.build(), oppgittPeriodeBuilder2.build()), true));
        revurderingScenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medSøknadHendelse().medFødselsDato(skjæringstidspunktOriginal.minusMonths(6), 1);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(skjæringstidspunktOriginal.minusMonths(6), 1);

        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId());
        assertThat(stp.getFørsteUttaksdato()).isEqualTo(skjæringstidspunktOriginal.plusWeeks(2));
        assertThat(stp.getFørsteUttaksdatoGrunnbeløp()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunktOriginal.plusWeeks(2)));
        assertThat(stp.getUtledetSkjæringstidspunkt()).isEqualTo(skjæringstidspunktOriginal.plusWeeks(2));
    }


}
