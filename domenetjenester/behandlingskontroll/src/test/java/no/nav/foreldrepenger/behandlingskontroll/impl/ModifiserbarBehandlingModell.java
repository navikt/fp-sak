package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Modell for testing som lar oss endre på referansedata uten å eksponere vanlig
 * api.
 */
public class ModifiserbarBehandlingModell {

    public static BehandlingModellImpl setupModell(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType, List<TestStegKonfig> resolve) {
        var finnSteg = DummySteg.map(resolve);

        var modell = new BehandlingModellImpl(behandlingType, fagsakYtelseType, finnSteg) {
            @Override
            protected void leggTilAksjonspunktDefinisjoner(BehandlingStegType stegType, BehandlingStegModellImpl entry) {
                // overstyrer denne - se under
            }
        };
        for (var konfig : resolve) {
            var stegType = konfig.getBehandlingStegType();

            // fake legg til behandlingSteg og vureringspunkter
            modell.leggTil(stegType, behandlingType, fagsakYtelseType);

            konfig.getInngangAksjonspunkter().forEach(a -> modell.internFinnSteg(stegType).leggTilAksjonspunktVurderingInngang(a));
            konfig.getUtgangAksjonspunkter().forEach(a -> modell.internFinnSteg(stegType).leggTilAksjonspunktVurderingUtgang(a));

        }
        return modell;

    }

}
