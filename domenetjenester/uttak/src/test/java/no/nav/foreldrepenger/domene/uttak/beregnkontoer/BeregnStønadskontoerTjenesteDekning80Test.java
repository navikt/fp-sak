package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.BARE_FAR_RETT;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FAR_RUNDT_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
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

class BeregnStønadskontoerTjenesteDekning80Test {

    private static final LocalDate IVERKSATT = LocalDate.of(2024, Month.JANUARY,1);
    private static final UttakCore2024 UTTAK_CORE_2024 = new UttakCore2024(IVERKSATT, IVERKSATT);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRepository(),
        repositoryProvider.getFagsakRelasjonRepository());
    private final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
    private final DekningsgradTjeneste dekningsgradTjeneste = new DekningsgradTjeneste(ytelsesFordelingRepository);

    @Test
    void bådeMorOgFarHarRettTermin() {
        var termindato = LocalDate.now().plusMonths(4);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(termindato, null, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        assertThat(stønadskontoberegning.get().getRegelInput()).contains("\"regelvalgsdato\" : null");

        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(5)
            .containsOnlyKeys(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE, FAR_RUNDT_FØDSEL);
        assertThat(stønadskontoer).containsEntry(FELLESPERIODE, 101);
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

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(5)
            .containsOnlyKeys(FORELDREPENGER_FØR_FØDSEL, MØDREKVOTE, FEDREKVOTE, FELLESPERIODE, FAR_RUNDT_FØDSEL);
        assertThat(stønadskontoer).containsEntry(FELLESPERIODE, 101);
    }

    private UttakInput input(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
    }

    @Test
    void morAleneomsorgFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(2)
            .containsOnlyKeys(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
        assertThat(stønadskontoer).containsEntry(FORELDREPENGER, 291);
    }

    @Test
    void bareMorHarRettFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForMor(AktørId.dummy());

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(2)
            .containsOnlyKeys(FORELDREPENGER_FØR_FØDSEL, FORELDREPENGER);
        assertThat(stønadskontoer).containsEntry(FORELDREPENGER, 291);
    }

    @Test
    void barefarHarRettFødsel() {
        var fødselsdato = LocalDate.now().minusWeeks(1);
        var behandling = opprettBehandlingForFar(AktørId.dummy());

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(3)
            .containsOnlyKeys(BARE_FAR_RETT, FAR_RUNDT_FØDSEL, FORELDREPENGER);
        assertThat(stønadskontoer).containsEntry(FORELDREPENGER, 261);
        assertThat(stønadskontoer).containsEntry(BARE_FAR_RETT, 50);
    }

    @Test
    void barefarHarRettFødsel_Eldre() {
        var fødselsdato = IVERKSATT.minusMonths(6);
        var behandling = opprettBehandlingForFar(AktørId.dummy());

        var dekningsgrad = Dekningsgrad._80;
        var behandlingId = behandling.getId();
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());

        var yf = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yf.build());

        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);

        // Act
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(repositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste,
            dekningsgradTjeneste, new StønadskontoRegelAdapter(UTTAK_CORE_2024));
        var input = input(behandling, fpGrunnlag(familieHendelser));
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);

        // Assert
        var stønadskontoberegning = repositoryProvider.getFagsakRelasjonRepository()
            .finnRelasjonFor(input.getBehandlingReferanse().saksnummer())
            .getStønadskontoberegning();
        assertThat(stønadskontoberegning).isPresent();
        var stønadskontoer = stønadskontoberegning.get().getStønadskontoutregning();

        assertThat(stønadskontoer)
            .hasSize(3)
            .containsOnlyKeys(BARE_FAR_RETT, FAR_RUNDT_FØDSEL, FORELDREPENGER);
        assertThat(stønadskontoer).containsEntry(FORELDREPENGER, 250);
        assertThat(stønadskontoer).containsEntry(BARE_FAR_RETT, 40);
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
