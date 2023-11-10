package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;

@BehandlingStegRef(BehandlingStegType.IVERKSETT_VEDTAK)
@BehandlingTypeRef(BehandlingType.INNSYN) // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteInnsynVedtakStegFelles implements IverksetteVedtakSteg {
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private BehandlingVedtakEventPubliserer eventPubliserer;

    public IverksetteInnsynVedtakStegFelles() {
        // for CDI proxy
    }

    @Inject
    protected IverksetteInnsynVedtakStegFelles(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                               BehandlingRepositoryProvider repositoryProvider,
                                               BehandlingVedtakEventPubliserer eventPubliserer) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.eventPubliserer = eventPubliserer;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var bestillBrevDto = brevDto(behandling);
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);

        var vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        if (vedtak != null) {
            vedtak.setIverksettingStatus(IverksettingStatus.IVERKSATT);
            vedtakRepository.lagre(vedtak, kontekst.getSkriveLås());
            eventPubliserer.fireEvent(vedtak, behandling);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private BestillBrevDto brevDto(Behandling behandling) {
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        var begrunnelse = ap.getBegrunnelse();
        var fritekst = nullOrEmpty(begrunnelse) ? " " : begrunnelse;

        return new BestillBrevDto(behandling.getId(), behandling.getUuid(), DokumentMalType.INNSYN_SVAR, fritekst);
    }

    private boolean nullOrEmpty(String begrunnelse) {
        return Objects.isNull(begrunnelse) || Objects.equals(begrunnelse, "");
    }
}
