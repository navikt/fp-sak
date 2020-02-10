package no.nav.foreldrepenger.jsonfeed.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererTjeneste;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerOpphoert;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;

public class HendelsePublisererTjenesteImplTest {

    private static final LocalDate FØRSTE_PERIODE_FØRSTE_DAG = LocalDate.now();
    private static final LocalDate FØRSTE_PERIODE_SISTE_DAG = FØRSTE_PERIODE_FØRSTE_DAG.plusMonths(1);
    private static final LocalDate ANDRE_PERIODE_FØRSTE_DAG = FØRSTE_PERIODE_SISTE_DAG.plusDays(1);
    private static final LocalDate ANDRE_PERIODE_SISTE_DAG = ANDRE_PERIODE_FØRSTE_DAG.plusMonths(1);

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
    private SvangerskapspengerUttakResultatRepository uttakRepository = new SvangerskapspengerUttakResultatRepository(repoRule.getEntityManager());
    private BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private EtterkontrollRepository etterkontrollRepository = new EtterkontrollRepository(repoRule.getEntityManager());
    private HendelsePublisererTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new HendelsePublisererTjenesteImpl(feedRepository, uttakRepository, repositoryProvider, behandlingsresultatRepository, etterkontrollRepository);
    }

    @Test
    public void skal_lagre_ned_førstegangsbehandling_innvilget() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of());

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_INNVILGET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_SISTE_DAG);
        assertThat(innvilget.getGsakId()).isEqualTo(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_ikke_lagre_ned_vedtak_som_ikke_endrer_stønadsperiode() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeRv = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of(uttakPeriodeRv));

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).isEmpty();
    }

    @Test
    public void skal_lagre_ned_revurdering_innvilget() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeRv = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of(uttakPeriodeRv));

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_INNVILGET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(innvilget.getGsakId()).isEqualTo(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_lagre_ned_revurdering_endret() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeRv = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.FORELDREPENGER_ENDRET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of(uttakPeriodeRv));

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_ENDRET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerEndret endret = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerEndret.class);
        assertThat(endret.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(endret.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(endret.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(endret.getGsakId()).isEqualTo(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_lagre_ned_revurdering_opphørt() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeRv = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, Set.of(uttakPeriodeFg), Set.of(uttakPeriodeRv));

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerOpphoert opphørt = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(opphørt.getGsakId()).isEqualTo(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_ikke_lagre_ned_beslutningsvedtak() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeRv = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INGEN_ENDRING, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of(uttakPeriodeRv));

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).isEmpty();
    }

    @Test
    public void skal_lagre_fagsak_avsluttet() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg), Set.of());
        AktørId aktørId = vedtak.getBehandlingsresultat().getBehandling().getAktørId();
        Long fagsakId = vedtak.getBehandlingsresultat().getBehandling().getFagsakId();
        FagsakStatusEvent event = new FagsakStatusEvent(fagsakId, aktørId, FagsakStatus.LØPENDE, FagsakStatus.AVSLUTTET);

        // Act
        tjeneste.lagreFagsakAvsluttet(event);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(FAGSAK_PREFIX + fagsakId.toString());
        SvangerskapspengerOpphoert opphørt = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(aktørId.getId()).isNotNull();
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_FØRSTE_DAG);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_SISTE_DAG);
        assertThat(opphørt.getGsakId()).isEqualTo(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_benytte_første_periode_med_utbetalingsgrad_større_enn_0() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg1 = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(0L));
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg2 = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L));
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg1, uttakPeriodeFg2), Set.of());

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
    }

    @Test
    public void skal_benytte_første_periode_som_er_innvilget() {
        // Arrange
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg1 = opprettUttakPeriode(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, new BigDecimal(100L), PeriodeResultatType.AVSLÅTT);
        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriodeFg2 = opprettUttakPeriode(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, new BigDecimal(100L), PeriodeResultatType.INNVILGET);
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, Set.of(uttakPeriodeFg1, uttakPeriodeFg2), Set.of());

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).hasSize(1);
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
    }

    private BehandlingVedtak byggBehandlingVedtak(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType,
                                                  VedtakResultatType nyttVedtakResultat, Set<SvangerskapspengerUttakResultatPeriodeEntitet> uttakPerioderFg,
                                                  Set<SvangerskapspengerUttakResultatPeriodeEntitet> uttakPerioderRv) {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medBehandlingType(behandlingType);
        FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        Behandlingsresultat.Builder behandlingresultatBuilder = Behandlingsresultat.builder();
        behandlingresultatBuilder.medBehandlingResultatType(behandlingResultatType);
        scenario.medBehandlingsresultat(behandlingresultatBuilder);

        Behandling behandling = scenario.lagre(repositoryProvider);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        BehandlingVedtak.Builder vedtakBuilder = scenario.medBehandlingVedtak();
        vedtakBuilder.medBehandlingsresultat(getBehandlingsresultat(behandling))
            .medVedtakstidspunkt(FØRSTE_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navsdotter")
            .build();
        vedtakRepo.lagre(vedtakBuilder.build(), behandlingRepository.taSkriveLås(behandling));

        uttakRepository.lagre(behandling.getId(), opprettUttakResultat(behandling, uttakPerioderFg));
        Long behandlingId = behandling.getId();

        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).medOriginalBehandling(behandling)).build();
            Behandlingsresultat.Builder nyttBehandlingresultatBuilder = Behandlingsresultat.builder();
            nyttBehandlingresultatBuilder.medBehandlingResultatType(behandlingResultatType);
            Behandlingsresultat nyttBehandingsresultat = nyttBehandlingresultatBuilder.buildFor(revurdering);
            behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
            BehandlingVedtak.Builder nyttVedtakBuilder = scenario.medBehandlingVedtak();
            nyttVedtakBuilder.medBehandlingsresultat(nyttBehandingsresultat)
                .medVedtakstidspunkt(FØRSTE_PERIODE_FØRSTE_DAG.minusDays(7).atStartOfDay())
                .medVedtakResultatType(nyttVedtakResultat)
                .medAnsvarligSaksbehandler("Nav Navsdotter")
                .build();
            vedtakRepo.lagre(nyttVedtakBuilder.build(), behandlingRepository.taSkriveLås(revurdering));

            SvangerskapspengerUttakResultatEntitet uttakResultat = opprettUttakResultat(revurdering, uttakPerioderRv);
            uttakRepository.lagre(revurdering.getId(), uttakResultat);
            behandlingId = revurdering.getId();
        }

        repoRule.getEntityManager().flush();

        Optional<BehandlingVedtak> hentBehandlingvedtakForBehandlingId = vedtakRepo.hentBehandlingvedtakForBehandlingId(behandlingId);
        return hentBehandlingvedtakForBehandlingId.orElse(null);
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet opprettUttakPeriode(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad) {
        return opprettUttakPeriode(fom, tom, utbetalingsgrad, PeriodeResultatType.INNVILGET);
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet opprettUttakPeriode(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad, PeriodeResultatType periodeResultatType) {
            SvangerskapspengerUttakResultatPeriodeEntitet.Builder builder = new SvangerskapspengerUttakResultatPeriodeEntitet
                .Builder(fom, tom)
                .medRegelInput("{}")
                .medRegelEvaluering("{}")
                .medUtbetalingsgrad(utbetalingsgrad)
                .medPeriodeResultatType(periodeResultatType);

            if (!PeriodeResultatType.INNVILGET.equals(periodeResultatType)) {
                builder.medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT);
            }
            return builder.build();
    }

    private SvangerskapspengerUttakResultatEntitet opprettUttakResultat(Behandling behandling, Set<SvangerskapspengerUttakResultatPeriodeEntitet> uttakPerioder) {
        SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder builder = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID);

        for (SvangerskapspengerUttakResultatPeriodeEntitet periode : uttakPerioder) {
            builder.medPeriode(periode);
        }

        SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakArbeidsforhold = builder.build();

        return new SvangerskapspengerUttakResultatEntitet
            .Builder(behandlingsresultatRepository.hent(behandling.getId()))
            .medUttakResultatArbeidsforhold(uttakArbeidsforhold)
            .build();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hent(behandling.getId());
    }
}
