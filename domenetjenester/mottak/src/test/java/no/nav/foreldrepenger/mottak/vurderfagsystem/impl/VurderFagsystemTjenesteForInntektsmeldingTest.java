package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.byggFødselGrunnlag;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.lagNavBruker;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ARBEIDSFORHOLDSID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.JOURNALPOST_ID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.VIRKSOMHETSNUMMER;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.buildFagsakMedUdefinertRelasjon;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingUdefinert;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystemForInntektsmelding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.fp.VurderFagsystemTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
class VurderFagsystemTjenesteForInntektsmeldingTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private FamilieHendelseRepository grunnlagRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepositoryMock;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepositoryMock;

    @Mock
    private FagsakRepository fagsakRepositoryMock;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private final Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    @BeforeEach
    void setUp() {
        lenient().when(fagsakTjenesteMock.hentJournalpost(any())).thenReturn(Optional.empty());
        var repositoryProvider = mock(BehandlingRepositoryProvider.class);
        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        lenient().when(repositoryProvider.getBeregningsresultatRepository()).thenReturn(beregningsresultatRepositoryMock);
        lenient().when(repositoryProvider.getYtelsesFordelingRepository()).thenReturn(ytelsesFordelingRepositoryMock);
        var mottatteDokumentTjenesteMock = Mockito.mock(MottatteDokumentTjeneste.class);

        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any()))
                .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now()).medUtledetSkjæringstidspunkt(LocalDate.now()).build());
        var familieTjeneste = new FamilieHendelseTjeneste(null, grunnlagRepository);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var fellesUtil = new VurderFagsystemFellesUtils(repositoryProvider, familieTjeneste, mottatteDokumentTjenesteMock, inntektsmeldingTjeneste,
            skjæringstidspunktTjeneste, fagsakRelasjonTjeneste);
        var tjenesteFP = new VurderFagsystemTjenesteImpl(fellesUtil);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjenesteMock, fellesUtil, new UnitTestLookupInstanceImpl<>(tjenesteFP));
    }

    @Test
    void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesOgÅrsakInnsendingErEndring() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
                LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(null, null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereManuellVurderingMedNårFlereÅpneSakerEneHalvGammel() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(9));
        var fsmock2 = lagMockFagsak(2L, new Saksnummer("2345"), true);
        lenient().when(fsmock2.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(4));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1, fsmock2));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        var behandling2 = lagMockBehandling(2L, fsmock2);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock2.getId())).thenReturn(Optional.of(behandling2));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(9)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(9)).build());
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling2.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(4)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(4)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningVurderingMedNårFlereÅpneSakerEneErGammel() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(13));
        var fsmock2 = lagMockFagsak(2L, new Saksnummer("2345"), true);
        lenient().when(fsmock2.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(1));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1, fsmock2));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        var behandling2 = lagMockBehandling(2L, fsmock2);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock2.getId())).thenReturn(Optional.of(behandling2));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(12)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(12)).build());
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling2.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(1)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(1)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereManuellVurderingMedNårFlereÅpneSakerAlleErGamle() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(33));
        var fsmock2 = lagMockFagsak(2L, new Saksnummer("2345"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(13));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1, fsmock2));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        var behandling2 = lagMockBehandling(2L, fsmock2);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock2.getId())).thenReturn(Optional.of(behandling2));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(30)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(30)).build());
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling2.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(12)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(12)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));
        when(beregningsresultatRepositoryMock.hentUtbetBeregningsresultat(behandling1.getId()))
            .thenReturn(Optional.of(lagBeregningsresultatGradert(LocalDate.now().minusMonths(30), LocalDate.now().minusMonths(15))));
        when(beregningsresultatRepositoryMock.hentUtbetBeregningsresultat(behandling2.getId()))
            .thenReturn(Optional.of(lagBeregningsresultatGradert(LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(1))));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningVurderingMedNårÅpenSakerErGammelMenInnenforBR() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(13));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(12)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(12)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));
        when(beregningsresultatRepositoryMock.hentUtbetBeregningsresultat(behandling1.getId()))
            .thenReturn(Optional.of(lagBeregningsresultatGradert(LocalDate.now().minusMonths(12), LocalDate.now().plusMonths(2))));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningVurderingNårInnenforÅpenBehandlingMedSøknad() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(13));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(12)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(12)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));
        when(ytelsesFordelingRepositoryMock.hentAggregatHvisEksisterer(behandling1.getId()))
            .thenReturn(Optional.of(YtelseFordelingAggregat.Builder.nytt()
                .medOppgittFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                    .medPeriode(LocalDate.now().minusWeeks(2), LocalDate.now().plusMonths(2))
                    .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD).build()), true,false)).build()));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereManuellVurderingMedÅpenSakAlleÅpenSakerErGammelOgUtenforBR() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
            LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        var fsmock1 = lagMockFagsak(1L, new Saksnummer("1234"), true);
        lenient().when(fsmock1.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusMonths(13));

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(List.of(fsmock1));

        var behandling1 = lagMockBehandling(1L, fsmock1);
        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fsmock1.getId())).thenReturn(Optional.of(behandling1));
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling1.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now().minusMonths(12)).medUtledetSkjæringstidspunkt(LocalDate.now().minusMonths(12)).build());
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(anyLong())).thenReturn(Optional.of(grunnlag));
        when(beregningsresultatRepositoryMock.hentUtbetBeregningsresultat(behandling1.getId()))
            .thenReturn(Optional.of(lagBeregningsresultatGradert(LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(1))));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    private Fagsak lagMockFagsak(Long id, Saksnummer saksnummer, boolean åpen) {
        var fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getId()).thenReturn(id);
        lenient().when(fagsak.getSaksnummer()).thenReturn(saksnummer);
        lenient().when(fagsak.erÅpen()).thenReturn(åpen);
        lenient().when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        return fagsak;
    }

    private Behandling lagMockBehandling(Long id, Fagsak fagsak) {
        var behandling= mock(Behandling.class);
        lenient().when(behandling.getId()).thenReturn(id);
        lenient().when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        return behandling;
    }

    private BeregningsresultatEntitet lagBeregningsresultatGradert(LocalDate periodeFom, LocalDate periodeTom) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(25))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    @Test
    void skalReturnereVLNårBrukerIkkeHarSakIVL() {
        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_NY, BehandlingTema.FORELDREPENGER,
                LocalDateTime.now(), AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, VIRKSOMHETSNUMMER);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        lenient().when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());
        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.emptyList());

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalFinneArbeidsforholdForArbeidsgiverSomErPrivatperson() {
        var arbeidsgiverAktørId = AktørId.dummy();

        var fagsystem = byggVurderFagsystemForInntektsmelding(VurderFagsystem.ÅRSAK_ENDRING, BehandlingTema.FORELDREPENGER,
                LocalDateTime.now(),
                AktørId.dummy(), JOURNALPOST_ID, ARBEIDSFORHOLDSID, null);
        fagsystem.setArbeidsgiverAktørId(arbeidsgiverAktørId);
        fagsystem.setStartDatoForeldrepengerInntektsmelding(LocalDate.now());

        when(fagsakTjenesteMock.finnFagsakerForAktør(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(null, null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }
}
