package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;

@ApplicationScoped
public class DokumentForhåndsvisningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentForhåndsvisningTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private KlageRepository klageRepository;
    private Brev brev;

    public DokumentForhåndsvisningTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentForhåndsvisningTjeneste(BehandlingRepository behandlingRepository,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           BehandlingVedtakRepository behandlingVedtakRepository,
                                           KlageRepository klageRepository,
                                           Brev brev) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageRepository = klageRepository;
        this.brev = brev;
    }

    public byte[] forhåndsvisBrev(DokumentbestillingDto bestilling) {
        LOG.info("Forhåndsviser {} brev for {}", bestilling.getDokumentMal(), bestilling.getBehandlingUuid());

        if (bestilling.getDokumentMal() == null) {
            LOG.info("Utleder dokumentMal for {}", bestilling.getBehandlingUuid());

            var behandling = behandlingRepository.hentBehandling(bestilling.getBehandlingUuid());
            var resultat = behandlingsresultatRepository.hent(behandling.getId());
            var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());

            var dokumentMal = velgDokumentMalForVedtak(behandling,
                resultat.getBehandlingResultatType(),
                vedtak.getVedtakResultatType(),
                vedtak.isBeslutningsvedtak(),
                klageRepository);

            LOG.info("Utleder {} dokumentMal for {}", dokumentMal, bestilling.getBehandlingUuid());
            bestilling.setDokumentMal(dokumentMal.getKode());
        }

        return brev.forhåndsvis(bestilling);
    }
}
