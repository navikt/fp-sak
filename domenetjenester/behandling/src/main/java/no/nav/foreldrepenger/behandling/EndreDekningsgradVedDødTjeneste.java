package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class EndreDekningsgradVedDødTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(EndreDekningsgradVedDødTjeneste.class);

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;

    EndreDekningsgradVedDødTjeneste() {
        // CDI
    }

    @Inject
    public EndreDekningsgradVedDødTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                           DekningsgradTjeneste dekningsgradTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           HistorikkRepository historikkRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
    }

    public void endreDekningsgradTil100(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var gjeldendeDekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(behandling);
        if (gjeldendeDekningsgrad.equals(Dekningsgrad._100)) {
            // Eneste kjente case for å endre dekningsgrad er ved barnets død, som alltid skal ha dekningsgrad på 100.
            // Om denne allerede er satt trenger vi ikke endre
            return;
        }
        oppdaterFagsakRelasjon(behandling, Dekningsgrad._100);
        lagHistorikkinnslag(behandling);
        LOG.info("Endrer dekningsgrad for behandling {} automatisk på grunn av død", behandlingId);
    }

    private void lagHistorikkinnslag(Behandling behandling) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.ENDRET_DEKNINGSGRAD);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());
        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.ENDRET_DEKNINGSGRAD)
            .medBegrunnelse("Dekningsgraden er endret fra 80% til 100% grunnet opplysninger om død");
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    private void oppdaterFagsakRelasjon(Behandling behandling, Dekningsgrad nyDekningsgrad) {
        fagsakRelasjonTjeneste.overstyrDekningsgrad(behandling.getFagsak(), nyDekningsgrad);
    }
}
