package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class AvklarHendelseAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var resultat = new ArrayList<AksjonspunktDefinisjon>();

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT)
            || input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE);
        }

        if (input.harBehandlingÅrsakRelatertTilDød() || input.isOpplysningerOmDødEndret()) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);
        }

        return resultat;
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }
}
