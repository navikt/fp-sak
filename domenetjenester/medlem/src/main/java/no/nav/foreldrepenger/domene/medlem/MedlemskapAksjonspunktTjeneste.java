package no.nav.foreldrepenger.domene.medlem;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.api.BekreftBosattVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.api.BekreftErMedlemVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.api.BekreftOppholdVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.impl.AvklarFortsattMedlemskapAksjonspunkt;
import no.nav.foreldrepenger.domene.medlem.impl.BekreftBosattVurderingAksjonspunkt;
import no.nav.foreldrepenger.domene.medlem.impl.BekreftErMedlemVurderingAksjonspunkt;
import no.nav.foreldrepenger.domene.medlem.impl.BekreftOppholdsrettVurderingAksjonspunkt;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemskapAksjonspunktTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    MedlemskapAksjonspunktTjeneste() {
        // CDI
    }

    @Inject
    public MedlemskapAksjonspunktTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void aksjonspunktBekreftMeldlemVurdering(Long behandlingId, BekreftErMedlemVurderingAksjonspunktDto adapter) {
        new BekreftErMedlemVurderingAksjonspunkt(repositoryProvider).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktBekreftOppholdVurdering(Long behandlingId, BekreftOppholdVurderingAksjonspunktDto adapter) {
        new BekreftOppholdsrettVurderingAksjonspunkt(repositoryProvider).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktBekreftBosattVurdering(Long behandlingId, BekreftBosattVurderingAksjonspunktDto adapter) {
        new BekreftBosattVurderingAksjonspunkt(repositoryProvider).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktAvklarFortsattMedlemskap(Long behandlingId, AvklarFortsattMedlemskapAksjonspunktDto adapter) {
        new AvklarFortsattMedlemskapAksjonspunkt(repositoryProvider, skjæringstidspunktTjeneste, historikkTjenesteAdapter).oppdater(behandlingId, adapter);
    }
}
