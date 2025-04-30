package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.SakInfoV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.kontrakter.fordel.VurderFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class FordelRestTjenesteTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();

    @Mock
    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjenesteMock;
    @Mock
    private OpprettSakTjeneste opprettSakTjenesteMock;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;
    @Mock
    private VurderFagsystemFellesTjeneste vurderFagsystemTjenesteMock;
    @Mock
    private FamilieHendelseRepository familieHendelseRepositoryMock;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProviderMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private SakInfoDtoTjeneste sakInfoDtoTjenesteMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjenesteMock;

    private FordelRestTjeneste fordelRestTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository());
        fordelRestTjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjeneste, opprettSakTjenesteMock, repositoryProvider, vurderFagsystemTjenesteMock, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);
    }

    @Test
    void skalReturnereFagsystemVedtaksløsning() {
        var saksnummer = new Saksnummer("12345");
        var innDto = new VurderFagsystemDto("1234", true, AKTØR_ID_MOR.getId(), "ab0047");
        var behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING, saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        var result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.getSaksnummer()).contains(saksnummer.getVerdi());
        assertThat(result.isBehandlesIVedtaksløsningen()).isTrue();
    }

    @Test
    void skalReturnereFagsystemManuell() {
        var saksnummer = new Saksnummer("TEST1");
        var journalpostId = new JournalpostId("1234");
        var innDto = new VurderFagsystemDto(journalpostId.getVerdi(), false, AKTØR_ID_MOR.getId(), "ab0047");
        innDto.setDokumentTypeIdOffisiellKode(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL.getOffisiellKode());
        var behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING, saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        var result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.isManuellVurdering()).isTrue();
    }

    @Test
    void skalReturnereFagsakinformasjonMedBehandlingTemaOgAktørId() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(new Saksnummer("TEST2")).medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);
        var result = fordelRestTjeneste.fagsak(new SaksnummerDto("TEST2"));

        assertThat(result).isNotNull();
        assertThat(new AktørId(result.getAktørId())).isEqualTo(AKTØR_ID_MOR);
        assertThat(result.getBehandlingstemaOffisiellKode()).isEqualTo(BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode());
    }

    @Test
    void skalReturnereNullNårFagsakErStengt() {
        var saknr = new Saksnummer("TEST3");
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(saknr).medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRepository().fagsakSkalStengesForBruk(behandling.getFagsakId());
        var result = fordelRestTjeneste.fagsak(new SaksnummerDto("TEST3"));

        assertThat(result).isNull();
    }

    @Test
    void skalReturnereAlleBrukersSaker() {
        var saknr1 = new Saksnummer("TEST3");
        var saknr2 = new Saksnummer("TEST4");
        var foreldrepenger = FagsakYtelseType.FORELDREPENGER;
        var forventetYtelseType = YtelseTypeDto.FORELDREPENGER;
        var forventetStatus = SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING;
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var opprettetTidSak2 = LocalDateTime.now();
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);
        var førsteuttaksdato = LocalDate.now().minusMonths(6);


        var fagsak1 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr1);
        fagsak1.setOpprettetTidspunkt(opprettetTidSak1);
        fagsak1.setId(125L);

        var fagsak2 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr2);
        fagsak2.setOpprettetTidspunkt(opprettetTidSak2);
        fagsak2.setEndretTidspunkt(opprettetTidSak2);
        fagsak2.setId(126L);

        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(behandlingRepositoryProviderMock.getFamilieHendelseRepository()).thenReturn(familieHendelseRepositoryMock);

        var sakDto1 = new SakInfoV2Dto(new SaksnummerDto(saknr1.getVerdi()),  forventetYtelseType, forventetStatus, new SakInfoV2Dto.FamiliehendelseInfoDto(skjæringstidspunkt, SakInfoV2Dto.FamilieHendelseTypeDto.FØDSEL), opprettetTidSak1.toLocalDate(), førsteuttaksdato);
        var sakDto2 = new SakInfoV2Dto(new SaksnummerDto(saknr2.getVerdi()), forventetYtelseType, forventetStatus, null, opprettetTidSak2.toLocalDate(), null);

        when(sakInfoDtoTjenesteMock.mapSakInfoV2Dto(fagsak1)).thenReturn(sakDto1);
        when(sakInfoDtoTjenesteMock.mapSakInfoV2Dto(fagsak2)).thenReturn(sakDto2);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.finnAlleSakerForBrukerV2(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Skal kaste exceptions om aktørId er ikke gyldig.")
    void exception_om_ikke_gyldig_aktørId() {
        var tjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjenesteMock, opprettSakTjenesteMock, mock(
            BehandlingRepositoryProvider.class), vurderFagsystemTjenesteMock, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var aktørIdDto = new FordelRestTjeneste.AktørIdDto("ikke_gyldig_id_haha:)");
        var exception = assertThrows(IllegalArgumentException.class, () -> tjeneste.finnAlleSakerForBrukerV2(aktørIdDto));

        var expectedMessage = "Oppgitt aktørId er ikke en gyldig ident.";
        var actualMessage = exception.getMessage();

        assertThat(actualMessage).contains(expectedMessage);
    }

    @Test
    void skal_ikke_finne_sak_når_feil_ytelse() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        fagsak2.setId(2L);

        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));

        var b1 = mock(Behandling.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.sjekkSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.SVANGERSKAPSPENGER));

        var response = (FordelRestTjeneste.SakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.søkerHarSak()).isFalse();
    }

    @Test
    void skal_finne_sak_når_rett_ytelse() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak2.setId(2L);

        var b1 = mock(Behandling.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);

        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.sjekkSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.SVANGERSKAPSPENGER));

        var response = (FordelRestTjeneste.SakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.søkerHarSak()).isTrue();
    }

    @Test
    void skal_ikke_finne_sak_når_søkt_for_tidlig() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD);
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.OPPRETTET);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.sjekkSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.SakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.søkerHarSak()).isFalse();
    }

    @Test
    void skal_finne_sak_når_vanlig_åpent_aksjonspunkt() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        var a2 = mock(Aksjonspunkt.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.OPPRETTET);
        when(a2.getStatus()).thenReturn(AksjonspunktStatus.UTFØRT);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1, a2));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.sjekkSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.SakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.søkerHarSak()).isTrue();
    }

    @Test
    void skal_finne_sak_når_søkt_for_tidlig_er_utført() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.UTFØRT);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.sjekkSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.SakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.søkerHarSak()).isTrue();
    }

    @Test
    void info_sak_inntektsmelding_når_søkt_for_tidlig() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        var førsteUttaksdato = LocalDate.now().minusWeeks(5);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD);
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.OPPRETTET);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));
        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(b1.getId())).thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdato).build());


        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.infoOmSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.InfoOmSakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.statusInntektsmelding()).isEqualTo(FordelRestTjeneste.StatusSakInntektsmelding.SØKT_FOR_TIDLIG);
        assertThat(response.førsteUttaksdato()).isEqualTo(førsteUttaksdato);
    }

    @Test
    void info_om_sak_inntektsmelding_når_søkt_for_tidlig_er_utført() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.UTFØRT);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));
        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(b1.getId())).thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(LocalDate.now()).build());

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.infoOmSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.InfoOmSakInntektsmeldingResponse) result.getEntity();

        assertThat(response).isNotNull();
        assertThat(response.statusInntektsmelding()).isEqualTo(FordelRestTjeneste.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING);
        assertThat(response.førsteUttaksdato()).isEqualTo(LocalDate.now());
    }

    @Test
    void info_om_sak_inntektsmelding_når_feil_ytelse() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        fagsak2.setId(2L);

        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);
        var result = tjeneste.infoOmSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.SVANGERSKAPSPENGER));

        var response = (FordelRestTjeneste.InfoOmSakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.førsteUttaksdato()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(response.statusInntektsmelding()).isEqualTo(FordelRestTjeneste.StatusSakInntektsmelding.INGEN_BEHANDLING);
    }

    @Test
    void info_om_sak_inntektsmelding_når_åpent_aksjonspunkt_papirsøknad() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER);
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.OPPRETTET);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));
        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(b1.getId())).thenReturn(Skjæringstidspunkt.builder().build());

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.infoOmSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.InfoOmSakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.statusInntektsmelding()).isEqualTo(FordelRestTjeneste.StatusSakInntektsmelding.PAPIRSØKNAD_IKKE_REGISTRERT);
        assertThat(response.førsteUttaksdato()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    void info_om_sak_inntektsmelding_når_vanlig_åpent_aksjonspunkt() {
        var saknr = new Saksnummer("TEST3");
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak1.setId(1L);
        var b1 = mock(Behandling.class);
        var a1 = mock(Aksjonspunkt.class);
        var a2 = mock(Aksjonspunkt.class);
        var førsteUttaksdato = LocalDate.now().minusWeeks(2);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(b1));
        when(a1.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        when(a1.getStatus()).thenReturn(AksjonspunktStatus.OPPRETTET);
        when(a2.getStatus()).thenReturn(AksjonspunktStatus.UTFØRT);
        when(b1.getAksjonspunkter()).thenReturn(Set.of(a1, a2));
        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1));
        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(b1.getId())).thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdato).build());

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock);

        var result = tjeneste.infoOmSakForInntektsmelding(new FordelRestTjeneste.SakInntektsmeldingDto(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()), FordelRestTjeneste.SakInntektsmeldingDto.YtelseType.FORELDREPENGER));

        var response = (FordelRestTjeneste.InfoOmSakInntektsmeldingResponse) result.getEntity();
        assertThat(response).isNotNull();
        assertThat(response.statusInntektsmelding()).isEqualTo(FordelRestTjeneste.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING);
        assertThat(response.førsteUttaksdato()).isEqualTo(førsteUttaksdato);
    }
}
