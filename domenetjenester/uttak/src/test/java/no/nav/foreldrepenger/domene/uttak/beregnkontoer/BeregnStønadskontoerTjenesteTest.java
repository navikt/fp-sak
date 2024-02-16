package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
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
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class BeregnStønadskontoerTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRepository(), null,
        repositoryProvider.getFagsakRelasjonRepository());
    private final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());

    @Test
    void bådeMorOgFarHarRettTermin() {
        var termindato = LocalDate.now().plusMonths(4);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(termindato, null, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(4);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(FamilieHendelser familieHendelser) {
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
    }

    @Test
    void bådeMorOgFarHarRettFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(4);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType)
            .containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE);
    }

    private UttakInput input(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag);
    }

    @Test
    void morAleneomsorgFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = OppgittDekningsgradEntitet.bruk80();
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(2);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType).containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
    }

    @Test
    void bareMorHarRettFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(new OppgittRettighetEntitet(false, false, false, false, false))
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(2);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType).containsExactlyInAnyOrder(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
    }

    @Test
    void barefarHarRettFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForFar(AktørId.dummy());

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), Dekningsgrad.grad(dekningsgrad.getDekningsgrad()));

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(new OppgittRettighetEntitet(false, false, false, false, false))
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoer();

        assertThat(stønadskontoer).hasSize(1);
        assertThat(stønadskontoer).extracting(Stønadskonto::getStønadskontoType).containsExactlyInAnyOrder(FORELDREPENGER);
    }

    private Behandling opprettBehandlingForMor(AktørId aktørId) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingForFar(AktørId aktørId) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        return scenario.lagre(repositoryProvider);
    }

}
