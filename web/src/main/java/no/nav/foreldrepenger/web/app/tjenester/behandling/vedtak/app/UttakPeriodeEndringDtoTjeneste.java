package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnresultatgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.uttak.UttakPeriodeEndringDto;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderEndringTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakHistorikkinnslagTjeneste;

@ApplicationScoped
public class UttakPeriodeEndringDtoTjeneste {

    private static final Set<AksjonspunktDefinisjon> PROSESS_UTTAK = Set.of(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER,
        AksjonspunktDefinisjon.FASTSETT_UTTAK_STORTINGSREPRESENTANT, AksjonspunktDefinisjon.FASTSETT_UTTAK_ETTER_NESTE_SAK,
        AksjonspunktDefinisjon.KONTROLLER_ANNENPART_EØS,
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
                                                                   Behandling behandling,
                                                                   Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        if (PROSESS_UTTAK.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            return totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getUttakResultatEntitetId)
                .map(urid -> fastsettePerioderEndringTjeneste.finnEndringerMellomOpprinneligOgOverstyrt(urid))
                .orElseGet(() -> fastsettePerioderEndringTjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(behandling.getId()));
        }
        if (FAKTA_UTTAK.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            // Filtrer på perioder med endring som i FUFT
            var yfAggregat = totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getYtelseFordelingGrunnlagEntitetId)
                .map(yfid -> ytelseFordelingTjeneste.hentAggregatForGrunnlagId(yfid))
                .orElseGet(() -> ytelseFordelingTjeneste.hentAggregat(behandling.getId()));
            if (yfAggregat == null) {
                return List.of();
            }
            return FaktaUttakHistorikkinnslagTjeneste.utledPerioderMedEndring(yfAggregat.getOppgittFordeling().getPerioder(),
                            yfAggregat.getGjeldendeFordeling().getPerioder());
        }
        return Collections.emptyList();
    }
}
