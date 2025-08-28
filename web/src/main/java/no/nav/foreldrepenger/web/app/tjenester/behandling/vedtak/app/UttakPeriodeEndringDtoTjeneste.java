package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.uttak.UttakPeriodeEndringDto;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderEndringTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakHistorikkinnslagTjeneste;

@ApplicationScoped
public class UttakPeriodeEndringDtoTjeneste {

    private static final Set<AksjonspunktDefinisjon> PROSESS_UTTAK = Set.of(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER,
        AksjonspunktDefinisjon.FASTSETT_UTTAK_STORTINGSREPRESENTANT,
        AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE, AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST,
        AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD,
        AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);

    private static final Set<AksjonspunktDefinisjon> FAKTA_UTTAK = Set.of(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK,
        AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER, AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET,
        AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG,
        AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO);

    private FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    protected UttakPeriodeEndringDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UttakPeriodeEndringDtoTjeneste(FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste,
                                          YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fastsettePerioderEndringTjeneste = fastsettePerioderEndringTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    public List<UttakPeriodeEndringDto> hentEndringPåUttakPerioder(Totrinnsvurdering aksjonspunkt,
                                                                   Behandling behandling) {
        if (PROSESS_UTTAK.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            var endringer = fastsettePerioderEndringTjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(behandling.getId());
            if (!endringer.isEmpty()) {
                return endringer;
            }
        }
        if (FAKTA_UTTAK.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            // Filtrer på perioder med endring som i FUFT
            return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
                .map(yf -> FaktaUttakHistorikkinnslagTjeneste.utledPerioderMedEndring(yf.getOppgittFordeling().getPerioder(), yf.getGjeldendeFordeling().getPerioder()))
                .orElseGet(Collections::emptyList);
        }
        return Collections.emptyList();
    }
}
