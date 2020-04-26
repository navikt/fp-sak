package no.nav.foreldrepenger.dokumentarkiv.journal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.journal.InngåendeJournalAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.ArkivSak;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Arkivfiltyper;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinnhold;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentkategorier;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumenttypeIder;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Mottakskanaler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.vedtak.felles.integrasjon.inngaaendejournal.InngaaendeJournalConsumer;

public class InngaaendeJournalAdapterImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private InngåendeJournalAdapter adapter; // objektet vi tester

    private InngaaendeJournalConsumer mockConsumer;

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("JP-ID");

    private ArkivFilType arkivFilTypeH;
    private VariantFormat variantFormatH;
    private DokumentKategori dokumentKategoriH;
    private DokumentTypeId dokumenttypeH;
    private static final String DOKUMENT_ID_H = "DOKID-H";

    @Before
    public void setup() {
        mockConsumer = mock(InngaaendeJournalConsumer.class);
        adapter = new InngåendeJournalAdapter(mockConsumer);

        dokumenttypeH = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        arkivFilTypeH = ArkivFilType.PDF;

        dokumentKategoriH = DokumentKategori.ELEKTRONISK_SKJEMA;

        variantFormatH = VariantFormat.ARKIV;

    }

    @Test
    public void test_hentEnhet_ok()
        throws HentJournalpostJournalpostIkkeFunnet, HentJournalpostJournalpostIkkeInngaaende,
        HentJournalpostUgyldigInput, HentJournalpostSikkerhetsbegrensning {

        final String ENHET = "4292";

        Dokumentinnhold dokinnholdHoved = lagDokumentinnhold(arkivFilTypeH, variantFormatH);
        Dokumentinformasjon dokinfoHoved = lagDokumentinformasjon(dokumentKategoriH, dokumenttypeH, DOKUMENT_ID_H, dokinnholdHoved);

        Mottakskanaler mottakskanal = new Mottakskanaler();

        InngaaendeJournalpost inngaaendeJournalpost = new InngaaendeJournalpost();
        inngaaendeJournalpost.setMottakskanal(mottakskanal);
        inngaaendeJournalpost.setJournaltilstand(Journaltilstand.ENDELIG);
        inngaaendeJournalpost.setHoveddokument(dokinfoHoved);
        ArkivSak sak = new ArkivSak();
        sak.setArkivSakId("123456");
        inngaaendeJournalpost.setArkivSak(sak);

        HentJournalpostResponse response = new HentJournalpostResponse();
        response.setInngaaendeJournalpost(inngaaendeJournalpost);

        when(mockConsumer.hentJournalpost(any(HentJournalpostRequest.class))).thenReturn(response);

        ArkivJournalPost arkivJournalPost = adapter.hentInngåendeJournalpostHoveddokument(JOURNALPOST_ID);

        assertThat(arkivJournalPost.getJournalEnhet()).isEmpty();

        inngaaendeJournalpost.setJournalfEnhet(ENHET);

        ArkivJournalPost arkivJournalPost2 = adapter.hentInngåendeJournalpostHoveddokument(JOURNALPOST_ID);

        assertThat(arkivJournalPost2.getJournalEnhet()).isPresent();
        assertThat(arkivJournalPost2.getJournalEnhet().get()).isEqualTo(ENHET);

    }


    private Dokumentinformasjon lagDokumentinformasjon(
        DokumentKategori dokumentKategori, DokumentTypeId dokumentTypeId, String dokumentId, Dokumentinnhold... dokumentinnholdArray) {

        Dokumentinformasjon dokinfo = new Dokumentinformasjon();

        if (dokumentKategori != null) {
            Dokumentkategorier dokumentkategori = new Dokumentkategorier();
            dokumentkategori.setValue(dokumentKategori.getOffisiellKode());
            dokinfo.setDokumentkategori(dokumentkategori);
        }
        if (dokumentTypeId != null) {
            DokumenttypeIder dokumenttypeId = new DokumenttypeIder();
            dokumenttypeId.setValue(dokumentTypeId.getOffisiellKode());
            dokinfo.setDokumenttypeId(dokumenttypeId);
        }
        dokinfo.setDokumentId(dokumentId);
        for (Dokumentinnhold dokumentinnhold : dokumentinnholdArray) {
            dokinfo.getDokumentInnholdListe().add(dokumentinnhold);
        }

        return dokinfo;
    }

    private Dokumentinnhold lagDokumentinnhold(ArkivFilType arkivFilType, VariantFormat variantFormat) {

        Dokumentinnhold dokumentinnhold = new Dokumentinnhold();

        if (arkivFilType != null) {
            Arkivfiltyper arkivfiltype = new Arkivfiltyper();
            arkivfiltype.setValue(arkivFilType.getOffisiellKode());
            dokumentinnhold.setArkivfiltype(arkivfiltype);
        }
        if (variantFormat != null) {
            Variantformater variantformat = new Variantformater();
            variantformat.setValue(variantFormat.getOffisiellKode());
            dokumentinnhold.setVariantformat(variantformat);
        }

        return dokumentinnhold;
    }

}
