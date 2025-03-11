package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;

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
            .map(oppgave -> new OppgaveDto(OppgaveType.fra(getOppgavetypeForKode(oppgave.oppgavetype())), oppgave.beskrivelse()))
            .toList();
    }

    Oppgavetype getOppgavetypeForKode(String kode) {
        return Arrays.stream(Oppgavetype.values())
            .filter(type -> type.getKode().equals(kode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke Oppgavetype for kode " + kode));
    }
}
