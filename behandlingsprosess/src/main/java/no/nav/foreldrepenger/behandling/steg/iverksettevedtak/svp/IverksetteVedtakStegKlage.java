package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.impl.OpprettProsessTaskIverksettKlage;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-003") //Klage
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class IverksetteVedtakStegKlage extends IverksetteVedtakStegFelles {

    IverksetteVedtakStegKlage() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegKlage(BehandlingRepositoryProvider repositoryProvider,
                                       OpprettProsessTaskIverksettKlage opprettProsessTaskIverksettKlage) {
        super(repositoryProvider, opprettProsessTaskIverksettKlage);
    }
}
