package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.domene.uttak.fakta.dokumentasjon.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderUttakDokumentasjonDto.class, adapter = AksjonspunktOppdaterer.class)
class VurderUttakDokumentasjonOppdaterer implements AksjonspunktOppdaterer<VurderUttakDokumentasjonDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private VurderUttakDokumentasjonAksjonspunktUtleder utleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    VurderUttakDokumentasjonOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                       VurderUttakDokumentasjonAksjonspunktUtleder utleder,
                                       UttakInputTjeneste uttakInputTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.utleder = utleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    VurderUttakDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderUttakDokumentasjonDto dto, AksjonspunktOppdaterParameter param) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder();

        var vurderingTimeline = new LocalDateTimeline<>(
            dto.getVurderingBehov().stream().map(vb -> new LocalDateSegment<>(vb.fom(), vb.tom(), vb)).toList());
        var yfTimeline = new LocalDateTimeline<>(gjeldendePerioder.stream().map(op -> new LocalDateSegment<>(op.getFom(), op.getTom(), op)).toList());

        var nyFordeling = yfTimeline.combine(vurderingTimeline, ((datoInterval, oppgittPeriode, vurdering) -> {
            var nyPeriode = OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode.getValue())
                .medPeriode(datoInterval.getFomDato(), datoInterval.getTomDato())
                .medDokumentasjonVurdering(mapVurdering(vurdering.getValue()))
                .build();
            return new LocalDateSegment<>(datoInterval, nyPeriode);
        }), LocalDateTimeline.JoinStyle.LEFT_JOIN).toSegments().stream().map(s -> s.getValue()).toList();

        ytelseFordelingTjeneste.overstyrSøknadsperioder(param.getBehandlingId(), nyFordeling, List.of());;
        //TODO TFP-4873 historikkinnslag
        return OppdateringResultat.utenTransisjon().medBeholdAksjonspunktÅpent(!harLøstAksjonspunktet(param)).build();
    }

    private boolean harLøstAksjonspunktet(AksjonspunktOppdaterParameter param) {
        var input = uttakInputTjeneste.lagInput(param.getBehandlingId());
        return utleder.utledAksjonspunkterFor(input).isEmpty();
    }

    private DokumentasjonVurdering mapVurdering(DokumentasjonVurderingBehovDto dto) {
        return switch (dto.type()) {
            case UTSETTELSE -> mapUtsettelseVurdering(dto.årsak(), dto.vurdering());
        };
    }

    private DokumentasjonVurdering mapUtsettelseVurdering(DokumentasjonVurderingBehovDto.Årsak årsak,
                                                          DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        return switch (årsak) {
            case INNLEGGELSE_SØKER -> vurdering.equals(
                DokumentasjonVurderingBehovDto.Vurdering.GODKJENT) ? DokumentasjonVurdering.INNLEGGELSE_SØKER_GODKJENT : DokumentasjonVurdering.INNLEGGELSE_SØKER_IKKE_GODKJENT;
            case INNLEGGELSE_BARN -> vurdering.equals(
                DokumentasjonVurderingBehovDto.Vurdering.GODKJENT) ? DokumentasjonVurdering.INNLEGGELSE_BARN_GODKJENT : DokumentasjonVurdering.INNLEGGELSE_BARN_IKKE_GODKJENT;
            case HV_OVELSE -> vurdering.equals(
                DokumentasjonVurderingBehovDto.Vurdering.GODKJENT) ? DokumentasjonVurdering.HV_OVELSE_GODKJENT : DokumentasjonVurdering.HV_OVELSE_IKKE_GODKJENT;
            case NAV_TILTAK -> vurdering.equals(
                DokumentasjonVurderingBehovDto.Vurdering.GODKJENT) ? DokumentasjonVurdering.NAV_TILTAK_GODKJENT : DokumentasjonVurdering.NAV_TILTAK_IKKE_GODKJENT;
            case SYKDOM_SØKER -> vurdering.equals(
                DokumentasjonVurderingBehovDto.Vurdering.GODKJENT) ? DokumentasjonVurdering.SYKDOM_SØKER_GODKJENT : DokumentasjonVurdering.SYKDOM_SØKER_IKKE_GODKJENT;
        };
    }
}
