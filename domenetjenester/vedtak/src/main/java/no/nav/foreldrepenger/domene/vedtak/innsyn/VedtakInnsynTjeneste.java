package no.nav.foreldrepenger.domene.vedtak.innsyn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;

@ApplicationScoped
public class VedtakInnsynTjeneste {

    private LagretVedtakRepository lagretVedtakRepository;

    public VedtakInnsynTjeneste() {
        //CDI
    }

    @Inject
    public VedtakInnsynTjeneste(LagretVedtakRepository lagretVedtakRepository) {
        this.lagretVedtakRepository = lagretVedtakRepository;
    }

    public String hentVedtaksdokument(Long behandlingId) {
        return VedtakXMLTilHTMLTransformator.transformer(lagretVedtakRepository.hentLagretVedtakForBehandling(behandlingId).getXmlClob(), behandlingId);
    }

}
