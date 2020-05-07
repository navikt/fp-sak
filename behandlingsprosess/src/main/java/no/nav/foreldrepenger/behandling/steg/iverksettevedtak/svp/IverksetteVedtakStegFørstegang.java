package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.svp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegTilgrensendeFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.VurderOpphørAvYtelserTask;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-002") // Førstegangsbehandling
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class IverksetteVedtakStegFørstegang extends IverksetteVedtakStegTilgrensendeFelles {

    IverksetteVedtakStegFørstegang() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegFørstegang(BehandlingRepositoryProvider repositoryProvider,
                                             @FagsakYtelseTypeRef("SVP") OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                             VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                             IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, opprettProsessTaskIverksett, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse);
    }
    @Override
    public List<String> getInitielleTasks() {
        return List.of(VurderOpphørAvYtelserTask.TASKTYPE);
    }
}
