package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentDto;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;

@ApplicationScoped
public class OppgaveDtoTjeneste {
    private OppgaveTjeneste oppgaveTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public OppgaveDtoTjeneste() {
        //For CDI
    }

    @Inject
    public OppgaveDtoTjeneste(OppgaveTjeneste oppgaveTjeneste, DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public List<OppgaveDto> mapTilDto(AktørId aktørId) {
        return oppgaveTjeneste.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(aktørId).stream().map(oppgave -> {
            var oppgaveType = getOppgaveTypeForKode(oppgave.oppgavetype());
            DokumentDto hovedDokument = null;
            List<DokumentDto> andreDokumenter = Collections.emptyList();
            if (oppgaveType == OppgaveType.VUR_DOKUMENT && oppgave.journalpostId() != null) {
                var optionalArkivPost = dokumentArkivTjeneste.hentJournalpostForSak(new JournalpostId(oppgave.journalpostId()));
                hovedDokument = optionalArkivPost.map(journalpost -> new DokumentDto(journalpost, journalpost.getHovedDokument())).orElse(null);
                andreDokumenter = optionalArkivPost.map(
                        journalpost -> journalpost.getAndreDokument().stream().map(dokument -> new DokumentDto(journalpost, dokument)).toList())
                    .orElse(Collections.emptyList());
            }
            List<OppgaveDto.Beskrivelse> beskrivelser = formaterBeskrivelse(oppgave.beskrivelse());
            var nyesteBeskrivelse = beskrivelser.isEmpty() ? null : beskrivelser.getFirst();
            List<OppgaveDto.Beskrivelse> eldreBeskrivelser =
                beskrivelser.size() > 1 ? beskrivelser.subList(1, beskrivelser.size()) : Collections.emptyList();
            return new OppgaveDto(oppgaveType, nyesteBeskrivelse, eldreBeskrivelser, hovedDokument, andreDokumenter);
        }).toList();
    }

    static OppgaveType getOppgaveTypeForKode(Oppgavetype oppgavetype) {
        return switch (oppgavetype) {
            case Oppgavetype.VURDER_KONSEKVENS_YTELSE -> OppgaveType.VUR_KONSEKVENS;
            case Oppgavetype.VURDER_DOKUMENT -> OppgaveType.VUR_DOKUMENT;
            default -> throw new IllegalArgumentException("Ukjent oppgavetype " + oppgavetype + " skal ikke være mulig å få her");
        };
    }

    static List<OppgaveDto.Beskrivelse> formaterBeskrivelse(String gosysBeskrivelse) {
        List<OppgaveDto.Beskrivelse> beskrivelser = new ArrayList<>();
        if (gosysBeskrivelse != null && !gosysBeskrivelse.trim().isEmpty()) {
            for (String beskrivelse : gosysBeskrivelse.split("\n\n")) {
                String[] headerOgKommentar = beskrivelse.split("\n", 2);
                if (headerOgKommentar.length == 2) {
                    beskrivelser.add(new OppgaveDto.Beskrivelse(headerOgKommentar[0], headerOgKommentar[1]));
                } else if (headerOgKommentar.length == 1) {
                    beskrivelser.add(new OppgaveDto.Beskrivelse("", headerOgKommentar[0]));
                }
            }
        }
        return beskrivelser;
    }
}
