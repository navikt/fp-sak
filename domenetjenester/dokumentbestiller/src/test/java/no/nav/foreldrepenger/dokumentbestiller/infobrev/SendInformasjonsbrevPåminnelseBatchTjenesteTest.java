package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class SendInformasjonsbrevPåminnelseBatchTjenesteTest {

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private ProsessTaskTjeneste taskTjenesteMock;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private InformasjonssakRepository repository;

    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private SendInformasjonsbrevPåminnelseBatchTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new SendInformasjonsbrevPåminnelseBatchTjeneste(repository, taskTjenesteMock);
    }

    @Test
    void skal_finne_en_sak_som_trenger_påminnelse_når_fødselsdatoen_er_innenfor_intervallet(EntityManager em) {
        opprettTestdata(em, LocalDate.now().plusDays(2));
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-1");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_fødselsdatoen_er_utenfor_intervallet(EntityManager em) {
        opprettTestdata(em, LocalDate.now().plusDays(20));
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_fødselsdatoen_er_før_første_oktober_2021(EntityManager em) {
        opprettTestdata(em, LocalDate.of(2021, 6, 2));
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 3)));
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_barnet_er_dødt(EntityManager em) {
        // Arrange
        var behandlingMor = opprettTestdata(em, LocalDate.now().plusDays(2)).behandlingMor;
        var fødtBarnInfo = new FødtBarnInfo.Builder()
            .medIdent(new PersonIdent("11"))
            .medFødselsdato(LocalDate.now().plusDays(2))
            .medDødsdato(LocalDate.now().plusDays(2))
            .build();
        familieHendelseTjeneste.oppdaterFødselPåGrunnlag(behandlingMor.getId(), of(fødtBarnInfo));

        // Act
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));

        // Assert
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_far_har_søkt(EntityManager em) {
        // Arrange
        var behandlingFar = opprettTestdata(em, LocalDate.now().plusDays(2)).behandlingFar;
        var søknad = new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now()).build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandlingFar, søknad);

        // Act
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));

        // Assert
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_mor_har_en_nyere_sak(EntityManager em) {
        // Arrange
        var behandlingMor = opprettTestdata(em, LocalDate.now().plusDays(2)).behandlingMor;
        var nyFagsakMor = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, behandlingMor.getFagsak().getNavBruker());
        repositoryProvider.getFagsakRepository().opprettNy(nyFagsakMor);

        // Act
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));

        // Assert
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    void skal_ikke_finne_en_sak_som_trenger_påminnelse_når_far_har_en_nyere_sak(EntityManager em) {
        // Arrange
        var behandlingFar = opprettTestdata(em, LocalDate.now().plusDays(2)).behandlingFar;
        var nyFagsakFar = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, behandlingFar.getFagsak().getNavBruker());
        repositoryProvider.getFagsakRepository().opprettNy(nyFagsakFar);

        // Act
        var resultat = tjeneste.launch(settOppBatchArguments(LocalDate.now(), LocalDate.now().plusDays(3)));

        // Assert
        assertThat(resultat).isEqualTo(SendInformasjonsbrevPåminnelseBatchTjeneste.BATCHNAVN + "-0");
    }

    private Properties settOppBatchArguments(LocalDate fom, LocalDate tom) {
        var arguments = new Properties();
        arguments.setProperty(BatchTjeneste.FOM_KEY, fom.toString());
        arguments.setProperty(BatchTjeneste.TOM_KEY, tom.toString());
        return arguments;
    }

    private MorOgFarBehandling opprettTestdata(EntityManager em, LocalDate fødselsdato) {
        var scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(fødselsdato.minusDays(40));
        var aktørIdFar = new AktørId("1111111111111");
        scenarioMor.medSøknadAnnenPart().medAktørId(aktørIdFar).medNavn("Bruker Brukersen").build();

        scenarioMor.medBekreftetHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1);

        scenarioMor.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandlingMor = scenarioMor.lagre(repositoryProvider);
        behandlingMor.avsluttBehandling();
        var lås = behandlingRepository.taSkriveLås(behandlingMor);
        behandlingRepository.lagre(behandlingMor, lås);

        var kontoMor = Stønadskontoberegning.builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE).medMaxDager(75).build())
            .medRegelInput("{ blablabla }").medRegelEvaluering("{ blablabla }");

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandlingMor.getFagsak());
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandlingMor.getFagsak(), kontoMor.build());

        var fagsakFar = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(aktørIdFar), new Saksnummer("9999"));
        repositoryProvider.getFagsakRepository().opprettNy(fagsakFar);
        var behandlingFar = Behandling.forFørstegangssøknad(fagsakFar).medBehandlendeEnhet(new OrganisasjonsEnhet("1000", "Enheten")).build();
        lås = behandlingRepository.taSkriveLås(behandlingFar);
        repositoryProvider.getBehandlingRepository().lagre(behandlingFar, lås);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingMor.getFagsak(), fagsakFar);

        var kontoFar = Stønadskontoberegning.builder()
            .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.FEDREKVOTE).medMaxDager(75).build())
            .medRegelInput("{ blablabla }").medRegelEvaluering("{ blablabla }");
        repositoryProvider.getFagsakRelasjonRepository().lagre(fagsakFar, kontoFar.build());

        em.flush();
        em.clear();
        return new MorOgFarBehandling(em.find(Behandling.class, behandlingMor.getId()), em.find(Behandling.class, behandlingFar.getId()));
    }

    private record MorOgFarBehandling(Behandling behandlingMor, Behandling behandlingFar) {}
}
