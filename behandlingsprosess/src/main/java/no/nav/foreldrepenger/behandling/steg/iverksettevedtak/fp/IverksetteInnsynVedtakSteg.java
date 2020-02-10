package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteInnsynVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-006") //Innsyn
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class IverksetteInnsynVedtakSteg extends IverksetteInnsynVedtakStegFelles {

    IverksetteInnsynVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    IverksetteInnsynVedtakSteg(DokumentBestillerApplikasjonTjeneste dokumentBestillerTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(dokumentBestillerTjeneste, repositoryProvider);
    }
}
