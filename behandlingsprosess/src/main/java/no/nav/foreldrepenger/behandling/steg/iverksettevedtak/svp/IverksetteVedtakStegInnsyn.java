package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.svp;

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
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class IverksetteVedtakStegInnsyn extends IverksetteInnsynVedtakStegFelles {

    IverksetteVedtakStegInnsyn() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegInnsyn(DokumentBestillerApplikasjonTjeneste dokumentBestillerTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(dokumentBestillerTjeneste, repositoryProvider);
    }
}
