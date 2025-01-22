package no.nav.foreldrepenger.dokumentarkiv;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste.DEFAULT_CONTENT_DISPOSITION_SAF;
import static no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste.DEFAULT_CONTENT_TYPE_SAF;
import static no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste.tilDokumentRespons;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.saf.DokumentInfo;
import no.nav.saf.Dokumentoversikt;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.Journalpost;
import no.nav.saf.Journalposttype;
import no.nav.saf.Journalstatus;
import no.nav.saf.Tema;
import no.nav.saf.Tilleggsopplysning;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.Saf;

@ExtendWith(MockitoExtension.class)
class DokumentArkivTjenesteTest {

    private static final JournalpostId JOURNAL_ID = new JournalpostId("42");
    private static final Saksnummer SAF_SAK = new Saksnummer("987123456");
    private static final LocalDateTime NOW = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10));
    private static final LocalDateTime YESTERDAY = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10));
    private static final DokumentTypeId SØK_ENG_FØDSEL = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

    @Mock
    private Saf saf;

    private DokumentArkivTjeneste dokumentApplikasjonTjeneste;

    private Long DOKUMENT_ID = 66L;

    @BeforeEach
    public void setUp() {
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(saf);
        dokumentApplikasjonTjeneste.emptyCache(SAF_SAK.getVerdi());
    }

    @Test
    void skalRetunereDokumentListeMedJournalpostTypeUt() {
        Dokumentoversikt response = lagResponse();
        response.getJournalposter().add(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.U));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        var arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterCached(SAF_SAK);

        assertThat(arkivDokuments).isNotEmpty();
        var arkivJournalPost = arkivDokuments.get(0);
        var arkivDokument = arkivJournalPost.getHovedDokument();
        assertThat(arkivJournalPost.getJournalpostId()).isEqualTo(JOURNAL_ID);
        assertThat(arkivDokument.getTittel()).isEqualTo(SØK_ENG_FØDSEL.getNavn());
        assertThat(arkivJournalPost.getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivJournalPost.getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
    }

    @Test
    void skalRetunereDokumentListeMedJournalpostTypeInn() {
        var response = lagResponse();
        response.getJournalposter().add(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.I));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        var arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterCached(SAF_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    void skalRetunereDokumentListeMedUansettInnhold() {

        when(saf.hentJournalpostInfo(any(), any())).thenReturn(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.I));

        var arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    void skalRetunereDokumenterAvVariantFormatARKIV() {
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(createJournalpost(Variantformat.ORIGINAL),
                createJournalpost(Variantformat.ARKIV),
                createJournalpost(Variantformat.ARKIV),
                createJournalpost(Variantformat.ORIGINAL)));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);


        var arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterCached(SAF_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    void skalRetunereDokumentListeMedSisteTidspunktØverst() {
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.U),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusHours(1), Journalposttype.I)));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        var arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterCached(SAF_SAK);

        assertThat(arkivDokuments.stream().anyMatch(d -> d.getTidspunkt().equals(NOW) && Kommunikasjonsretning.UT.equals(d.getKommunikasjonsretning()))).isTrue();
        assertThat(arkivDokuments.stream().anyMatch(d -> d.getTidspunkt().equals(YESTERDAY.minusHours(1)) && Kommunikasjonsretning.INN.equals(d.getKommunikasjonsretning()))).isTrue();
    }

    @Test
    void skalRetunereAlleDokumentTyper() {
        var lege = DokumentTypeId.LEGEERKLÆRING;
        var innlegg = DokumentTypeId.DOK_INNLEGGELSE;
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.U),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusDays(1), Journalposttype.I)));
        response.getJournalposter().get(1).getDokumenter().addAll(List.of(
            createDokumentinfo(Variantformat.ARKIV, null, lege.getNavn()),
            createDokumentinfo(Variantformat.ARKIV, null, innlegg.getNavn())));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        var arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(SAF_SAK, LocalDate.MIN);

        assertThat(arkivDokumentTypeIds)
                .contains(DokumentTypeId.LEGEERKLÆRING)
                .contains(DokumentTypeId.DOK_INNLEGGELSE);
    }

    @Test
    void skalRetunereDokumentTyperSiden() {
        var lege = DokumentTypeId.LEGEERKLÆRING;
        var arbeid = DokumentTypeId.BEKREFTELSE_FRA_ARBEIDSGIVER;
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.I),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusDays(1), Journalposttype.I)));
        response.getJournalposter().get(0).getDokumenter().add(
            createDokumentinfo(Variantformat.ARKIV, null, lege.getNavn()));
        response.getJournalposter().get(1).getDokumenter().add(
            createDokumentinfo(Variantformat.ARKIV, null, arbeid.getNavn()));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);


        var arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(SAF_SAK, NOW.toLocalDate());

        assertThat(arkivDokumentTypeIds)
            .contains(DokumentTypeId.LEGEERKLÆRING)
            .doesNotContain(DokumentTypeId.BEKREFTELSE_FRA_ARBEIDSGIVER);
    }

    @Test
    void skal_kalle_web_service_og_oversette_fra_() {
        // Arrange
        final byte[] bytesForventet = { 1, 2, 7 };
        var headers = HttpHeaders.of(Map.of(
            CONTENT_TYPE, List.of(DEFAULT_CONTENT_TYPE_SAF),
            CONTENT_DISPOSITION, List.of(DEFAULT_CONTENT_DISPOSITION_SAF)
        ), (x, y) -> true);
        var httpRespons = mock(HttpResponse.class);
        when(httpRespons.body()).thenReturn(bytesForventet);
        when(httpRespons.headers()).thenReturn(headers);
        when(saf.hentDokumentResponse(any())).thenReturn(httpRespons);

        // Act
        var dokumentRespons = dokumentApplikasjonTjeneste.hentDokument(new JournalpostId("123"), "456");

        // Assert
        assertThat(dokumentRespons.innhold()).isEqualTo(bytesForventet);
        assertThat(dokumentRespons.contentType()).isEqualTo(DEFAULT_CONTENT_TYPE_SAF);
        assertThat(dokumentRespons.contentDisp()).isEqualTo(DEFAULT_CONTENT_DISPOSITION_SAF);
    }


    @Test
    void skalBrukeHeadereHvisSatt() {
        final byte[] bytesForventet = { 1, 2, 7 };
        var contentTypeForventet = "application/jpeg";
        var contentDispForventet = "filename=bilde.jpeg";
        var headers = HttpHeaders.of(Map.of(
                CONTENT_TYPE, List.of(contentTypeForventet),
                CONTENT_DISPOSITION, List.of(contentDispForventet)
        ), (x, y) -> true);

        var dokumentrespons = tilDokumentRespons(bytesForventet, headers);
        assertThat(dokumentrespons.innhold()).isEqualTo(bytesForventet);
        assertThat(dokumentrespons.contentType()).isEqualTo(contentTypeForventet);
        assertThat(dokumentrespons.contentDisp()).isEqualTo(contentDispForventet);
    }

    @Test
    void skalBrukePDFSomDefaultNårHeadereIkkeErSatt() {
        final byte[] bytesForventet = { 1, 2, 7 };
        var headers = HttpHeaders.of(Map.of(), (x, y) -> true);

        var dokumentrespons = tilDokumentRespons(bytesForventet, headers);
        assertThat(dokumentrespons.innhold()).isEqualTo(bytesForventet);
        assertThat(dokumentrespons.contentType()).isEqualTo(DEFAULT_CONTENT_TYPE_SAF);
        assertThat(dokumentrespons.contentDisp()).isEqualTo(DEFAULT_CONTENT_DISPOSITION_SAF);
    }

    private Dokumentoversikt lagResponse() {
        return new Dokumentoversikt(new ArrayList<>(), null);
    }

    private Journalpost createJournalpost(Variantformat variantFormatKonst) {
        return createJournalpost(variantFormatKonst, NOW, Journalposttype.U);
    }

    private Journalpost createJournalpost(Variantformat variantFormatKonst, LocalDateTime sendt,
                                          Journalposttype kommunikasjonsretning) {
        var zone = ZoneId.systemDefault();
        var journalpost = new Journalpost();
        journalpost.setJournalpostId(JOURNAL_ID.getVerdi());
        journalpost.setTema(Tema.FOR);
        journalpost.setJournalstatus(Journalstatus.JOURNALFOERT);
        journalpost.setJournalposttype(kommunikasjonsretning);
        journalpost.setTittel(SØK_ENG_FØDSEL.getNavn());
        journalpost.setTilleggsopplysninger(List.of(new Tilleggsopplysning(DokumentArkivTjeneste.FP_DOK_TYPE, SØK_ENG_FØDSEL.getOffisiellKode())));
        journalpost.setDatoOpprettet(Date.from(Instant.from(sendt.toInstant(zone.getRules().getOffset(sendt)))));
        journalpost.setDokumenter(new ArrayList<>());
        journalpost.getDokumenter().add(createDokumentinfo(variantFormatKonst,
            NAVSkjema.SKJEMA_ENGANGSSTØNAD_FØDSEL.getOffisiellKode(), SØK_ENG_FØDSEL.getNavn()));
        return journalpost;
    }

    private DokumentInfo createDokumentinfo(Variantformat variantformat, String brevkode, String tittel) {
        var dokumentinfo = new DokumentInfo();
        dokumentinfo.setDokumentInfoId(String.valueOf(DOKUMENT_ID++));
        dokumentinfo.setBrevkode(brevkode);
        dokumentinfo.setTittel(tittel);
        dokumentinfo.setLogiskeVedlegg(List.of());
        dokumentinfo.setDokumentvarianter(new ArrayList<>());
        dokumentinfo.getDokumentvarianter().add(new Dokumentvariant(variantformat, null, null, null, null, true, null));
        return dokumentinfo;
    }
}
