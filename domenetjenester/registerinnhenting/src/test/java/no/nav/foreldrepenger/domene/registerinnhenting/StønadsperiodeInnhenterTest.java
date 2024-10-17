package no.nav.foreldrepenger.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(MockitoExtension.class)
class StønadsperiodeInnhenterTest extends EntityManagerAwareTest {



    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId MEDF_AKTØR_ID = AktørId.dummy();

    private static final LocalDate FH_DATO = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(2));
    private static final LocalDate STP_NORMAL = FH_DATO.minusWeeks(3);

    private static final LocalDate FH_DATO_ELDRE = VirkedagUtil.fomVirkedag(FH_DATO.minusYears(2));
    private static final LocalDate FH_DATO_YNGRE = VirkedagUtil.fomVirkedag(FH_DATO.plusWeeks(45));


    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;
    @Mock
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    @Mock
    private FamilieHendelseGrunnlagEntitet fhGrunnlagAktuellMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseAktuellMock;
    @Mock
    private FamilieHendelseGrunnlagEntitet fhGrunnlagAnnenMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseAnnenMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private Skjæringstidspunkt skjæringstidspunkt;

    private StønadsperioderInnhenter stønadsperioderInnhenter;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        stønadsperioderInnhenter = new StønadsperioderInnhenter(repositoryProvider, grunnlagRepositoryProvider, familieHendelseTjeneste,
            stønadsperiodeTjeneste, skjæringstidspunktTjeneste, fagsakRelasjonTjeneste);
    }

    /*
     * Cases
     * 1. Mor FP, ingen senere saker, FP for tidligere barn
     * 2. Mor FP, to tette
     * 3. Mor SVP, finnes innvilget FP for samme barn
     * 4. Far FP, er oppgitt som annenpart i vedtatt sak for nyere barn
     * 5. Far FP, er oppgitt som annenpart i vedtatt sak for nyere barn - far søker uttak for B1 etter mor har begynt for B2
     * 6. Far FP B1, er oppgitt som annenpart i vedtatt sak for B2 og Far har søkt for B2 (men ikke fått vedtak)
     * 7. SVP flere saker med ulik termin for samme barn
     * 8. adopsjon?
     */

    @Test
    void behandlingMorFPIngenNyereSaker() {
        var eldreBehandling = lagBehandlingMor(FH_DATO_ELDRE, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(eldreBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_ELDRE);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(eldreBehandling.getFagsak())).thenReturn(Optional.of(FH_DATO_ELDRE.minusWeeks(2)));
        avsluttBehandling(eldreBehandling);

        var behandling = lagBehandlingMor(FH_DATO, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(any())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(STP_NORMAL);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak())).thenReturn(Optional.of(STP_NORMAL));

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(behandling);
        assertThat(muligSak).isEmpty();
    }

    @Test
    void behandlingMorFPToTette() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyereBehandling.getFagsak())).thenReturn(Optional.of(FH_DATO_YNGRE));
        avsluttBehandling(nyereBehandling);

        var behandling = lagBehandlingMor(FH_DATO, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(STP_NORMAL);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak())).thenReturn(Optional.of(STP_NORMAL));

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.startdato()).isEqualTo(FH_DATO_YNGRE);
        });
    }

    @Test
    void behandlingMorSVPHarInnvilgetFPSammeBarn() {

        var avsluttetFPBehMor = lagBehandlingMor(FH_DATO.minusWeeks(1), AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(avsluttetFPBehMor.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO.minusWeeks(1));
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(STP_NORMAL, STP_NORMAL)));
        avsluttetFPBehMor.avsluttBehandling();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyBehSVPOverlapper.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.minusWeeks(12));
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehSVPOverlapper.getFagsak())).thenReturn(Optional.of(FH_DATO.minusWeeks(12)));

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(nyBehSVPOverlapper);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(avsluttetFPBehMor.getFagsak().getSaksnummer());
            assertThat(v.startdato()).isEqualTo(STP_NORMAL);
        });
    }

    @Test
    void behandlingFarDerMorHarNySakTette() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyereBehandling.getFagsak())).thenReturn(Optional.of(FH_DATO_YNGRE.minusWeeks(3)));
        avsluttBehandling(nyereBehandling);

        var behandling = lagBehandlingFar(FH_DATO, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.plusWeeks(34));
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak())).thenReturn(Optional.of(FH_DATO.plusWeeks(34)));

        assertThat(repositoryProvider.getPersonopplysningRepository().fagsakerMedOppgittAnnenPart(MEDF_AKTØR_ID)).isNotEmpty();

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.startdato()).isEqualTo(FH_DATO_YNGRE.minusWeeks(3));
        });
    }

    @Test
    void behandlingFarSøkerForEldreBarnEtterMorHarSakForNyttBarn() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyereBehandling.getFagsak())).thenReturn(Optional.of(FH_DATO_YNGRE.minusWeeks(3)));
        avsluttBehandling(nyereBehandling);

        var behandling = lagBehandlingFar(FH_DATO, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE.plusWeeks(8));
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak())).thenReturn(Optional.of(FH_DATO_YNGRE.plusWeeks(8)));

        assertThat(repositoryProvider.getPersonopplysningRepository().fagsakerMedOppgittAnnenPart(MEDF_AKTØR_ID)).isNotEmpty();

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.startdato()).isEqualTo(FH_DATO_YNGRE.minusWeeks(3));
        });
    }

    @Test
    void behandlingFarDerMorHarNySakMensFarHarSøktNySak() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyereBehandling.getFagsak())).thenReturn(Optional.of(FH_DATO_YNGRE.minusWeeks(3)));
        avsluttBehandling(nyereBehandling);

        var nyereÅpenBehandlingFar = lagBehandlingFar(FH_DATO_YNGRE, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereÅpenBehandlingFar.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE.plusMonths(2));
        lenient().when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyereÅpenBehandlingFar.getFagsak())).thenReturn(Optional.empty());

        var gammelBehandlingMor = lagBehandlingFar(FH_DATO, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(gammelBehandlingMor.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        lenient().when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.plusWeeks(34));
        lenient().when(stønadsperiodeTjeneste.stønadsperiodeStartdato(gammelBehandlingMor.getFagsak())).thenReturn(Optional.of(FH_DATO.minusWeeks(3)));

        var behandling = lagBehandlingFar(FH_DATO, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.plusWeeks(34));
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak())).thenReturn(Optional.of(FH_DATO.plusWeeks(34)));

        fagsakRelasjonTjeneste.opprettRelasjon(gammelBehandlingMor.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(gammelBehandlingMor.getFagsak(), behandling.getFagsak());
        fagsakRelasjonTjeneste.opprettRelasjon(nyereBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(nyereBehandling.getFagsak(), nyereÅpenBehandlingFar.getFagsak());

        assertThat(repositoryProvider.getPersonopplysningRepository().fagsakerMedOppgittAnnenPart(MEDF_AKTØR_ID)).isNotEmpty();

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.startdato()).isEqualTo(FH_DATO_YNGRE.minusWeeks(3));
        });
    }

    @Test
    void behandlingMorSVPHarTidligereInnvilgetSVPAnnenTermindato() {

        var avsluttetFPBehMor = lagBehandlingSVP(AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(avsluttetFPBehMor.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO.plusWeeks(1));
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FH_DATO.minusWeeks(15), FH_DATO)));
        avsluttetFPBehMor.avsluttBehandling();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyBehSVPOverlapper.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.minusWeeks(12));

        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehSVPOverlapper.getFagsak())).thenReturn(Optional.of(FH_DATO.minusWeeks(12)));

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperiode(nyBehSVPOverlapper);
        assertThat(muligSak).isEmpty();
    }

    private Behandling lagBehandlingMor(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioMor = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioMor.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioMor.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenarioMor.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioMor.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        return scenarioMor.lagre(repositoryProvider);
    }


    private Behandling lagBehandlingFar(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioFar.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioFar.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Is Pinne")
                .medType(SøknadAnnenPartType.MOR);
        }
        scenarioFar.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFar.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        return scenarioFar.lagre(repositoryProvider);
    }

    private Behandling lagBehandlingFPAdopsjonMor(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);

        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonFar(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);

        return behandling;
    }

    private Behandling lagBehandlingSVP(AktørId aktørId) {
        var scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioSVP.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenarioSVP.medDefaultOppgittTilknytning();
        scenarioSVP.medSøknadHendelse().medTerminbekreftelse(scenarioSVP.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGEN MIN")
                .medTermindato(FH_DATO)
                .medUtstedtDato(LocalDate.now().minusDays(3)))
            .medAntallBarn(1);
        scenarioSVP.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioSVP.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        return scenarioSVP.lagre(repositoryProvider);
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
    }
}
