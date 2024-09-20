package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public class Transisjoner {

    private static final List<StegTransisjon> ALLE_TRANSISJONER = Arrays.asList(
            new Startet(),
            new Utført(),
            new HenleggelseTransisjon(),
            new SettPåVent(),
            new TilbakeføringTransisjon(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT.getId()),
            new TilbakeføringTransisjon(FellesTransisjoner.TILBAKEFØRT_TIL_MEDLEMSKAP.getId(), BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_FATTE_VEDTAK.getId(), BehandlingStegType.FATTE_VEDTAK),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_VEDTAK.getId(), BehandlingStegType.FORESLÅ_VEDTAK),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT.getId(),
                    BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT.getId(),
                    BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN.getId(), BehandlingStegType.SØKNADSFRIST_FORELDREPENGER),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_BEREGN_YTELSE.getId(), BehandlingStegType.BEREGN_YTELSE),
            new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_IVERKSETT_VEDTAK.getId(), BehandlingStegType.IVERKSETT_VEDTAK),
            new SpolFremoverTransisjon(BehandlingStegType.KONTROLLER_FAKTA),
            new SpolFremoverTransisjon(BehandlingStegType.DEKNINGSGRAD),
            new SpolFremoverTransisjon(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING),
            new SpolFremoverTransisjon(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG),
            new SpolFremoverTransisjon(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
            new SpolFremoverTransisjon(BehandlingStegType.SØKERS_RELASJON_TIL_BARN),
            new SpolFremoverTransisjon(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR),
            new SpolFremoverTransisjon(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP),
            new SpolFremoverTransisjon(BehandlingStegType.BEREGN_YTELSE),
            new SpolFremoverTransisjon(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE));

    private Transisjoner() {
        // skal ikke instansieres
    }

    public static StegTransisjon finnTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        for (var transisjon : ALLE_TRANSISJONER) {
            if (transisjon.getId().equals(transisjonIdentifikator.getId())) {
                return transisjon;
            }
        }
        throw new IllegalArgumentException("Ukjent transisjon: " + transisjonIdentifikator);
    }
}
