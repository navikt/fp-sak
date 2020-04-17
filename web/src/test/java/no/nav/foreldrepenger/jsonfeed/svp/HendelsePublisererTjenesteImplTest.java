package no.nav.foreldrepenger.jsonfeed.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
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

    private static final LocalDate FØRSTE_PERIODE_FØRSTE_DAG = fomMandag(LocalDate.now());
    private static final LocalDate FØRSTE_PERIODE_SISTE_DAG = tomFredag(FØRSTE_PERIODE_FØRSTE_DAG.plusMonths(1));
    private static final LocalDate ANDRE_PERIODE_FØRSTE_DAG = fomMandag(FØRSTE_PERIODE_SISTE_DAG.plusDays(1));
    private static final LocalDate ANDRE_PERIODE_SISTE_DAG = tomFredag(ANDRE_PERIODE_FØRSTE_DAG.plusMonths(1));

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
    private BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private EtterkontrollRepository etterkontrollRepository = new EtterkontrollRepository(repoRule.getEntityManager());
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();

    private HendelsePublisererTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new HendelsePublisererTjenesteImpl(feedRepository, repositoryProvider, behandlingsresultatRepository, etterkontrollRepository);
    }

    @Test
    public void skal_lagre_ned_førstegangsbehandling_innvilget() {
        // Arrange
        BeregningsresultatEntitet berResFG  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, berResFG, null);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_INNVILGET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(FØRSTE_PERIODE_SISTE_DAG);
        assertThat(innvilget.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_ikke_lagre_ned_vedtak_som_ikke_endrer_stønadsperiode() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BeregningsresultatEntitet berResRv  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, berResFg, berResRv);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).isEmpty();
    }

    @Test
    public void skal_lagre_ned_revurdering_innvilget() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BeregningsresultatEntitet berResRv  = opprettBerRes(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, berResFg, berResRv);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_INNVILGET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerInnvilget innvilget = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerInnvilget.class);
        assertThat(innvilget.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(innvilget.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(innvilget.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(innvilget.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_lagre_ned_revurdering_endret() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BeregningsresultatEntitet berResRv  = opprettBerRes(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.FORELDREPENGER_ENDRET, VedtakResultatType.INNVILGET, berResFg, berResRv);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_ENDRET.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerEndret endret = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerEndret.class);
        assertThat(endret.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(endret.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(endret.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(endret.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_lagre_ned_revurdering_opphørt() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BeregningsresultatEntitet berResRv  = opprettBerRes(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, berResFg, berResRv);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        assertThat(alle).hasSize(1);
        assertThat(alle.get(0).getType()).isEqualTo(Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.getType());
        assertThat(alle.get(0).getKildeId()).isEqualTo(VEDTAK_PREFIX + vedtak.getId().toString());
        SvangerskapspengerOpphoert opphørt = JsonMapper.fromJson(alle.get(0).getPayload(), SvangerskapspengerOpphoert.class);
        assertThat(opphørt.getAktoerId()).isEqualTo(alle.get(0).getAktørId()).isNotNull();
        assertThat(opphørt.getFoersteStoenadsdag()).isEqualTo(ANDRE_PERIODE_FØRSTE_DAG);
        assertThat(opphørt.getSisteStoenadsdag()).isEqualTo(ANDRE_PERIODE_SISTE_DAG);
        assertThat(opphørt.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_ikke_lagre_ned_beslutningsvedtak() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BeregningsresultatEntitet berResRv  = opprettBerRes(ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.REVURDERING, BehandlingResultatType.INGEN_ENDRING, VedtakResultatType.INNVILGET, berResFg, berResRv);

        // Act
        tjeneste.lagreVedtak(vedtak);

        // Assert
        List<SvpVedtakUtgåendeHendelse> alle = feedRepository.hentAlle(SvpVedtakUtgåendeHendelse.class);
        assertThat(alle).isEmpty();
    }

    @Test
    public void skal_lagre_fagsak_avsluttet() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 100);
        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, berResFg, null);

        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        AktørId aktørId = behandling.getAktørId();
        Long fagsakId = behandling.getFagsakId();
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
        assertThat(opphørt.getGsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
    }

    @Test
    public void skal_benytte_første_periode_med_utbetalingsgrad_større_enn_0() {
        // Arrange
        BeregningsresultatEntitet berResFg  = opprettBerRes(FØRSTE_PERIODE_FØRSTE_DAG, FØRSTE_PERIODE_SISTE_DAG, 0);
        leggTilBeregningsresPeriode(berResFg, ANDRE_PERIODE_FØRSTE_DAG, ANDRE_PERIODE_SISTE_DAG, 100);

        BehandlingVedtak vedtak = byggBehandlingVedtak(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, VedtakResultatType.INNVILGET, berResFg, null);

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
                                                  VedtakResultatType nyttVedtakResultat, BeregningsresultatEntitet berResFg,
                                                  BeregningsresultatEntitet berResRv) {
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

        beregningsresultatRepository.lagre(behandling, berResFg);
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

            beregningsresultatRepository.lagre(revurdering, berResRv);
            behandlingId = revurdering.getId();
        }

        repoRule.getEntityManager().flush();

        Optional<BehandlingVedtak> hentBehandlingvedtakForBehandlingId = vedtakRepo.hentBehandlingvedtakForBehandlingId(behandlingId);
        return hentBehandlingvedtakForBehandlingId.orElse(null);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hent(behandling.getId());
    }

    private BeregningsresultatEntitet opprettBerRes(LocalDate fom, LocalDate tom, long utbetalingsgrad) {
        int dagsats = (int) utbetalingsgrad;
        return lagBeregningsresultat(fom, tom, dagsats, utbetalingsgrad );
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

    private void leggTilBeregningsresPeriode(BeregningsresultatEntitet beregningsresultatEntitet, LocalDate fom, LocalDate tom, long utbetalingsgrad) {
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats((int)utbetalingsgrad)
            .medDagsatsFraBg((int)utbetalingsgrad)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(utbetalingsgrad))
            .medStillingsprosent(BigDecimal.valueOf(utbetalingsgrad))
            .build(beregningsresultatPeriode);
        beregningsresultatEntitet.addBeregningsresultatPeriode(beregningsresultatPeriode);
    }

    private static LocalDate fomMandag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }

    private static LocalDate tomFredag(LocalDate tom) {
        DayOfWeek ukedag = DayOfWeek.from(tom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return tom.minusDays(2);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return tom.minusDays(1);
        return tom;
    }
}
