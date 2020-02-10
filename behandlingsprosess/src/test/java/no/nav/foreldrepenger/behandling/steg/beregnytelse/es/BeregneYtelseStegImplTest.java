package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class BeregneYtelseStegImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private Repository repository = repoRule.getRepository();
    @Inject
    @KonfigVerdi(value = "es.maks.stønadsalder.adopsjon", defaultVerdi = "15")
    private int maksStønadsalder;
    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(repoRule.getEntityManager());
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
        new RegisterInnhentingIntervall(Period.of(0, 1, 0), Period.of(0, 6, 0)));

    private BeregneYtelseEngangsstønadStegImpl beregneYtelseSteg;
    private Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();

    private BeregningSats sats;
    private BeregningSats sats2017;

    @Before
    public void oppsett() {
        repository.lagre(fagsak.getNavBruker());
        repository.lagre(fagsak);
        repository.flush();

        sats = repository.hentAlle(BeregningSats.class).stream()
            .filter(sats -> sats.getSatsType().equals(BeregningSatsType.ENGANG) &&
                sats.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(1))))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Feil i testdataoppsett"));

        sats2017 = repository.hentAlle(BeregningSats.class).stream()
            .filter(sats -> sats.getSatsType().equals(BeregningSatsType.ENGANG) &&
                sats.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2017, 10, 1), LocalDate.of(2017, 11, 1))))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Feil i testdataoppsett"));

        beregneYtelseSteg = new BeregneYtelseEngangsstønadStegImpl(repositoryProvider, beregningRepository, maksStønadsalder, skjæringstidspunktTjeneste);
    }

    @Test
    public void skal_beregne_sats_basert_på_antall_barn() {
        // Arrange
        int antallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, LocalDate.now());

        // Act
        beregneYtelseSteg.utførSteg(kontekst);

        // Assert
        LegacyESBeregningsresultat beregningResultat = getBehandlingsresultat(repository.hent(Behandling.class, kontekst.getBehandlingId())).getBeregningResultat();
        assertThat(beregningResultat.getSisteBeregning().get()).isNotNull();

        LegacyESBeregning beregning = beregningResultat.getSisteBeregning().get();
        assertThat(beregning.getSatsVerdi()).isEqualTo(sats.getVerdi());
        assertThat(beregning.getBeregnetTilkjentYtelse()).isEqualTo(sats.getVerdi() * antallBarn);
    }

    @Test
    public void skal_beregne_sats_for_fødsel_i_2017() {
        // Arrange
        int antallBarn = 2;
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagForFødsel(antallBarn, LocalDate.of(2017, 10, 1));

        // Act
        beregneYtelseSteg.utførSteg(kontekst);

        // Assert
        Behandling behandling = repository.hent(Behandling.class, kontekst.getBehandlingId());
        LegacyESBeregningsresultat beregningResultat = getBehandlingsresultat(behandling).getBeregningResultat();
        assertThat(beregningResultat.getSisteBeregning().get()).isNotNull();

        LegacyESBeregning beregning = beregningResultat.getSisteBeregning().get();
        assertThat(beregning.getSatsVerdi()).isEqualTo(sats2017.getVerdi());
        assertThat(beregning.getBeregnetTilkjentYtelse()).isEqualTo(sats2017.getVerdi() * antallBarn);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    @Test
    public void skal_ved_tilbakehopp_fremover_rydde_avklarte_fakta() {
        // Arrange
        int antallBarn = 1;
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(antallBarn, LocalDate.now());
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
            .medBeregning(new LegacyESBeregning(1000L, antallBarn, 1000L, LocalDateTime.now()))
            .buildFor(behandling);
        beregningRepository.lagre(beregningResultat, kontekst.getSkriveLås());

        // Act
        beregneYtelseSteg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(repository.hent(Behandling.class, kontekst.getBehandlingId()));
        assertThat(behandlingsresultat.getBeregningResultat().getBeregninger()).isEmpty();
    }

    @Test
    public void skal_ved_tilbakehopp_fremover_ikke_rydde_overstyrte_beregninger() {
        // Arrange
        int antallBarn = 1;
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(antallBarn, LocalDate.now());
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
            .medBeregning(new LegacyESBeregning(1000L, antallBarn, 1000L, LocalDateTime.now(), false, null))
            .medBeregning(new LegacyESBeregning(500L, antallBarn, 1000L, LocalDateTime.now(), true, 1000L))
            .buildFor(behandling);
        beregningRepository.lagre(beregningResultat, kontekst.getSkriveLås());

        // Act
        beregneYtelseSteg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(repository.hent(Behandling.class, kontekst.getBehandlingId()));
        assertThat(behandlingsresultat.getBeregningResultat().getBeregninger()).hasSize(2);
        assertThat(behandlingsresultat.getBeregningResultat().getBeregninger()).extracting(LegacyESBeregning::isOverstyrt)
            .contains(true)
            .contains(false); // en av hver
    }

    @Test
    public void skal_ved_fremhopp_rydde_avklarte_fakta_inkludert_overstyrte_beregninger() {
        // Arrange
        int antallBarn = 1;
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(antallBarn, LocalDate.now());
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
            .medBeregning(new LegacyESBeregning(1000L, antallBarn, 1000L, LocalDateTime.now(), false, null))
            .medBeregning(new LegacyESBeregning(500L, antallBarn, 1000L, LocalDateTime.now(), true, 1000L))
            .buildFor(behandling);
        beregningRepository.lagre(beregningResultat, kontekst.getSkriveLås());

        // Act
        beregneYtelseSteg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_FRAMOVER, null, null
        );

        // Assert
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(repository.hent(Behandling.class, kontekst.getBehandlingId()));
        assertThat(behandlingsresultat.getBeregningResultat().getSisteBeregning()).isEmpty();
    }

    private Tuple<Behandling, BehandlingskontrollKontekst> byggGrunnlag(int antallBarn, LocalDate fødselsdato) {
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        final FamilieHendelseBuilder søknadVersjon = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medFødselsDato(fødselsdato)
            .medAntallBarn(antallBarn);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadVersjon);
        final FamilieHendelseBuilder bekreftetVersjon = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medAntallBarn(antallBarn).tilbakestillBarn();
        IntStream.range(0, antallBarn).forEach(it -> bekreftetVersjon.medFødselsDato(fødselsdato));
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, bekreftetVersjon);
        SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        return new Tuple<>(behandling, kontekst);
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlagForFødsel(int antallBarn, LocalDate fødselsdato) {
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingskontekst = byggGrunnlag(antallBarn, fødselsdato);
        return behandlingskontekst.getElement2();
    }

}
