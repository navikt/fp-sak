package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.AvklarFaktaUttakPerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.UttakPeriodeEndringDto;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderEndringTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakHistorikkinnslagTjeneste;

@ApplicationScoped
public class UttakPeriodeEndringDtoTjeneste {

    private static final Set<AksjonspunktDefinisjon> PROSESS_UTTAK = Set.of(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER,
        AksjonspunktDefinisjon.FASTSETT_UTTAK_STORTINGSREPRESENTANT, AksjonspunktDefinisjon.KONTROLLER_ANNENPART_EØS,
        AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE, AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST,
        AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_FORDELING_AV_STØNADSPERIODEN, AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD,
        AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);

    private static final Set<AksjonspunktDefinisjon> FAKTA_OM_UTTAK = Set.of(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
        AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING, AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK,
        AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO,
        AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET,
        AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);

    private static final Set<AksjonspunktDefinisjon> FAKTA_UTTAK = Set.of(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK,
        AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER, AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET,
        AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG,
        AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO);


    private AvklarFaktaUttakPerioderTjeneste avklarFaktaUttakTjeneste;
    private FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    protected UttakPeriodeEndringDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UttakPeriodeEndringDtoTjeneste(AvklarFaktaUttakPerioderTjeneste kontrollerFaktaUttakTjeneste,
                                          FastsettePerioderEndringTjeneste fastsettePerioderEndringTjeneste,
                                          YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.avklarFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
        this.fastsettePerioderEndringTjeneste = fastsettePerioderEndringTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    public List<UttakPeriodeEndringDto> hentEndringPåUttakPerioder(Totrinnsvurdering aksjonspunkt,
                                                                   Behandling behandling,
                                                                   Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        if (FAKTA_OM_UTTAK.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            return totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getYtelseFordelingGrunnlagEntitetId)
                .map(yfid -> avklarFaktaUttakTjeneste.finnEndringMellomOppgittOgGjeldendePerioder(yfid))
                .orElseGet(() -> avklarFaktaUttakTjeneste.finnEndringMellomOppgittOgGjeldendePerioderForBehandling(behandling.getId()));
        }

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
            return yfAggregat != null ? FaktaUttakHistorikkinnslagTjeneste.utledPerioderMedEndring(yfAggregat.getOppgittFordeling().getPerioder(),
                yfAggregat.getGjeldendeFordeling().getPerioder()) : List.of();
        }
        return Collections.emptyList();
    }
}
