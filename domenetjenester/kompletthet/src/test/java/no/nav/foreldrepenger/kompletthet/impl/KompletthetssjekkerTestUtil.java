package no.nav.foreldrepenger.kompletthet.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class KompletthetssjekkerTestUtil {

    public static final AktørId AKTØR_ID  = AktørId.dummy();

    private final BehandlingRepositoryProvider repositoryProvider;
    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;

    public KompletthetssjekkerTestUtil(BehandlingRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    public ScenarioMorSøkerForeldrepenger opprettRevurderingsscenarioForMor() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
        var førstegangsbehandling = opprettOgAvsluttFørstegangsbehandling(scenario);
        settRelasjonPåFagsak(førstegangsbehandling.getFagsakId(), RelasjonsRolleType.MORA);

        return ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(AKTØR_ID)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING);
    }

    public ScenarioFarSøkerForeldrepenger opprettRevurderingsscenarioForFar() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID);
        var førstegangsbehandling = opprettOgAvsluttFørstegangsbehandling(scenario);
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
        var førstegangsbehandling = scenario.lagre(repositoryProvider);
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
        byggOppgittFordelingMedUtsettelse(behandling, UtsettelseÅrsak.ARBEID);
        byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, erEndringssøknad);
    }

    public void byggOgLagreSøknadMedEksisterendeOppgittFordeling(Behandling behandling, boolean erEndringssøknad) {
        byggOgLagreSøknadMedEksisterendeOppgittFordeling(behandling, erEndringssøknad, LocalDate.now());
    }

    public void byggOgLagreSøknadMedEksisterendeOppgittFordeling(Behandling behandling, boolean erEndringssøknad, LocalDate søknadsDato) {
        var oppgittFordeling = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getOppgittFordeling();
        Objects.requireNonNull(oppgittFordeling, "OppgittFordeling må være lagret på forhånd");

        byggFamilieHendelse(behandling.getId());
        var søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsDato)
            .medMottattDato(LocalDate.now())
            .medErEndringssøknad(erEndringssøknad)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }


    public void byggOgLagreFørstegangsSøknadMedMottattdato(Behandling behandling, LocalDate søknadsdato, LocalDate stp) {
        byggOppgittFordelingMedUtsettelse(behandling, stp, null);
        byggFamilieHendelse(behandling.getId());
        var søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true)
            .medSøknadsdato(søknadsdato)
            .medMottattDato(søknadsdato)
            .medErEndringssøknad(false)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }

    public void byggOppgittFordelingMedUtsettelse(Behandling behandling, Årsak utsettelseÅrsak) {
        byggOppgittFordelingMedUtsettelse(behandling, LocalDate.now(), utsettelseÅrsak);
    }

    private void byggOppgittFordelingMedUtsettelse(Behandling behandling, LocalDate stp, Årsak utsettelseÅrsak) {

        var builder = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(stp, stp.plusWeeks(10).minusDays(1));

        if (utsettelseÅrsak != null) {
            builder.medÅrsak(utsettelseÅrsak);
        }

        var fpPeriode = builder.build();
        var oppgittFordeling = new OppgittFordelingEntitet(List.of(fpPeriode), true);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private FamilieHendelseEntitet byggFamilieHendelse(Long behandlingId) {
        var søknadHendelse = repositoryProvider.getFamilieHendelseRepository().opprettBuilderForSøknad(behandlingId)
            .medAntallBarn(1)
            .medFødselsDato(LocalDate.now().minusDays(1));
        repositoryProvider.getFamilieHendelseRepository().lagreSøknadHendelse(behandlingId, søknadHendelse);
        return repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId).getSøknadVersjon();
    }
}
