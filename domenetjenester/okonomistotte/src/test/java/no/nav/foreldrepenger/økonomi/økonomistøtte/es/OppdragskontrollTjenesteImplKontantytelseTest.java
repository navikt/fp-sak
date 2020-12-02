package no.nav.foreldrepenger.økonomi.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.økonomi.økonomistøtte.FinnNyesteOppdragForSak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactoryProvider;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettBehandlingForOppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollManagerFactoryKontantytelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class OppdragskontrollTjenesteImplKontantytelseTest extends EntityManagerAwareTest {

    private static final String KODE_KLASSIFIK_FODSEL = "FPENFOD-OP";
    private static final String TYPE_SATS_ES = "ENG";

    private Repository repository;

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private LegacyESBeregningRepository beregningRepository;
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;

    private BehandlingVedtak behandlingVedtak;
    private Fagsak fagsak;
    private final PersonIdent personIdent = PersonIdent.fra("12345678901");

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        beregningRepository = new LegacyESBeregningRepository(entityManager);
        BehandlingVedtakRepository behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        FamilieHendelseRepository familieHendelseRepository = new FamilieHendelseRepository(entityManager);

        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        var tpsTjeneste = mock(PersoninfoAdapter.class);
        FinnNyesteOppdragForSak finnNyesteOppdragForSak = new FinnNyesteOppdragForSak(økonomioppdragRepository);
        var mapBehandlingInfo = new MapBehandlingInfoES(finnNyesteOppdragForSak, tpsTjeneste, beregningRepository,
            behandlingVedtakRepository, familieHendelseRepository);
        var oppdragskontrollEngangsstønad = new OppdragskontrollEngangsstønad(mapBehandlingInfo);
        RevurderingEndring revurderingEndring = mock(RevurderingEndring.class);

        var oppdragskontrollManagerFactory = new OppdragskontrollManagerFactoryKontantytelse(revurderingEndring,
            oppdragskontrollEngangsstønad);
        var providerMock = mock(OppdragskontrollManagerFactoryProvider.class);
        oppdragskontrollTjeneste = new OppdragskontrollTjenesteImpl(repositoryProvider, økonomioppdragRepository, providerMock);
        when(providerMock.getTjeneste(any(FagsakYtelseType.class))).thenReturn(oppdragskontrollManagerFactory);
        when(tpsTjeneste.hentFnrForAktør(any(AktørId.class))).thenReturn(personIdent);

    }

    @Test
    public void opprettOppdragTestES() {
        // Arrange
        final long prosessTaskId = 22L;
        Behandling behandling = opprettOgLagreBehandlingES();
        // Act
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), prosessTaskId)
            .get();

        // Assert
        verifiserOppdragskontroll(oppdragskontroll, prosessTaskId);
        List<Oppdrag110> oppdrag110Liste = verifiserOppdrag110(oppdragskontroll);
        verifiserAvstemming115(oppdrag110Liste);
        verifiserOppdragsenhet120(oppdrag110Liste);
        List<Oppdragslinje150> oppdragslinje150Liste = verifiserOppdragslinje150(oppdrag110Liste, behandling);
        verifiserAttestant180(oppdragslinje150Liste);
    }

    @Test
    public void hentOppdragskontrollTestES() {
        // Arrange
        Behandling behandling = opprettOgLagreBehandlingES();
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            behandling);
        Long oppdrkontrollId = økonomioppdragRepository.lagre(originaltOppdrag);
        assertThat(oppdrkontrollId).isNotNull();

        // Act
        Oppdragskontroll oppdrkontroll = økonomioppdragRepository.hentOppdragskontroll(oppdrkontrollId);

        // Assert
        assertThat(oppdrkontroll).isNotNull();
        assertThat(oppdrkontroll.getOppdrag110Liste()).hasSize(1);

        Oppdrag110 oppdrag110 = oppdrkontroll.getOppdrag110Liste().get(0);
        assertThat(oppdrag110).isNotNull();
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        assertThat(oppdrag110.getOppdragsenhet120Liste()).hasSize(1);
        assertThat(oppdrag110.getAvstemming115()).isNotNull();

        Oppdragslinje150 oppdrlinje150 = oppdrag110.getOppdragslinje150Liste().get(0);
        assertThat(oppdrlinje150).isNotNull();
        assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
        assertThat(oppdrlinje150.getAttestant180Liste()).hasSize(1);
        assertThat(oppdrlinje150.getAttestant180Liste().get(0)).isNotNull();
    }

    @Test
    public void innvilgelseSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        Behandling behandling = opprettOgLagreBehandlingES();
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            behandling);
        Oppdrag110 originaltOppdrag110 = originaltOppdrag.getOppdrag110Liste().get(0);

        // Arrange 2: Revurdering
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, 2);
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            revurdering);

        // Assert 2: Revurdering
        Oppdragslinje150 originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering, ØkonomiKodeEndring.UEND,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, ØkonomiKodeEndringLinje.NY, null,
            originalOppdragslinje150.getDelytelseId() + 1, originalOppdragslinje150.getDelytelseId(),
            originaltOppdrag110.getFagsystemId(), 2 * OpprettBehandlingForOppdrag.SATS);
    }

    @Test
    public void avslagSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        Behandling behandling = opprettOgLagreBehandlingES();
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            behandling);
        Oppdrag110 originaltOppdrag110 = originaltOppdrag.getOppdrag110Liste().get(0);

        // Arrange 2: Revurdering
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.AVSLAG, 0);

        // Act 2
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            revurdering);

        // Assert 2: Revurdering
        Oppdragslinje150 originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering, ØkonomiKodeEndring.UEND,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, ØkonomiKodeEndringLinje.ENDR, ØkonomiKodeStatusLinje.OPPH,
            originalOppdragslinje150.getDelytelseId(), null, null, OpprettBehandlingForOppdrag.SATS);
    }

    @Test
    public void zavslagSomReferererTilForrigeOppdragSomTilhørerFørsteRevurderingPåSammeSak() {
        // Act 1: Førstegangsbehandling
        Behandling behandling = opprettOgLagreBehandlingES(true);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Arrange 2: Første revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, 1);

        // Act 2
        Oppdragskontroll oppdragFørsteRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(
            oppdragskontrollTjeneste, førsteRevurdering);
        assertThat(oppdragFørsteRevurdering.getOppdrag110Liste()).hasSize(1);
        Oppdrag110 førsteRevurderingOpp110 = oppdragFørsteRevurdering.getOppdrag110Liste().get(0);

        // Arrange 3: Andre revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.AVSLAG, 1);

        // Act 3
        Oppdragskontroll oppdragAndreRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste,
            andreRevurdering);

        // Assert 3: Revurdering
        assertThat(førsteRevurderingOpp110.getOppdragslinje150Liste()).hasSize(1);
        Oppdragslinje150 førsteRevurderingOpp150 = førsteRevurderingOpp110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 andreRevurderingopp150 = verifiserOppdrag110(oppdragAndreRevurdering, ØkonomiKodeEndring.UEND,
            førsteRevurderingOpp110.getFagsystemId());
        assertThat(andreRevurderingopp150.getDatoVedtakFom()).isEqualTo(førsteRevurderingOpp150.getDatoVedtakFom());
        assertThat(andreRevurderingopp150.getDatoVedtakTom()).isEqualTo(førsteRevurderingOpp150.getDatoVedtakTom());
        assertThat(andreRevurderingopp150.getDatoStatusFom()).isEqualTo(førsteRevurderingOpp150.getDatoVedtakFom());

        verifiserOppdragslinje150(andreRevurderingopp150, ØkonomiKodeEndringLinje.ENDR, ØkonomiKodeStatusLinje.OPPH,
            førsteRevurderingOpp150.getDelytelseId(), null, null, OpprettBehandlingForOppdrag.SATS);
    }

    private Oppdragslinje150 verifiserOppdrag110(Oppdragskontroll oppdragskontroll,
                                                 ØkonomiKodeEndring kodeEndring,
                                                 Long fagsystemId) {
        assertThat(oppdragskontroll.getOppdrag110Liste()).hasSize(1);
        Oppdrag110 oppdrag110 = oppdragskontroll.getOppdrag110Liste().get(0);
        assertThat(oppdrag110.getKodeEndring()).isEqualTo(kodeEndring.name());
        assertThat(oppdrag110.getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        return oppdrag110.getOppdragslinje150Liste().get(0);
    }

    private void verifiserOppdragslinje150(Oppdragslinje150 oppdragslinje150,
                                           ØkonomiKodeEndringLinje kodeEndringLinje,
                                           ØkonomiKodeStatusLinje kodeStatusLinje,
                                           Long delYtelseId,
                                           Long refDelytelseId,
                                           Long refFagsystemId,
                                           long sats) {
        assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(kodeEndringLinje.name());
        if (kodeStatusLinje == null) {
            assertThat(oppdragslinje150.getKodeStatusLinje()).isNull();
        } else {
            assertThat(oppdragslinje150.getKodeStatusLinje()).isEqualTo(kodeStatusLinje.name());
        }
        assertThat(oppdragslinje150.getRefFagsystemId()).isEqualTo(refFagsystemId);
        assertThat(oppdragslinje150.getSats()).isEqualTo(sats);
        assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(refDelytelseId);
        assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(delYtelseId);
    }

    private void verifiserAttestant180(List<Oppdragslinje150> oppdragslinje150List) {
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            var attestant180Liste = oppdragslinje150.getAttestant180Liste();
            assertThat(attestant180Liste).hasSize(1);
            var attestant180 = attestant180Liste.get(0);
            assertThat(attestant180.getAttestantId()).isEqualTo(behandlingVedtak.getAnsvarligSaksbehandler());
        });
    }

    private List<Oppdragslinje150> verifiserOppdragslinje150(List<Oppdrag110> oppdrag110Liste, Behandling behandling) {
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110Liste.stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        LocalDate vedtaksdatoES = behandlingVedtak.getVedtaksdato();

        long løpenummer = 100L;
        for (Oppdrag110 oppdrag110 : oppdrag110Liste) {
            assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
            Oppdragslinje150 oppdragslinje150 = oppdragslinje150List.get(0);
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(ØkonomiKodeEndringLinje.NY.name());
            assertThat(oppdragslinje150.getVedtakId()).isEqualTo(vedtaksdatoES.toString());
            assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(
                concatenateValues(oppdrag110.getFagsystemId(), løpenummer));
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KODE_KLASSIFIK_FODSEL);
            assertThat(oppdragslinje150.getDatoVedtakFom()).isEqualTo(vedtaksdatoES);
            assertThat(oppdragslinje150.getDatoVedtakTom()).isEqualTo(vedtaksdatoES);
            assertThat(oppdragslinje150.getSats()).isEqualTo(getBehandlingsresultat(behandling).getBeregningResultat()
                .getSisteBeregning()
                .get()
                .getBeregnetTilkjentYtelse());
            assertThat(oppdragslinje150.getTypeSats()).isEqualTo(TYPE_SATS_ES);
            assertThat(oppdragslinje150.getHenvisning()).isEqualTo(behandling.getId());
            assertThat(oppdragslinje150.getUtbetalesTilId()).isEqualTo(personIdent.getIdent());
            assertThat(oppdragslinje150.getSaksbehId()).isEqualTo(behandlingVedtak.getAnsvarligSaksbehandler());
            assertThat(oppdragslinje150.getBrukKjoreplan()).isEqualTo("N");
            assertThat(oppdragslinje150.getOppdrag110()).isEqualTo(oppdrag110);
            assertThat(oppdragslinje150.getAttestant180Liste()).hasSize(1);
        }
        return oppdragslinje150List;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void verifiserAvstemming115(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            Avstemming115 avstemming115 = oppdrag110.getAvstemming115();
            assertThat(avstemming115).isNotNull();
            assertThat(avstemming115.getKodekomponent()).isEqualTo(ØkonomiKodekomponent.VLFP.getKodekomponent());
        });
    }

    private void verifiserOppdragsenhet120(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            var oppdragsenhet120List = oppdrag110.getOppdragsenhet120Liste();
            assertThat(oppdragsenhet120List).hasSize(1);
            Oppdragsenhet120 oppdragsenhet120 = oppdragsenhet120List.get(0);
            assertThat(oppdragsenhet120.getTypeEnhet()).isEqualTo("BOS");
            assertThat(oppdragsenhet120.getEnhet()).isEqualTo("8020");
            assertThat(oppdragsenhet120.getDatoEnhetFom()).isEqualTo(LocalDate.of(1900, 1, 1));
        });
    }

    private List<Oppdrag110> verifiserOppdrag110(Oppdragskontroll oppdragskontroll) {
        List<Oppdrag110> oppdrag110List = oppdragskontroll.getOppdrag110Liste();

        long initialLøpenummer = 100L;
        for (Oppdrag110 oppdrag110 : oppdrag110List) {
            assertThat(oppdrag110.getKodeAksjon()).isEqualTo(ØkonomiKodeAksjon.EN.getKodeAksjon());
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.NY.name());
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.REFUTG.name());
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(
                concatenateValues(Long.parseLong(fagsak.getSaksnummer().getVerdi()), initialLøpenummer++));
            assertThat(oppdrag110.getSaksbehId()).isEqualTo(behandlingVedtak.getAnsvarligSaksbehandler());
            assertThat(oppdrag110.getUtbetFrekvens()).isEqualTo(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens());
            assertThat(oppdrag110.getOppdragGjelderId()).isEqualTo(personIdent.getIdent());
            assertThat(oppdrag110.getOppdragskontroll()).isEqualTo(oppdragskontroll);
            assertThat(oppdrag110.getAvstemming115()).isNotNull();
        }

        return oppdrag110List;
    }

    private void verifiserOppdragskontroll(Oppdragskontroll oppdrskontroll, long prosessTaskId) {
        assertThat(oppdrskontroll.getSaksnummer()).isEqualTo(fagsak.getSaksnummer());
        assertThat(oppdrskontroll.getVenterKvittering()).isEqualTo(Boolean.TRUE);
        assertThat(oppdrskontroll.getProsessTaskId()).isEqualTo(prosessTaskId);
    }

    private Long concatenateValues(Long... values) {
        List<Long> valueList = List.of(values);
        String result = valueList.stream().map(Object::toString).collect(Collectors.joining());

        return Long.valueOf(result);
    }

    private Behandling opprettOgLagreRevurdering(Behandling originalBehandling,
                                                 VedtakResultatType vedtakResultatType,
                                                 int antallbarn) {

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)
                .medOriginalBehandlingId(originalBehandling.getId()))
            .build();

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);
        repositoryProvider.getFamilieHendelseRepository()
            .kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
        OpprettBehandlingForOppdrag.genererBehandlingOgResultat(revurdering, vedtakResultatType, antallbarn);
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(revurdering);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), behandlingLås);
        if (VedtakResultatType.INNVILGET.equals(vedtakResultatType)) {
            beregningRepository.lagre(behandlingsresultat.getBeregningResultat(), behandlingLås);
        }
        repository.lagre(behandlingsresultat);

        BehandlingVedtak behandlingVedtak = OpprettBehandlingForOppdrag.opprettBehandlingVedtak(revurdering,
            behandlingsresultat, vedtakResultatType);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, behandlingLås);
        repository.flush();

        return revurdering;
    }

    private Behandling opprettOgLagreBehandlingES() {
        return opprettOgLagreBehandlingES(false);
    }

    private Behandling opprettOgLagreBehandlingES(boolean vedtaksdatoFørIDag) {
        ScenarioMorSøkerEngangsstønad scenario = OpprettBehandlingForOppdrag.opprettBehandlingMedTermindato();

        Behandling behandling = scenario.lagre(repositoryProvider);
        fagsak = scenario.getFagsak();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        OpprettBehandlingForOppdrag.genererBehandlingOgResultat(behandling, VedtakResultatType.INNVILGET, 1);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        beregningRepository.lagre(behandlingsresultat.getBeregningResultat(), lås);
        repository.lagre(behandlingsresultat);

        behandlingVedtak = OpprettBehandlingForOppdrag.opprettBehandlingVedtak(behandling, behandlingsresultat,
            VedtakResultatType.INNVILGET, vedtaksdatoFørIDag);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);

        repository.flush();

        return behandling;
    }
}
