package no.nav.foreldrepenger.web.app.tjenester.dokument;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste.tilRespons;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.DokumentRespons;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
class DokumentRestTjenesteTest {

    private static final String ORGNR = KUNSTIG_ORG;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;

    private DokumentRestTjeneste tjeneste;

    @BeforeEach
    public void setUp() {
        tjeneste = new DokumentRestTjeneste(dokumentArkivTjeneste, inntektsmeldingTjeneste, fagsakRepository, mottatteDokumentTjeneste,
                virksomhetTjeneste, behandlingRepository);
    }

    @Test
    void skal_gi_tom_liste_ved_ikkeeksisterende_saksnummer() {
        when(fagsakRepository.hentSakGittSaksnummer(any())).thenReturn(Optional.empty());
        var response = tjeneste.hentAlleDokumenterForSak(new SaksnummerDto("123456"));
        assertThat(response).isEmpty();
    }

    @Test
    void skal_returnere_to_dokument() {
        Long fagsakId = 5L;
        Long behandlingId = 150L;
        var aktørId = AktørId.dummy();
        var navBruker = new NavBrukerBuilder().medAktørId(aktørId).build();
        var fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medBruker(navBruker)
                .medSaksnummer(new Saksnummer("123456"))
                .build();
        fagsak.setId(fagsakId);
        when(fagsakRepository.hentSakGittSaksnummer(any())).thenReturn(Optional.of(fagsak));
        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(mock(Behandling.class));

        var søknad = ArkivDokument.Builder.ny()
            .medTittel("Søknad")
            .medDokumentId("456")
            .medDokumentTypeId(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medAlleDokumenttyper(Set.of(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL))
            .build();
        var søknadJP = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId("123"))
            .medTidspunkt(LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.of(10, 10)))
            .medHoveddokument(søknad)
            .build();

        var vedlegg = ArkivDokument.Builder.ny()
            .medTittel("vedlegg")
            .medDokumentId("123")
            .medDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL)
            .medAlleDokumenttyper(Set.of(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL))
            .build();
        var søknadV = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId("125"))
            .medHoveddokument(vedlegg)
            .build();

        var im = ArkivDokument.Builder.ny()
            .medTittel("Inntektsmelding")
            .medDokumentId("789")
            .medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING)
            .medAlleDokumenttyper(Set.of(DokumentTypeId.INNTEKTSMELDING))
            .build();
        var imJP = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId("124"))
            .medTidspunkt(LocalDateTime.of(LocalDate.now().minusDays(4), LocalTime.of(10, 10)))
            .medHoveddokument(im)
            .build();

        when(dokumentArkivTjeneste.hentAlleDokumenterCached(any())).thenReturn(List.of(søknadJP, søknadV, imJP));

        var mds = new MottattDokument.Builder().medId(1001L).medJournalPostId(new JournalpostId("123"))
                .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).medFagsakId(fagsakId).medBehandlingId(behandlingId).build();
        var mdim = new MottattDokument.Builder().medId(1002L).medJournalPostId(new JournalpostId("124"))
                .medDokumentType(DokumentTypeId.INNTEKTSMELDING).medFagsakId(fagsakId).medBehandlingId(behandlingId).build();
        when(mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId)).thenReturn(List.of(mdim, mds));

        var vnavn = "Sinsen Septik og Snarmat";
        var sinsen = new Virksomhet.Builder().medNavn(vnavn).medOrgnr(ORGNR).build();
        var imelda = InntektsmeldingBuilder.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medJournalpostId(mdim.getJournalpostId()).medInnsendingstidspunkt(LocalDateTime.now()).build();

        when(inntektsmeldingTjeneste.hentAlleInntektsmeldingerForAngitteBehandlinger(any())).thenReturn(Collections.singletonList(imelda));

        when(virksomhetTjeneste.finnOrganisasjon(any())).thenReturn(Optional.of(sinsen));

        var response = tjeneste.hentAlleDokumenterForSak(new SaksnummerDto("123456"));
        assertThat(response).hasSize(3);

        assertThat(response.iterator().next().getTidspunkt()).isNull();
        var imdto = response.stream().filter(dto -> dto.getGjelderFor() != null).findAny();
        assertThat(imdto).isPresent();
        assertThat(imdto.get().getGjelderFor()).isEqualTo(vnavn);
        assertThat(imdto.get().getBehandlinger()).hasSize(1);
    }

    @Test
    void verifisererKorrektMappingFraDokumentResponsTilResponsObjekt() {
        final byte[] bytesExpected = { 1, 2, 7 };
        var contentType = "application/pdf";
        var contentDisp = "filename=test.pdf";
        var dokumentRespons = new DokumentRespons(bytesExpected, contentType, contentDisp);

        var response = tilRespons(dokumentRespons);

        assertThat(((ByteArrayInputStream) response.getEntity()).readAllBytes()).isEqualTo(bytesExpected);
        assertThat(response.getHeaders().getFirst(CONTENT_TYPE)).hasToString(contentType);
        assertThat(response.getHeaders().getFirst(CONTENT_DISPOSITION)).hasToString(contentDisp);
    }
}
