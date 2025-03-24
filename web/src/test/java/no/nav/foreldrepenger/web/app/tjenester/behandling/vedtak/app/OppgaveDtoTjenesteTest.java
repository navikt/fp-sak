package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;

@ExtendWith(MockitoExtension.class)
class OppgaveDtoTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjenesteMock;
    @InjectMocks
    private OppgaveDtoTjeneste oppgaveDtoTjeneste;

    @Test
    void henter_oppgaver_og_mapper_til_dto() {
        var vurderDokumentJournalpostId = new JournalpostId("123");
        Oppgave vurderDokument = opprettOppgave(Oppgavetype.VURDER_DOKUMENT, "vurderDokumentBeskrivelse", vurderDokumentJournalpostId.getVerdi());
        Oppgave vurderKonsekvens = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE, "vurderKonsekvensBeskrivelse", null);
        List<Oppgave> forventedeOppgaver = List.of(vurderDokument, vurderKonsekvens);
        ArkivJournalPost arkivJournalPost = opprettArkivJournalPost(vurderDokumentJournalpostId);
        when(oppgaveTjenesteMock.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(AKTØR_ID)).thenReturn(forventedeOppgaver);
        when(dokumentArkivTjenesteMock.hentJournalpostForSak(vurderDokumentJournalpostId)).thenReturn(Optional.of(arkivJournalPost));

        var oppgaver = oppgaveDtoTjeneste.mapTilDto(AKTØR_ID);

        assertThat(oppgaver).hasSize(2);
        assertThat(oppgaver.getFirst().oppgavetype()).isEqualTo(OppgaveType.VUR_DOKUMENT);
        assertThat(oppgaver.getFirst().nyesteBeskrivelse().kommentar()).isEqualTo(forventedeOppgaver.getFirst().beskrivelse());
        assertThat(oppgaver.getFirst().eldreBeskrivelser()).isEmpty();
        assertThat(oppgaver.getFirst().hovedDokument().getDokumentId()).isEqualTo(arkivJournalPost.getHovedDokument().getDokumentId());
        assertThat(oppgaver.getFirst().andreDokumenter()).hasSameSizeAs(arkivJournalPost.getAndreDokument());
        assertThat(oppgaver.getLast().oppgavetype()).isEqualTo(OppgaveType.VUR_KONSEKVENS);
        assertThat(oppgaver.getLast().nyesteBeskrivelse().kommentar()).isEqualTo(forventedeOppgaver.getLast().beskrivelse());
        assertThat(oppgaver.getLast().eldreBeskrivelser()).isEmpty();
        assertThat(oppgaver.getLast().hovedDokument()).isNull();
        assertThat(oppgaver.getLast().andreDokumenter()).isEmpty();
    }

    @Test
    void skal_hente_riktig_oppgavetype() {
        assertThat(OppgaveDtoTjeneste.getOppgaveTypeForKode(Oppgavetype.VURDER_DOKUMENT)).isEqualTo(OppgaveType.VUR_DOKUMENT);
        assertThat(OppgaveDtoTjeneste.getOppgaveTypeForKode(Oppgavetype.VURDER_KONSEKVENS_YTELSE)).isEqualTo(OppgaveType.VUR_KONSEKVENS);
    }

    @Test
    void skal_formatere_beskrivelse() {
        var beskrivelse = "header\nkommentarMedHeader\n\nheader2\nkommentarMedHeader2\n\nkommentarUtenHeader";
        var beskrivelser = OppgaveDtoTjeneste.formaterBeskrivelse(beskrivelse);

        assertThat(beskrivelser).hasSize(3);
        assertThat(beskrivelser.getFirst().header()).isEqualTo("header");
        assertThat(beskrivelser.getFirst().kommentar()).isEqualTo("kommentarMedHeader");
        assertThat(beskrivelser.get(1).header()).isEqualTo("header2");
        assertThat(beskrivelser.get(1).kommentar()).isEqualTo("kommentarMedHeader2");
        assertThat(beskrivelser.getLast().header()).isEmpty();
        assertThat(beskrivelser.getLast().kommentar()).isEqualTo("kommentarUtenHeader");
    }

    @Test
    void formaterBeskrivelse_håndterer_tom_beskrivelse() {
        var beskrivelser = OppgaveDtoTjeneste.formaterBeskrivelse("");
        assertThat(beskrivelser).isEmpty();
    }

    private static Oppgave opprettOppgave(Oppgavetype oppgavetype, String beskrivelse, String journalpostId) {
        return new Oppgave(99L, journalpostId, null, null, null, Tema.FOR.getOffisiellKode(), null, oppgavetype, null, 2, "4805",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, beskrivelse, null);
    }

    private static ArkivJournalPost opprettArkivJournalPost(JournalpostId journalpostId) {
        var hovedDokument = opprettDokument(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, "456");
        ArrayList<ArkivDokument> andreDokumenter = new ArrayList<>();
        andreDokumenter.add(opprettDokument(DokumentTypeId.DOK_REISE, "678"));
        andreDokumenter.add(opprettDokument(DokumentTypeId.KLAGE_DOKUMENT, "987"));
        return ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalpostId)
            .medTidspunkt(LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.of(10, 10)))
            .medHoveddokument(hovedDokument)
            .medAndreDokument(andreDokumenter)
            .build();
    }

    private static ArkivDokument opprettDokument(DokumentTypeId dokumentTypeId, String dokumentId) {
        return ArkivDokument.Builder.ny()
            .medTittel(dokumentTypeId.getNavn())
            .medDokumentId(dokumentId)
            .medDokumentTypeId(dokumentTypeId)
            .medAlleDokumenttyper(Set.of(dokumentTypeId))
            .build();
    }
}
