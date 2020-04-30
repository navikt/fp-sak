package no.nav.foreldrepenger.jsonfeed;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererTjeneste;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.vedtak.exception.TekniskException;

public class HendelsePublisererTjenesteTest {

    private static final LocalDate INNVILGET_PERIODE_FØRSTE_DAG = VirkedagUtil.fomVirkedag(LocalDate.now());
    private static final LocalDate INNVILGET_PERIODE_SISTE_DAG = VirkedagUtil.tomVirkedag(LocalDate.now().plusMonths(3));
    private static final LocalDate AVSLÅTT_PERIODE_START = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(3));
    private static final LocalDate AVSLÅTT_PERIODE_SLUTT = VirkedagUtil.tomVirkedag(LocalDate.now().minusDays(1));
    private static final LocalDate NY_PERIODE_FØRSTE_DAG = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(3));
    private static final LocalDate NY_PERIODE_SISTE_DAG = VirkedagUtil.tomVirkedag(LocalDate.now().plusMonths(4));

    private static final String FAGSAK_PREFIX = "FS";
    private static final String VEDTAK_PREFIX = "VT";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private FeedRepository feedRepository = new FeedRepository(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = new BehandlingRepository(repoRule.getEntityManager());
    private BehandlingVedtakRepository vedtakRepo = new BehandlingVedtakRepository(repoRule.getEntityManager(), behandlingRepository);
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private EtterkontrollRepository etterkontrollRepository = new EtterkontrollRepository(repoRule.getEntityManager());

    private HendelsePublisererTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new HendelsePublisererTjeneste(etterkontrollRepository, repositoryProvider, feedRepository );
    }

    @Test
    public void skal_kaste_teknisk_feil_ved_manglende_uttaksplan() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-343184:Finner ikke noen relevant uttaksplan for vedtak");

        BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(false, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET,
            null, VedtakResultatType.INNVILGET);
        tjeneste.lagreVedtak(vedtak);
    }

    @Test
    public void skal_sette_første_og_siste_stønadsdag_lik_fom_på_første_periode_om_beh_er_opphørt(){
        BeregningsresultatEntitet berRes = lagBeregningsresultat(AVSLÅTT_PERIODE_START, AVSLÅTT_PERIODE_SLUTT, 0, 0);
        BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.REVURDERING, BehandlingResultatType.OPPHØR, berRes,
            VedtakResultatType.OPPHØR);
        tjeneste.lagreVedtak(vedtak);

        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        ForeldrepengerOpphoert opphørt = JsonMapper.fromJson(alle.get(0).getPayload(), ForeldrepengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(AVSLÅTT_PERIODE_START);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(AVSLÅTT_PERIODE_START);
        assertThat(opphørt.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

        @Test
        public void skal_lagre_ned_førstegangssøknad() {
            BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET,
                null, VedtakResultatType.INNVILGET);
            tjeneste.lagreVedtak(vedtak);

            List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
            Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

            assertThat(alle).hasSize(1);
            assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_INNVILGET.getType());
            assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
            ForeldrepengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), ForeldrepengerInnvilget.class);
            assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
            assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_FØRSTE_DAG);
            assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_SISTE_DAG);
            assertThat(innvilget.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        }

        @Test
        public void skal_ikke_lagre_ned_vedtak_som_ikke_endrer_stønadsperiode() {
            BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET,
                opprettNyttBerRes(), VedtakResultatType.INNVILGET);
            tjeneste.lagreVedtak(vedtak);

            List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
            assertThat(alle).isEmpty();
        }


        @Test
        public void skal_lagre_ned_revurdering_endret() {
            BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.REVURDERING, BehandlingResultatType.FORELDREPENGER_ENDRET,
                opprettNyttBerRes(), VedtakResultatType.INNVILGET);
            tjeneste.lagreVedtak(vedtak);

            List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
            Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

            assertThat(alle).hasSize(1);
            assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_ENDRET.getType());
            assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
            ForeldrepengerEndret endret = JsonMapper.fromJson(alle.get(0).getPayload(), ForeldrepengerEndret.class);
            assertThat(endret.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
            assertThat(endret.getFoersteStoenadsdag()).isEqualTo(NY_PERIODE_FØRSTE_DAG);
            assertThat(endret.getSisteStoenadsdag()).isEqualTo(NY_PERIODE_SISTE_DAG);
            assertThat(endret.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        }

        @Test
        public void skal_lagre_ned_revurdering_opphørt() {
            BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.REVURDERING, BehandlingResultatType.OPPHØR,
                opprettNyttBerRes(), VedtakResultatType.OPPHØR);
            tjeneste.lagreVedtak(vedtak);

            List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
            Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

            assertThat(alle).hasSize(1);
            assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_OPPHOERT.getType());
            assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
            ForeldrepengerOpphoert opphørt = JsonMapper.fromJson(alle.get(0).getPayload(), ForeldrepengerOpphoert.class);
            assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
            assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(NY_PERIODE_FØRSTE_DAG);
            assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(NY_PERIODE_SISTE_DAG);
            assertThat(opphørt.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        }

        @Test
        public void skal_ikkje_lagre_ned_beslutningsvedtak() {
            BehandlingVedtak vedtak = byggBehandlingVedtakOgBehandling(true, BehandlingType.REVURDERING, BehandlingResultatType.INGEN_ENDRING,
                opprettNyttBerRes(), VedtakResultatType.INNVILGET);
            tjeneste.lagreVedtak(vedtak);

            List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(FpVedtakUtgåendeHendelse.class);
            assertThat(alle).isEmpty();
        }

        private BehandlingVedtak byggBehandlingVedtakHenlagtSpesial() {
            ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
            scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
            final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
            familieHendelseBuilder.medAntallBarn(1)
                .medFødselsDato(LocalDate.now().minusMonths(10));
            Behandlingsresultat.Builder behandlingresultatBuilder = Behandlingsresultat.builder();
            behandlingresultatBuilder.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
            scenario.medBehandlingsresultat(behandlingresultatBuilder);

            Behandling behandling = scenario.lagre(repositoryProvider);
            behandling.avsluttBehandling();
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            BehandlingVedtak.Builder vedtakBuilder = scenario.medBehandlingVedtak();
            vedtakBuilder.medBehandlingsresultat(getBehandlingsresultat(behandling))
                .medVedtakstidspunkt(LocalDateTime.now().minusMonths(3))
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medAnsvarligSaksbehandler("Nav Navsdotter")
                .build();
            vedtakRepo.lagre(vedtakBuilder.build(), behandlingRepository.taSkriveLås(behandling));
            beregningsresultatRepository.lagre(behandling, lagBeregningsresultat(LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(1), 100, 100));

            Long behandlingId = behandling.getId();

            Behandling nyBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).medOriginalBehandling(behandling)).build();
            Behandlingsresultat.Builder nyttBehandlingresultatBuilder = Behandlingsresultat.builder();
            nyttBehandlingresultatBuilder.medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
            @SuppressWarnings("unused")
            Behandlingsresultat nyttBehandingsresultat = nyttBehandlingresultatBuilder.build();
            behandlingRepository.lagre(nyBehandling, behandlingRepository.taSkriveLås(nyBehandling));
            repoRule.getEntityManager().flush();

            Optional<BehandlingVedtak> hentBehandlingvedtakForBehandlingId = vedtakRepo.hentBehandlingvedtakForBehandlingId(behandlingId);
            return hentBehandlingvedtakForBehandlingId.orElse(null);
        }

    private BehandlingVedtak byggBehandlingVedtakOgBehandling(boolean opprBerRes, BehandlingType behandlingType, BehandlingResultatType behandlingResultatType,
                                                  BeregningsresultatEntitet nyttBerRes, VedtakResultatType nyttVedtakResultat) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medBehandlingType(behandlingType);
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        Behandlingsresultat.Builder behandlingresultatBuilder = Behandlingsresultat.builder();
        behandlingresultatBuilder.medBehandlingResultatType(behandlingResultatType);
        scenario.medBehandlingsresultat(behandlingresultatBuilder);

        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        BehandlingVedtak.Builder vedtakBuilder = scenario.medBehandlingVedtak();
        vedtakBuilder.medBehandlingsresultat(getBehandlingsresultat(behandling))
            .medVedtakstidspunkt(INNVILGET_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navsdotter")
            .build();
        vedtakRepo.lagre(vedtakBuilder.build(), behandlingRepository.taSkriveLås(behandling));

        if (opprBerRes) {
            beregningsresultatRepository.lagre(behandling, opprettNyttBerRes());
        }

        Long behandlingId = behandling.getId();
        if (nyttBerRes != null) {
            Behandling nyBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).medOriginalBehandling(behandling)).build();
            Behandlingsresultat.Builder nyttBehandlingresultatBuilder = Behandlingsresultat.builder();
            nyttBehandlingresultatBuilder.medBehandlingResultatType(behandlingResultatType);
            Behandlingsresultat nyttBehandingsresultat = nyttBehandlingresultatBuilder.buildFor(nyBehandling);
            nyBehandling.avsluttBehandling();
            behandlingRepository.lagre(nyBehandling, behandlingRepository.taSkriveLås(nyBehandling));
            BehandlingVedtak.Builder nyttVedtakBuilder = scenario.medBehandlingVedtak();
            nyttVedtakBuilder.medBehandlingsresultat(nyttBehandingsresultat)
                .medVedtakstidspunkt(INNVILGET_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
                .medVedtakResultatType(nyttVedtakResultat)
                .medAnsvarligSaksbehandler("Nav Navsdotter")
                .build();
            vedtakRepo.lagre(nyttVedtakBuilder.build(), behandlingRepository.taSkriveLås(nyBehandling));
            behandlingId = nyBehandling.getId();
            beregningsresultatRepository.lagre(nyBehandling, opprettOpphørtBerResultat());
        }

        repoRule.getEntityManager().flush();

        Optional<BehandlingVedtak> hentBehandlingvedtakForBehandlingId = vedtakRepo.hentBehandlingvedtakForBehandlingId(behandlingId);
        return hentBehandlingvedtakForBehandlingId.orElse(null);
    }

    private BeregningsresultatEntitet opprettOpphørtBerResultat() {
        return lagBeregningsresultat(INNVILGET_PERIODE_FØRSTE_DAG, INNVILGET_PERIODE_SISTE_DAG, 0, 0);
    }

    private BeregningsresultatEntitet opprettNyttBerRes() {
        return lagBeregningsresultat(NY_PERIODE_FØRSTE_DAG, NY_PERIODE_SISTE_DAG, 100, 100);
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, int dagsats, double utbetalingsgrad) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(utbetalingsgrad))
            .medStillingsprosent(BigDecimal.valueOf(utbetalingsgrad))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
    }
}
