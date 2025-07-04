package no.nav.foreldrepenger.jsonfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.HendelseCriteria;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class HendelsePublisererTjenesteTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    private static final LocalDate INNVILGET_PERIODE_FØRSTE_DAG = VirkedagUtil.fomVirkedag(LocalDate.now());
    private static final LocalDate INNVILGET_PERIODE_SISTE_DAG = VirkedagUtil.tomVirkedag(
        LocalDate.now().plusMonths(3));
    private static final LocalDate AVSLÅTT_PERIODE_START = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(3));
    private static final LocalDate AVSLÅTT_PERIODE_SLUTT = VirkedagUtil.tomVirkedag(LocalDate.now().minusDays(1));
    private static final LocalDate NY_PERIODE_FØRSTE_DAG = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(3));
    private static final LocalDate NY_PERIODE_SISTE_DAG = VirkedagUtil.tomVirkedag(LocalDate.now().plusMonths(4));

    private static final String VEDTAK_PREFIX = "VT";
    private static final String FNR = "12345678901";

    private FeedRepository feedRepository;
    private HendelsePublisererTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        feedRepository = new FeedRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        when(personinfoAdapter.hentFnr(any())).thenReturn(Optional.of(new PersonIdent(FNR)));

        tjeneste = new HendelsePublisererTjeneste(repositoryProvider, feedRepository, personinfoAdapter);
    }

    @Test
    void skal_sette_første_og_siste_stønadsdag_lik_siste_innvilgede_periode() {
        var berRes = lagBeregningsresultat(INNVILGET_PERIODE_FØRSTE_DAG, INNVILGET_PERIODE_SISTE_DAG, 100, 100);
        var nyttBer = lagBeregningsresultat(AVSLÅTT_PERIODE_START, AVSLÅTT_PERIODE_SLUTT, 0, 0);
        var vedtak = byggBehandlingVedtakOgBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING,
            BehandlingResultatType.OPPHØR, berRes, nyttBer, VedtakResultatType.OPPHØR);
        tjeneste.lagreVedtak(vedtak);

        var behandling = repositoryProvider.getBehandlingRepository()
            .hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var alle = hentUtgåendeHendelser(behandling.getAktørId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        var opphørt = StandardJsonConfig.fromJson(alle.get(0).getPayload(), ForeldrepengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(opphørt.getFnr()).isEqualTo(FNR);
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_FØRSTE_DAG);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_SISTE_DAG);
        assertThat(opphørt.getGsakId()).isEqualTo(behandling.getSaksnummer().getVerdi());
    }

    private List<FpVedtakUtgåendeHendelse> hentUtgåendeHendelser(AktørId aktørId) {
        var criteria = HendelseCriteria.builder()
            .medAktørId(aktørId.getId())
            .medMaxAntall(10L)
            .medSisteLestSekvensId(0L)
            .build();
        return feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class, criteria);
    }

    @Test
    void skal_lagre_ned_førstegangssøknad() {
        var berRes = lagBeregningsresultat(INNVILGET_PERIODE_FØRSTE_DAG, INNVILGET_PERIODE_SISTE_DAG, 100, 100);

        var vedtak = byggBehandlingVedtakOgBehandling(null, BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.INNVILGET, berRes, null, VedtakResultatType.INNVILGET);
        tjeneste.lagreVedtak(vedtak);

        var behandling = repositoryProvider.getBehandlingRepository()
            .hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var alle = hentUtgåendeHendelser(behandling.getAktørId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_INNVILGET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        var innvilget = StandardJsonConfig.fromJson(alle.get(0).getPayload(), ForeldrepengerInnvilget.class);
        assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(innvilget.getFnr()).isEqualTo(FNR);
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_SISTE_DAG);
        assertThat(innvilget.getGsakId()).isEqualTo(behandling.getSaksnummer().getVerdi());
    }

    @Test
    void skal_ikke_lagre_ned_vedtak_som_ikke_endrer_stønadsperiode() {
        var berRes = lagBeregningsresultat(NY_PERIODE_FØRSTE_DAG, NY_PERIODE_SISTE_DAG, 100, 100);
        var nyttBer = lagBeregningsresultat(NY_PERIODE_FØRSTE_DAG, NY_PERIODE_SISTE_DAG, 100, 100);

        var vedtak = byggBehandlingVedtakOgBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING,
            BehandlingResultatType.INNVILGET, berRes, nyttBer, VedtakResultatType.INNVILGET);
        tjeneste.lagreVedtak(vedtak);

        var behandling = repositoryProvider.getBehandlingRepository()
            .hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var alle = hentUtgåendeHendelser(behandling.getAktørId());

        assertThat(alle).isEmpty();
    }

    @Test
    void skal_lagre_ned_revurdering_med_endrede_utbet_perioder() {
        var berRes = lagBeregningsresultat(INNVILGET_PERIODE_FØRSTE_DAG, INNVILGET_PERIODE_SISTE_DAG, 100, 100);
        var nyttBer = lagBeregningsresultat(NY_PERIODE_FØRSTE_DAG, NY_PERIODE_SISTE_DAG, 100, 100);

        var vedtak = byggBehandlingVedtakOgBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING,
            BehandlingResultatType.FORELDREPENGER_ENDRET, berRes, nyttBer, VedtakResultatType.INNVILGET);
        tjeneste.lagreVedtak(vedtak);

        var behandling = repositoryProvider.getBehandlingRepository()
            .hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var alle = hentUtgåendeHendelser(behandling.getAktørId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_ENDRET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        var endret = StandardJsonConfig.fromJson(alle.get(0).getPayload(), ForeldrepengerEndret.class);
        assertThat(endret.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(endret.getFnr()).isEqualTo(FNR);
        assertThat(endret.getFoersteStoenadsdag()).isEqualTo(NY_PERIODE_FØRSTE_DAG);
        assertThat(endret.getSisteStoenadsdag()).isEqualTo(NY_PERIODE_SISTE_DAG);
        assertThat(endret.getGsakId()).isEqualTo(behandling.getSaksnummer().getVerdi());
    }

    @Test
    void skal_lagre_ned_revurdering_opphørt() {
        var berRes = lagBeregningsresultat(INNVILGET_PERIODE_FØRSTE_DAG,
            INNVILGET_PERIODE_SISTE_DAG, 100, 100);
        var nyttBer = lagBeregningsresultat(AVSLÅTT_PERIODE_START, AVSLÅTT_PERIODE_SLUTT, 0, 0);

        var vedtak = byggBehandlingVedtakOgBehandling(BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingType.REVURDERING, BehandlingResultatType.OPPHØR, berRes, nyttBer, VedtakResultatType.OPPHØR);
        tjeneste.lagreVedtak(vedtak);

        var behandling = repositoryProvider.getBehandlingRepository()
            .hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var alle = hentUtgåendeHendelser(behandling.getAktørId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.FORELDREPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        var opphørt = StandardJsonConfig.fromJson(alle.get(0).getPayload(), ForeldrepengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(opphørt.getFnr()).isEqualTo(FNR);
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_FØRSTE_DAG);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(INNVILGET_PERIODE_SISTE_DAG);
        assertThat(opphørt.getGsakId()).isEqualTo(behandling.getSaksnummer().getVerdi());
    }

    private BehandlingVedtak byggBehandlingVedtakOgBehandling(BehandlingType behandlingTypeOppr,
                                                              BehandlingType behandlingType,
                                                              BehandlingResultatType behandlingResultatType,
                                                              BeregningsresultatEntitet berRes,
                                                              BeregningsresultatEntitet nyttBerRes,
                                                              VedtakResultatType nyttVedtakResultat) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medBehandlingType(behandlingTypeOppr == null ? behandlingType : behandlingTypeOppr);
        var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1).medFødselsDato(LocalDate.now());
        var behandlingresultatBuilder = Behandlingsresultat.builder();
        behandlingresultatBuilder.medBehandlingResultatType(
            nyttBerRes == null ? behandlingResultatType : BehandlingResultatType.INNVILGET);
        scenario.medBehandlingsresultat(behandlingresultatBuilder);

        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var vedtakBuilder = scenario.medBehandlingVedtak();
        vedtakBuilder.medBehandlingsresultat(getBehandlingsresultat(behandling))
            .medVedtakstidspunkt(INNVILGET_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navsdotter")
            .build();
        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        behandlingVedtakRepository.lagre(vedtakBuilder.build(), behandlingRepository.taSkriveLås(behandling));

        var beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        if (berRes != null) {
            beregningsresultatRepository.lagre(behandling, berRes);
        }

        var behandlingId = behandling.getId();
        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            var nyBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                    .medOriginalBehandlingId(behandling.getId()))
                .build();
            var nyttBehandlingresultatBuilder = Behandlingsresultat.builder();
            nyttBehandlingresultatBuilder.medBehandlingResultatType(behandlingResultatType);
            var nyttBehandingsresultat = nyttBehandlingresultatBuilder.buildFor(nyBehandling);
            nyBehandling.avsluttBehandling();
            behandlingRepository.lagre(nyBehandling, behandlingRepository.taSkriveLås(nyBehandling));
            var nyttVedtakBuilder = scenario.medBehandlingVedtak();
            nyttVedtakBuilder.medBehandlingsresultat(nyttBehandingsresultat)
                .medVedtakstidspunkt(INNVILGET_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
                .medVedtakResultatType(nyttVedtakResultat)
                .medAnsvarligSaksbehandler("Nav Navsdotter")
                .build();
            behandlingVedtakRepository.lagre(nyttVedtakBuilder.build(), behandlingRepository.taSkriveLås(nyBehandling));
            behandlingId = nyBehandling.getId();
            beregningsresultatRepository.lagre(nyBehandling, nyttBerRes);
        }

        var hentBehandlingvedtakForBehandlingId = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(
            behandlingId);
        return hentBehandlingvedtakForBehandlingId.orElse(null);
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom,
                                                            LocalDate periodeTom,
                                                            int dagsats,
                                                            double utbetalingsgrad) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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
