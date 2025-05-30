package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
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
        return oppgaveTjeneste.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(aktørId).stream().map(this::mapTilOppgaveDto).toList();
    }

    private OppgaveDto mapTilOppgaveDto(Oppgave oppgave) {
        var oppgaveType = getOppgaveTypeForKode(oppgave.oppgavetype());

        List<OppgaveDto.Dokument> dokumenter = Collections.emptyList();
        if (oppgaveType == OppgaveType.VUR_DOKUMENT && oppgave.journalpostId() != null) {
            dokumenter = dokumentArkivTjeneste.hentJournalpostForSak(new JournalpostId(oppgave.journalpostId()))
                .map(journalpost -> mapOgSorterDokumenter(journalpost.getJournalpostId(), journalpost))
                .orElseGet(Collections::emptyList);
        }

        List<OppgaveDto.Beskrivelse> beskrivelser = splittBeskrivelser(oppgave.beskrivelse());

        return new OppgaveDto(new OppgaveDto.OppgaveId(oppgave.id().toString()), oppgaveType, beskrivelser, dokumenter);
    }

    static OppgaveType getOppgaveTypeForKode(Oppgavetype oppgavetype) {
        return switch (oppgavetype) {
            case Oppgavetype.VURDER_KONSEKVENS_YTELSE -> OppgaveType.VUR_KONSEKVENS;
            case Oppgavetype.VURDER_DOKUMENT -> OppgaveType.VUR_DOKUMENT;
            default -> throw new IllegalArgumentException("Ukjent oppgavetype " + oppgavetype + " skal ikke være mulig å få her");
        };
    }

    private static List<OppgaveDto.Dokument> mapOgSorterDokumenter(JournalpostId journalpostId, ArkivJournalPost journalpost) {
        List<OppgaveDto.Dokument> dokumenter = new ArrayList<>();

        if (journalpost.getHovedDokument() != null) {
            dokumenter.add(
                new OppgaveDto.Dokument(journalpostId, journalpost.getHovedDokument().getDokumentId(), journalpost.getHovedDokument().getTittel()));
        }
        journalpost.getAndreDokument()
            .stream()
            .map(dokument -> new OppgaveDto.Dokument(journalpostId, dokument.getDokumentId(), dokument.getTittel()))
            .forEach(dokumenter::add);

        dokumenter.sort(Comparator.comparing(OppgaveDto.Dokument::tittel));

        return dokumenter;
    }

    static List<OppgaveDto.Beskrivelse> splittBeskrivelser(String oppgaveBeskrivelse) {
        List<OppgaveDto.Beskrivelse> beskrivelser = new ArrayList<>();

        if (oppgaveBeskrivelse != null && !oppgaveBeskrivelse.trim().isEmpty()) {
            // Splitter beskrivelse opp i (indeks 0) headere med innhold og (indeks 1) eventuell VL-kommentar
            String[] splittetBeskrivelse = oppgaveBeskrivelse.split("VL: ", 2);

            String resterendeBeskrivelse = splittetBeskrivelse[0].trim();
            splittPåHeaderOgLeggTilBeskrivelse(resterendeBeskrivelse, beskrivelser);

            if (splittetBeskrivelse.length > 1) {
                leggTilBeskrivelse(beskrivelser, null, List.of("VL: " + splittetBeskrivelse[1].trim().lines().collect(Collectors.joining("\n"))));
            }
        }
        return beskrivelser;
    }

    private static void splittPåHeaderOgLeggTilBeskrivelse(String resterendeBeskrivelse, List<OppgaveDto.Beskrivelse> beskrivelser) {
        // Matcher som finner mønsteret for header (--- tekst ---)
        Matcher headerMatcher = Pattern.compile("--- .*? ---").matcher(resterendeBeskrivelse);

        String currentHeader = "";
        int lastIndex = 0;

        while (headerMatcher.find()) {
            // Hvis det finnes tekst før den neste headeren, legg den til som beskrivelse med nåværende header
            if (lastIndex < headerMatcher.start()) {
                håndterBeskrivelse(beskrivelser, currentHeader, resterendeBeskrivelse.substring(lastIndex, headerMatcher.start()).trim());
            }
            currentHeader = headerMatcher.group();
            lastIndex = headerMatcher.end();
        }

        // Hvis det er tekst igjen etter siste headeren, legg den til
        if (lastIndex < resterendeBeskrivelse.length()) {
            håndterBeskrivelse(beskrivelser, currentHeader, resterendeBeskrivelse.substring(lastIndex).trim());
        }
    }

    private static void håndterBeskrivelse(List<OppgaveDto.Beskrivelse> beskrivelser, String header, String kommentar) {
        if (!kommentar.isEmpty()) {
            if (beskrivelser.isEmpty()) {
                // Hvis beskrivelser er tom er man på den første beskrivelsen, den skal ha spesiell håndtering
                splittOgLeggTilBeskrivelse(beskrivelser, header, kommentar);
            } else {
                leggTilBeskrivelse(beskrivelser, header, List.of(kommentar.split("\n")));
            }
        }
    }

    private static void splittOgLeggTilBeskrivelse(List<OppgaveDto.Beskrivelse> beskrivelser, String header, String kommentar) {
        List<String> kommentarer = List.of(kommentar.split("\n"));
        // Splitter opp slik at det kun vises maks 3 linjer i første beskrivelse
        if (kommentarer.size() > 3) {
            leggTilBeskrivelse(beskrivelser, header, kommentarer.subList(0, 3));
            leggTilBeskrivelse(beskrivelser, null, kommentarer.subList(3, kommentarer.size()));
        } else {
            leggTilBeskrivelse(beskrivelser, header, kommentarer);
        }
    }

    private static void leggTilBeskrivelse(List<OppgaveDto.Beskrivelse> beskrivelser, String header, List<String> kommentarer) {
        // Oppretter OppgaveDto.Beskrivelse med overskrift og kommentarer delt opp i linjer
        beskrivelser.add(new OppgaveDto.Beskrivelse(header, kommentarer));
    }
}
