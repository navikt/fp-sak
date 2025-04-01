package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;

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
        assertOppgave(oppgaver.get(0), OppgaveType.VUR_DOKUMENT, "vurderDokumentBeskrivelse", 3);
        assertOppgave(oppgaver.get(1), OppgaveType.VUR_KONSEKVENS, "vurderKonsekvensBeskrivelse", 0);
    }

    @Test
    void skal_hente_riktig_oppgavetype() {
        assertThat(OppgaveDtoTjeneste.getOppgaveTypeForKode(Oppgavetype.VURDER_DOKUMENT)).isEqualTo(OppgaveType.VUR_DOKUMENT);
        assertThat(OppgaveDtoTjeneste.getOppgaveTypeForKode(Oppgavetype.VURDER_KONSEKVENS_YTELSE)).isEqualTo(OppgaveType.VUR_KONSEKVENS);
    }

    @Test
    void skal_formatere_beskrivelse() {
        var beskrivelse = """
            --- 20.03.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---
            Bruker sier at han har søkt Foreldrepenger, han er i permisjon nå. Han har ikke fått svar, han skriver at saksnr er: 12341234.
            Han har AAP, så det er greit å vite om han får Foreldrepenger før man evt stanser denne ytelsen.
            --- 19.01.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---
            Må ringe bruker for å avklare AAP og Foreldrepenger.
            Undersøk dette før vi går videre
            VL: Søknad om foreldrepenger ved fødsel""";
        var beskrivelser = OppgaveDtoTjeneste.splittBeskrivelser(beskrivelse);

        assertThat(beskrivelser).hasSize(3);
        assertBeskrivelse(beskrivelser.getFirst(), "--- 20.03.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---",
            "Bruker sier at han har søkt Foreldrepenger, han er i permisjon nå. Han har ikke fått svar, han skriver at saksnr er: 12341234.",
            "Han har AAP, så det er greit å vite om han får Foreldrepenger før man evt stanser denne ytelsen.");
        assertBeskrivelse(beskrivelser.get(1), "--- 19.01.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---",
            "Må ringe bruker for å avklare AAP og Foreldrepenger.", "Undersøk dette før vi går videre");
        assertBeskrivelse(beskrivelser.getLast(), null, "VL: Søknad om foreldrepenger ved fødsel");
    }

    @Test
    void første_kommentar_splittes_for_over_tre_linjer() {
        var beskrivelse = """
            --- 20.03.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---
            Bruker sier at han har søkt Foreldrepenger, han er i permisjon nå. Han har ikke fått svar, han skriver at saksnr er: 12341234.
            Han har AAP, så det er greit å vite om han får Foreldrepenger før man evt stanser denne ytelsen.
            Det må også vurderes om det er riktig å stanse AAP.
            Ta kontakt med aktuell saksbehandler.
            VL: Søknad om foreldrepenger ved fødsel""";
        var beskrivelser = OppgaveDtoTjeneste.splittBeskrivelser(beskrivelse);

        assertThat(beskrivelser).hasSize(3);
        assertBeskrivelse(beskrivelser.getFirst(), "--- 20.03.2025 11:24 F_Z990245 E_Z990245 (Z990245, 0219) ---",
            "Bruker sier at han har søkt Foreldrepenger, han er i permisjon nå. Han har ikke fått svar, han skriver at saksnr er: 12341234.",
            "Han har AAP, så det er greit å vite om han får Foreldrepenger før man evt stanser denne ytelsen.",
            "Det må også vurderes om det er riktig å stanse AAP.");
        assertBeskrivelse(beskrivelser.get(1), null, "Ta kontakt med aktuell saksbehandler.");
        assertBeskrivelse(beskrivelser.getLast(), null, "VL: Søknad om foreldrepenger ved fødsel");
    }

    @Test
    void formaterBeskrivelse_håndterer_tom_beskrivelse() {
        assertThat(OppgaveDtoTjeneste.splittBeskrivelser("")).isEmpty();
    }

    private static void assertOppgave(OppgaveDto oppgave, OppgaveType type, String beskrivelse, int antallDokumenter) {
        assertThat(oppgave.oppgavetype()).isEqualTo(type);
        assertThat(oppgave.nyesteBeskrivelse().kommentarer()).containsExactly(beskrivelse);
        assertThat(oppgave.eldreBeskrivelser()).isEmpty();
        assertThat(oppgave.dokumenter()).hasSize(antallDokumenter);
    }

    private static void assertBeskrivelse(OppgaveDto.Beskrivelse beskrivelse, String header, String... kommentarer) {
        assertThat(beskrivelse.header()).isEqualTo(header);
        assertThat(beskrivelse.kommentarer()).containsExactly(kommentarer);
    }

    private static Oppgave opprettOppgave(Oppgavetype oppgavetype, String beskrivelse, String journalpostId) {
        return new Oppgave(99L, journalpostId, null, null, null, Tema.FOR.getOffisiellKode(), null, oppgavetype, null, 2, "4805",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, beskrivelse, null);
    }

    private static ArkivJournalPost opprettArkivJournalPost(JournalpostId journalpostId) {
        var hovedDokument = opprettDokument(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, "456");
        var andreDokumenter = List.of(opprettDokument(DokumentTypeId.DOK_REISE, "678"), opprettDokument(DokumentTypeId.KLAGE_DOKUMENT, "987"));
        return ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalpostId)
            .medTidspunkt(LocalDateTime.now())
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
