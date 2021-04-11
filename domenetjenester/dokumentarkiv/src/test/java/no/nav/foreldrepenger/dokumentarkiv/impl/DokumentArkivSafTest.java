package no.nav.foreldrepenger.dokumentarkiv.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.Kommunikasjonsretning;
import no.nav.foreldrepenger.dokumentarkiv.NAVSkjema;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.saf.DokumentInfo;
import no.nav.saf.Dokumentoversikt;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.Journalpost;
import no.nav.saf.Journalposttype;
import no.nav.saf.Journalstatus;
import no.nav.saf.Tema;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.Saf;

public class DokumentArkivSafTest {

    private static final JournalpostId JOURNAL_ID = new JournalpostId("42");
    private static final Saksnummer SAF_SAK = new Saksnummer("987123456");
    private static final LocalDateTime NOW = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10));
    private static final LocalDateTime YESTERDAY = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10));
    private static DokumentTypeId SØK_ENG_FØDSEL;

    private DokumentArkivTjeneste dokumentApplikasjonTjeneste;
    private Saf saf;

    private Long DOKUMENT_ID = 66L;

    @BeforeEach
    public void setUp() {
        saf = mock(Saf.class);
        SØK_ENG_FØDSEL = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(saf);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeUt() {
        var response = lagResponse();
        response.getJournalposter().add(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.U));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(SAF_SAK);

        assertThat(arkivDokuments).isNotEmpty();
        ArkivJournalPost arkivJournalPost = arkivDokuments.get(0);
        ArkivDokument arkivDokument = arkivJournalPost.getHovedDokument();
        assertThat(arkivJournalPost.getJournalpostId()).isEqualTo(JOURNAL_ID);
        assertThat(arkivDokument.getTittel()).isEqualTo(SØK_ENG_FØDSEL.getNavn());
        assertThat(arkivJournalPost.getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivJournalPost.getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeInn() {
        var response = lagResponse();
        response.getJournalposter().add(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.I));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(SAF_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereDokumentListeMedUansettInnhold() {

        when(saf.hentJournalpostInfo(any(), any())).thenReturn(createJournalpost(Variantformat.ARKIV, YESTERDAY, Journalposttype.I));

        Optional<ArkivJournalPost> arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    public void skalRetunereDokumenterAvVariantFormatARKIV() {
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(createJournalpost(Variantformat.ORIGINAL),
                createJournalpost(Variantformat.ARKIV),
                createJournalpost(Variantformat.ARKIV),
                createJournalpost(Variantformat.ORIGINAL)));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);


        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(SAF_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    public void skalRetunereDokumentListeMedSisteTidspunktØverst() {
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.U),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusHours(1), Journalposttype.I)));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(SAF_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(NOW);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
        assertThat(arkivDokuments.get(1).getTidspunkt()).isEqualTo(YESTERDAY.minusHours(1));
        assertThat(arkivDokuments.get(1).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereAlleDokumentTyper() {
        DokumentTypeId lege = DokumentTypeId.LEGEERKLÆRING;
        DokumentTypeId innlegg = DokumentTypeId.DOK_INNLEGGELSE;
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.U),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusDays(1), Journalposttype.I)));
        response.getJournalposter().get(1).getDokumenter().addAll(List.of(
            createDokumentinfo(Variantformat.ARKIV, null, lege.getNavn()),
            createDokumentinfo(Variantformat.ARKIV, null, innlegg.getNavn())));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(SAF_SAK, LocalDate.MIN);

        assertThat(arkivDokumentTypeIds).hasSize(3);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.LEGEERKLÆRING);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.DOK_INNLEGGELSE);
    }

    @Test
    public void skalRetunereDokumentTyperSiden() {
        DokumentTypeId lege = DokumentTypeId.LEGEERKLÆRING;
        DokumentTypeId innlegg = DokumentTypeId.DOK_INNLEGGELSE;
        var response = lagResponse();
        response.getJournalposter().addAll(List.of(
            createJournalpost(Variantformat.ARKIV, NOW, Journalposttype.I),
            createJournalpost(Variantformat.ARKIV, YESTERDAY.minusDays(1), Journalposttype.I)));
        response.getJournalposter().get(0).getDokumenter().add(
            createDokumentinfo(Variantformat.ARKIV, null, lege.getNavn()));
        response.getJournalposter().get(1).getDokumenter().add(
            createDokumentinfo(Variantformat.ARKIV, null, innlegg.getNavn()));
        when(saf.dokumentoversiktFagsak(any(), any())).thenReturn(response);


        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(SAF_SAK, NOW.toLocalDate());

        assertThat(arkivDokumentTypeIds).hasSize(2);
        assertThat(arkivDokumentTypeIds).contains(DokumentTypeId.LEGEERKLÆRING);
        assertThat(arkivDokumentTypeIds).doesNotContain(DokumentTypeId.DOK_INNLEGGELSE);
    }

    @Test
    public void skal_kalle_web_service_og_oversette_fra_() {
        // Arrange

        final byte[] bytesExpected = { 1, 2, 7 };
        when(saf.hentDokument(any())).thenReturn(bytesExpected);

        // Act

        byte[] bytesActual = dokumentApplikasjonTjeneste.hentDokument(SAF_SAK, new JournalpostId("123"), "456");

        // Assert
        assertThat(bytesActual).isEqualTo(bytesExpected);
    }

    private Dokumentoversikt lagResponse() {
        return new Dokumentoversikt(new ArrayList<>(), null);
    }

    private Journalpost createJournalpost(Variantformat variantFormatKonst) {
        return createJournalpost(variantFormatKonst, NOW, Journalposttype.U);
    }

    private Journalpost createJournalpost(Variantformat variantFormatKonst, LocalDateTime sendt,
                                          Journalposttype kommunikasjonsretning) {
        ZoneId zone = ZoneId.systemDefault();
        Journalpost journalpost = new Journalpost();
        journalpost.setJournalpostId(JOURNAL_ID.getVerdi());
        journalpost.setTema(Tema.FOR);
        journalpost.setJournalstatus(Journalstatus.JOURNALFOERT);
        journalpost.setJournalposttype(kommunikasjonsretning);
        journalpost.setTittel(SØK_ENG_FØDSEL.getNavn());
        journalpost.setDatoOpprettet(Date.from(Instant.from(sendt.toInstant(zone.getRules().getOffset(sendt)))));
        journalpost.setDokumenter(new ArrayList<>());
        journalpost.getDokumenter().add(createDokumentinfo(variantFormatKonst,
            NAVSkjema.SKJEMA_ENGANGSSTØNAD_FØDSEL.getOffisiellKode(), SØK_ENG_FØDSEL.getNavn()));
        return journalpost;
    }

    private DokumentInfo createDokumentinfo(Variantformat variantformat, String brevkode, String tittel) {
        DokumentInfo dokumentinfo = new DokumentInfo();
        dokumentinfo.setDokumentInfoId(String.valueOf(DOKUMENT_ID++));
        dokumentinfo.setBrevkode(brevkode);
        dokumentinfo.setTittel(tittel);
        dokumentinfo.setLogiskeVedlegg(List.of());
        dokumentinfo.setDokumentvarianter(new ArrayList<>());
        dokumentinfo.getDokumentvarianter().add(new Dokumentvariant(variantformat, null, null, null, true, null));
        return dokumentinfo;
    }
}
