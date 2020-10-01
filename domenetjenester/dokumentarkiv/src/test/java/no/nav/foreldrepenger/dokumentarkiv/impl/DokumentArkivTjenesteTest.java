package no.nav.foreldrepenger.dokumentarkiv.impl;

import static no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil.convertToXMLGregorianCalendar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.Kommunikasjonsretning;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Arkivfiltyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Dokumentkategorier;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.DokumenttypeIder;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DokumentInnhold;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;

public class DokumentArkivTjenesteTest {

    private static final JournalpostId JOURNAL_ID = new JournalpostId("42");
    private static final String DOKUMENT_ID = "66";
    private static final Saksnummer KJENT_SAK = new Saksnummer("123456");
    private static final String DOKUMENT_TITTEL_TERMINBEKREFTELSE = "Bekreftelse på ventet fødselsdato";
    private static final LocalDateTime NOW = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10));
    private static final LocalDateTime YESTERDAY = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10));
    private static DokumentTypeId SØK_ENG_FØDSEL;

    private DokumentArkivTjeneste dokumentApplikasjonTjeneste;
    private JournalConsumer mockJournalProxyService;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Before
    public void setUp() {
        mockJournalProxyService = mock(JournalConsumer.class);
        final FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        final Fagsak fagsak = mock(Fagsak.class);
        final Optional<Fagsak> mock1 = Optional.of(fagsak);
        when(fagsakRepository.hentSakGittSaksnummer(any(Saksnummer.class))).thenReturn(mock1);
        SØK_ENG_FØDSEL = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(mockJournalProxyService, fagsakRepository);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeInn() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().add(
                createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV, YESTERDAY, NOW, "U"));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).isNotEmpty();
        ArkivJournalPost arkivJournalPost = arkivDokuments.get(0);
        ArkivDokument arkivDokument = arkivJournalPost.getHovedDokument();
        assertThat(arkivJournalPost.getJournalpostId()).isEqualTo(JOURNAL_ID);
        assertThat(arkivDokument.getDokumentId()).isEqualTo(DOKUMENT_ID);
        assertThat(arkivDokument.getTittel()).isEqualTo(SØK_ENG_FØDSEL.getNavn());
        assertThat(arkivJournalPost.getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivJournalPost.getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeUt() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().add(
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY, NOW, "I"));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereDokumentListeMedUansettInnhold() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY, NOW, "I"),
                createJournalpost(ArkivFilType.XLS, VariantFormat.ARKIV)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        Optional<ArkivJournalPost> arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(KJENT_SAK, JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    public void skalRetunereDokumenterAvFiltypePDF() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(createJournalpost(ArkivFilType.XML, VariantFormat.ARKIV),
                createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
                createJournalpost(ArkivFilType.XML, VariantFormat.ARKIV)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
    }

    @Test
    public void skalRetunereDokumenterAvVariantFormatARKIV() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(createJournalpost(ArkivFilType.XML, VariantFormat.ORIGINAL),
                createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV),
                createJournalpost(ArkivFilType.XML, VariantFormat.ORIGINAL)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    public void skalRetunereDokumentListeMedSisteTidspunktØverst() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "U"),
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(NOW);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
        assertThat(arkivDokuments.get(1).getTidspunkt()).isEqualTo(YESTERDAY.minusDays(1));
        assertThat(arkivDokuments.get(1).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereAlleDokumentTyper() throws Exception {
        DokumentTypeId lege = DokumentTypeId.LEGEERKLÆRING;
        DokumentTypeId innlegg = DokumentTypeId.DOK_INNLEGGELSE;
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "U"),
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        hentJournalpostListeResponse.getJournalpostListe().get(1).withVedleggListe(
                createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode(), lege.getOffisiellKode(), lege.getNavn()),
                createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode(), innlegg.getOffisiellKode(), innlegg.getNavn()));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, LocalDate.MIN);

        assertThat(arkivDokumentTypeIds).hasSize(3);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.LEGEERKLÆRING);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.DOK_INNLEGGELSE);
    }

    @Test
    public void skalRetunereDokumentTyperSiden() throws Exception {
        DokumentTypeId lege = DokumentTypeId.LEGEERKLÆRING;
        DokumentTypeId innlegg = DokumentTypeId.DOK_INNLEGGELSE;
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "I"),
                createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        hentJournalpostListeResponse.getJournalpostListe().get(0).withVedleggListe(
                createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode(), lege.getOffisiellKode(), lege.getNavn()));
        hentJournalpostListeResponse.getJournalpostListe().get(1).withVedleggListe(
                createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode(), innlegg.getOffisiellKode(), innlegg.getNavn()));

        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class)))
                .thenReturn(hentJournalpostListeResponse);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, NOW.toLocalDate());

        assertThat(arkivDokumentTypeIds).hasSize(2);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.LEGEERKLÆRING);
        assertThat(arkivDokumentTypeIds).doesNotContain(DokumentTypeId.DOK_INNLEGGELSE);
    }

    @Test
    public void skal_kalle_web_service_og_oversette_fra_()
            throws HentDokumentDokumentIkkeFunnet, HentDokumentJournalpostIkkeFunnet, HentDokumentSikkerhetsbegrensning {
        // Arrange

        final byte[] bytesExpected = { 1, 2, 7 };
        HentDokumentResponse response = new HentDokumentResponse();
        response.setDokument(bytesExpected);
        when(mockJournalProxyService.hentDokument(any())).thenReturn(response);

        // Act

        byte[] bytesActual = dokumentApplikasjonTjeneste.hentDokument(new JournalpostId("123"), "456");

        // Assert
        assertThat(bytesActual).isEqualTo(bytesExpected);
    }

    private Journalpost createJournalpost(ArkivFilType arkivFilTypeKonst, VariantFormat variantFormatKonst) {
        return createJournalpost(arkivFilTypeKonst, variantFormatKonst, NOW, NOW, "U");
    }

    private Journalpost createJournalpost(ArkivFilType arkivFilTypeKonst, VariantFormat variantFormatKonst, LocalDateTime sendt,
            LocalDateTime mottatt, String kommunikasjonsretning) {
        Journalpost journalpost = new Journalpost();
        journalpost.setJournalpostId(JOURNAL_ID.getVerdi());
        journalpost.setHoveddokument(createDokumentinfoRelasjon(arkivFilTypeKonst.getOffisiellKode(), variantFormatKonst.getOffisiellKode(),
                SØK_ENG_FØDSEL.getOffisiellKode(), SØK_ENG_FØDSEL.getNavn()));
        Journalposttyper kommunikasjonsretninger = new Journalposttyper();
        kommunikasjonsretninger.setValue(kommunikasjonsretning);
        journalpost.setJournalposttype(kommunikasjonsretninger);
        journalpost.setForsendelseJournalfoert(convertToXMLGregorianCalendar(sendt));
        journalpost.setForsendelseMottatt(convertToXMLGregorianCalendar(mottatt));
        return journalpost;
    }

    private DetaljertDokumentinformasjon createDokumentinfoRelasjon(String filtype, String variantformat, String dokumentTypeId, String tittel) {
        DetaljertDokumentinformasjon dokumentinfoRelasjon = new DetaljertDokumentinformasjon();
        dokumentinfoRelasjon.setDokumentId(DOKUMENT_ID);
        DokumenttypeIder dokumenttyper = new DokumenttypeIder();
        dokumenttyper.setValue(dokumentTypeId);
        dokumentinfoRelasjon.setDokumentTypeId(dokumenttyper);
        Dokumentkategorier dokumentkategorier = new Dokumentkategorier();
        dokumentkategorier.setValue(DokumentKategori.SØKNAD.getOffisiellKode());
        dokumentinfoRelasjon.setDokumentkategori(dokumentkategorier);
        dokumentinfoRelasjon.setTittel(tittel);
        DokumentInnhold dokumentInnhold = new DokumentInnhold();
        Arkivfiltyper arkivfiltyper = new Arkivfiltyper();
        arkivfiltyper.setValue(filtype);
        dokumentInnhold.setArkivfiltype(arkivfiltyper);
        Variantformater variantformater = new Variantformater();
        variantformater.setValue(variantformat);
        dokumentInnhold.setVariantformat(variantformater);
        dokumentinfoRelasjon.getDokumentInnholdListe().add(dokumentInnhold);
        return dokumentinfoRelasjon;
    }
}
