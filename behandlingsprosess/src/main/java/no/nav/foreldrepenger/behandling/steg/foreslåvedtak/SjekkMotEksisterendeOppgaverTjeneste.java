package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;

@ApplicationScoped
public class SjekkMotEksisterendeOppgaverTjeneste {
    private OppgaveTjeneste oppgaveTjeneste;

    SjekkMotEksisterendeOppgaverTjeneste() {
        // CDI proxy
    }

    @Inject
    public SjekkMotEksisterendeOppgaverTjeneste(OppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public List<AksjonspunktDefinisjon> sjekkMotEksisterendeGsakOppgaver(AktørId aktørid, Behandling behandling) {
        if (!SpesialBehandling.skalUttakVurderes(behandling) || sjekkMotEksisterendeGsakOppgaverUtført(behandling)) {
            return List.of();
        }
        return mapTilAksjonspunktDefinisjonerForOppgaver(oppgaveTjeneste.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(aktørid));
    }

    private boolean sjekkMotEksisterendeGsakOppgaverUtført(Behandling behandling) {
        return behandling.getAksjonspunkter()
            .stream()
            .anyMatch(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK) && ap.erUtført()
                || ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK) && ap.erUtført());
    }

    static List<AksjonspunktDefinisjon> mapTilAksjonspunktDefinisjonerForOppgaver(List<Oppgave> oppgaver) {
        return oppgaver.stream()
            .map(Oppgave::oppgavetype)
            .map(oppgavetype -> Objects.equals(oppgavetype,
                Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode()) ? AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK : AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK)
            .distinct()
            .toList();
    }
}
