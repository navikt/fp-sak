package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonEventPubliserer;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class BeregnStønadskontoerTjenesteTest extends EntityManagerAwareTest {

    private UttakRepositoryProvider repositoryProvider;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(fagsakRelasjonRepository, FagsakRelasjonEventPubliserer.NULL_EVENT_PUB, fagsakRepository);
        uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());

    }

    @Test
    public void bådeMorOgFarHarRettTermin() {
        LocalDate termindato = LocalDate.now().plusMonths(4);
        Behandling behandling = opprettBehandlingForMor(AktørId.dummy());

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        ytelsesFordelingRepository.lagre(behandlingId, rettighet);
        var familieHendelse = FamilieHendelse.forFødsel(termindato, null, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider,fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        Optional<Stønadskontoberegning> stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().getSaksnummer()).getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        Set<Stønadskonto> stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(4);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(FamilieHendelser familieHendelser) {
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
    }

    @Test
    public void bådeMorOgFarHarRettFødsel() {
        LocalDate fødselsdato = LocalDate.now().minusWeeks(1);
        Behandling behandling = opprettBehandlingForMor(AktørId.dummy());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        ytelsesFordelingRepository.lagre(behandlingId, rettighet);

        // Act
        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider,fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        Optional<Stønadskontoberegning> stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().getSaksnummer()).getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        Set<Stønadskonto> stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(4);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE);
    }

    private UttakInput input(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag);
    }

    @Test
    public void morAleneomsorgFødsel() {
        LocalDate fødselsdato = LocalDate.now().minusWeeks(1);
        Behandling behandling = opprettBehandlingForMor(AktørId.dummy());

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk80();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, true);
        ytelsesFordelingRepository.lagre(behandlingId, rettighet);
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato,
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider,fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        Optional<Stønadskontoberegning> stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().getSaksnummer()).getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        Set<Stønadskonto> stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(2);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
    }

    @Test
    public void bareMorHarRettFødsel() {
        LocalDate fødselsdato = LocalDate.now().minusWeeks(1);
        Behandling behandling = opprettBehandlingForMor(AktørId.dummy());

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        ytelsesFordelingRepository.lagre(behandlingId, rettighet);
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato,
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider,fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        Optional<Stønadskontoberegning> stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().getSaksnummer()).getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        Set<Stønadskonto> stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(2);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
    }

    @Test
    public void barefarHarRettFødsel() {
        LocalDate fødselsdato = LocalDate.now().minusWeeks(1);
        Behandling behandling = opprettBehandlingForFar(AktørId.dummy());

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, true, false);
        ytelsesFordelingRepository.lagre(behandlingId, rettighet);
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato,
            List.of(new Barn()), 1);
        FamilieHendelser familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider,fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        Optional<Stønadskontoberegning> stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository().finnRelasjonFor(input.getBehandlingReferanse().getSaksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        Set<Stønadskonto> stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(1);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER);
    }

    private Behandling opprettBehandlingForMor(AktørId aktørId) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFar(AktørId aktørId) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        return scenario.lagre(repositoryProvider);
    }

}
