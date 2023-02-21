package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.ALENEOMSORG_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.ALENEOMSORG_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.BARE_SØKER_RETT_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.BARE_SØKER_RETT_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.HV_OVELSE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.HV_OVELSE_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_BARN_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_BARN_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.NAV_TILTAK_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.NAV_TILTAK_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderUttakDokumentasjonDto.class, adapter = AksjonspunktOppdaterer.class)
class VurderUttakDokumentasjonOppdaterer implements AksjonspunktOppdaterer<VurderUttakDokumentasjonDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private VurderUttakDokumentasjonAksjonspunktUtleder utleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private VurderUttakDokumentasjonHistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    VurderUttakDokumentasjonOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                       VurderUttakDokumentasjonAksjonspunktUtleder utleder,
                                       VurderUttakDokumentasjonHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                       UttakInputTjeneste uttakInputTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.utleder = utleder;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    VurderUttakDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderUttakDokumentasjonDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getVurderingBehov().isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en vurdering");
        }

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeFordeling().getPerioder();

        var vurderingTimeline = new LocalDateTimeline<>(
            dto.getVurderingBehov().stream().map(vb -> new LocalDateSegment<>(vb.fom(), vb.tom(), vb)).toList());
        var yfTimeline = new LocalDateTimeline<>(gjeldendePerioder.stream().map(op -> new LocalDateSegment<>(op.getFom(), op.getTom(), op)).toList());

        if (vurderingTimeline.getMinLocalDate().isBefore(yfTimeline.getMinLocalDate())
            || vurderingTimeline.getMaxLocalDate().isAfter(yfTimeline.getMaxLocalDate())) {
            throw new IllegalArgumentException("Vurdering " + vurderingTimeline + " ligger utenfor yf perioder " + yfTimeline);
        }

        var nyFordeling = yfTimeline.combine(vurderingTimeline, ((datoInterval, oppgittPeriode, vurdering) -> {
            var nyPeriode = OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode.getValue())
                .medPeriode(datoInterval.getFomDato(), datoInterval.getTomDato())
                .medDokumentasjonVurdering(vurdering == null || vurdering.getValue().vurdering() == null
                    ? oppgittPeriode.getValue().getDokumentasjonVurdering() :  mapVurdering(vurdering.getValue()))
                .build();
            return new LocalDateSegment<>(datoInterval, nyPeriode);
        }), LocalDateTimeline.JoinStyle.LEFT_JOIN).toSegments().stream().map(s -> s.getValue()).toList();

        ytelseFordelingTjeneste.overstyrSøknadsperioder(param.getBehandlingId(), nyFordeling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(dto, gjeldendePerioder);
        return OppdateringResultat.utenTransisjon().medBeholdAksjonspunktÅpent(!harLøstAksjonspunktet(param)).build();
    }

    private boolean harLøstAksjonspunktet(AksjonspunktOppdaterParameter param) {
        var input = uttakInputTjeneste.lagInput(param.getBehandlingId());
        return utleder.utledAksjonspunkterFor(input).isEmpty();
    }

    static DokumentasjonVurdering mapVurdering(DokumentasjonVurderingBehovDto dto) {
        return switch (dto.type()) {
            case UTSETTELSE -> mapUtsettelseVurdering(dto.årsak(), dto.vurdering());
            case UTTAK -> mapUttak(dto.årsak(), dto.vurdering());
            case OVERFØRING -> mapOverføringVurdering(dto.årsak(), dto.vurdering());
        };
    }

    private static DokumentasjonVurdering mapUttak(DokumentasjonVurderingBehov.Behov.Årsak årsak, DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        return switch (årsak) {
            case AKTIVITETSKRAV_INNLAGT, AKTIVITETSKRAV_IKKE_OPPGITT, AKTIVITETSKRAV_ARBEID_OG_UTDANNING, AKTIVITETSKRAV_ARBEID,
                AKTIVITETSKRAV_INTROPROG, AKTIVITETSKRAV_KVALPROG, AKTIVITETSKRAV_UTDANNING, AKTIVITETSKRAV_TRENGER_HJELP ->
                switch (vurdering) {
                    case GODKJENT -> MORS_AKTIVITET_GODKJENT;
                    case IKKE_GODKJENT -> MORS_AKTIVITET_IKKE_GODKJENT;
                    case IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
                };
            case TIDLIG_OPPSTART_FAR -> switch (vurdering) {
                case GODKJENT -> TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;
            };
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };
    }

    @SuppressWarnings("DuplicatedCode")
    private static DokumentasjonVurdering mapOverføringVurdering(DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                                 DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        if (vurdering == null) {
            return null;
        }
        return switch (årsak) {
            case INNLEGGELSE_ANNEN_FORELDER -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_ANNEN_FORELDER_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT;
            };
            case SYKDOM_ANNEN_FORELDER -> switch (vurdering) {
                case GODKJENT -> SYKDOM_ANNEN_FORELDER_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT;
            };
            case BARE_SØKER_RETT -> switch (vurdering) {
                case GODKJENT -> BARE_SØKER_RETT_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> BARE_SØKER_RETT_IKKE_GODKJENT;
            };
            case ALENEOMSORG -> switch (vurdering) {
                case GODKJENT -> ALENEOMSORG_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> ALENEOMSORG_IKKE_GODKJENT;
            };
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };
    }

    @SuppressWarnings("DuplicatedCode")
    private static DokumentasjonVurdering mapUtsettelseVurdering(DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                                 DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        return switch (årsak) {
            case INNLEGGELSE_SØKER -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_SØKER_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_SØKER_IKKE_GODKJENT;
            };
            case INNLEGGELSE_BARN -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_BARN_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_BARN_IKKE_GODKJENT;
            };
            case HV_ØVELSE -> switch (vurdering) {
                case GODKJENT -> HV_OVELSE_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> HV_OVELSE_IKKE_GODKJENT;
            };
            case NAV_TILTAK -> switch (vurdering) {
                case GODKJENT -> NAV_TILTAK_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> NAV_TILTAK_IKKE_GODKJENT;
            };
            case SYKDOM_SØKER -> switch (vurdering) {
                case GODKJENT -> SYKDOM_SØKER_GODKJENT;
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> SYKDOM_SØKER_IKKE_GODKJENT;
            };
            case AKTIVITETSKRAV_INNLAGT, AKTIVITETSKRAV_IKKE_OPPGITT, AKTIVITETSKRAV_ARBEID_OG_UTDANNING, AKTIVITETSKRAV_ARBEID,
                AKTIVITETSKRAV_INTROPROG, AKTIVITETSKRAV_KVALPROG, AKTIVITETSKRAV_UTDANNING, AKTIVITETSKRAV_TRENGER_HJELP ->
                switch (vurdering) {
                    case GODKJENT -> MORS_AKTIVITET_GODKJENT;
                    case IKKE_GODKJENT -> MORS_AKTIVITET_IKKE_GODKJENT;
                    case IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
                };
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };
    }
}
