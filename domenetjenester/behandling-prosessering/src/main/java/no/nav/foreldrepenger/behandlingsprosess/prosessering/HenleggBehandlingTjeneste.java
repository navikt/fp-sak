package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
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
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private AksjonspunktkontrollTjeneste aksjonspunktKontrollTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingEventPubliserer eventPubliserer;
    private HistorikkinnslagRepository historikkRepository;


    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     AksjonspunktkontrollTjeneste aksjonspunktKontrollTjeneste,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     BehandlingEventPubliserer eventPubliserer) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.aksjonspunktKontrollTjeneste = aksjonspunktKontrollTjeneste;
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

        // Avbryt alle åpne autopunkt
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktAvbruttForHenleggelse(kontekst, behandling);
        }

        doForberedHenleggelse(behandling, lås, resultat, historikkAktør, begrunnelse);

        behandlingskontrollTjeneste.henleggBehandling(kontekst);
    }

    public void forberedHenleggelseFraSteg(Behandling behandling, BehandlingLås lås, BehandlingResultatType resultat, String begrunnelse) {
        doForberedHenleggelse(behandling, lås, resultat, HistorikkAktør.VEDTAKSLØSNINGEN, begrunnelse);
    }

    private void doForberedHenleggelse(Behandling behandling, BehandlingLås skriveLås, BehandlingResultatType resultat,
                                       HistorikkAktør historikkAktør, String begrunnelse) {
        aksjonspunktKontrollTjeneste.lagreAksjonspunkterAvbrutt(behandling, skriveLås, behandling.getÅpneAksjonspunkter());
        // Logistikk: Behandlingsresultat, historikk, brev og tasks
        lagreBehandlingsresultatForHenleggelse(behandling, skriveLås, resultat);
        lagHistorikkinnslagForHenleggelse(behandling, resultat, begrunnelse, historikkAktør);
        eventPubliserer.publiserBehandlingEvent(new BehandlingHenlagtEvent(behandling, resultat));
    }

    private void lagreBehandlingsresultatForHenleggelse(Behandling behandling, BehandlingLås skriveLås, BehandlingResultatType resultat) {
        var eksisterende = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (eksisterende.isEmpty()) {
            Behandlingsresultat.builder().medBehandlingResultatType(resultat).buildFor(behandling);
        } else {
            Behandlingsresultat.builderEndreEksisterende(eksisterende.get()).medBehandlingResultatType(resultat);
        }
        behandlingRepository.lagre(behandling, skriveLås);
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
