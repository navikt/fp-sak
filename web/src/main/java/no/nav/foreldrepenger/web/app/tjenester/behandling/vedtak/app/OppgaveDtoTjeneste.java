package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;

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
        return oppgaveTjeneste.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(aktørId)
            .stream()
            .map(oppgave -> new OppgaveDto(OppgaveType.fraKode(oppgave.oppgavetype()), oppgave.beskrivelse()))
            .toList();
    }
}
