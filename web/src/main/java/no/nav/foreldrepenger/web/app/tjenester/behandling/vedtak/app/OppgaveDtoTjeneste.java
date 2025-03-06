package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;

@ApplicationScoped
public class OppgaveDtoTjeneste {
    private OppgaveTjeneste oppgaveTjeneste;

    public OppgaveDtoTjeneste() {
        //For CDI
    }

    @Inject
    public OppgaveDtoTjeneste(OppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public List<OppgaveDto> mapTilDto(AktørId aktørId) {
        return hentÅpneOppgaver(aktørId).stream().map(oppgave -> new OppgaveDto(oppgave.oppgavetype(), oppgave.beskrivelse())).toList();
    }

    private List<Oppgave> hentÅpneOppgaver(AktørId aktørId) {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.addAll(oppgaveTjeneste.hentÅpneVurderKonsekvensOppgaver(aktørId));
        oppgaver.addAll(oppgaveTjeneste.hentÅpneVurderDokumentOppgaver(aktørId));
        return oppgaver;
    }
}
