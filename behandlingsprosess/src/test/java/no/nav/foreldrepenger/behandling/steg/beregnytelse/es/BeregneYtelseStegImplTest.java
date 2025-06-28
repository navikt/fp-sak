package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
class BeregneYtelseStegImplTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SatsRepository satsRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private EntityManager entityManager;
    @Inject
    @KonfigVerdi(value = "es.maks.stønadsalder.adopsjon", defaultVerdi = "15")
    private int maksStønadsalder = 15;
    private EngangsstønadBeregningRepository beregningRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BeregneYtelseEngangsstønadStegImpl beregneYtelseSteg;
    private Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();
    private BeregningSats sats;
    private BeregningSats sats2017;

    @BeforeEach
    public void oppsett(EntityManager em) {
        entityManager = em;
        satsRepository = new SatsRepository(em);
        beregningRepository = new EngangsstønadBeregningRepository(em);
        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
        entityManager.persist(fagsak.getNavBruker());
        entityManager.persist(fagsak);
        entityManager.flush();

        sats = satsRepository.finnGjeldendeSats(BeregningSatsType.ENGANG);

        sats2017 = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.of(2017, 10, 1));

        beregneYtelseSteg = new BeregneYtelseEngangsstønadStegImpl(repositoryProvider, beregningRepository, maksStønadsalder, satsRepository,
                skjæringstidspunktTjeneste);
    }

    @Test
    void skal_beregne_sats_basert_på_antall_barn() {
        // Arrange
        var antallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, LocalDate.now());

        // Act
        var behandling = entityManager.find(Behandling.class, kontekst.getBehandlingId());
        beregneYtelseSteg.utførSteg(kontekst);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Assert
        var beregningResultat = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        assertThat(beregningResultat).isPresent();

        var beregning = beregningResultat.get();
        assertThat(beregning.getSatsVerdi()).isEqualTo(sats.getVerdi());
        assertThat(beregning.getBeregnetTilkjentYtelse()).isEqualTo(sats.getVerdi() * antallBarn);
    }

    @Test
    void skal_beregne_sats_for_fødsel_i_2017() {
        // Arrange
        var antallBarn = 2;
        var kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, LocalDate.of(2017, 10, 1));

        // Act
        var behandling = entityManager.find(Behandling.class, kontekst.getBehandlingId());
        beregneYtelseSteg.utførSteg(kontekst);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Assert
        behandling = entityManager.find(Behandling.class, kontekst.getBehandlingId());
        var beregningResultat = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        assertThat(beregningResultat).isPresent();

        var beregning = beregningResultat.get();
        assertThat(beregning.getSatsVerdi()).isEqualTo(sats2017.getVerdi());
        assertThat(beregning.getBeregnetTilkjentYtelse()).isEqualTo(sats2017.getVerdi() * antallBarn);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }

    @Test
    void skal_ved_tilbakehopp_fremover_rydde_avklarte_fakta() {
        // Arrange
        var antallBarn = 1;
        var behandling = byggGrunnlag(antallBarn, LocalDate.now());
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        beregningRepository.lagre(behandling.getId(), new EngangsstønadBeregning(behandling.getId(), 1000L, antallBarn, 1000L, LocalDateTime.now()));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Act
        beregneYtelseSteg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        var beregning = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        assertThat(beregning).isEmpty();
    }

    @Test
    void skal_ved_fremhopp_rydde_avklarte_fakta_inkludert_beregninger() {
        // Arrange
        var antallBarn = 1;
        var behandling = byggGrunnlag(antallBarn, LocalDate.now());
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        beregningRepository.lagre(behandling.getId(), new EngangsstønadBeregning(behandling.getId(), 1000L, antallBarn, 1000L, LocalDateTime.now()));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Act
        beregneYtelseSteg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_FRAMOVER, null, null);

        // Assert
        var beregning = beregningRepository.hentEngangsstønadBeregning(behandling.getId());
        assertThat(beregning).isEmpty();
    }

    private Behandling byggGrunnlag(int antallBarn, LocalDate fødselsdato) {
        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var søknadVersjon = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderForSøknad(behandling.getId())
            .medFødselsDato(fødselsdato, antallBarn)
            .medAntallBarn(antallBarn);
        repositoryProvider.getFamilieHendelseRepository().lagreSøknadHendelse(behandling.getId(), søknadVersjon);
        var bekreftetVersjon = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderForRegister(behandling.getId())
            .medAntallBarn(antallBarn)
            .tilbakestillBarn();
        IntStream.range(0, antallBarn).forEach(it -> bekreftetVersjon.leggTilBarn(fødselsdato));
        repositoryProvider.getFamilieHendelseRepository().lagreRegisterHendelse(behandling.getId(), bekreftetVersjon);
        var søknad = new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        return behandling;
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlagForFødsel(int antallBarn, LocalDate fødselsdato) {
        var behandling = byggGrunnlag(antallBarn, fødselsdato);
        var lås = behandlingRepository.taSkriveLås(behandling);
        return behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
    }

}
