package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingHenlagtEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingEventPubliserer eventPubliserer;
    private HistorikkinnslagRepository historikkRepository;


    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            BehandlingEventPubliserer eventPubliserer) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.historikkRepository = repositoryProvider.getHistorikkinnslagRepository();
    }

    public void henleggBehandlingManuell(Long behandlingId, BehandlingResultatType resultat, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        doHenleggBehandling(behandling, lås, resultat, HistorikkAktør.SAKSBEHANDLER, begrunnelse);
    }

    public void henleggBehandlingManuell(Behandling behandling, BehandlingLås lås, BehandlingResultatType resultat, String begrunnelse) {
        doHenleggBehandling(behandling, lås, resultat, HistorikkAktør.SAKSBEHANDLER, begrunnelse);
    }

    public void henleggBehandlingTeknisk(Behandling behandling, BehandlingLås lås, BehandlingResultatType resultat, String begrunnelse) {
        doHenleggBehandling(behandling, lås, resultat, HistorikkAktør.VEDTAKSLØSNINGEN, begrunnelse);
    }

    private void doHenleggBehandling(Behandling behandling, BehandlingLås lås, BehandlingResultatType resultat, HistorikkAktør historikkAktør, String begrunnelse) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        if (behandling.isBehandlingPåVent()) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktAvbruttForHenleggelse(behandling, kontekst);
        }
        behandlingskontrollTjeneste.henleggBehandling(kontekst, resultat);
        eventPubliserer.publiserBehandlingEvent(new BehandlingHenlagtEvent(behandling, resultat));

        lagHistorikkinnslagForHenleggelse(behandling, resultat, begrunnelse, historikkAktør);
    }

    public void lagHistorikkInnslagForHenleggelseFraSteg(Behandling behandling, BehandlingResultatType resultat, String begrunnelse) {
        lagHistorikkinnslagForHenleggelse(behandling, resultat, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void lagHistorikkinnslagForHenleggelse(Behandling behandling, BehandlingResultatType resultat, String begrunnelse, HistorikkAktør aktør) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(aktør)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medTittel("Behandling er henlagt")
            .addLinje(resultat.getNavn())
            .addLinje(begrunnelse)
            .build();

        historikkRepository.lagre(historikkinnslag);
    }
}
