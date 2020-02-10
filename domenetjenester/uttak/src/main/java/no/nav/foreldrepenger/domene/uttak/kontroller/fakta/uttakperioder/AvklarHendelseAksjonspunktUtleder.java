package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class AvklarHendelseAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var resultat = new ArrayList<AksjonspunktDefinisjon>();

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT)
            || input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD)
            || input.isOpplysningerOmDødEndret()) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_INNVILGET)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_INNVILGET);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_OPPHØRT)) {
            resultat.add(AksjonspunktDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_OPPHØRT);
        }

        return resultat;
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }
}
