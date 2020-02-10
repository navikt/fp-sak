package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderEndringTjeneste;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.AvklarFaktaUttakPerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.UttakPeriodeEndringDto;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class UttakPeriodeEndringDtoTjeneste {
    private AvklarFaktaUttakPerioderTjeneste avklarFaktaUttakTjeneste;
    private FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste;

    protected UttakPeriodeEndringDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UttakPeriodeEndringDtoTjeneste(AvklarFaktaUttakPerioderTjeneste kontrollerFaktaUttakTjeneste,
                                          FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste) {
        this.avklarFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
        this.fastsettePerioderEndringTjeneste = fastsettePerioderEndringTjeneste;
    }

    public List<UttakPeriodeEndringDto> hentEndringPåUttakPerioder(Totrinnsvurdering aksjonspunkt,
                                                                   Behandling behandling,
                                                                   Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER) ||
            aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING)) {
            if (totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getYtelseFordelingGrunnlagEntitetId).isPresent()) {
                return avklarFaktaUttakTjeneste.finnEndringMellomOppgittOgGjeldendePerioder(
                    totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getYtelseFordelingGrunnlagEntitetId).get()); // NOSONAR
            }
            return avklarFaktaUttakTjeneste.finnEndringMellomOppgittOgGjeldendePerioder(behandling);
        }
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER) ||
            aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER) ||
            aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.TILKNYTTET_STORTINGET)) {
            if (totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getUttakResultatEntitetId).isPresent()) {
                return fastsettePerioderEndringTjeneste
                    .finnEndringerMellomOpprinneligOgOverstyrt(totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getUttakResultatEntitetId).get()); // NOSONAR
            }
            return fastsettePerioderEndringTjeneste.finnEndringerMellomOpprinneligOgOverstyrt(behandling);
        }
        return Collections.emptyList();
    }
}
