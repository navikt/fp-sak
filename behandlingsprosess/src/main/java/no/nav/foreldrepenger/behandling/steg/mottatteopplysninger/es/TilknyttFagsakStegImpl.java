package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@BehandlingStegRef(kode = "INSØK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class TilknyttFagsakStegImpl implements TilknyttFagsakSteg {

    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;

    TilknyttFagsakStegImpl() {
        // for CDI proxy
    }

    @Inject
    public TilknyttFagsakStegImpl(BehandlingRepositoryProvider provider,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                OppgaveTjeneste oppgaveTjeneste) {
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        avsluttTidligereRegistreringsoppgave(behandling);
        oppdaterEnhetMedAnnenPart(behandling);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void avsluttTidligereRegistreringsoppgave(Behandling behandling) {
        oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, OppgaveÅrsak.REGISTRER_SØKNAD);
    }

    private void oppdaterEnhetMedAnnenPart(Behandling behandling) {
        behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandling)
            .ifPresent(enhet -> behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.VEDTAKSLØSNINGEN, "Annen part"));
    }
}
