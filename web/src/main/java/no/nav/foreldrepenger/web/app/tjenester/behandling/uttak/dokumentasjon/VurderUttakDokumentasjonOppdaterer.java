package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.ALENEOMSORG_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.ALENEOMSORG_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.BARE_SØKER_RETT_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.BARE_SØKER_RETT_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.HV_OVELSE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.HV_OVELSE_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_BARN_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_BARN_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.NAV_TILTAK_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.NAV_TILTAK_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
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

        var ref = param.getRef();
        var behandlingId = ref.behandlingId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);

        var gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeFordeling().getPerioder();

        var vurderingTimeline = new LocalDateTimeline<>(
            dto.getVurderingBehov().stream().map(vb -> new LocalDateSegment<>(vb.fom(), vb.tom(), vb)).toList());
        var yfTimeline = new LocalDateTimeline<>(gjeldendePerioder.stream().map(op -> new LocalDateSegment<>(op.getFom(), op.getTom(), op)).toList());

        if (!vurderingTimeline.disjoint(yfTimeline).isEmpty()) {
            throw new IllegalArgumentException("Vurdering " + vurderingTimeline + " ligger utenfor yf perioder " + yfTimeline);
        }

        var nyFordeling = yfTimeline.combine(vurderingTimeline, (datoInterval, oppgittPeriode, vurdering) -> {
            var nyPeriode = OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode.getValue())
                .medPeriode(datoInterval.getFomDato(), datoInterval.getTomDato());
            if (vurdering != null && vurdering.getValue().vurdering() != null) {
                nyPeriode.medDokumentasjonVurdering(mapVurdering(vurdering.getValue()));
            }
            return new LocalDateSegment<>(datoInterval, nyPeriode.build());
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN).toSegments().stream().map(LocalDateSegment::getValue).toList();

        ytelseFordelingTjeneste.overstyrSøknadsperioder(behandlingId, nyFordeling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(ref, dto.getBegrunnelse(), gjeldendePerioder, nyFordeling);
        return OppdateringResultat.utenTransisjon().medBeholdAksjonspunktÅpent(!harLøstAksjonspunktet(param)).build();
    }

    private boolean harLøstAksjonspunktet(AksjonspunktOppdaterParameter param) {
        var input = uttakInputTjeneste.lagInput(param.getBehandlingId());
        return !utleder.utledAksjonspunktFor(input);
    }

    static DokumentasjonVurdering mapVurdering(DokumentasjonVurderingBehovDto dto) {
        if (dto.vurdering() == DokumentasjonVurderingBehovDto.Vurdering.GODKJENT_AUTOMATISK) {
            return null;
        }
        var type = switch (dto.type()) {
            case UTSETTELSE -> mapUtsettelseVurdering(dto.årsak(), dto.vurdering());
            case UTTAK -> mapUttak(dto.årsak(), dto.vurdering());
            case OVERFØRING -> mapOverføringVurdering(dto.årsak(), dto.vurdering());
        };
        return new DokumentasjonVurdering(type, dto.morsStillingsprosent());
    }

    private static DokumentasjonVurdering.Type mapUttak(DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                        DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        return switch (årsak) {
            case AKTIVITETSKRAV_INNLAGT, AKTIVITETSKRAV_IKKE_OPPGITT, AKTIVITETSKRAV_ARBEID_OG_UTDANNING, AKTIVITETSKRAV_ARBEID,
                 AKTIVITETSKRAV_INTROPROG, AKTIVITETSKRAV_KVALPROG, AKTIVITETSKRAV_UTDANNING, AKTIVITETSKRAV_TRENGER_HJELP -> switch (vurdering) {
                case GODKJENT -> MORS_AKTIVITET_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT -> MORS_AKTIVITET_IKKE_GODKJENT;
                case IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
            };
            case TIDLIG_OPPSTART_FAR -> switch (vurdering) {
                case GODKJENT -> TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;
            };
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
        };
    }

    @SuppressWarnings("DuplicatedCode")
    private static DokumentasjonVurdering.Type mapOverføringVurdering(DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                                      DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        if (vurdering == null) {
            return null;
        }
        return switch (årsak) {
            case INNLEGGELSE_ANNEN_FORELDER -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_ANNEN_FORELDER_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT;
            };
            case SYKDOM_ANNEN_FORELDER -> switch (vurdering) {
                case GODKJENT -> SYKDOM_ANNEN_FORELDER_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT;
            };
            case BARE_SØKER_RETT -> switch (vurdering) {
                case GODKJENT -> BARE_SØKER_RETT_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> BARE_SØKER_RETT_IKKE_GODKJENT;
            };
            case ALENEOMSORG -> switch (vurdering) {
                case GODKJENT -> ALENEOMSORG_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> ALENEOMSORG_IKKE_GODKJENT;
            };
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
        };
    }

    private static IllegalStateException godkjentAutomatiskException() {
        return new IllegalStateException("GODKJENT_AUTOMATISK skal ikke lagres");
    }

    @SuppressWarnings("DuplicatedCode")
    private static DokumentasjonVurdering.Type mapUtsettelseVurdering(DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                                      DokumentasjonVurderingBehovDto.Vurdering vurdering) {
        return switch (årsak) {
            case INNLEGGELSE_SØKER -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_SØKER_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_SØKER_IKKE_GODKJENT;
            };
            case INNLEGGELSE_BARN -> switch (vurdering) {
                case GODKJENT -> INNLEGGELSE_BARN_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> INNLEGGELSE_BARN_IKKE_GODKJENT;
            };
            case HV_ØVELSE -> switch (vurdering) {
                case GODKJENT -> HV_OVELSE_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> HV_OVELSE_IKKE_GODKJENT;
            };
            case NAV_TILTAK -> switch (vurdering) {
                case GODKJENT -> NAV_TILTAK_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> NAV_TILTAK_IKKE_GODKJENT;
            };
            case SYKDOM_SØKER -> switch (vurdering) {
                case GODKJENT -> SYKDOM_SØKER_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT, IKKE_DOKUMENTERT -> SYKDOM_SØKER_IKKE_GODKJENT;
            };
            case AKTIVITETSKRAV_INNLAGT, AKTIVITETSKRAV_IKKE_OPPGITT, AKTIVITETSKRAV_ARBEID_OG_UTDANNING, AKTIVITETSKRAV_ARBEID,
                 AKTIVITETSKRAV_INTROPROG, AKTIVITETSKRAV_KVALPROG, AKTIVITETSKRAV_UTDANNING, AKTIVITETSKRAV_TRENGER_HJELP -> switch (vurdering) {
                case GODKJENT -> MORS_AKTIVITET_GODKJENT;
                case GODKJENT_AUTOMATISK -> throw godkjentAutomatiskException();
                case IKKE_GODKJENT -> MORS_AKTIVITET_IKKE_GODKJENT;
                case IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
            };
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
        };
    }
}
