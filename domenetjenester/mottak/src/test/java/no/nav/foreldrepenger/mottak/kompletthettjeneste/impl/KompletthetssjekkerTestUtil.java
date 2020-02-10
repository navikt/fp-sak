package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl;

import static java.util.Collections.singletonList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.FPDateUtil;

public class KompletthetssjekkerTestUtil {

    public static final AktørId AKTØR_ID  = AktørId.dummy();
    public static final String ARBGIVER1 = "123456789";
    public static final String ARBGIVER2 = "234567890";

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    public KompletthetssjekkerTestUtil(BehandlingRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    public ScenarioMorSøkerForeldrepenger opprettRevurderingsscenarioForMor() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
        Behandling førstegangsbehandling = opprettOgAvsluttFørstegangsbehandling(scenario);
        settRelasjonPåFagsak(førstegangsbehandling.getFagsakId(), RelasjonsRolleType.MORA);

        ScenarioMorSøkerForeldrepenger scenario2 = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(AKTØR_ID)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING);
        return scenario2;
    }

    public ScenarioFarSøkerForeldrepenger opprettRevurderingsscenarioForFar() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
        Behandling førstegangsbehandling = opprettOgAvsluttFørstegangsbehandling(scenario);
        settRelasjonPåFagsak(førstegangsbehandling.getFagsakId(), RelasjonsRolleType.FARA);

        return ScenarioFarSøkerForeldrepenger.forFødselUtenSøknad(AKTØR_ID)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING);
    }

    private Behandling opprettOgAvsluttFørstegangsbehandling(AbstractTestScenario<?> scenario) {
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(7))
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navsdotter")
            .build();
        Behandling førstegangsbehandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(førstegangsbehandling);
        return førstegangsbehandling;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private void settRelasjonPåFagsak(Long fagsakId, RelasjonsRolleType relasjonsRolleType) {
        fagsakRepository.oppdaterRelasjonsRolle(fagsakId, relasjonsRolleType);
    }

    public void byggOgLagreSøknadMedNyOppgittFordeling(Behandling behandling, boolean erEndringssøknad) {
        byggOppgittFordeling(behandling, UtsettelseÅrsak.ARBEID, BigDecimal.valueOf(100), true, false, false);
        byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, erEndringssøknad);
    }

    public void byggOgLagreSøknadMedEksisterendeOppgittFordeling(Behandling behandling, boolean erEndringssøknad) {
        byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, erEndringssøknad, FPDateUtil.iDag());
    }

    public void byggOgLagreSøknadMedEksisterendeOppgittFordeling(Behandling behandling, boolean erEndringssøknad, LocalDate søknadsDato) {
        OppgittFordelingEntitet oppgittFordeling = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getOppgittFordeling();
        Objects.requireNonNull(oppgittFordeling, "OppgittFordeling må være lagret på forhånd"); // NOSONAR //$NON-NLS-1$

        byggFamilieHendelse(behandling);
        SøknadEntitet søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsDato)
            .medMottattDato(LocalDate.now())
            .medErEndringssøknad(erEndringssøknad)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }


    public void byggOgLagreFørstegangsSøknadMedMottattdato(Behandling behandling, LocalDate søknadsdato, LocalDate stp) {
        byggOppgittFordeling(behandling, stp, null, null, true, false, false);
        byggFamilieHendelse(behandling);
        SøknadEntitet søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsdato)
            .medMottattDato(søknadsdato)
            .medErEndringssøknad(false)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }

    public void byggOppgittFordeling(Behandling behandling, Årsak utsettelseÅrsak, BigDecimal arbeidsprosent, boolean erArbeidstaker, boolean erFrilanser, boolean erSelvstendig) {
        byggOppgittFordeling(behandling, FPDateUtil.iDag(), utsettelseÅrsak, arbeidsprosent, erArbeidstaker, erFrilanser, erSelvstendig);
    }

    private void byggOppgittFordeling(Behandling behandling, LocalDate stp,  Årsak utsettelseÅrsak, BigDecimal arbeidsprosent, boolean erArbeidstaker, boolean erFrilanser, boolean erSelvstendig) {

        OppgittPeriodeBuilder builder = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(stp, stp.plusWeeks(10).minusDays(1))
            .medErArbeidstaker(erArbeidstaker)
            .medErFrilanser(erFrilanser)
            .medErSelvstendig(erSelvstendig);

        if (utsettelseÅrsak != null) {
            builder.medÅrsak(utsettelseÅrsak);
        }
        if (arbeidsprosent != null) {
            builder.medArbeidsgiver(Arbeidsgiver.virksomhet(ARBGIVER1));
            builder.medArbeidsprosent(arbeidsprosent);
        }

        OppgittPeriodeEntitet fpPeriode = builder.build();
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(singletonList(fpPeriode), true);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), oppgittFordeling);
    }

    private FamilieHendelseEntitet byggFamilieHendelse(Behandling behandling) {
        FamilieHendelseBuilder søknadHendelse = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(LocalDate.now().minusDays(1));
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);
        return repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getSøknadVersjon();
    }
}
