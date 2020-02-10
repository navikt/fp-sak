package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegTilgrensendeFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-004") // Revurdering
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class IverksetteVedtakStegRevurdering extends IverksetteVedtakStegTilgrensendeFelles {

    IverksetteVedtakStegRevurdering() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                             @FagsakYtelseTypeRef("FP") OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                             VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                             IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, opprettProsessTaskIverksett, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse);
    }

    @Override
    public List<String> getInitielleTasks() {
        return List.of(StartBerørtBehandlingTask.TASKTYPE);
    }
}
